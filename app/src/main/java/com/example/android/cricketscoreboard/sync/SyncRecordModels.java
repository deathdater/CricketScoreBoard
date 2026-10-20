package com.example.android.cricketscoreboard.sync;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Data transfer records, JSON/Map serializers, and deterministic conflict-resolution
 * policies for offline-first synchronization between SQLite ({@code DatabaseHandler})
 * and Cloud Firestore.
 */
public final class SyncRecordModels {

    public static final String ENTITY_TEAM = "TEAM";
    public static final String ENTITY_GROUND = "GROUND";
    public static final String ENTITY_PLAYER = "PLAYER";
    public static final String ENTITY_SCHEDULE = "SCHEDULE";

    public static final String OP_UPSERT = "UPSERT";
    public static final String OP_DELETE = "DELETE";

    public static final String STATUS_DIRTY = "DIRTY";
    public static final String STATUS_SYNCING = "SYNCING";
    public static final String STATUS_SYNCED = "SYNCED";
    public static final String STATUS_CONFLICT = "CONFLICT";

    private SyncRecordModels() {
    }

    public static String normalizeName(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.US);
    }

    /**
     * Represents a persistent row in {@code SYNC_OUTBOX}.
     */
    public static final class OutboxItem {
        public long outboxId;
        public String entityType;
        public String entityUuid;
        public String operation;
        public String payloadJson;
        public long createdAt;
        public int retryCount;
        public String lastError;
    }

    /**
     * Synchronizable representation of a row in {@code TEAM_DETAILS}.
     */
    public static final class TeamSyncRecord {
        public int localTeamId;
        public String syncUuid;
        public String clubId;
        public String userEmailId;
        public String teamName;
        public String teamNameNormalized;
        public int matchWon;
        public int matchLost;
        public int matchDraw;
        public int captainId;
        public String captainSyncUuid;
        public long updatedAt;
        public boolean isDeleted;
        public long remoteVersion;
        public String syncStatus;

        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("syncUuid", syncUuid);
            obj.put("clubId", clubId == null ? SyncPreferences.DEFAULT_LOCAL_CLUB_ID : clubId);
            obj.put("userEmailId", userEmailId == null ? "" : userEmailId);
            obj.put("teamName", teamName == null ? "" : teamName);
            obj.put("teamNameNormalized", normalizeName(teamName));
            obj.put("matchWon", matchWon);
            obj.put("matchLost", matchLost);
            obj.put("matchDraw", matchDraw);
            obj.put("captainId", captainId);
            obj.put("captainSyncUuid", captainSyncUuid == null ? "" : captainSyncUuid);
            obj.put("updatedAt", updatedAt);
            obj.put("isDeleted", isDeleted);
            obj.put("remoteVersion", remoteVersion);
            return obj;
        }

        public static TeamSyncRecord fromJson(JSONObject obj) {
            TeamSyncRecord r = new TeamSyncRecord();
            r.syncUuid = obj.optString("syncUuid", "");
            r.clubId = obj.optString("clubId", SyncPreferences.DEFAULT_LOCAL_CLUB_ID);
            r.userEmailId = obj.optString("userEmailId", "aditya.juet@gmail.com");
            r.teamName = obj.optString("teamName", "");
            r.teamNameNormalized = normalizeName(r.teamName);
            r.matchWon = obj.optInt("matchWon", 0);
            r.matchLost = obj.optInt("matchLost", 0);
            r.matchDraw = obj.optInt("matchDraw", 0);
            r.captainId = obj.optInt("captainId", 0);
            r.captainSyncUuid = obj.optString("captainSyncUuid", "");
            r.updatedAt = obj.optLong("updatedAt", 0L);
            r.isDeleted = obj.optBoolean("isDeleted", false);
            r.remoteVersion = obj.optLong("remoteVersion", 0L);
            return r;
        }

        public Map<String, Object> toFirestoreMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("syncUuid", syncUuid);
            map.put("clubId", clubId == null ? SyncPreferences.DEFAULT_LOCAL_CLUB_ID : clubId);
            map.put("userEmailId", userEmailId == null ? "" : userEmailId);
            map.put("teamName", teamName == null ? "" : teamName);
            map.put("teamNameNormalized", normalizeName(teamName));
            map.put("matchWon", matchWon);
            map.put("matchLost", matchLost);
            map.put("matchDraw", matchDraw);
            map.put("captainId", captainId);
            map.put("captainSyncUuid", captainSyncUuid == null ? "" : captainSyncUuid);
            map.put("updatedAt", updatedAt);
            map.put("isDeleted", isDeleted);
            map.put("remoteVersion", remoteVersion);
            return map;
        }

        public static TeamSyncRecord fromFirestoreMap(Map<String, Object> map) {
            TeamSyncRecord r = new TeamSyncRecord();
            r.syncUuid = asString(map.get("syncUuid"), "");
            r.clubId = asString(map.get("clubId"), SyncPreferences.DEFAULT_LOCAL_CLUB_ID);
            r.userEmailId = asString(map.get("userEmailId"), "aditya.juet@gmail.com");
            r.teamName = asString(map.get("teamName"), "");
            r.teamNameNormalized = normalizeName(r.teamName);
            r.matchWon = asInt(map.get("matchWon"), 0);
            r.matchLost = asInt(map.get("matchLost"), 0);
            r.matchDraw = asInt(map.get("matchDraw"), 0);
            r.captainId = asInt(map.get("captainId"), 0);
            r.captainSyncUuid = asString(map.get("captainSyncUuid"), "");
            r.updatedAt = asLong(map.get("updatedAt"), 0L);
            r.isDeleted = asBoolean(map.get("isDeleted"), false);
            r.remoteVersion = asLong(map.get("remoteVersion"), 0L);
            return r;
        }
    }

    /**
     * Synchronizable representation of a row in {@code PLAYER_DETAILS}.
     */
    public static final class PlayerSyncRecord {
        public int localPlayerId;
        public String syncUuid;
        public String clubId;
        public String userEmailId;
        public String playerName;
        public String playerNameNormalized;
        public String playerEmail;
        public int playerBatsman;
        public int playerBowler;
        public int playerWicketKeeper;
        public int playerDob;
        public int localTeamId;
        public String teamSyncUuid;
        public int totalRuns;
        public int fifty;
        public int hundred;
        public int twoHundred;
        public int ballsFaced;
        public int matchPlayed;
        public int oversBowled;
        public int wicketsTaken;
        public int maidenBowled;
        public int widesCount;
        public int noBallCount;
        public int runsGiven;
        public int catches;
        public long updatedAt;
        public boolean isDeleted;
        public long remoteVersion;
        public String syncStatus;

        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("syncUuid", syncUuid);
            obj.put("clubId", clubId == null ? SyncPreferences.DEFAULT_LOCAL_CLUB_ID : clubId);
            obj.put("userEmailId", userEmailId == null ? "" : userEmailId);
            obj.put("playerName", playerName == null ? "" : playerName);
            obj.put("playerNameNormalized", normalizeName(playerName));
            obj.put("playerEmail", playerEmail == null ? "NotSet" : playerEmail);
            obj.put("playerBatsman", playerBatsman);
            obj.put("playerBowler", playerBowler);
            obj.put("playerWicketKeeper", playerWicketKeeper);
            obj.put("playerDob", playerDob);
            obj.put("teamSyncUuid", teamSyncUuid == null ? "" : teamSyncUuid);
            obj.put("totalRuns", totalRuns);
            obj.put("fifty", fifty);
            obj.put("hundred", hundred);
            obj.put("twoHundred", twoHundred);
            obj.put("ballsFaced", ballsFaced);
            obj.put("matchPlayed", matchPlayed);
            obj.put("oversBowled", oversBowled);
            obj.put("wicketsTaken", wicketsTaken);
            obj.put("maidenBowled", maidenBowled);
            obj.put("widesCount", widesCount);
            obj.put("noBallCount", noBallCount);
            obj.put("runsGiven", runsGiven);
            obj.put("catches", catches);
            obj.put("updatedAt", updatedAt);
            obj.put("isDeleted", isDeleted);
            obj.put("remoteVersion", remoteVersion);
            return obj;
        }

        public static PlayerSyncRecord fromJson(JSONObject obj) {
            PlayerSyncRecord r = new PlayerSyncRecord();
            r.syncUuid = obj.optString("syncUuid", "");
            r.clubId = obj.optString("clubId", SyncPreferences.DEFAULT_LOCAL_CLUB_ID);
            r.userEmailId = obj.optString("userEmailId", "aditya.juet@gmail.com");
            r.playerName = obj.optString("playerName", "");
            r.playerNameNormalized = normalizeName(r.playerName);
            r.playerEmail = obj.optString("playerEmail", "NotSet");
            r.playerBatsman = obj.optInt("playerBatsman", 0);
            r.playerBowler = obj.optInt("playerBowler", 0);
            r.playerWicketKeeper = obj.optInt("playerWicketKeeper", 0);
            r.playerDob = obj.optInt("playerDob", 0);
            r.teamSyncUuid = obj.optString("teamSyncUuid", "");
            r.totalRuns = obj.optInt("totalRuns", 0);
            r.fifty = obj.optInt("fifty", 0);
            r.hundred = obj.optInt("hundred", 0);
            r.twoHundred = obj.optInt("twoHundred", 0);
            r.ballsFaced = obj.optInt("ballsFaced", 0);
            r.matchPlayed = obj.optInt("matchPlayed", 0);
            r.oversBowled = obj.optInt("oversBowled", 0);
            r.wicketsTaken = obj.optInt("wicketsTaken", 0);
            r.maidenBowled = obj.optInt("maidenBowled", 0);
            r.widesCount = obj.optInt("widesCount", 0);
            r.noBallCount = obj.optInt("noBallCount", 0);
            r.runsGiven = obj.optInt("runsGiven", 0);
            r.catches = obj.optInt("catches", 0);
            r.updatedAt = obj.optLong("updatedAt", 0L);
            r.isDeleted = obj.optBoolean("isDeleted", false);
            r.remoteVersion = obj.optLong("remoteVersion", 0L);
            return r;
        }

        public Map<String, Object> toFirestoreMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("syncUuid", syncUuid);
            map.put("clubId", clubId == null ? SyncPreferences.DEFAULT_LOCAL_CLUB_ID : clubId);
            map.put("userEmailId", userEmailId == null ? "" : userEmailId);
            map.put("playerName", playerName == null ? "" : playerName);
            map.put("playerNameNormalized", normalizeName(playerName));
            map.put("playerEmail", playerEmail == null ? "NotSet" : playerEmail);
            map.put("playerBatsman", playerBatsman);
            map.put("playerBowler", playerBowler);
            map.put("playerWicketKeeper", playerWicketKeeper);
            map.put("playerDob", playerDob);
            map.put("teamSyncUuid", teamSyncUuid == null ? "" : teamSyncUuid);
            map.put("totalRuns", totalRuns);
            map.put("fifty", fifty);
            map.put("hundred", hundred);
            map.put("twoHundred", twoHundred);
            map.put("ballsFaced", ballsFaced);
            map.put("matchPlayed", matchPlayed);
            map.put("oversBowled", oversBowled);
            map.put("wicketsTaken", wicketsTaken);
            map.put("maidenBowled", maidenBowled);
            map.put("widesCount", widesCount);
            map.put("noBallCount", noBallCount);
            map.put("runsGiven", runsGiven);
            map.put("catches", catches);
            map.put("updatedAt", updatedAt);
            map.put("isDeleted", isDeleted);
            map.put("remoteVersion", remoteVersion);
            return map;
        }

        public static PlayerSyncRecord fromFirestoreMap(Map<String, Object> map) {
            PlayerSyncRecord r = new PlayerSyncRecord();
            r.syncUuid = asString(map.get("syncUuid"), "");
            r.clubId = asString(map.get("clubId"), SyncPreferences.DEFAULT_LOCAL_CLUB_ID);
            r.userEmailId = asString(map.get("userEmailId"), "aditya.juet@gmail.com");
            r.playerName = asString(map.get("playerName"), "");
            r.playerNameNormalized = normalizeName(r.playerName);
            r.playerEmail = asString(map.get("playerEmail"), "NotSet");
            r.playerBatsman = asInt(map.get("playerBatsman"), 0);
            r.playerBowler = asInt(map.get("playerBowler"), 0);
            r.playerWicketKeeper = asInt(map.get("playerWicketKeeper"), 0);
            r.playerDob = asInt(map.get("playerDob"), 0);
            r.teamSyncUuid = asString(map.get("teamSyncUuid"), "");
            r.totalRuns = asInt(map.get("totalRuns"), 0);
            r.fifty = asInt(map.get("fifty"), 0);
            r.hundred = asInt(map.get("hundred"), 0);
            r.twoHundred = asInt(map.get("twoHundred"), 0);
            r.ballsFaced = asInt(map.get("ballsFaced"), 0);
            r.matchPlayed = asInt(map.get("matchPlayed"), 0);
            r.oversBowled = asInt(map.get("oversBowled"), 0);
            r.wicketsTaken = asInt(map.get("wicketsTaken"), 0);
            r.maidenBowled = asInt(map.get("maidenBowled"), 0);
            r.widesCount = asInt(map.get("widesCount"), 0);
            r.noBallCount = asInt(map.get("noBallCount"), 0);
            r.runsGiven = asInt(map.get("runsGiven"), 0);
            r.catches = asInt(map.get("catches"), 0);
            r.updatedAt = asLong(map.get("updatedAt"), 0L);
            r.isDeleted = asBoolean(map.get("isDeleted"), false);
            r.remoteVersion = asLong(map.get("remoteVersion"), 0L);
            return r;
        }
    }

    /**
     * Synchronizable representation of a row in {@code SCHEDULE_DETAILS}.
     */
    public static final class ScheduleSyncRecord {
        public int localScheduleId;
        public String syncUuid;
        public String clubId;
        public String userEmailId;
        public String matchDate;
        public int localTeamAId;
        public int localTeamBId;
        public String teamASyncUuid;
        public String teamBSyncUuid;
        public int matchOvers;
        public String matchStatus;
        public long updatedAt;
        public boolean isDeleted;
        public long remoteVersion;
        public String syncStatus;

        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("syncUuid", syncUuid);
            obj.put("clubId", clubId == null ? SyncPreferences.DEFAULT_LOCAL_CLUB_ID : clubId);
            obj.put("userEmailId", userEmailId == null ? "" : userEmailId);
            obj.put("matchDate", matchDate == null ? "" : matchDate);
            obj.put("teamASyncUuid", teamASyncUuid == null ? "" : teamASyncUuid);
            obj.put("teamBSyncUuid", teamBSyncUuid == null ? "" : teamBSyncUuid);
            obj.put("matchOvers", matchOvers);
            obj.put("matchStatus", matchStatus == null ? "SCHEDULED" : matchStatus);
            obj.put("updatedAt", updatedAt);
            obj.put("isDeleted", isDeleted);
            obj.put("remoteVersion", remoteVersion);
            return obj;
        }

        public static ScheduleSyncRecord fromJson(JSONObject obj) {
            ScheduleSyncRecord r = new ScheduleSyncRecord();
            r.syncUuid = obj.optString("syncUuid", "");
            r.clubId = obj.optString("clubId", SyncPreferences.DEFAULT_LOCAL_CLUB_ID);
            r.userEmailId = obj.optString("userEmailId", "aditya.juet@gmail.com");
            r.matchDate = obj.optString("matchDate", "");
            r.teamASyncUuid = obj.optString("teamASyncUuid", "");
            r.teamBSyncUuid = obj.optString("teamBSyncUuid", "");
            r.matchOvers = obj.optInt("matchOvers", 20);
            r.matchStatus = obj.optString("matchStatus", "SCHEDULED");
            r.updatedAt = obj.optLong("updatedAt", 0L);
            r.isDeleted = obj.optBoolean("isDeleted", false);
            r.remoteVersion = obj.optLong("remoteVersion", 0L);
            return r;
        }

        public Map<String, Object> toFirestoreMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("syncUuid", syncUuid);
            map.put("clubId", clubId == null ? SyncPreferences.DEFAULT_LOCAL_CLUB_ID : clubId);
            map.put("userEmailId", userEmailId == null ? "" : userEmailId);
            map.put("matchDate", matchDate == null ? "" : matchDate);
            map.put("teamASyncUuid", teamASyncUuid == null ? "" : teamASyncUuid);
            map.put("teamBSyncUuid", teamBSyncUuid == null ? "" : teamBSyncUuid);
            map.put("matchOvers", matchOvers);
            map.put("matchStatus", matchStatus == null ? "SCHEDULED" : matchStatus);
            map.put("updatedAt", updatedAt);
            map.put("isDeleted", isDeleted);
            map.put("remoteVersion", remoteVersion);
            return map;
        }

        public static ScheduleSyncRecord fromFirestoreMap(Map<String, Object> map) {
            ScheduleSyncRecord r = new ScheduleSyncRecord();
            r.syncUuid = asString(map.get("syncUuid"), "");
            r.clubId = asString(map.get("clubId"), SyncPreferences.DEFAULT_LOCAL_CLUB_ID);
            r.userEmailId = asString(map.get("userEmailId"), "aditya.juet@gmail.com");
            r.matchDate = asString(map.get("matchDate"), "");
            r.teamASyncUuid = asString(map.get("teamASyncUuid"), "");
            r.teamBSyncUuid = asString(map.get("teamBSyncUuid"), "");
            r.matchOvers = asInt(map.get("matchOvers"), 20);
            r.matchStatus = asString(map.get("matchStatus"), "SCHEDULED");
            r.updatedAt = asLong(map.get("updatedAt"), 0L);
            r.isDeleted = asBoolean(map.get("isDeleted"), false);
            r.remoteVersion = asLong(map.get("remoteVersion"), 0L);
            return r;
        }
    }

    /**
     * Domain-specific and deterministic conflict resolution policies.
     */
    public static final class ConflictResolutionPolicy {

        /**
         * Merges a dirty local Team record with a newly pulled remote Team record.
         * - Match counters (won/lost/draw) take {@code Math.max(local, remote)} to prevent regression.
         * - Profile fields (name, captain) use newer {@code updatedAt}.
         * - Active edits with newer {@code updatedAt} beat stale tombstones.
         */
        public static TeamSyncRecord resolveTeamConflict(TeamSyncRecord local, TeamSyncRecord remote) {
            if (local == null) {
                return remote;
            }
            if (remote == null) {
                return local;
            }
            boolean remoteNewer = isRemoteNewer(local.updatedAt, local.syncUuid, remote.updatedAt, remote.syncUuid);
            TeamSyncRecord merged = new TeamSyncRecord();
            merged.localTeamId = local.localTeamId;
            merged.syncUuid = remote.syncUuid != null && !remote.syncUuid.isEmpty() ? remote.syncUuid : local.syncUuid;
            merged.clubId = remote.clubId != null && !remote.clubId.isEmpty() ? remote.clubId : local.clubId;
            merged.userEmailId = remoteNewer ? remote.userEmailId : local.userEmailId;
            merged.teamName = remoteNewer ? remote.teamName : local.teamName;
            merged.teamNameNormalized = normalizeName(merged.teamName);
            merged.matchWon = Math.max(local.matchWon, remote.matchWon);
            merged.matchLost = Math.max(local.matchLost, remote.matchLost);
            merged.matchDraw = Math.max(local.matchDraw, remote.matchDraw);
            merged.captainId = remoteNewer ? remote.captainId : local.captainId;
            merged.captainSyncUuid = remoteNewer ? remote.captainSyncUuid : local.captainSyncUuid;
            merged.updatedAt = Math.max(local.updatedAt, remote.updatedAt);
            merged.isDeleted = remoteNewer ? remote.isDeleted : local.isDeleted;
            merged.remoteVersion = Math.max(local.remoteVersion, remote.remoteVersion);
            merged.syncStatus = STATUS_DIRTY;
            return merged;
        }

        /**
         * Merges a dirty local Player record with a newly pulled remote Player record.
         * - Career cumulative counters take {@code Math.max(local, remote)}.
         * - Role flags, email, and team assignment use newer {@code updatedAt}.
         */
        public static PlayerSyncRecord resolvePlayerConflict(PlayerSyncRecord local, PlayerSyncRecord remote) {
            if (local == null) {
                return remote;
            }
            if (remote == null) {
                return local;
            }
            boolean remoteNewer = isRemoteNewer(local.updatedAt, local.syncUuid, remote.updatedAt, remote.syncUuid);
            PlayerSyncRecord merged = new PlayerSyncRecord();
            merged.localPlayerId = local.localPlayerId;
            merged.syncUuid = remote.syncUuid != null && !remote.syncUuid.isEmpty() ? remote.syncUuid : local.syncUuid;
            merged.clubId = remote.clubId != null && !remote.clubId.isEmpty() ? remote.clubId : local.clubId;
            merged.userEmailId = remoteNewer ? remote.userEmailId : local.userEmailId;
            merged.playerName = remoteNewer ? remote.playerName : local.playerName;
            merged.playerNameNormalized = normalizeName(merged.playerName);
            merged.playerEmail = remoteNewer ? remote.playerEmail : local.playerEmail;
            merged.playerBatsman = remoteNewer ? remote.playerBatsman : local.playerBatsman;
            merged.playerBowler = remoteNewer ? remote.playerBowler : local.playerBowler;
            merged.playerWicketKeeper = remoteNewer ? remote.playerWicketKeeper : local.playerWicketKeeper;
            merged.playerDob = remoteNewer ? remote.playerDob : local.playerDob;
            merged.teamSyncUuid = remoteNewer ? remote.teamSyncUuid : local.teamSyncUuid;

            merged.totalRuns = Math.max(local.totalRuns, remote.totalRuns);
            merged.fifty = Math.max(local.fifty, remote.fifty);
            merged.hundred = Math.max(local.hundred, remote.hundred);
            merged.twoHundred = Math.max(local.twoHundred, remote.twoHundred);
            merged.ballsFaced = Math.max(local.ballsFaced, remote.ballsFaced);
            merged.matchPlayed = Math.max(local.matchPlayed, remote.matchPlayed);
            merged.oversBowled = Math.max(local.oversBowled, remote.oversBowled);
            merged.wicketsTaken = Math.max(local.wicketsTaken, remote.wicketsTaken);
            merged.maidenBowled = Math.max(local.maidenBowled, remote.maidenBowled);
            merged.widesCount = Math.max(local.widesCount, remote.widesCount);
            merged.noBallCount = Math.max(local.noBallCount, remote.noBallCount);
            merged.runsGiven = Math.max(local.runsGiven, remote.runsGiven);
            merged.catches = Math.max(local.catches, remote.catches);

            merged.updatedAt = Math.max(local.updatedAt, remote.updatedAt);
            merged.isDeleted = remoteNewer ? remote.isDeleted : local.isDeleted;
            merged.remoteVersion = Math.max(local.remoteVersion, remote.remoteVersion);
            merged.syncStatus = STATUS_DIRTY;
            return merged;
        }

        /**
         * Merges a dirty local Schedule record with a newly pulled remote Schedule record.
         * - Match status follows monotonic progression: {@code SCHEDULED < IN_PROGRESS < COMPLETED}.
         * - Fixture date, teams, and overs use newer {@code updatedAt}.
         */
        public static ScheduleSyncRecord resolveScheduleConflict(ScheduleSyncRecord local, ScheduleSyncRecord remote) {
            if (local == null) {
                return remote;
            }
            if (remote == null) {
                return local;
            }
            boolean remoteNewer = isRemoteNewer(local.updatedAt, local.syncUuid, remote.updatedAt, remote.syncUuid);
            ScheduleSyncRecord merged = new ScheduleSyncRecord();
            merged.localScheduleId = local.localScheduleId;
            merged.syncUuid = remote.syncUuid != null && !remote.syncUuid.isEmpty() ? remote.syncUuid : local.syncUuid;
            merged.clubId = remote.clubId != null && !remote.clubId.isEmpty() ? remote.clubId : local.clubId;
            merged.userEmailId = remoteNewer ? remote.userEmailId : local.userEmailId;
            merged.matchDate = remoteNewer ? remote.matchDate : local.matchDate;
            merged.teamASyncUuid = remoteNewer ? remote.teamASyncUuid : local.teamASyncUuid;
            merged.teamBSyncUuid = remoteNewer ? remote.teamBSyncUuid : local.teamBSyncUuid;
            merged.matchOvers = remoteNewer ? remote.matchOvers : local.matchOvers;
            merged.matchStatus = pickMoreAdvancedMatchStatus(local.matchStatus, remote.matchStatus);
            merged.updatedAt = Math.max(local.updatedAt, remote.updatedAt);
            merged.isDeleted = remoteNewer ? remote.isDeleted : local.isDeleted;
            merged.remoteVersion = Math.max(local.remoteVersion, remote.remoteVersion);
            merged.syncStatus = STATUS_DIRTY;
            return merged;
        }

        private static String pickMoreAdvancedMatchStatus(String statusA, String statusB) {
            int rankA = statusRank(statusA);
            int rankB = statusRank(statusB);
            return rankB >= rankA ? (statusB == null ? "SCHEDULED" : statusB) : (statusA == null ? "SCHEDULED" : statusA);
        }

        private static int statusRank(String status) {
            if (status == null) {
                return 0;
            }
            String s = status.trim().toUpperCase(Locale.US);
            if ("COMPLETED".equals(s) || "FINISHED".equals(s)) {
                return 2;
            }
            if ("IN_PROGRESS".equals(s) || "LIVE".equals(s)) {
                return 1;
            }
            return 0;
        }

        private static boolean isRemoteNewer(long localTs, String localId, long remoteTs, String remoteId) {
            if (remoteTs != localTs) {
                return remoteTs > localTs;
            }
            String a = localId == null ? "" : localId;
            String b = remoteId == null ? "" : remoteId;
            return b.compareTo(a) >= 0;
        }
    }

    /**
     * Summary returned by a sync execution.
     */
    public static final class SyncSummary {
        public boolean success;
        public boolean requiresMergeConfirmation;
        public int localExistingCount;
        public int remoteExistingCount;
        public int pushedCount;
        public int pulledCount;
        public int conflictsResolvedCount;
        public String message;
    }

    private static String asString(Object val, String fallback) {
        return val != null ? String.valueOf(val) : fallback;
    }

    private static int asInt(Object val, int fallback) {
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        if (val instanceof String) {
            try {
                return Integer.parseInt((String) val);
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    private static long asLong(Object val, long fallback) {
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        if (val instanceof String) {
            try {
                return Long.parseLong((String) val);
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    private static boolean asBoolean(Object val, boolean fallback) {
        if (val instanceof Boolean) {
            return (Boolean) val;
        }
        if (val instanceof String) {
            return Boolean.parseBoolean((String) val);
        }
        return fallback;
    }
}
