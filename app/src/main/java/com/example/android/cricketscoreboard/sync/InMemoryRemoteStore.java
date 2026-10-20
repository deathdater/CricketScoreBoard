package com.example.android.cricketscoreboard.sync;

import com.example.android.cricketscoreboard.sync.SyncRecordModels.PlayerSyncRecord;
import com.example.android.cricketscoreboard.sync.SyncRecordModels.ScheduleSyncRecord;
import com.example.android.cricketscoreboard.sync.SyncRecordModels.TeamSyncRecord;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Thread-safe in-memory cloud store emulating Cloud Firestore's
 * {@code /clubs/{clubId}/{teams|players|schedules}/{syncUuid}} hierarchy.
 * Used when {@code google-services.json} is not present and for deterministic
 * multi-device, retry, conflict-resolution, and club-isolation instrumentation tests.
 */
public final class InMemoryRemoteStore implements SyncRemoteStore {

    private static final InMemoryRemoteStore INSTANCE = new InMemoryRemoteStore();

    private final Map<String, Map<String, Map<String, Object>>> clubTeams = new HashMap<>();
    private final Map<String, Map<String, Map<String, Object>>> clubPlayers = new HashMap<>();
    private final Map<String, Map<String, Map<String, Object>>> clubSchedules = new HashMap<>();
    private int transientFailuresRemaining = 0;

    private InMemoryRemoteStore() {
    }

    public static InMemoryRemoteStore getInstance() {
        return INSTANCE;
    }

    public synchronized void clearAll() {
        clubTeams.clear();
        clubPlayers.clear();
        clubSchedules.clear();
        transientFailuresRemaining = 0;
    }

    public synchronized void setSimulateTransientFailureCount(int count) {
        this.transientFailuresRemaining = Math.max(0, count);
    }

    private synchronized void checkSimulatedFailure() throws IOException {
        if (transientFailuresRemaining > 0) {
            transientFailuresRemaining--;
            throw new IOException("Simulated transient network interruption");
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getBackendName() {
        return "Local Cloud Emulator";
    }

    @Override
    public synchronized void upsertTeam(String clubId, TeamSyncRecord record) throws Exception {
        checkSimulatedFailure();
        String safeClub = SyncPreferences.sanitizeClubId(clubId);
        Map<String, Map<String, Object>> map = clubTeams.get(safeClub);
        if (map == null) {
            map = new HashMap<>();
            clubTeams.put(safeClub, map);
        }
        map.put(record.syncUuid, new HashMap<>(record.toFirestoreMap()));
    }

    @Override
    public synchronized void upsertPlayer(String clubId, PlayerSyncRecord record) throws Exception {
        checkSimulatedFailure();
        String safeClub = SyncPreferences.sanitizeClubId(clubId);
        Map<String, Map<String, Object>> map = clubPlayers.get(safeClub);
        if (map == null) {
            map = new HashMap<>();
            clubPlayers.put(safeClub, map);
        }
        map.put(record.syncUuid, new HashMap<>(record.toFirestoreMap()));
    }

    @Override
    public synchronized void upsertSchedule(String clubId, ScheduleSyncRecord record) throws Exception {
        checkSimulatedFailure();
        String safeClub = SyncPreferences.sanitizeClubId(clubId);
        Map<String, Map<String, Object>> map = clubSchedules.get(safeClub);
        if (map == null) {
            map = new HashMap<>();
            clubSchedules.put(safeClub, map);
        }
        map.put(record.syncUuid, new HashMap<>(record.toFirestoreMap()));
    }

    @Override
    public synchronized List<TeamSyncRecord> fetchTeamsUpdatedSince(String clubId, long sinceTimestamp) throws Exception {
        checkSimulatedFailure();
        String safeClub = SyncPreferences.sanitizeClubId(clubId);
        Map<String, Map<String, Object>> map = clubTeams.get(safeClub);
        List<TeamSyncRecord> list = new ArrayList<>();
        if (map != null) {
            for (Map<String, Object> raw : map.values()) {
                TeamSyncRecord rec = TeamSyncRecord.fromFirestoreMap(raw);
                if (rec.updatedAt > sinceTimestamp) {
                    list.add(rec);
                }
            }
        }
        Collections.sort(list, Comparator.comparingLong(a -> a.updatedAt));
        return list;
    }

    @Override
    public synchronized List<PlayerSyncRecord> fetchPlayersUpdatedSince(String clubId, long sinceTimestamp) throws Exception {
        checkSimulatedFailure();
        String safeClub = SyncPreferences.sanitizeClubId(clubId);
        Map<String, Map<String, Object>> map = clubPlayers.get(safeClub);
        List<PlayerSyncRecord> list = new ArrayList<>();
        if (map != null) {
            for (Map<String, Object> raw : map.values()) {
                PlayerSyncRecord rec = PlayerSyncRecord.fromFirestoreMap(raw);
                if (rec.updatedAt > sinceTimestamp) {
                    list.add(rec);
                }
            }
        }
        Collections.sort(list, Comparator.comparingLong(a -> a.updatedAt));
        return list;
    }

    @Override
    public synchronized List<ScheduleSyncRecord> fetchSchedulesUpdatedSince(String clubId, long sinceTimestamp) throws Exception {
        checkSimulatedFailure();
        String safeClub = SyncPreferences.sanitizeClubId(clubId);
        Map<String, Map<String, Object>> map = clubSchedules.get(safeClub);
        List<ScheduleSyncRecord> list = new ArrayList<>();
        if (map != null) {
            for (Map<String, Object> raw : map.values()) {
                ScheduleSyncRecord rec = ScheduleSyncRecord.fromFirestoreMap(raw);
                if (rec.updatedAt > sinceTimestamp) {
                    list.add(rec);
                }
            }
        }
        Collections.sort(list, Comparator.comparingLong(a -> a.updatedAt));
        return list;
    }

    @Override
    public synchronized int countRemoteRecords(String clubId) {
        String safeClub = SyncPreferences.sanitizeClubId(clubId);
        int t = clubTeams.containsKey(safeClub) ? clubTeams.get(safeClub).size() : 0;
        int p = clubPlayers.containsKey(safeClub) ? clubPlayers.get(safeClub).size() : 0;
        int s = clubSchedules.containsKey(safeClub) ? clubSchedules.get(safeClub).size() : 0;
        return t + p + s;
    }
}
