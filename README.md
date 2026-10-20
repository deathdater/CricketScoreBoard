# Cricket Score Board (Android)

A native Android application for managing cricket clubs, teams, player rosters, match fixtures, grounds, and live ball-by-ball match scoring.

---

## Features

- **Club & Match Hub (`LandingPageActivity`):** Live summary banner showing registered teams, players, and scheduled fixtures, with quick access to all club management and live scoring workflows.
- **Team & Squad Management (`TeamDetails`, `ViewTeams`):** Create teams with custom logos (via the Android Photo Picker — zero storage permissions required), view win/loss/draw records, and assign or remove players from squads.
- **Player Profiles & Career Stats (`PlayerDetailsActivity`, `ViewPlayers`):** Register players with primary roles (Batsman, Bowler, Wicket-Keeper) and handedness (`RHB` / `LHB`), and inspect batting and bowling statistics.
- **Fixtures & Grounds (`ScheduleMatches`, `ViewSchedules`, `GroundDetailsActivity`, `ViewGrounds`):** Schedule matches between teams with configurable overs, inspect upcoming fixtures, and browse cricket grounds by city.
- **Live Ball-by-Ball Scorer (`ScorerPadActivity`, `ScorerTabActivity`):** Record runs (`0`–`6`), extras (`Wide Ball`, `No Ball`, `Free Hit`, `Byes`, `Leg Byes`), wickets (`Out`), over progression, and one-tap ball undo (`ScoreMan`).
- **UI Personalization (`UiPreferences`, `UiSettingsDialog`):** Persistent user customization for **Theme Mode** (System / Light / Dark), **Accent Palette** (Turf Green, Stadium Night Blue, Pavilion Classic Wood), **Layout Density** (Standard / Comfortable), **Scorer Haptics**, and **Startup Behavior** (Skip welcome screen), with one-tap **Reset Defaults**.

---

## Technology Stack & Build Configuration

| Component | Version / Configuration |
| :--- | :--- |
| **Language & UI** | Java 17 + Android View XML + Material Components (`Theme.MaterialComponents.DayNight`) |
| **Gradle / AGP** | Gradle `8.14.3` / Android Gradle Plugin `8.13.0` ([gradle/libs.versions.toml](gradle/libs.versions.toml)) |
| **SDK Levels** | `compileSdk = 36`, `targetSdk = 36`, `minSdk = 23` |
| **Key Libraries** | `androidx.appcompat:1.7.1`, `com.google.android.material:1.13.0`, `androidx.constraintlayout:2.2.1`, `androidx.activity:1.11.0`, `androidx.viewpager2:1.1.0` |
| **Persistence** | SQLite (`DatabaseHandler`, `cricketScore_DB` v2) for domain data; `SharedPreferences` (`UiPreferences`) for UI personalization |

---

## Setup, Build & Test Instructions

### Prerequisites
1. **Android SDK** with Platform 36 installed (`local.properties` pointing to `sdk.dir`).
2. **JDK 17 or 21** (Android Studio's bundled JBR at `/Applications/Android Studio.app/Contents/jbr/Contents/Home` works out of the box).

> **Note on External Volumes (macOS):** When the repository resides on a non-APFS external volume under `/Volumes/...`, [build.gradle](build.gradle) automatically redirects build outputs to `~/.gradle-builds/CricketScoreBoard/` on the local disk so macOS AppleDouble (`._*`) metadata files do not interfere with Android resource packaging.

### Build Debug APK
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug
```

### Run Unit Tests & Lint
```bash
./gradlew testDebugUnitTest lintDebug
```

### Run Instrumented Tests (Requires Running Emulator or Device)
```bash
./gradlew connectedDebugAndroidTest
```
Instrumented tests in [`app/src/androidTest`](app/src/androidTest/java/com/example/android/cricketscoreboard) verify:
- [`ExampleInstrumentedTest`](app/src/androidTest/java/com/example/android/cricketscoreboard/ExampleInstrumentedTest.java): Application context and package identity.
- [`ScorerTabActivityTest`](app/src/androidTest/java/com/example/android/cricketscoreboard/ScorerTabActivityTest.java): `ViewPager2` + `TabLayoutMediator` tab synchronization.
- [`ModernUiAndWorkflowTest`](app/src/androidTest/java/com/example/android/cricketscoreboard/ModernUiAndWorkflowTest.java): End-to-end UI preference persistence/reset, team creation, player registration, squad management, match scheduling, and live ball-by-ball scoring + undo.

---

## Architecture & Design System Documentation

See [docs/ARCHITECTURE_AND_DESIGN_SYSTEM.md](docs/ARCHITECTURE_AND_DESIGN_SYSTEM.md) for detailed documentation on the application architecture, database schema, Material DayNight design tokens, and customization extension points.
