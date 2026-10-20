package com.example.android.cricketscoreboard.sync;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Centralized, typed preferences for optional Cloud Firestore synchronization.
 * Stored in {@code cricket_sync_prefs}, strictly separated from UI personalization
 * preferences ({@code cricket_ui_prefs}) and operational SQLite tables ({@code cricketScore_DB}).
 */
public final class SyncPreferences {

    public static final String PREFS_NAME = "cricket_sync_prefs";
    public static final String DEFAULT_LOCAL_CLUB_ID = "local_default_club";

    private static final String KEY_SYNC_ENABLED = "sync_enabled";
    private static final String KEY_CLUB_ID = "club_id";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_LAST_SYNC_TIME = "last_sync_timestamp";
    private static final String KEY_LAST_SYNC_STATUS = "last_sync_status";
    private static final String KEY_LAST_SYNC_ERROR = "last_sync_error";
    private static final String KEY_PULL_CURSOR_PREFIX = "pull_cursor_";
    private static final String KEY_INITIAL_MIGRATION_PREFIX = "initial_migration_done_";
    private static final String KEY_MERGE_CONFIRMED_PREFIX = "merge_confirmed_";

    private final SharedPreferences prefs;

    private SyncPreferences(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static SyncPreferences get(Context context) {
        return new SyncPreferences(context);
    }

    public boolean isSyncEnabled() {
        return prefs.getBoolean(KEY_SYNC_ENABLED, false);
    }

    public void setSyncEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SYNC_ENABLED, enabled).apply();
    }

    public String getClubId() {
        String raw = prefs.getString(KEY_CLUB_ID, DEFAULT_LOCAL_CLUB_ID);
        if (raw == null || raw.trim().isEmpty()) {
            return DEFAULT_LOCAL_CLUB_ID;
        }
        return sanitizeClubId(raw);
    }

    public void setClubId(String clubId) {
        prefs.edit().putString(KEY_CLUB_ID, sanitizeClubId(clubId)).apply();
    }

    public static String sanitizeClubId(String rawClubId) {
        if (rawClubId == null || rawClubId.trim().isEmpty()) {
            return DEFAULT_LOCAL_CLUB_ID;
        }
        String cleaned = rawClubId.trim().toLowerCase()
                .replaceAll("[^a-z0-9_\\-]", "_");
        return cleaned.isEmpty() ? DEFAULT_LOCAL_CLUB_ID : cleaned;
    }

    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, "");
    }

    public void setUserEmail(String email) {
        prefs.edit().putString(KEY_USER_EMAIL, email == null ? "" : email.trim()).apply();
    }

    public long getLastSyncTimestamp() {
        return prefs.getLong(KEY_LAST_SYNC_TIME, 0L);
    }

    public void setLastSyncTimestamp(long epochMillis) {
        prefs.edit().putLong(KEY_LAST_SYNC_TIME, Math.max(0L, epochMillis)).apply();
    }

    public String getLastSyncStatus() {
        return prefs.getString(KEY_LAST_SYNC_STATUS, "Offline Only (Local)");
    }

    public void setLastSyncStatus(String status) {
        prefs.edit().putString(KEY_LAST_SYNC_STATUS, status == null ? "" : status).apply();
    }

    public String getLastSyncError() {
        return prefs.getString(KEY_LAST_SYNC_ERROR, "");
    }

    public void setLastSyncError(String error) {
        prefs.edit().putString(KEY_LAST_SYNC_ERROR, error == null ? "" : error).apply();
    }

    public long getLastPullCursor(String clubId) {
        return prefs.getLong(KEY_PULL_CURSOR_PREFIX + sanitizeClubId(clubId), 0L);
    }

    public void setLastPullCursor(String clubId, long timestamp) {
        prefs.edit().putLong(
                KEY_PULL_CURSOR_PREFIX + sanitizeClubId(clubId),
                Math.max(0L, timestamp)
        ).apply();
    }

    public boolean isInitialMigrationCompleted(String clubId) {
        return prefs.getBoolean(KEY_INITIAL_MIGRATION_PREFIX + sanitizeClubId(clubId), false);
    }

    public void setInitialMigrationCompleted(String clubId, boolean completed) {
        prefs.edit().putBoolean(
                KEY_INITIAL_MIGRATION_PREFIX + sanitizeClubId(clubId),
                completed
        ).apply();
    }

    public boolean isMergeConfirmedForClub(String clubId) {
        return prefs.getBoolean(KEY_MERGE_CONFIRMED_PREFIX + sanitizeClubId(clubId), false);
    }

    public void setMergeConfirmedForClub(String clubId, boolean confirmed) {
        prefs.edit().putBoolean(
                KEY_MERGE_CONFIRMED_PREFIX + sanitizeClubId(clubId),
                confirmed
        ).apply();
    }

    public void clearAll() {
        prefs.edit().clear().apply();
    }
}
