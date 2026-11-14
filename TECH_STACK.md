# Hisabh — Technology Stack & Architecture Documentation

This document provides a comprehensive technical breakdown of the programming languages, architectural patterns, official Android Jetpack libraries, database engines, and UI design frameworks used in **Hisabh**.

---

## 1. Core Platform & Specifications

| Component | Technology | Specification / Target | Rationale |
|---|---|---|---|
| **Programming Language** | **Kotlin 1.9.23** | JVM Target 17 | Type-safe, expressive, modern official Android language with first-class Coroutines and Flow support. |
| **Minimum SDK** | **API 26 (Android 8.0 Oreo)** | `minSdk = 26` | Ensures support for native Android `NotificationChannel`, modern `java.time` APIs, and coverage of >96% of active Android devices. |
| **Target / Compile SDK** | **API 34 (Android 14 UpsideDownCake)** | `targetSdk = 34` | Targets the latest platform security standards, privacy sandboxing, and Android 13+ `POST_NOTIFICATIONS` runtime permissions. |
| **Build System** | **Gradle 8.7** + **Android Gradle Plugin 8.4.1** | Kotlin DSL (`build.gradle.kts`) | Type-safe build scripts with centralized dependency version catalogs (`libs.versions.toml`). |
| **Annotation Processor** | **KSP (Kotlin Symbol Processing) 1.9.23-1.0.20** | `com.google.devtools.ksp` | Up to 2x faster than legacy KAPT for compiling Room Database DAOs and entities. |

---

## 2. Architecture & Design Patterns

Hisabh adheres to Google's official **Modern Android Architecture (UDF / MVI + Clean MVVM)**:

```
┌────────────────────────────────────────────────────────────────────────┐
│                              PRESENTATION                              │
│  - Jetpack Compose (Material 3) Declarative UI                        │
│  - HisabhViewModel (StateFlow<HisabhUiState>)                          │
│  - Unidirectional Data Flow (Events Up ──► State Down)                 │
└───────────────────────────────────▲────────────────────────────────────┘
                                    │ Flow / Observables
┌───────────────────────────────────▼────────────────────────────────────┐
│                                 DOMAIN                                 │
│  - HdfcSmsParser (Regex Engine + SHA-256 Fallback Hasher)              │
│  - SmsReaderHelper (ContentResolver Historical Inbox Importer)         │
│  - NotificationHelper (Android O+ Channel & PendingIntents)            │
│  - ExportManager (RFC 4180 CSV & Native Android PdfDocument Canvas)    │
└───────────────────────────────────▲────────────────────────────────────┘
                                    │ Repository Pattern
┌───────────────────────────────────▼────────────────────────────────────┐
│                                  DATA                                  │
│  - TransactionRepository                                               │
│  - Room Database (SQLite with WAL & Durability)                        │
│    ├── TransactionEntity (@Index(unique = true, ["utrNumber"]))        │
│    └── MerchantRuleEntity (merchantRaw -> category, displayName)       │
│  - DAOs: TransactionDao, MerchantRuleDao                               │
└───────────────────────────────────▲────────────────────────────────────┘
                                    │ Android OS Broadcasts
┌───────────────────────────────────┴────────────────────────────────────┐
│                           SYSTEM & HARDWARE                            │
│  - Telephony.Sms.Intents.SMS_RECEIVED_ACTION (Priority 999)            │
│  - PowerManager (Battery Optimization Whitelisting)                    │
│  - FileProvider (Scoped Storage Share URIs for PDF/CSV)                │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Libraries & Dependencies Matrix

All dependency coordinates and version numbers are cataloged in `gradle/libs.versions.toml`:

### A. UI & Jetpack Compose
* **Compose BOM (`2024.05.00`)**: Enforces tested, stable binary compatibility across all Compose components.
* **Material 3 (`1.2.1`)**: Google's latest Material Design system (customized with our minimalist cream & deep forest green palette).
* **Activity Compose (`1.9.0`)**: Bridges traditional Android Activity lifecycle into `setContent { HisabhTheme { ... } }`.
* **Lifecycle ViewModel Compose (`2.8.0`)**: Scopes `HisabhViewModel` lifecycle to the Compose UI tree and observes `StateFlow`.
* **Material Icons Extended**: Vector icons for all 9 default spending categories (`Restaurant`, `DirectionsCar`, `ShoppingCart`, etc.).

### B. Persistence & Storage (Room Database)
* **Room Runtime & KTX (`2.6.1`)**:
  * Abstraction over SQLite with Kotlin Coroutines and reactive `Flow` support.
  * Unique indexes prevent duplicate transaction inserts (`OnConflictStrategy.IGNORE`).
* **KSP Compiler (`2.6.1`)**: Generates optimized SQLite bytecode at compile time.

### C. Concurrency & Asynchronous Workflows
* **Kotlin Coroutines (`1.8.1`)**:
  * `Dispatchers.IO`: Background thread pool for SMS parsing, SQLite database operations, and PDF generation.
  * `SupervisorJob`: Ensures failure in one async operation doesn't cancel the background receiver scope.
* **Kotlin StateFlow & Flow**: Reactive data stream connecting Room SQLite queries directly to the Compose UI with zero polling.

### D. Export & File Sharing
* **Android Native `PdfDocument` Canvas API**:
  * Built-in vector drawing engine creating crisp, printable vector PDFs without heavy third-party libraries (e.g. iText).
* **AndroidX `FileProvider`**:
  * Generates secure `content://com.hisabh.app.fileprovider/exports/...` URIs to share statements directly to WhatsApp, Google Drive, or Gmail.

### E. Unit Testing
* **JUnit 4 (`4.13.2`)**: Unit test runner for SMS regex parsing, DLT header validation, noise exclusion, and SHA-256 fallback hashing.
* **Coroutines Test (`1.8.1`)**: Fast virtual-time testing for async repository functions.

---

## 4. Security, Privacy & Network Model

* **Zero Network Permissions**:
  Hisabh does **not** declare `<uses-permission android:name="android.permission.INTERNET" />` in `AndroidManifest.xml`.
* **Air-Gapped Privacy**:
  Your SMS messages, bank reference numbers, bank accounts, and expense details **never leave your device**.
* **Sandboxed Database**:
  Stored in `/data/user/0/com.hisabh.app/databases/hisabh_database.db`, protected by Android OS application sandboxing.

---

## 5. Visual Theme Tokens

The app uses an ultra-minimalist aesthetic tailored to warm off-white (cream/linen) and dark forest green:

| Role | Hex Code | Visual Tone | Used For |
|---|---|---|---|
| **Base Surface** | `#FAF8F5` | Warm Off-White / Cream | Main app background, dialog backdrops |
| **Card Containers** | `#FFFFFF` / `#F2EFE8` | Pure Ivory & Soft Linen | Transaction cards, breakdown boxes |
| **Hairline Borders** | `#EAE5DC` | Warm Sand Line | 1dp crisp borders separating elements | 
| **Primary Brand** | `#163827` | Deep Dark Forest Green | Net Savings card, primary buttons, FAB, titles |
| **Primary Text** | `#122118` | Deep Midnight Spruce | Headings, amounts, high-contrast text |
| **Muted Metadata** | `#5A6E62` | Soft Earth Sage | Subtitles, timestamps, UTR reference tags |
| **Debits (Expense)** | `#B43B33` | Refined Terracotta | Expense amounts (`-₹340.00`) |
| **Credits (Income)** | `#1E6845` | Deep Pine Green | Received money (`+₹65,000.00`) |

