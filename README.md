# NetTrace

**NetTrace** is an ultra-lightweight, native Android data usage tracker built with Kotlin and Jetpack Compose. It strictly adheres to a quiet dark, Apple-like design philosophy, runs 100% offline with zero Internet permissions, and maintains an APK footprint under 3 MB.

---

## Key Highlights

- **Ultra-Small APK (<3 MB)**: Zero third-party chart, image, networking, or analytics dependencies. Custom Canvas graphics and 10 bespoke vector icons keep the binary lean.
- **Strictly Offline**: Contains no `android.permission.INTERNET`. All queries, calculations, and data logs stay purely on-device.
- **Apple-like Quiet Dark Philosophy**: True-black background (`#000000`), layered dark surfaces (`#0F0F12`, `#1A1A1F`), 8% white hairline borders, and responsive spring physics with tactile haptic feedback.
- **Hardware-Accurate Telemetry**: Uses `NetworkStatsManager` (`querySummaryForDevice` and `querySummary` per UID) to measure Wi-Fi and cellular traffic directly from kernel network counters.
- **Dual-SIM Delta Sampling**: Avoids deprecated/blocked identifiers (`subscriberId`) on Android 10+. Employs an hourly low-battery WorkManager job that diffs mobile traffic and credits deltas to the active data subscription ID (`getDefaultDataSubscriptionId`), keeping a 60-day pruned DataStore log.
- **Pacing Forecast & Alerts**: Computes whether current daily consumption will breach plan limits before the billing cycle ends, with optional local notifications on Android 13+.
- **Zero-Permission CSV Export**: Uses `ACTION_CREATE_DOCUMENT` to export detailed 30-day and per-app usage spreadsheets without requiring storage permissions.

---

## Release & Size Optimizations

NetTrace employs every modern Android compiler and packaging optimization:

1. **R8 Full Mode (`android.enableR8.fullMode=true`)**:
   - Enables multi-pass global whole-program optimization and aggressive dead-code elimination.
   - Merges single-implementation interfaces and flattens package hierarchy (`-repackageclasses ''`).
2. **Resource Shrinking (`isShrinkResources = true`)**:
   - Analyzes resource usage in code and removes unused drawables, layouts, and strings.
3. **Log & Trace Stripping**:
   - Strips all `android.util.Log` and `System.out.println` calls from bytecode via ProGuard rules, removing string constants from DEX files.
4. **Metadata & File Stripping**:
   - Strips `META-INF/*.version`, `META-INF/LICENSE*`, `META-INF/NOTICE*`, `INDEX.LIST`, and debug probe files.
5. **No Bulky Fonts or Icons**:
   - Uses system font families and vector-drawn Compose paths instead of multi-megabyte icon font bundles or external typography packs.

---

## Tech Stack & Architecture

- **Language**: Kotlin 2.2+
- **UI Toolkit**: Jetpack Compose & Material 3
- **Minimum SDK**: Android 10 (API level 29)
- **Target SDK**: Android 15 (API level 35) / Compile SDK 36
- **Architecture**: Single-Activity MVVM with Flow & Coroutines
- **Dependencies**:
  - `androidx.compose.*` (Material 3, Foundation, UI)
  - `androidx.work:work-runtime-ktx` (Hourly background sampling)
  - `androidx.datastore:datastore-preferences` (Compact settings & 60-day SIM log)

---

## Permissions

| Permission | Protection | Purpose |
| :--- | :--- | :--- |
| `android.permission.PACKAGE_USAGE_STATS` | Special AppOps | Required to query `NetworkStatsManager` for device and per-UID traffic. Requested via Settings onboarding. |
| `android.permission.POST_NOTIFICATIONS` | Runtime (API 33+) | Sends threshold alerts when cycle data usage reaches target percentage (default: 80%). |
| `android.permission.READ_PHONE_STATE` | Optional | Displays carrier/SIM names if granted. The app functions completely without this permission. |
| `android.permission.INTERNET` | **NONE** | **Omitted intentionally**. NetTrace cannot connect to any network. |

---

## Build & Assembly Instructions

### Prerequisites
- Java Development Kit (JDK 17 or JDK 21)
- Android SDK Platform 35 / 36

### 1. Build Optimized Release APK (R8 Full Mode)
```bash
./gradlew assembleRelease
```
The optimized release APK will be generated at:
```text
app/build/outputs/apk/release/app-release.apk
```

### 2. Build Release App Bundle (AAB for Google Play)
```bash
./gradlew bundleRelease
```
Output:
```text
app/build/outputs/bundle/release/app-release.aab
```

### 3. Build Debug APK
```bash
./gradlew assembleDebug
```
Output:
```text
app/build/outputs/apk/debug/app-debug.apk
```

### 4. Install on Device
```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

---

## CI / CD Workflows

NetTrace includes two GitHub Actions workflows in `.github/workflows/`:

- **`build.yml`**: Triggers on pull requests and pushes to `main` to build and upload the debug APK.
- **`release.yml`**: Triggers on version tags (`v*`), GitHub release publications, or manual dispatch. It executes full R8 optimizations, builds both the **Release APK** and **Release App Bundle (AAB)**, displays artifact sizes, and automatically attaches the binaries to the GitHub Release.
