# VI–III Shift Companion

Offline native Android application built with Kotlin, Jetpack Compose, Jetpack DataStore, and Android `AlarmManager` / `Notification` APIs for tracking the 24.5-hour **VI–III** operational shift (`09:30` through `10:00` the following day).

## Features

- **Exact 24.5-Hour Shift Timetable**
  - `09:30` — Briefing begins (open-ended; manual **Mark briefing complete** action transitions to the first break without inventing an ending time)
  - `After briefing – 12:00` — Break
  - `12:00 – 13:30` — Checkpoint VI (*საკონტროლო გამშვები პუნქტი VI*)
  - `13:30 – 15:00` — Checkpoint III (*საკონტროლო გამშვები პუნქტი III*)
  - `15:00 – 18:00` — Break
  - `18:00 – 19:30` — Checkpoint VI
  - `19:30 – 21:00` — Checkpoint III
  - `21:00 – 00:00` — Break
  - `00:00 – 03:00` — Checkpoint VI *(next calendar day)*
  - `03:00 – 06:00` — Checkpoint III *(next calendar day)*
  - `06:00 – 08:30` — Break *(next calendar day)*
  - `08:30 – 10:00` — Attention *(next calendar day)*
- **Zero-Drift Real-Time Countdown**:
  - Wall-clock `HH:mm:ss` live clock and countdown computed directly from `System.currentTimeMillis()` and `java.time`.
  - Synchronized to second boundaries, immediately recalculated on `ON_RESUME`, screen wake (`ACTION_SCREEN_ON`), `TIME_SET`, and `TIMEZONE_CHANGED`.
  - Defaults to `Asia/Tbilisi` with an optional **Device Time Zone** setting.
- **Android 16 Live Updates & Native AlarmManager Notifications**:
  - On Android 16 (API 36+), active shifts post a promoted Live Update (`POST_PROMOTED_NOTIFICATIONS`, `setRequestPromotedOngoing(true)`, `Notification.ProgressStyle` segmented shift timeline, `setShortCriticalText(...)` status chip, and live chronometer countdown via `setUsesChronometer` + `setChronometerCountDown`), with an inline **Mark briefing complete** action and graceful chronometer fallback on earlier Android versions.
  - Configurable pre-transition reminders (`5`, `10`, `15`, `30` minutes before) and exact-transition alerts using `setExactAndAllowWhileIdle()` when `canScheduleExactAlarms()` is granted, falling back to `setAndAllowWhileIdle()` when denied.
  - Restores future alarms after device reboot (`RECEIVE_BOOT_COMPLETED`), time/timezone changes, or exact-alarm permission changes.
- **Minimal Single-Viewport Aero & Office 2007 Interface**:
  - **Shift**, **Alerts**, and **About** fit within a single viewport without scrolling, while **Timetable** scrolls across the full 12-segment Day 1 / Day 2 rotation.
  - Glossy blue Aero gradients, Office 2007 top ribbon tabs, beveled buttons, optional Aero Dark theme, and live **English / Georgian** (`EN` / `KA`) toggle.

## Build & Test

Run local JVM & Robolectric tests:

```bash
gradle :app:testDebugUnitTest
```

Build debug APK:

```bash
gradle :app:assembleDebug
```

## Icon Attribution

Toolbar, status, and assignment icons are from the **FatCow Farm-Fresh Web Icons** collection (`https://github.com/gammasoft/fatcow`), licensed under **Creative Commons Attribution 3.0 (CC BY 3.0)**.
