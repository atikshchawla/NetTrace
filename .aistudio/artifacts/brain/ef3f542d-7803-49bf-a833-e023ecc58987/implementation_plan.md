# NetTrace Feature Expansion & History Stepper Redesign

A comprehensive product and architectural plan to refine historical telemetry navigation and introduce four high-value native features: **Home Screen Data Glance Widget**, **Real-Time Live Bandwidth Meter**, **Daily Data Limit Warnings**, and **Per-App Data Limits**.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following decisions were confirmed by the user during the planning phase:
> - **Month Navigation Redesign**: Replace the horizontal pill bar with a clean **Previous/Next Arrow Stepper with Month Header** (`<  November 2026  >`).
> - **Core New Features Confirmed**:
>   1. **Home Screen Glance Widget**: Lightweight, native home screen widget showing cycle consumption, remaining data, and pace ring.
>   2. **Real-Time Bandwidth & Speed Meter**: Instantaneous upload/download throughput counter with pulse animation on the Home tab.
>   3. **Daily Data Limit Warnings**: Configurable daily mobile cap (e.g., 500 MB/day) with proactive overage alerts.
>   4. **Per-App Usage Limits & Alerts**: Custom data ceilings for high-traffic apps (e.g., YouTube, Instagram) alerting the user when an individual app exceeds its threshold.

---

## 1. Overview & Core Concept

NetTrace is an ultra-lightweight, 100% offline network telemetry utility for Android. This release expands NetTrace from a passive cycle tracker into an active, real-time data monitor and home-screen companion without sacrificing battery efficiency or privacy.

### Key Value Delivered
- **Effortless Month-to-Month Auditing**: Clean stepper navigation makes comparing past months natural and tactile.
- **Glanceable Home Screen Telemetry**: Check remaining cellular balance without opening the app.
- **Live Speed Telemetry**: Identify rogue apps consuming bandwidth in real time.
- **Micro-Budget Control**: Avoid burning a full monthly plan in a few days via daily budgets and per-app limits.

---

## 2. User Experience & Visual Design

### 2.1 History Month Stepper Redesign
- **Header Structure**: A centered, quiet surface card (`NetTraceColors.Surface`) featuring:
  - **Left Chevron Arrow (`<`)**: Haptic-enabled button to step back to previous months (up to 6 months back).
  - **Centered Month Label**: Bold 18sp title (e.g., `November 2026`) with sub-caption showing cycle dates (e.g., `Nov 1 – Nov 30 • 30 Days`).
  - **Right Chevron Arrow (`>`)**: Stepper button to advance towards current month (disabled with dimmed opacity when viewing the current month).
  - **"Current Month" Quick Pill**: Subtle badge that appears only when viewing historical months to instantly jump back with a single tap.
- **Transition Motion**: 250ms horizontal sliding animated content transition (`slideInHorizontally` + `fadeIn`) as months change.

### 2.2 Real-Time Live Bandwidth Meter (Home Screen)
- **Compact Live Telemetry Bar**: Positioned directly below the hero Plan Ring on the Home screen.
- **Download & Upload Indicators**:
  - Download Speed (`↓ 2.4 MB/s` in `NetTraceColors.Wifi` blue)
  - Upload Speed (`↑ 180 KB/s` in `NetTraceColors.Mobile` green)
- **Live Activity Pulse**: A gentle breathing radar dot indicating live network activity.
- **Sampling Strategy**: Lightweight 1-second delta sampling via `TrafficStats.getTotalRxBytes()` / `TrafficStats.getTotalTxBytes()`, active only while the app is in the foreground (zero battery drain when backgrounded).

### 2.3 Daily Limit & Per-App Data Cap Controls (Settings Screen)
- **Daily Limit Section**:
  - Toggle: "Daily Cellular Limit Warning"
  - Stepper/Slider: Configurable daily threshold (e.g. 500 MB, 1 GB, 2 GB).
  - Warning notification trigger if mobile data within the current calendar day crosses this mark.
- **Per-App Limits Section**:
  - List of monitored apps with individual limit thresholds (e.g., YouTube limit: 2 GB/cycle).
  - Quick action in `AppsScreen` to tap any app and set an alert limit.

