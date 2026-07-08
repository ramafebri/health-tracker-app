# HealthTrackerApp

An Android step-counter and daily activity tracker. It runs a foreground
service that reads the device's hardware step sensor, tracks progress toward
a configurable daily step goal, and keeps a history of past days.

## Architecture

The app follows a layered (data / domain / UI) architecture with dependency
injection via Hilt:

- **`data`** — persistence and device I/O.
  - `local.db` — Room database (`AppDatabase`, `DailyStepsDao`, `DailyStepsEntity`) storing per-day step history.
  - `local.datastore` — `StepPreferencesDataSource`, a Jetpack DataStore-backed store for the sensor baseline, daily goal, and tracking-enabled flag.
  - `repository` — `StepRepositoryImpl`, the single source of truth that reconciles live sensor readings with persisted state.
- **`domain`** — business logic, independent of Android frameworks.
  - `repository.StepRepository` — the repository interface consumed by the domain/UI layers.
  - `usecase` — one use case per action: observing today's steps, observing/setting the daily goal, observing step history, and toggling tracking.
  - `util.StepBaselineCalculator` — pure function that derives "today's steps" from a raw cumulative sensor reading, handling day rollovers and device reboots (which reset the hardware counter).
  - `model.DailyStepRecord` — domain model for a single day's step history entry.
- **`service`** — `StepCounterService`, a foreground service that registers the `TYPE_STEP_COUNTER` sensor (falling back to `TYPE_STEP_DETECTOR` when unavailable), forwards readings to the repository, and maintains an ongoing notification (`StepCounterNotificationHelper`). `BootCompletedReceiver` restarts the service after device reboot if tracking was left enabled.
- **`ui`** — Jetpack Compose screens, one `ViewModel` per screen, navigated via Navigation Compose (`NavGraph`, `NavRoutes`):
  - `dashboard` — today's steps, goal editing, and permission requests.
  - `history` — list of past days' step totals.
  - `theme` — Material 3 theming.
- **`di`** — Hilt modules wiring the database, repository, and system services (`SensorManager`, `NotificationManager`).

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose, Material 3
- **DI:** Dagger Hilt
- **Persistence:** Room (step history), Jetpack DataStore Preferences (sensor baseline/settings)
- **Async:** Kotlin Coroutines & Flow
- **Navigation:** Navigation Compose
- **Build:** Gradle (Kotlin DSL) with KSP for annotation processing
- **Testing:** JUnit4, MockK, Turbine, Robolectric, Espresso, Compose UI Testing, Hilt Testing

## Features

- Foreground step tracking using the device's hardware step-counter (or step-detector fallback) sensor.
- Daily step goal that the user can view and edit from the dashboard.
- Persistent ongoing notification showing today's progress toward the goal.
- Automatic re-baselining across day rollovers and device reboots so step counts stay accurate.
- Auto-restart of tracking after device reboot (via `BOOT_COMPLETED`), if tracking was previously enabled.
- History screen listing past days' step totals.
- Runtime permission handling for activity recognition and notifications.

## Getting Started

### Prerequisites

- Android Studio (recent stable version)
- JDK 11+
- Android SDK with `compileSdk 37` / `minSdk 24` installed

### Build & Run

```bash
# Clone and enter the project
git clone https://github.com/ramafebri/health-tracker-app.git
cd health-tracker-app

# Build the debug APK
./gradlew assembleDebug

# Install and run on a connected device/emulator
./gradlew installDebug
```

### Testing

```bash
# Unit tests
./gradlew testDebugUnitTest

# Instrumented UI tests (requires a connected device/emulator)
./gradlew connectedAndroidTest
```

Alternatively, open the project in Android Studio and use the built-in Run/Test actions.
