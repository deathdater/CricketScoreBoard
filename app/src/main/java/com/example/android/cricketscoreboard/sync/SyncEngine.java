package com.example.android.cricketscoreboard.sync;

import android.content.Context;
import android.util.Log;

import com.example.android.cricketscoreboard.DatabaseHandler;
import com.example.android.cricketscoreboard.sync.SyncRecordModels.ConflictResolutionPolicy;
import com.example.android.cricketscoreboard.sync.SyncRecordModels.OutboxItem;
import com.example.android.cricketscoreboard.sync.SyncRecordModels.PlayerSyncRecord;
import com.example.android.cricketscoreboard.sync.SyncRecordModels.ScheduleSyncRecord;
import com.example.android.cricketscoreboard.sync.SyncRecordModels.SyncSummary;
import com.example.android.cricketscoreboard.sync.SyncRecordModels.TeamSyncRecord;

import org.json.JSONObject;

import java.util.List;

/**
 * Core offline-first synchronization engine.
 * Coordinates:
 * <ul>
 *   <li>Pre-sync local SQLite backup ({@link DatabaseBackupManager})</li>
 *   <li>First-time natural-key deduplication and merge confirmation</li>
 *   <li>Topological dependency push ({@code TEAM -> PLAYER -> SCHEDULE}) from {@code SYNC_OUTBOX}</li>
 *   <li>Incremental delta pull ({@code updatedAt > cursor}) and domain-specific conflict resolution</li>
 *   <li>Safe club switching and sign-out data isolation</li>
 * </ul>
 */
public final class SyncEngine {

    private static final String TAG = "SyncEngine";
    private static volatile SyncEngine instance;

    private final Context appContext;
    private SyncRemoteStore customRemoteStore;