### 2.4 Home Screen Data Glance Widget
- **Widget Layout**: 2x2 or 4x2 responsive Glance widget rendered using Android's native `RemoteViews` / `AppWidgetProvider`.
- **Content**:
  - Data plan name & remaining balance (e.g. `6.4 GB Left`).
  - Circular or linear progress bar showing % used.
  - Quick status badge: "On Pace" (Green) or "Pacing High" (Orange).
  - Tap target: Launches directly into NetTrace Home tab.

---

## 3. Key Product Decisions & Trade-Offs

| Decision | Chosen Approach | Rationale | Alternatives Considered |
|---|---|---|---|
| **Month Navigation** | `< Month Year >` Stepper Card | Clean, uncluttered, conforms to Apple/Material calendar steppers, eliminates awkward horizontal scrolling pills | Dropdown menu (less discoverable), Calendar matrix (too bulky) |
| **Live Speed Meter** | `TrafficStats` Foreground Polling (1s) | Standard Linux `/proc/net/dev` kernel counters; zero permission needed; instant response | Continuous background service (rejected to preserve zero background battery drain) |
| **Home Screen Widget** | Native `AppWidgetProvider` + `RemoteViews` | Minimal APK size (<30 KB overhead); zero third-party dependencies; instant rendering on all Android versions | Jetpack Glance library (adds 1.5 MB to APK size and extra Compose compiler overhead) |
| **Daily Alerts** | Handled inside existing `SimUsageSamplingWorker` | Reuses existing hourly background worker; checks daily delta without scheduling redundant background alarms | Separate alarm manager alarms (higher battery consumption) |

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                          NetTrace Application                          │
├────────────────────────────────────────────────────────────────────────┤
│  UI Layer (Jetpack Compose + Material 3)                               │
│  ├── HomeScreen: Hero Ring + Live Bandwidth Meter + Today/Cycle Cards  │
│  ├── AppsScreen: Top Apps with Monograms + App Limit Quick-Set Modal   │
│  ├── HistoryScreen: Month Stepper (< Month >) + Scrubbable Bar Canvas  │
│  └── SettingsScreen: Data Plans + Alerts + Daily/App Limits + Theme   │
├────────────────────────────────────────────────────────────────────────┤
│  Data Layer & System Services                                          │
│  ├── DataStoreRepository: Settings + Daily Limit + Per-App Limits Map  │
│  ├── NetworkStatsRepository: Range Math + Monthly Daily Bucketing     │
│  ├── LiveThroughputManager: 1s Foreground Flow (TrafficStats)         │
│  └── AlertNotificationManager: Cycle, Daily & App Threshold Alerts    │
├────────────────────────────────────────────────────────────────────────┤
│  Background & System Integrations                                      │
│  ├── NetTraceWidgetProvider: RemoteViews Home Screen Widget           │
│  └── SimUsageSamplingWorker: Hourly Traffic Delta + Alert Evaluation   │
└────────────────────────────────────────────────────────────────────────┘
```

### Data Storage Extensions (`DataStoreRepository`)
- `DAILY_LIMIT_MB`: Long (default: `0L` = disabled).
- `DAILY_ALERTS_ENABLED`: Boolean (default: `false`).
- `APP_LIMITS_JSON`: String (JSON map of `packageName -> limitMb`).
- `LAST_DAILY_ALERT_DAY`: Long (epoch day to prevent duplicate alerts).

### Implementation Stages
1. **Stage 1: History Month Stepper**: Replace horizontal pill row with responsive animated `<  Month Year  >` stepper header with jump-to-today affordance.
2. **Stage 2: Live Bandwidth Meter**: Implement `LiveThroughputManager` using `TrafficStats` and integrate clean pulse indicators into `HomeScreen`.
3. **Stage 3: Daily Limit & App Limits**: Add daily mobile caps and per-app limits in `DataStoreRepository`, `SettingsScreen`, and `AlertNotificationManager`.
4. **Stage 4: Home Screen Widget**: Implement native `NetTraceWidgetProvider` with `RemoteViews`, manifest receiver, and auto-refresh on data sample.
5. **Stage 5: Verification & APK Packaging**: Run `compile_applet`, verify edge-to-edge layout, and package both `NetTrace-release.apk` and `NetTrace-debug.apk`.
