# Agent Guide

## Project Summary

HealthTrackerApp is an Android app (package `com.rama.health`) that tracks
daily steps using the device's hardware sensors via a foreground service,
persists history in Room, and presents it through Jetpack Compose screens.

## Structure

- `app/src/main/java/com/rama/health/data/` — Room DB (`local/db`), DataStore preferences (`local/datastore`), and `StepRepositoryImpl` (`repository`).
- `app/src/main/java/com/rama/health/domain/` — `StepRepository` interface, use cases (`usecase`), pure baseline math (`util/StepBaselineCalculator`), and domain models (`model`).
- `app/src/main/java/com/rama/health/service/` — `StepCounterService` (foreground sensor service), `StepCounterNotificationHelper`, `BootCompletedReceiver`.
- `app/src/main/java/com/rama/health/ui/` — Compose screens/`ViewModel`s per feature (`dashboard`, `history`), navigation (`navigation`), and theming (`theme`).
- `app/src/main/java/com/rama/health/di/` — Hilt modules: `DatabaseModule`, `RepositoryModule`, `SystemServiceModule`.
- `app/src/test/` — JVM unit tests (JUnit4 + MockK + Turbine + Robolectric), mirroring the main package structure.
- `app/src/androidTest/` — Instrumented tests (Espresso, Compose UI testing, Hilt testing), including `HiltTestRunner`.
- `gradle/libs.versions.toml` — Centralized version catalog for all dependencies/plugins.

## Tech Stack

Kotlin, Jetpack Compose (Material 3), Dagger Hilt, Room, DataStore Preferences,
Kotlin Coroutines/Flow, Navigation Compose. Built with Gradle Kotlin DSL and KSP.
Min SDK 24, target/compile SDK 36/37.

## Conventions

- Layered architecture: `data` → `domain` → `ui`, with `domain` kept free of Android framework dependencies.
- One `UseCase` class per action (e.g. `ObserveTodayStepsUseCase`, `SetDailyGoalUseCase`), invoked via `operator fun invoke`.
- One `ViewModel` per screen, exposing a single `StateFlow<UiState>` built by combining use case flows (see `DashboardViewModel`, `HistoryViewModel`).
- Non-trivial logic (e.g. `StepBaselineCalculator`) is written as pure, framework-free functions/objects with detailed KDoc explaining edge cases (day rollover, sensor/device reboot), and is unit-tested directly.
- Dependency injection via Hilt constructor injection (`@Inject constructor`, `@HiltViewModel`, `@AndroidEntryPoint`); bindings/providers live in `di/*Module.kt`.
- Test files mirror the main source package path 1:1 under `src/test` or `src/androidTest`.

## Common Commands

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew installDebug           # Install on connected device/emulator
./gradlew testDebugUnitTest      # Run JVM unit tests
./gradlew connectedAndroidTest   # Run instrumented tests (needs device/emulator)
./gradlew lint                   # Run Android Lint
```

## Notes for AI Agents

- `local.properties` and `.gradle`/`build` directories are generated/local-only — never hand-edit or commit them.
- `StepCounterService.onCreate()` must call `startForeground(...)` synchronously (before any suspend/async work) to satisfy Android's 5-second foreground-promotion window — do not move notification setup behind a coroutine launch.
- `StepBaselineCalculator` encodes subtle invariants around day rollovers and sensor/device reboots (see its KDoc); changes here need corresponding unit test updates in `StepBaselineCalculatorTest`.
- When falling back to `TYPE_STEP_DETECTOR` (no `TYPE_STEP_COUNTER` on device), the service maintains its own synthetic cumulative counter seeded from the persisted baseline — see the seeding note in `StepCounterService`.
- Required runtime permissions: `ACTIVITY_RECOGNITION`, `POST_NOTIFICATIONS`; also declares `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_HEALTH`, and `RECEIVE_BOOT_COMPLETED`.