    private SyncEngine(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public static SyncEngine getInstance(Context context) {
        if (instance == null) {
            synchronized (SyncEngine.class) {
                if (instance == null) {
                    instance = new SyncEngine(context);
                }
            }
        }
        return instance;
    }

    /**
     * Allows tests or offline simulation to inject a specific {@link SyncRemoteStore}.
     */
    public synchronized void setRemoteStoreOverride(SyncRemoteStore store) {
        this.customRemoteStore = store;
    }

    public synchronized SyncRemoteStore getActiveRemoteStore() {
        if (customRemoteStore != null) {
            return customRemoteStore;
        }
        FirestoreRemoteStore firestoreStore = new FirestoreRemoteStore(appContext);
        if (firestoreStore.isAvailable()) {
            return firestoreStore;
        }
        return InMemoryRemoteStore.getInstance();
    }

    /**
     * Executes a full two-way synchronization pass for the active club.
     *
     * @param forceConfirmMerge if {@code true}, proceeds with merging pre-existing local and
     *                          remote records on first-time sync.
     */
    public synchronized SyncSummary syncNow(boolean forceConfirmMerge) {
        SyncPreferences prefs = SyncPreferences.get(appContext);
        SyncSummary summary = new SyncSummary();
        String clubId = prefs.getClubId();

        if (!prefs.isSyncEnabled()) {
            summary.success = false;
            summary.message = "Cloud Sync is disabled. Local SQLite storage is active.";
            prefs.setLastSyncStatus(summary.message);
            return summary;
        }

        DatabaseHandler db = new DatabaseHandler(appContext);
        SyncRemoteStore remoteStore = getActiveRemoteStore();

        try {
            // 1. First-time migration check & pre-sync backup
            if (!prefs.isInitialMigrationCompleted(clubId)) {
                int localCount = db.getAllTeamsForSync().size()
                        + db.getAllPlayersForSync().size()
                        + db.getAllSchedulesForSync().size();
                int remoteCount = remoteStore.countRemoteRecords(clubId);

                if (localCount > 0 && (!forceConfirmMerge || !DatabaseBackupManager.hasPreSyncBackup(appContext))) {
                    DatabaseBackupManager.createPreSyncBackup(appContext);
                }

                if (localCount > 0 && remoteCount > 0
                        && !forceConfirmMerge
                        && !prefs.isMergeConfirmedForClub(clubId)) {
                    summary.success = false;
                    summary.requiresMergeConfirmation = true;
                    summary.localExistingCount = localCount;
                    summary.remoteExistingCount = remoteCount;
                    summary.message = "Found " + localCount + " local records and " + remoteCount
                            + " cloud records in club '" + clubId + "'. Confirm merge to continue.";
                    prefs.setLastSyncStatus("Awaiting first-time merge confirmation");
                    return summary;
                }

                if (forceConfirmMerge) {
                    prefs.setMergeConfirmedForClub(clubId, true);
                }

                // Pull existing remote records first so natural-key deduplication binds local rows
                // to existing remote SYNC_UUIDs before enqueueing local records.
                pullDeltasInDependencyOrder(db, remoteStore, prefs, clubId, summary);
                db.enqueueAllLocalRecordsForClub(clubId);
                prefs.setInitialMigrationCompleted(clubId, true);
            }

            // 2. Push pending outbox items in strict dependency order (TEAM -> PLAYER -> SCHEDULE)
            pushOutboxInDependencyOrder(db, remoteStore, clubId, summary);

            // 3. Pull remote changes updated since last pull cursor
            pullDeltasInDependencyOrder(db, remoteStore, prefs, clubId, summary);

            long now = System.currentTimeMillis();
            prefs.setLastSyncTimestamp(now);
            prefs.setLastSyncError("");
            summary.success = true;
            summary.message = "Synced (" + summary.pushedCount + " pushed, "
                    + summary.pulledCount + " pulled)";
            prefs.setLastSyncStatus(summary.message);
            return summary;

        } catch (Exception e) {
            Log.w(TAG, "Synchronization pass failed", e);
            summary.success = false;
            summary.message = "Sync error: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            prefs.setLastSyncError(summary.message);
            prefs.setLastSyncStatus("Sync paused — retry pending");
            return summary;
        }
    }

    private void pushOutboxInDependencyOrder(DatabaseHandler db, SyncRemoteStore remoteStore,
                                             String clubId, SyncSummary summary) throws Exception {
        List<OutboxItem> items = db.getPendingOutboxItems();
        for (OutboxItem item : items) {
            try {
                if (SyncRecordModels.ENTITY_TEAM.equals(item.entityType)) {
                    TeamSyncRecord rec = db.getTeamBySyncUuid(item.entityUuid);
                    if (rec == null) {
                        rec = TeamSyncRecord.fromJson(new JSONObject(item.payloadJson));
                    }
                    rec.clubId = clubId;
                    rec.remoteVersion = rec.remoteVersion + 1;
                    if (rec.updatedAt <= 0L) {
                        rec.updatedAt = System.currentTimeMillis();
                    }
                    remoteStore.upsertTeam(clubId, rec);
                    db.markOutboxItemSynced(item.outboxId, item.entityType, item.entityUuid, rec.remoteVersion);
                    summary.pushedCount++;

                } else if (SyncRecordModels.ENTITY_PLAYER.equals(item.entityType)) {
                    // Read fresh from DB so any newly merged teamSyncUuid is reflected
                    PlayerSyncRecord rec = db.getPlayerBySyncUuid(item.entityUuid);
                    if (rec == null) {
                        rec = PlayerSyncRecord.fromJson(new JSONObject(item.payloadJson));
                    }
                    rec.clubId = clubId;
                    rec.remoteVersion = rec.remoteVersion + 1;
                    if (rec.updatedAt <= 0L) {
                        rec.updatedAt = System.currentTimeMillis();
                    }
                    remoteStore.upsertPlayer(clubId, rec);
                    db.markOutboxItemSynced(item.outboxId, item.entityType, item.entityUuid, rec.remoteVersion);
                    summary.pushedCount++;

                } else if (SyncRecordModels.ENTITY_SCHEDULE.equals(item.entityType)) {
                    ScheduleSyncRecord rec = db.getScheduleBySyncUuid(item.entityUuid);
                    if (rec == null) {
                        rec = ScheduleSyncRecord.fromJson(new JSONObject(item.payloadJson));
                    }
                    rec.clubId = clubId;
                    rec.remoteVersion = rec.remoteVersion + 1;
                    if (rec.updatedAt <= 0L) {
                        rec.updatedAt = System.currentTimeMillis();
                    }
                    remoteStore.upsertSchedule(clubId, rec);
                    db.markOutboxItemSynced(item.outboxId, item.entityType, item.entityUuid, rec.remoteVersion);
                    summary.pushedCount++;

                } else {
                    db.markOutboxItemSynced(item.outboxId, item.entityType, item.entityUuid, 1L);
                }
            } catch (Exception itemEx) {
                db.markOutboxItemFailed(item.outboxId, itemEx.getMessage());
                throw itemEx;
            }
        }
    }

    private void pullDeltasInDependencyOrder(DatabaseHandler db, SyncRemoteStore remoteStore,
                                             SyncPreferences prefs, String clubId,
                                             SyncSummary summary) throws Exception {
        long since = prefs.getLastPullCursor(clubId);
        long maxSeenTimestamp = since;

        // 1. Pull Teams first
        List<TeamSyncRecord> remoteTeams = remoteStore.fetchTeamsUpdatedSince(clubId, since);
        for (TeamSyncRecord remote : remoteTeams) {
            maxSeenTimestamp = Math.max(maxSeenTimestamp, remote.updatedAt);
            TeamSyncRecord local = db.getTeamBySyncUuid(remote.syncUuid);
            if (local == null && remote.teamName != null) {
                local = db.getTeamByNormalizedName(remote.teamName);
            }
            if (local != null && SyncRecordModels.STATUS_DIRTY.equals(local.syncStatus)
                    && (local.updatedAt != remote.updatedAt || !local.syncUuid.equals(remote.syncUuid))) {
                TeamSyncRecord merged = ConflictResolutionPolicy.resolveTeamConflict(local, remote);
                db.upsertTeamFromRemote(merged, false);
                summary.conflictsResolvedCount++;
            } else {
                db.upsertTeamFromRemote(remote, true);
            }
            summary.pulledCount++;
        }

        // 2. Pull Players second (resolving teamSyncUuid -> local TEAM_ID)
        List<PlayerSyncRecord> remotePlayers = remoteStore.fetchPlayersUpdatedSince(clubId, since);
        for (PlayerSyncRecord remote : remotePlayers) {
            maxSeenTimestamp = Math.max(maxSeenTimestamp, remote.updatedAt);
            PlayerSyncRecord local = db.getPlayerBySyncUuid(remote.syncUuid);
            if (local != null && SyncRecordModels.STATUS_DIRTY.equals(local.syncStatus)
                    && local.updatedAt != remote.updatedAt) {
                PlayerSyncRecord merged = ConflictResolutionPolicy.resolvePlayerConflict(local, remote);
                db.upsertPlayerFromRemote(merged, false);
                summary.conflictsResolvedCount++;
            } else {
                db.upsertPlayerFromRemote(remote, true);
            }
            summary.pulledCount++;
        }

        // 3. Pull Schedules third (resolving teamASyncUuid / teamBSyncUuid -> local TEAM_IDs)
        List<ScheduleSyncRecord> remoteSchedules = remoteStore.fetchSchedulesUpdatedSince(clubId, since);
        for (ScheduleSyncRecord remote : remoteSchedules) {
            maxSeenTimestamp = Math.max(maxSeenTimestamp, remote.updatedAt);
            ScheduleSyncRecord local = db.getScheduleBySyncUuid(remote.syncUuid);
            if (local != null && SyncRecordModels.STATUS_DIRTY.equals(local.syncStatus)
                    && local.updatedAt != remote.updatedAt) {
                ScheduleSyncRecord merged = ConflictResolutionPolicy.resolveScheduleConflict(local, remote);
                db.upsertScheduleFromRemote(merged, false);
                summary.conflictsResolvedCount++;
            } else {
                db.upsertScheduleFromRemote(remote, true);
            }
            summary.pulledCount++;
        }

        if (maxSeenTimestamp > since) {
            prefs.setLastPullCursor(clubId, maxSeenTimestamp);
        }
    }

    /**
     * Switches the active club ID with strict data isolation so records from the previous
     * club are never leaked into the new club. Creates a pre-switch local backup first.
     */
    public synchronized void switchClubIsolated(String newClubId) {
        SyncPreferences prefs = SyncPreferences.get(appContext);
        String sanitizedNew = SyncPreferences.sanitizeClubId(newClubId);
        String currentClub = prefs.getClubId();
        if (sanitizedNew.equals(currentClub)) {
            return;
        }
        DatabaseBackupManager.createPreSyncBackup(appContext);
        DatabaseHandler db = new DatabaseHandler(appContext);
        db.clearLocalDataForClubSwitch();
        prefs.setClubId(sanitizedNew);
        prefs.setLastSyncStatus("Switched to club: " + sanitizedNew);
    }
}
