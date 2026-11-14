# Hisabh — Personal Expense Tracker (Android)

<p align="center">
  <img src="app/src/main/res/drawable/ic_launcher_foreground.xml" width="100" height="100" alt="Hisabh Logo" />
</p>

<p align="center">
  <strong>An offline-first, privacy-centric personal expense tracker for Android.</strong><br>
  Automatically logs and categorizes HDFC Bank SMS (including PhonePe, Google Pay, and Paytm UPI) with zero cloud upload.
</p>

<p align="center">
  <a href="https://github.com/YugSurana/hisabh/releases/latest/download/app-debug.apk">
    <img src="https://img.shields.io/badge/Download-APK%20(v1.0.0)-163827?style=for-the-badge&logo=android&logoColor=white" alt="Download APK" />
  </a>
  <img src="https://img.shields.io/badge/Platform-Android%208.0%2B-blue?style=for-the-badge&logo=android" alt="Android 8.0+" />
  <img src="https://img.shields.io/badge/Privacy-100%25%20Offline-success?style=for-the-badge&logo=shield" alt="100% Offline" />
  <img src="https://img.shields.io/badge/Language-Kotlin-purple?style=for-the-badge&logo=kotlin" alt="Kotlin" />
</p>

---

## 📥 Download the App

You can install Hisabh directly onto your Android phone:

### 🔗 Direct APK Download:
* 📦 **[Download Hisabh APK (v1.0.0)](https://github.com/YugSurana/hisabh/releases/latest/download/app-debug.apk)** *(Ready to install)*

> **Tip:** You can also browse all versions directly from the **[GitHub Releases](https://github.com/YugSurana/hisabh/releases)** page.

### 📲 How to Install on Your Android Phone:
1. Tap the **[Download Hisabh APK](https://github.com/YugSurana/hisabh/releases/latest/download/app-debug.apk)** link from your phone's browser.
2. When the download finishes, tap **Open**.
3. If Android prompts *"For your security, your phone is not allowed to install unknown apps from this source"*, tap **Settings** and toggle **"Allow from this source"**.
4. Tap **Install**, then tap **Open**!
5. On the first launch, tap **"Enable Auto-Tracking"** and grant SMS permissions so Hisabh can detect your transactions.

---

## 🎨 Minimalist Design System
The app features an ultra-minimalist, typographic aesthetic with a high-contrast palette:
* **Background Surface**: Warm Off-White / Cream (`#FAF8F5`)
* **Card Containers**: Pure Ivory (`#FFFFFF`) & Soft Linen (`#F2EFE8`) with hairline sand borders (`#EAE5DC`)
* **Primary Brand & Buttons**: Deep Dark Forest Green (`#163827`)
* **Debits (Expenses)**: Refined Terracotta (`#B43B33`)
* **Credits (Income)**: Deep Pine Green (`#1E6845`)

---

## 🚀 Key Features

1. **Automated HDFC Bank SMS Parsing Engine**:
   * Registers `HdfcSmsReceiver` with high priority (`999`) for `android.provider.Telephony.SMS_RECEIVED`.
   * Filters strictly for official Telecom Regulatory DLT sender headers (`AD-HDFCBK-T`, `AD-HDFCBN-S`, `VM-HDFCBK-T`, or regex `^[A-Z]{2}-HDFC[A-Z0-9]{2}-[TS]$`).
   * Ignores non-transactional messages containing OTPs, failed, declined, requested, insufficient balance, or mandates.
   * Parses standard account debits, UPI transfers (PhonePe `@ybl`, GPay `@okaxis`), credits, and direct deposits.
   * Extracts amount, merchant, date, and 12-digit UPI reference number (falling back to a deterministic SHA-256 hash when absent).

2. **Deduplication & Local Persistence (Room DB)**:
   * `TransactionEntity` enforces `@Index(value = ["utrNumber"], unique = true)` with `OnConflictStrategy.IGNORE`, guaranteeing zero double-counting.
   * Stored locally in `/data/user/0/com.hisabh.app/databases/hisabh_database.db`.
   * **100% Offline**: Zero network permissions declared in `AndroidManifest.xml`. Your financial data never leaves your device.

3. **Smart Auto-Categorization & Merchant Rules**:
   * Auto-learns from user changes: checking *"Apply to all future payments from this merchant"* saves rules to `MerchantRuleEntity` and retroactively updates past records.
   * 9 Default Categories: *Food & Dining*, *Travel & Fuel*, *Groceries*, *Bills & Recharge*, *Shopping*, *Entertainment*, *Transfer & Friends*, *Pocket Money / Received*, and *Other*.

4. **Interactive Quick-Tagging Notification**:
   * Dispatches a high-priority Android notification on every new debit:
     `"Spent ₹[Amount] at [Merchant] — Tap to tag category or add note."`
   * Tapping directly deep-links into the Edit Bottom Sheet for that transaction.

5. **Monthly Statements & Native Sharing**:
   * **RFC 4180 CSV**: Clean spreadsheet export.
   * **Native Canvas PDF**: Formatted statement featuring headers, summary cards, category percentage breakdown, and itemized ledger table generated via `android.graphics.pdf.PdfDocument`.
   * Native Android Share Sheet via `FileProvider` (`content://...`) for sharing directly to WhatsApp, Drive, or Email.

6. **System Health & Battery Optimization Banner**:
   * Detects missing SMS permissions or aggressive OEM battery optimizations and provides one-tap whitelisting.

---

## 📂 Project Architecture

```
hisabh/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── res/
│       │   │   ├── drawable/ (ic_launcher_background.xml, ic_launcher_foreground.xml)
│       │   │   ├── mipmap-anydpi-v26/ (ic_launcher.xml, ic_launcher_round.xml)
│       │   │   ├── values/ (colors.xml, strings.xml, themes.xml)
│       │   │   └── xml/ (file_paths.xml, backup_rules.xml, data_extraction_rules.xml)
│       │   └── java/com/hisabh/app/
│       │       ├── HisabhApplication.kt
│       │       ├── MainActivity.kt
│       │       ├── data/
│       │       │   ├── local/ (HisabhDatabase, TransactionDao, MerchantRuleDao)
│       │       │   ├── entity/ (TransactionEntity, MerchantRuleEntity)
│       │       │   ├── model/ (Category, TransactionType)
│       │       │   └── repository/ (TransactionRepository)
│       │       ├── domain/
│       │       │   ├── export/ (CsvExporter, PdfStatementExporter, ExportManager)
│       │       │   ├── helper/ (SmsReaderHelper, NotificationHelper)
│       │       │   └── parser/ (HdfcSmsParser, ParsedTransaction)
│       │       ├── receiver/ (HdfcSmsReceiver)
│       │       ├── ui/
│       │       │   ├── screens/ (DashboardScreen, components, permissions)
│       │       │   ├── theme/ (Color.kt, Theme.kt, Type.kt)
│       │       │   └── viewmodel/ (HisabhViewModel, HisabhUiState, HisabhViewModelFactory)
│       │       └── util/ (CurrencyFormatter, DateUtils)
│       └── test/
│           └── java/com/hisabh/app/
│               └── HdfcSmsParserTest.kt
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/ (gradle-wrapper.jar, gradle-wrapper.properties)
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew & gradlew.bat
├── README.md
├── TECH_STACK.md
└── APP_SPECIFICATION.md
```

---

## 🛠️ How to Build from Source (Android Studio)

1. Clone this repository:
   ```bash
   git clone https://github.com/YugSurana/hisabh.git
   ```
2. Open **Android Studio** (Hedgehog or Ladybug).
3. Select **File > Open** and select the `hisabh` folder.
4. Let Gradle sync complete.
5. Click **Run ▶** (or press Shift + F10) to run on an emulator or physical phone.

### Building the Release APK via CLI:
```bash
./gradlew assembleRelease
```
The APK will be generated at `app/build/outputs/apk/release/app-release-unsigned.apk`.

---

## 🧪 Running Unit Tests
To verify the SMS parsing logic across all variations, run:
```bash
./gradlew testDebugUnitTest
```
Tests cover:
* Sender validation for DLT headers (`AD-HDFCBK-T`, `VM-HDFCBK-T`, `BZ-HDFCBN-S`).
* Exclusion filtering for OTPs, failed transactions, and mandates.
* UPI debits (`zomato@okaxis`, `ramesh.tea@ybl`).
* Account debits (`Sent Rs.150.00 from HDFC Bank A/C x1234 To CHAI TAPRI...`).
* Received & Credited transactions.
* SHA-256 fallback hash generation when UTR is absent.

---

## 📄 License
This project is open-source under the [MIT License](LICENSE).
