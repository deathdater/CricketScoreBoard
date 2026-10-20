package com.example.android.cricketscoreboard.sync;

import com.example.android.cricketscoreboard.sync.SyncRecordModels.PlayerSyncRecord;
import com.example.android.cricketscoreboard.sync.SyncRecordModels.ScheduleSyncRecord;
import com.example.android.cricketscoreboard.sync.SyncRecordModels.TeamSyncRecord;

import java.util.List;

/**
 * Remote storage contract for club synchronization.
 * Implemented by {@link FirestoreRemoteStore} for production Cloud Firestore and
 * {@link InMemoryRemoteStore} for local simulation and deterministic instrumentation tests.
 */
public interface SyncRemoteStore {

    boolean isAvailable();

    String getBackendName();

    void upsertTeam(String clubId, TeamSyncRecord record) throws Exception;

    void upsertPlayer(String clubId, PlayerSyncRecord record) throws Exception;

    void upsertSchedule(String clubId, ScheduleSyncRecord record) throws Exception;

    List<TeamSyncRecord> fetchTeamsUpdatedSince(String clubId, long sinceTimestamp) throws Exception;

    List<PlayerSyncRecord> fetchPlayersUpdatedSince(String clubId, long sinceTimestamp) throws Exception;

    List<ScheduleSyncRecord> fetchSchedulesUpdatedSince(String clubId, long sinceTimestamp) throws Exception;

    int countRemoteRecords(String clubId) throws Exception;
}
