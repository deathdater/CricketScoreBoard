# Firebase Cloud Synchronization Guide

## 1. Architectural Overview
`CricketScoreBoard` implements an **offline-first** synchronization architecture where the local SQLite database ([`DatabaseHandler.java`](file:///Volumes/Dater%205TB/1.0%20Python%20Projects/CricketScoreBoard/app/src/main/java/com/example/android/cricketscoreboard/DatabaseHandler.java), `cricketScore_DB` v3) remains the single source of truth for all UI screens.

```
UI Screens -> DatabaseHandler (SQLite v3) -> SYNC_OUTBOX -> SyncEngine / CricketSyncWorker -> Cloud Firestore
Cloud Firestore -> SyncEngine (Delta Pull & ConflictResolver) -> DatabaseHandler (SQLite v3) -> UI Screens
```

### Core Guarantees
- **100% Offline Operation:** All features (Team creation, Player registration, Roster assignment, Match scheduling, and Live ball-by-ball scoring) work without internet connectivity or a configured Firebase project.
- **Transactional Outbox:** Every local create/update/soft-delete in `DatabaseHandler` writes sync metadata (`SYNC_UUID`, `CLUB_ID`, `UPDATED_AT`, `IS_DELETED`, `SYNC_STATUS`, `REMOTE_VERSION`) and enqueues an idempotent entry in `SYNC_OUTBOX`.
- **Confirmed Sync State:** A locally saved record remains `DIRTY` (pending in `SYNC_OUTBOX`) until the remote store confirms the write, at which point `DatabaseHandler.markOutboxItemSynced(...)` removes the outbox row and marks the record `SYNCED`.

---

## 2. Enabling Production Cloud Firestore
1. Create a Firebase project in the [Firebase Console](https://console.firebase.google.com/).
2. Register the Android package `com.example.android.cricketscoreboard`.
3. Download `google-services.json` and place it at `app/google-services.json`.
   - [`app/build.gradle`](file:///Volumes/Dater%205TB/1.0%20Python%20Projects/CricketScoreBoard/app/build.gradle) automatically detects `app/google-services.json` and applies `com.google.gms.google-services`.
   - When `app/google-services.json` is absent (e.g., local offline builds or CI), [`FirestoreRemoteStore.isFirebaseConfigured(context)`](file:///Volumes/Dater%205TB/1.0%20Python%20Projects/CricketScoreBoard/app/src/main/java/com/example/android/cricketscoreboard/sync/FirestoreRemoteStore.java) falls back cleanly to [`InMemoryRemoteStore`](file:///Volumes/Dater%205TB/1.0%20Python%20Projects/CricketScoreBoard/app/src/main/java/com/example/android/cricketscoreboard/sync/InMemoryRemoteStore.java).
4. Enable **Cloud Firestore** and **Firebase Authentication** (Email/Password) in the Firebase Console.
5. Deploy [`firestore.rules`](file:///Volumes/Dater%205TB/1.0%20Python%20Projects/CricketScoreBoard/firestore.rules):
   ```bash
   firebase deploy --only firestore:rules
   ```

---

## 3. Entity Mapping & Foreign-Key Translation

| Local SQLite Table | Local PK | Cloud Firestore Collection | Foreign-Key Translation |
| :--- | :--- | :--- | :--- |
| `TEAM_DETAILS` | `TEAM_ID` (`INTEGER AUTOINCREMENT`) | `/clubs/{clubId}/teams/{syncUuid}` | Stores `syncUuid` and normalized natural key `teamNameNormalized`. |
| `PLAYER_DETAILS` | `PLAYER_ID` (`INTEGER AUTOINCREMENT`) | `/clubs/{clubId}/players/{syncUuid}` | Translates `PLAYER_TEAM_ID` (`int`) $\leftrightarrow$ `teamSyncUuid` (`String`). |
| `SCHEDULE_DETAILS` | `SCHEDULE_MATCH_ID` (`INTEGER AUTOINCREMENT`) | `/clubs/{clubId}/schedules/{syncUuid}` | Translates `SCHEDULE_TEAMA_ID` & `SCHEDULE_TEAMB_ID` (`int`) $\leftrightarrow$ `teamASyncUuid` & `teamBSyncUuid` (`String`). |
| `GROUND_DETAILS` | `GROUND_ID` (`INTEGER AUTOINCREMENT`) | `/clubs/{clubId}/grounds/{syncUuid}` | Schema-ready sync columns (`SYNC_UUID`, `CLUB_ID`, `UPDATED_AT`, `IS_DELETED`). |

Both outbox pushes and delta pulls execute in strict topological dependency order:
**`TEAM` $\rightarrow$ `GROUND` $\rightarrow$ `PLAYER` $\rightarrow$ `SCHEDULE`**.

---

## 4. First-Time Migration, Conflict Resolution & Rollback
- **Automatic Pre-Sync Backup:** Before the first cloud merge or club switch, [`DatabaseBackupManager.createPreSyncBackup(context)`](file:///Volumes/Dater%205TB/1.0%20Python%20Projects/CricketScoreBoard/app/src/main/java/com/example/android/cricketscoreboard/sync/DatabaseBackupManager.java) copies `cricketScore_DB` to `cricketScore_DB.pre_sync.bak`.
- **Merge Confirmation:** If both local SQLite and the target cloud club already contain records on first-time sync, [`SyncEngine.syncNow(false)`](file:///Volumes/Dater%205TB/1.0%20Python%20Projects/CricketScoreBoard/app/src/main/java/com/example/android/cricketscoreboard/sync/SyncEngine.java) pauses and requests user confirmation via [`CloudSyncDialog`](file:///Volumes/Dater%205TB/1.0%20Python%20Projects/CricketScoreBoard/app/src/main/java/com/example/android/cricketscoreboard/sync/CloudSyncDialog.java).
- **Natural-Key Deduplication:** Pre-existing offline teams and players match remote records by normalized name (`LOWER(TRIM(name))`) so repeated or first-time sync never creates duplicate rows or triggers `SQLiteConstraintException`.
- **Domain-Specific Conflict Policies ([`ConflictResolutionPolicy`](file:///Volumes/Dater%205TB/1.0%20Python%20Projects/CricketScoreBoard/app/src/main/java/com/example/android/cricketscoreboard/sync/SyncRecordModels.java)):**
  - **Teams:** Cumulative `matchWon`, `matchLost`, and `matchDraw` counters take `Math.max(local, remote)`.
  - **Players:** Cumulative career stats (`totalRuns`, `wicketsTaken`, `ballsFaced`, etc.) take `Math.max(local, remote)`; role flags and `teamSyncUuid` resolve by latest `updatedAt`.
  - **Schedules:** Match status progresses monotonically (`SCHEDULED < IN_PROGRESS < COMPLETED`).
  - **Tombstones:** Soft-deletes set `IS_DELETED = 1` (`isDeleted: true`) and cannot be resurrected by stale records with an older `updatedAt`.
