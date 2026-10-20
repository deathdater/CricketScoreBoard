package com.example.android.cricketscoreboard.sync;

import android.content.Context;
import android.util.Log;

import com.example.android.cricketscoreboard.DatabaseHandler;
import com.example.android.cricketscoreboard.sync.SyncRecordModels.PlayerSyncRecord;
import com.example.android.cricketscoreboard.sync.SyncRecordModels.ScheduleSyncRecord;
import com.example.android.cricketscoreboard.sync.SyncRecordModels.TeamSyncRecord;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Manages safe local SQLite snapshots ({@code cricketScore_DB.pre_sync.bak}) prior to
 * first-time cloud synchronization or club switching, enabling one-tap rollback without
 * corrupting open SQLite WAL descriptors.
 */
public final class DatabaseBackupManager {

    private static final String TAG = "DatabaseBackupManager";
    private static final String DB_NAME = "cricketScore_DB";
    private static final String BACKUP_FILE_NAME = "cricketScore_DB.pre_sync.bak";

    private DatabaseBackupManager() {
    }

    public static File getBackupFile(Context context) {
        File dbFile = context.getDatabasePath(DB_NAME);
        return new File(dbFile.getParentFile(), BACKUP_FILE_NAME);
    }

    public static boolean hasPreSyncBackup(Context context) {
        File backup = getBackupFile(context);
        return backup.exists() && backup.length() > 0;
    }

    public static void clearPreSyncBackup(Context context) {
        File backup = getBackupFile(context);
        if (backup.exists()) {
            //noinspection ResultOfMethodCallIgnored
            backup.delete();
        }
    }

    /**
     * Serializes the active {@code cricketScore_DB} tables to {@code cricketScore_DB.pre_sync.bak}.
     */
    public static boolean createPreSyncBackup(Context context) {
        try {
            DatabaseHandler db = new DatabaseHandler(context);
            List<TeamSyncRecord> teams = db.getAllTeamsForSync();
            List<PlayerSyncRecord> players = db.getAllPlayersForSync();
            List<ScheduleSyncRecord> schedules = db.getAllSchedulesForSync();

            JSONObject root = new JSONObject();
            JSONArray teamsArr = new JSONArray();
            for (TeamSyncRecord t : teams) {
                JSONObject obj = t.toJson();
                obj.put("syncStatus", t.syncStatus == null ? SyncRecordModels.STATUS_DIRTY : t.syncStatus);
                teamsArr.put(obj);
            }
            JSONArray playersArr = new JSONArray();
            for (PlayerSyncRecord p : players) {
                JSONObject obj = p.toJson();
                obj.put("syncStatus", p.syncStatus == null ? SyncRecordModels.STATUS_DIRTY : p.syncStatus);
                playersArr.put(obj);
            }
            JSONArray schedulesArr = new JSONArray();
            for (ScheduleSyncRecord s : schedules) {
                JSONObject obj = s.toJson();
                obj.put("syncStatus", s.syncStatus == null ? SyncRecordModels.STATUS_DIRTY : s.syncStatus);
                schedulesArr.put(obj);
            }
            root.put("teams", teamsArr);
            root.put("players", playersArr);
            root.put("schedules", schedulesArr);

            File backupFile = getBackupFile(context);
            File parent = backupFile.getParentFile();
            if (parent != null && !parent.exists()) {
                //noinspection ResultOfMethodCallIgnored
                parent.mkdirs();
            }
            try (FileOutputStream fos = new FileOutputStream(backupFile, false)) {
                fos.write(root.toString().getBytes(StandardCharsets.UTF_8));
                fos.flush();
            }
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Failed to create pre-sync SQLite backup", e);
            return false;
        }
    }

    /**
     * Restores {@code cricketScore_DB.pre_sync.bak} into {@code cricketScore_DB} cleanly via
     * {@link DatabaseHandler} in topological dependency order ({@code TEAM -> PLAYER -> SCHEDULE}).
     */
    public static boolean restorePreSyncBackup(Context context) {
        File backupFile = getBackupFile(context);
        if (!backupFile.exists() || backupFile.length() == 0) {
            return false;
        }
        try {
            StringBuilder sb = new StringBuilder();
            try (FileInputStream fis = new FileInputStream(backupFile);
                 InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
                 BufferedReader reader = new BufferedReader(isr)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            JSONObject root = new JSONObject(sb.toString());
            DatabaseHandler db = new DatabaseHandler(context);
            db.clearLocalDataForClubSwitch();

            JSONArray teamsArr = root.optJSONArray("teams");
            if (teamsArr != null) {
                for (int i = 0; i < teamsArr.length(); i++) {
                    JSONObject obj = teamsArr.getJSONObject(i);
                    TeamSyncRecord t = TeamSyncRecord.fromJson(obj);
                    boolean synced = SyncRecordModels.STATUS_SYNCED.equals(
                            obj.optString("syncStatus", SyncRecordModels.STATUS_DIRTY));
                    db.upsertTeamFromRemote(t, synced);
                }
            }

            JSONArray playersArr = root.optJSONArray("players");
            if (playersArr != null) {
                for (int i = 0; i < playersArr.length(); i++) {
                    JSONObject obj = playersArr.getJSONObject(i);
                    PlayerSyncRecord p = PlayerSyncRecord.fromJson(obj);
                    boolean synced = SyncRecordModels.STATUS_SYNCED.equals(
                            obj.optString("syncStatus", SyncRecordModels.STATUS_DIRTY));
                    db.upsertPlayerFromRemote(p, synced);
                }
            }

            JSONArray schedulesArr = root.optJSONArray("schedules");
            if (schedulesArr != null) {
                for (int i = 0; i < schedulesArr.length(); i++) {
                    JSONObject obj = schedulesArr.getJSONObject(i);
                    ScheduleSyncRecord s = ScheduleSyncRecord.fromJson(obj);
                    boolean synced = SyncRecordModels.STATUS_SYNCED.equals(
                            obj.optString("syncStatus", SyncRecordModels.STATUS_DIRTY));
                    db.upsertScheduleFromRemote(s, synced);
                }
            }
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Failed to restore pre-sync SQLite backup", e);
            return false;
        }
    }
}
