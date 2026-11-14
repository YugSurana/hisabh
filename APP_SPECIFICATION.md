# Hisabh — Personal Expense Tracker (Android)
## Comprehensive Architecture & Technical Specification Document

---

### 1. Executive Summary & Objectives
**Hisabh** is an offline-first, privacy-centric personal expense tracker engineered for Android (Minimum SDK 26, Target SDK 34) using **Kotlin**, **Jetpack Compose (Material 3)**, and **Room Database**. 

The application solves the friction of manual expense tracking by automatically intercepting, parsing, and categorizing transactional SMS sent by **HDFC Bank** (India's largest private bank), while also supporting manual cash logging, smart categorization rules, instant interactive notifications, and instant monthly CSV/PDF export.

---

### 2. High-Level Architecture Overview

Hisabh follows Google's recommended **Clean Architecture** and **Modern Android Architecture (MVI / Unidirectional Data Flow with MVVM)**:

```
┌────────────────────────────────────────────────────────────────────────┐
│                               UI LAYER                                 │
│  - Jetpack Compose (Material 3)                                       │
│  - MainActivity & Navigation State                                     │
│  - Screens: DashboardScreen, EditTransactionSheet, AddSheet, Export    │
│  - ViewModels: HisabhViewModel (StateFlow<HisabhUiState>)              │
└────────────────────────────────────▲───────────────────────────────────┘
                                     │ StateFlow / User Events
┌────────────────────────────────────▼───────────────────────────────────┐
│                              DOMAIN LAYER                              │
│  - HdfcSmsParser (Regex Extraction Engine, SHA-256 UTR Fallback)       │
│  - SmsReaderHelper (ContentResolver Historical Inbox Importer)         │
│  - ExportManager (CSV Exporter & Android PdfDocument Canvas Generator) │
│  - NotificationHelper (Android O+ Channels, PendingIntents)            │
└────────────────────────────────────▲───────────────────────────────────┘
                                     │ Repository Pattern
┌────────────────────────────────────▼───────────────────────────────────┐
│                               DATA LAYER                               │
│  - TransactionRepository                                               │
│  - Room Database (HisabhDatabase v1)                                   │
│    ├── TransactionEntity (@Index(unique = true, ["utrNumber"]))        │
│    └── MerchantRuleEntity (merchantRaw -> category, displayName)       │
│  - DAOs: TransactionDao, MerchantRuleDao                               │
└────────────────────────────────────▲───────────────────────────────────┘
                                     │ SMS Broadcasts
┌────────────────────────────────────┴───────────────────────────────────┐
│                            HARDWARE & OS                               │
│  - Telephony.Sms.Intents.SMS_RECEIVED_ACTION (HdfcSmsReceiver)         │
│  - ContentResolver (content://sms/inbox)                               │
│  - Android NotificationManager (Channel: hisabh_transactions)          │
│  - PowerManager (Battery Optimization Whitelist Intent)                │
└────────────────────────────────────────────────────────────────────────┘
```

---

### 3. Detailed Component Breakdown & Functionalities

#### 3.1. Automated HDFC Bank SMS Parsing Engine
* **BroadcastReceiver (`HdfcSmsReceiver`)**:
  * Registers with high priority (`android:priority="999"`) for `android.provider.Telephony.SMS_RECEIVED`.
  * **DLT Sender Filtering**: Validates incoming sender against regex `^[A-Z]{2}-HDFC[A-Z0-9]{2}-[TS]$` as well as strict headers (`AD-HDFCBK-T`, `AD-HDFCBN-S`, `VM-HDFCBK`, etc.).
  * **Blacklist Filtering**: Drops non-transactional messages containing `OTP`, `failed`, `declined`, `requested`, `insufficient`, `mandate`, or `code` (case-insensitive).
  * Asynchronously parses matching payloads and commits to Room via `CoroutineScope(SupervisorJob() + Dispatchers.IO)`.
* **Regex Engine (`HdfcSmsParser`)**:
  * **Debit Format 1 (Standard Account/UPI Debit)**:
    `"Sent Rs.150.00 from HDFC Bank A/C x1234 To CHAI TAPRI On 12/11/26 Ref 631894021123"`
  * **Debit Format 2 (VPA UPI Debit)**:
    `"Rs. 240.00 debited from a/c *1234 on 12-11-26 to VPA zomato@okaxis (UPI Ref No 631894021123)"`
  * **Credit Format 1 (VPA UPI Credit)**:
    `"Rs. 500.00 credited to a/c *1234 on 12-11-26 by VPA sender@okicici (UPI Ref No 631894021123)"`
  * **Credit Format 2 (Account Received)**:
    `"Received Rs.500.00 in your a/c x1234 on 12/11/26 from John Doe Ref 631894021123"`
  * **Extraction Fields**:
    * `amount`: Parsed as `Double`.
    * `txType`: `TransactionType.DEBIT` or `TransactionType.CREDIT`.
    * `merchantRaw`: Extracted from "To ...", "to VPA ...", "by VPA ...", or "from ...", stripped of noise prefixes like "VPA ", and normalized.
    * `utrNumber`: 12-digit UPI reference number. If absent, a deterministic **SHA-256 hash** of `amount + timestamp + rawSms` is computed to ensure database uniqueness.
    * `timestamp`: Extracted from date tokens (`dd/MM/yy` or `dd-MM-yy`), falling back to SMS device timestamp.
* **Historical Inbox Scanner (`SmsReaderHelper`)**:
  * Reads `Telephony.Sms.CONTENT_URI` (`content://sms/inbox`) for the current month or specified timestamp window.
  * Filters HDFC senders, parses valid transactions, and runs batch `insertTransactions(list)` with `OnConflictStrategy.IGNORE` so duplicates are seamlessly skipped.

---

#### 3.2. Local Storage & Smart Auto-Categorization (Room DB)
* **Entities**:
  1. `TransactionEntity`:
     * `id: Long` (Primary Key, autoGenerate = true)
     * `utrNumber: String` (Unique Index `@Index(value = ["utrNumber"], unique = true)`)
     * `amount: Double`
     * `txType: TransactionType` (DEBIT, CREDIT)
     * `merchantRaw: String`
     * `displayName: String`
     * `category: String`
     * `note: String?`
     * `timestamp: Long`
     * `isManual: Boolean`
  2. `MerchantRuleEntity`:
     * `merchantRaw: String` (Primary Key)
     * `assignedCategory: String`
     * `customDisplayName: String`
* **Smart Auto-Categorization Pipeline**:
  * Step 1: Query `MerchantRuleEntity` for `merchantRaw`.
  * Step 2: If rule found, assign `assignedCategory` and `customDisplayName`.
  * Step 3: If no rule, run heuristic keyword matcher:
    * *Food & Dining*: `swiggy`, `zomato`, `chai`, `tea`, `cafe`, `restaurant`, `mcdonald`, `burger`, `starbucks`, `eat`
    * *Groceries*: `blinkit`, `zepto`, `instamart`, `dmart`, `supermarket`, `grocery`, `bigbasket`, `nature basket`
    * *Travel & Fuel*: `uber`, `ola`, `rapido`, `petrol`, `fuel`, `hpcl`, `bpcl`, `ioc`, `irctc`, `metro`
    * *Bills & Recharge*: `airtel`, `jio`, `vi`, `bescom`, `electricity`, `broadband`, `bill`, `recharge`, `tatasky`
    * *Shopping*: `amazon`, `flipkart`, `myntra`, `ajio`, `zara`, `retail`
    * *Entertainment*: `bookmyshow`, `pvr`, `netflix`, `spotify`, `prime`
    * *Transfer & Friends*: `upi`, `transfer`, `okaxis`, `okhdfcbank`, `okicici`, `paytm`
    * *Pocket Money / Received*: Default for incoming CREDIT transactions
    * *Other*: Unmatched fallback
  * Step 4: When user updates category/name in the UI and checks *"Apply to all future payments from this merchant"*, upsert the rule in `MerchantRuleEntity` and retroactively update all historical transactions matching `merchantRaw`.

---

#### 3.3. Interactive Quick-Tagging Notification
* **Notification Channel**: `hisabh_transactions` (Importance: `IMPORTANCE_HIGH`, lights, vibration enabled).
* **Notification Payload**:
  * Title: `New Expense Tracked`
  * Body: `"Spent ₹[Amount] at [Merchant] — Tap to tag category or add note."`
  * Action: Clicking launches `MainActivity` with deep link extra `EXTRA_EDIT_TRANSACTION_ID = transactionId`.
* **Runtime Permission Handling**: Checks `Manifest.permission.POST_NOTIFICATIONS` on API 33+ (Android 13+) before dispatching.

---

#### 3.4. User Interface & Experience (Minimalist Jetpack Compose Material 3)
* **Design Philosophy**: Ultra-minimalist, typographic clarity, generous breathing room, and zero clutter.
* **Color Palette (Off-White & Deep Forest Green)**:
  * **Base Background**: Warm Luxury Off-White / Cream (`#FAF8F5`)
  * **Elevated Surfaces / Cards**: Pure Ivory (`#FFFFFF`) & Soft Cream (`#F2EFE8`) with subtle hairline sand borders (`#EAE5DC`)
  * **Primary Brand & Buttons**: Deep Dark Forest Green (`#163827` / `#1B3B2B`)
  * **Primary Typography**: Deep Spruce / Midnight Forest (`#122118`)
  * **Muted Labels & Timestamps**: Soft Sage / Moss (`#5A6E62` / `#6B7C72`)
  * **Debit (Expense) Text**: Tasteful Refined Terracotta (`#B43B33`)
  * **Credit (Income) Text**: Deep Pine Green (`#1E6845`)
  * **Category Badges**: Soft Pale Sage (`#EAF2ED`) with Dark Forest text (`#163827`)
1. **System Health & Permission Banner**:
   * Displays persistent warning banner if `READ_SMS` or `RECEIVE_SMS` permissions are missing.
   * Checks `PowerManager.isIgnoringBatteryOptimizations` and displays a direct shortcut to disable battery restrictions so `HdfcSmsReceiver` is never killed by OEM battery killers (MIUI, OxygenOS, OneUI).
2. **Dashboard / Home**:
   * **Month Selector**: `< November 2026 >` with quick jump.
   * **Summary Metric Cards**:
     * Total Spent (Debit) in Indian Currency (`₹`, `Locale("en", "IN")`).
     * Total Received (Credit) in Indian Currency.
     * Net Balance (`Total Received - Total Spent`).
   * **Category Spending Breakdown**: Donut / progress visualizer showing percentage and amount per category.
   * **Chronological Transaction Stream**:
     * Date headers (`Today`, `Yesterday`, `dd MMM yyyy`).
     * Category avatar icon with colored badge.
     * Merchant display name, time, and category pill.
     * Amount badge: Crimson Red for DEBIT, Emerald Green for CREDIT.
   * **FAB (+)**: Quick manual transaction entry modal.
   * **Action Bar**: "Scan SMS" importer and "Export" statement modal.
3. **Edit / Detail Bottom Sheet**:
   * Clean Material 3 ModalBottomSheet.
   * Edit Display Name (e.g. rename cryptic `PAYTM-UPI-MERCHANT-84920` to `Sharma Groceries`).
   * Category Picker: Horizontally scrollable FlowRow of category chips.
   * Note input field.
   * Checkbox / Switch: *"Apply to all future payments from this merchant"*.
   * Delete button with confirmation dialog.
4. **Manual Add Expense Bottom Sheet**:
   * Mode switch: Expense (Debit) vs Income (Credit).
   * Amount field with Rupee symbol prefix.
   * Merchant / Person name.
   * Category selector chips.
   * Date & Time picker.
   * Optional note.
5. **Export Modal (CSV & PDF)**:
   * Month selection.
   * **CSV Engine**: Generates RFC 4180 compliant CSV: `Date, Time, Type, Merchant, Category, Amount, UTR, Note`.
   * **PDF Engine**: Uses native Android `android.graphics.pdf.PdfDocument`:
     * Custom Canvas rendering (no heavy 3rd-party dependencies like iText).
     * Header with Hisabh branding, Month title, and generation timestamp.
     * Summary Cards (Debits, Credits, Net).
     * Category Breakdown summary table.
     * Paginated itemized transaction table with alternating row striping.
   * Native Android Share Sheet via `FileProvider` (`content://...`) for direct WhatsApp / Drive / Email sharing.

---

### 4. Technical Specifications & Dependencies

| Tool / Library | Version / Specification | Purpose |
|---|---|---|
| **Language** | Kotlin 1.9.23 / 2.0.0 | Primary modern Android language |
| **Min SDK** | 26 (Android 8.0 Oreo) | Notification channels, modern Java Time APIs |
| **Target / Compile SDK** | 34 (Android 14) | Latest Android platform APIs & security models |
| **UI Toolkit** | Jetpack Compose + Material 3 (1.2.x+) | Declarative modern UI |
| **Database** | Room 2.6.1 + KSP | SQLite ORM with reactive Flow support |
| **Concurrency** | Kotlin Coroutines & StateFlow | Async execution & Unidirectional Data Flow |
| **PDF Generation** | Android `PdfDocument` Canvas | Lightweight, zero-dependency printable PDF reports |
| **File Sharing** | AndroidX `FileProvider` | Secure content URI provider for PDF/CSV |
| **Unit Testing** | JUnit 4 + Google Truth / Kotlin Test | Comprehensive SMS regex parser test suite |

---

### 5. Project Directory & Package Structure

```
hisabh/
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── gradle/libs.versions.toml
├── build.gradle.kts
├── settings.gradle.kts
└── app/
    ├── build.gradle.kts
    ├── src/
    │   ├── main/
    │   │   ├── AndroidManifest.xml
    │   │   ├── res/
    │   │   │   ├── drawable/
    │   │   │   ├── mipmap-.../
    │   │   │   ├── values/
    │   │   │   │   ├── colors.xml
    │   │   │   │   ├── strings.xml
    │   │   │   │   └── themes.xml
    │   │   │   └── xml/
    │   │   │       └── file_paths.xml
    │   │   └── java/com/hisabh/app/
    │   │       ├── HisabhApplication.kt
    │   │       ├── MainActivity.kt
    │   │       ├── data/
    │   │       │   ├── local/
    │   │       │   │   ├── HisabhDatabase.kt
    │   │       │   │   ├── dao/
    │   │       │   │   │   ├── TransactionDao.kt
    │   │       │   │   │   └── MerchantRuleDao.kt
    │   │       │   │   └── entity/
    │   │       │   │       ├── TransactionEntity.kt
    │   │       │   │       └── MerchantRuleEntity.kt
    │   │       │   ├── model/
    │   │       │   │   ├── Category.kt
    │   │       │   │   └── TransactionType.kt
    │   │       │   └── repository/
    │   │       │       └── TransactionRepository.kt
    │   │       ├── domain/
    │   │       │   ├── parser/
    │   │       │   │   ├── HdfcSmsParser.kt
    │   │       │   │   └── ParsedTransaction.kt
    │   │       │   ├── export/
    │   │       │   │   ├── CsvExporter.kt
    │   │       │   │   └── PdfStatementExporter.kt
    │   │       │   └── helper/
    │   │       │       ├── SmsReaderHelper.kt
    │   │       │       └── NotificationHelper.kt
    │   │       ├── receiver/
    │   │       │   └── HdfcSmsReceiver.kt
    │   │       ├── ui/
    │   │       │   ├── theme/
    │   │       │   │   ├── Color.kt
    │   │       │   │   ├── Theme.kt
    │   │       │   │   └── Type.kt
    │   │       │   ├── viewmodel/
    │   │       │   │   ├── HisabhViewModel.kt
    │   │       │   │   └── HisabhUiState.kt
    │   │       │   └── screens/
    │   │       │       ├── DashboardScreen.kt
    │   │       │       ├── components/
    │   │       │       │   ├── MonthSelector.kt
    │   │       │       │   ├── SummaryCards.kt
    │   │       │       │   ├── CategoryBreakdownChart.kt
    │   │       │       │   ├── TransactionListItem.kt
    │   │       │       │   ├── HealthBanner.kt
    │   │       │       │   ├── EditTransactionSheet.kt
    │   │       │       │   ├── AddTransactionSheet.kt
    │   │       │       │   └── ExportDialog.kt
    │   │       │       └── permissions/
    │   │       │           └── OnboardingPermissionScreen.kt
    │   │       └── util/
    │   │           ├── CurrencyFormatter.kt
    │   │           └── DateUtils.kt
    │   └── test/
    │       └── java/com/hisabh/app/
    │           └── HdfcSmsParserTest.kt
```

---

### 6. Edge Case & Robustness Strategy
1. **SMS Sender Spoofing & Variations**:
   * Uses both regex pattern matching and exact header matching against HDFC Bank's Telecom Commercial Communications Customer Preference Regulations (TCCCPR) DLT header standards.
2. **Deduplication Guarantee**:
   * The Room unique index on `utrNumber` with `OnConflictStrategy.IGNORE` prevents double-logging if an SMS is received multiple times or rescanned from the inbox.
   * If an SMS lacks an official 12-digit UTR, a deterministic SHA-256 hash of `amount + date + rawText` is generated as the unique key.
3. **Background Delivery Reliability**:
   * The app checks OEM battery restrictions and assists the user in whitelisting Hisabh via `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.
4. **Data Privacy**:
   * 100% offline, zero network permissions required in `AndroidManifest.xml`. User financial data never leaves the device.

