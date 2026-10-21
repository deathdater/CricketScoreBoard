# Cricket Score Board (Android)

A native Android application for managing cricket clubs, teams, player rosters, match fixtures, grounds, and live ball-by-ball match scoring.

---

## Features

- **Club & Match Hub (`LandingPageActivity`):** Live summary banner showing registered teams, players, and scheduled fixtures, with quick access to all club management and live scoring workflows.
- **Team & Squad Management (`TeamDetails`, `ViewTeams`):** Create teams with custom logos (via the Android Photo Picker — zero storage permissions required), view win/loss/draw records, and assign or remove players from squads.
- **Player Profiles & Career Stats (`PlayerDetailsActivity`, `ViewPlayers`):** Register players with primary roles (Batsman, Bowler, Wicket-Keeper) and handedness (`RHB` / `LHB`), and inspect batting and bowling statistics.
- **Fixtures & Grounds (`ScheduleMatches`, `ViewSchedules`, `GroundDetailsActivity`, `ViewGrounds`):** Schedule matches between teams with configurable overs, inspect upcoming fixtures, and browse cricket grounds by city.
- **Deterministic Configurable Scoring Engine (`com.example.android.cricketscoreboard.scoring`):** Event-sourced ball-by-ball engine (`CricketScoringEngine`, `DeliveryEvent`, `MatchState`) with explicit, versioned rules profiles (`CricketRulesProfile`) supporting `Standard T20`, `One-Day 50 Overs (ODI)`, `Express T10`, `The Hundred (5-Ball Sets)`, `Tape-Ball / Street Last-Man-Standing`, `Youth Academy (Retire at 30*)`, and `Multi-Day Test Cricket`. Enforces MCC dismissal laws on Wides, No-Balls, and Free Hits, automatic strike rotation, maiden detection, innings transitions, chase target tracking, and deterministic undo/replay.
- **Sunlight-Ready Scorer & 4-Tab Live Match Center (`ScorerPadActivity`, `ScorerTabActivity`):** Record runs (`0`–`6`), extras (`Wide Ball`, `No Ball`, `Free Hit`, `Byes`, `Leg Byes`), dismissals (`Bowled`, `Caught`, `LBW`, `Run Out`, `Stumped`, `Hit Wicket`, `Retired Hurt`, `Retired Out`), and one-tap ball undo backed by persistent SQLite `v4` event logs (`MATCH_SESSIONS`, `MATCH_DELIVERY_EVENTS`). Includes live Batting/Bowling/FOW Scorecards, Over-by-Over Manhattan & Worm Analytics (`CricketAnalyticsCalculator`), Tournament Points & Net Run Rate (`PointsTableCalculator`), Official Result Confirmation (`PROVISIONAL -> CONFIRMED -> FINALIZED`), and zero-account **One-Tap Live Score Sharing**.
- **UI Personalization (`UiPreferences`, `UiSettingsDialog`):** Persistent user customization for **Theme Mode** (System / Light / Dark), **Accent Palette** (Turf Green, Stadium Night Blue, Pavilion Classic Wood), **Layout Density** (Standard / Comfortable), **Scorer Haptics**, and **Startup Behavior** (Skip welcome screen), with one-tap **Reset Defaults**.
- **Optional Offline-First Cloud Synchronization (`SyncEngine`, `CloudSyncDialog`, `CricketSyncWorker`):** Synchronizes club teams, players, squad assignments, and match schedules with Cloud Firestore (`/clubs/{clubId}/...`) while keeping local SQLite (`DatabaseHandler` v4) as the primary offline source of truth. Includes persistent outbox tracking (`SYNC_OUTBOX`), topological dependency ordering, natural-key first-time migration, automatic pre-sync SQLite backup/rollback, and club workspace isolation.

---

## Technology Stack & Build Configuration

| Component | Version / Configuration |
| :--- | :--- |
| **Language & UI** | Java 17 + Android View XML + Material Components (`Theme.MaterialComponents.DayNight`) |
| **Gradle / AGP** | Gradle `8.14.3` / Android Gradle Plugin `8.13.0` ([gradle/libs.versions.toml](gradle/libs.versions.toml)) |
| **SDK Levels** | `compileSdk = 36`, `targetSdk = 36`, `minSdk = 23` |
| **Key Libraries** | `androidx.appcompat:1.7.1`, `com.google.android.material:1.13.0`, `androidx.constraintlayout:2.2.1`, `androidx.activity:1.11.0`, `androidx.viewpager2:1.1.0`, `androidx.work:work-runtime:2.10.0`, `com.google.firebase:firebase-bom:33.10.0` (`firebase-auth`, `firebase-firestore`) |
| **Persistence** | SQLite (`DatabaseHandler`, `cricketScore_DB` v3) for domain data + `SYNC_OUTBOX`; `SharedPreferences` (`UiPreferences`, `SyncPreferences`) for UI & sync configuration |

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
- [`FirebaseSyncAndMigrationTest`](app/src/androidTest/java/com/example/android/cricketscoreboard/FirebaseSyncAndMigrationTest.java): End-to-end verification of offline outbox queueing, dependency-ordered synchronization (`TEAM -> PLAYER -> SCHEDULE`), FK UUID translation, transient failure retry idempotency, first-time natural-key deduplication + pre-sync SQLite backup rollback, conflict resolution, and club workspace isolation.

---

## Architecture, Design System & Firebase Sync Documentation

- [docs/ARCHITECTURE_AND_DESIGN_SYSTEM.md](docs/ARCHITECTURE_AND_DESIGN_SYSTEM.md): Application architecture, database schema, Material DayNight design tokens, and UI customization extension points.
- [docs/FIREBASE_SYNC_GUIDE.md](docs/FIREBASE_SYNC_GUIDE.md): Offline-first Cloud Firestore synchronization architecture, setup instructions (`google-services.json` & [`firestore.rules`](firestore.rules)), entity mapping, conflict resolution policies, and backup/rollback recovery.
