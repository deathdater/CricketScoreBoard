package com.example.android.cricketscoreboard.sync;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.example.android.cricketscoreboard.DatabaseHandler;
import com.example.android.cricketscoreboard.R;
import com.example.android.cricketscoreboard.sync.SyncRecordModels.SyncSummary;

import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.Executors;

/**
 * Interactive Cloud Synchronization & Club Workspace dialog.
 * Allows enabling/disabling optional cloud sync, triggering manual synchronization,
 * confirming first-time local/cloud record merges, switching clubs safely without
 * data leakage, and restoring the pre-sync local SQLite backup.
 */
public final class CloudSyncDialog {

    private CloudSyncDialog() {
    }

    public static void show(@NonNull Activity activity, Runnable onDataChangedCallback) {
        SyncPreferences prefs = SyncPreferences.get(activity);
        SyncEngine engine = SyncEngine.getInstance(activity);
        DatabaseHandler db = new DatabaseHandler(activity);

        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_cloud_sync, null);

        TextView backendText = view.findViewById(R.id.sync_backend_text);
        TextView statusText = view.findViewById(R.id.sync_status_text);
        TextView pendingText = view.findViewById(R.id.sync_pending_text);
        TextView lastTimeText = view.findViewById(R.id.sync_last_time_text);
        CheckBox enableBox = view.findViewById(R.id.switch_sync_enabled);
        EditText clubIdEdit = view.findViewById(R.id.edit_sync_club_id);
        Button switchClubBtn = view.findViewById(R.id.btn_switch_club);
        Button syncNowBtn = view.findViewById(R.id.btn_sync_now);
        Button restoreBackupBtn = view.findViewById(R.id.btn_restore_backup);

        enableBox.setChecked(prefs.isSyncEnabled());
        clubIdEdit.setText(prefs.getClubId());

        Runnable refreshDialogStatus = () -> {
            String backendName = engine.getActiveRemoteStore().getBackendName();
            backendText.setText("Cloud Layer: " + backendName + " • Club: " + prefs.getClubId());
            String err = prefs.getLastSyncError();
            String status = prefs.getLastSyncStatus();
            statusText.setText("Status: " + (err != null && !err.isEmpty() ? err : status));
            int pending = db.getPendingOutboxCount();
            pendingText.setText("Pending Unsynced Changes: " + pending);
            long lastTs = prefs.getLastSyncTimestamp();
            if (lastTs > 0L) {
                String formatted = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                        .format(new Date(lastTs));
                lastTimeText.setText("Last Confirmed Sync: " + formatted);
            } else {
                lastTimeText.setText("Last Confirmed Sync: Never");
            }
            restoreBackupBtn.setEnabled(DatabaseBackupManager.hasPreSyncBackup(activity));
        };

        refreshDialogStatus.run();

        enableBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.setSyncEnabled(isChecked);
            prefs.setLastSyncStatus(isChecked ? "Cloud Sync enabled" : "Offline Only (Local)");
            refreshDialogStatus.run();
            if (isChecked) {
                CricketSyncWorker.enqueueIfEnabled(activity);
            }
            if (onDataChangedCallback != null) {
                onDataChangedCallback.run();
            }
        });

        switchClubBtn.setOnClickListener(v -> {
            String requestedClub = SyncPreferences.sanitizeClubId(clubIdEdit.getText().toString());
            String currentClub = prefs.getClubId();
            if (requestedClub.equals(currentClub)) {
                Toast.makeText(activity, "Already on club: " + requestedClub, Toast.LENGTH_SHORT).show();
                return;
            }
            int pendingCount = db.getPendingOutboxCount();
            String message = pendingCount > 0
                    ? "You have " + pendingCount + " unsynced local changes in '" + currentClub
                    + "'. Switching to '" + requestedClub + "' will back up and isolate local data. Continue?"
                    : "Switch active club workspace from '" + currentClub + "' to '" + requestedClub + "'?";

            new AlertDialog.Builder(activity)
                    .setTitle("Switch Club Workspace")
                    .setMessage(message)
                    .setPositiveButton("Switch Club", (d, w) -> {
                        engine.switchClubIsolated(requestedClub);
                        clubIdEdit.setText(requestedClub);
                        refreshDialogStatus.run();
                        if (onDataChangedCallback != null) {
                            onDataChangedCallback.run();
                        }
                        Toast.makeText(activity, "Switched to club: " + requestedClub, Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        syncNowBtn.setOnClickListener(v -> {
            String enteredClub = SyncPreferences.sanitizeClubId(clubIdEdit.getText().toString());
            if (!enteredClub.isEmpty() && SyncPreferences.DEFAULT_LOCAL_CLUB_ID.equals(prefs.getClubId())) {
                prefs.setClubId(enteredClub);
            }
            if (!prefs.isSyncEnabled()) {
                prefs.setSyncEnabled(true);
                enableBox.setChecked(true);
            }
            runSyncAsync(activity, engine, false, refreshDialogStatus, onDataChangedCallback);
        });

        restoreBackupBtn.setOnClickListener(v -> {
            if (!DatabaseBackupManager.hasPreSyncBackup(activity)) {
                Toast.makeText(activity, "No pre-sync backup available.", Toast.LENGTH_SHORT).show();
                return;
            }
            new AlertDialog.Builder(activity)
                    .setTitle("Restore Pre-Sync Local Backup")
                    .setMessage("Disable Cloud Sync and restore the local SQLite database to its exact state before synchronization?")
                    .setPositiveButton("Restore Backup", (d, w) -> {
                        prefs.setSyncEnabled(false);
                        enableBox.setChecked(false);
                        boolean ok = DatabaseBackupManager.restorePreSyncBackup(activity);
                        prefs.setLastSyncStatus(ok ? "Restored pre-sync local backup" : "Backup restore failed");
                        refreshDialogStatus.run();
                        if (onDataChangedCallback != null) {
                            onDataChangedCallback.run();
                        }
                        Toast.makeText(activity,
                                ok ? "Pre-sync local database restored" : "Failed to restore backup",
                                Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        new AlertDialog.Builder(activity)
                .setTitle("Cloud Synchronization")
                .setView(view)
                .setPositiveButton("Close", (d, w) -> {
                    if (onDataChangedCallback != null) {
                        onDataChangedCallback.run();
                    }
                })
                .show();
    }

    private static void runSyncAsync(Activity activity, SyncEngine engine, boolean forceConfirmMerge,
                                     Runnable refreshDialogStatus, Runnable onDataChangedCallback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            SyncSummary summary = engine.syncNow(forceConfirmMerge);
            activity.runOnUiThread(() -> {
                refreshDialogStatus.run();
                if (onDataChangedCallback != null) {
                    onDataChangedCallback.run();
                }
                if (summary.requiresMergeConfirmation) {
                    new AlertDialog.Builder(activity)
                            .setTitle("Confirm First-Time Cloud Merge")
                            .setMessage(summary.message
                                    + "\n\nA local backup has been created automatically. Merge local and cloud records without losing any data?")
                            .setPositiveButton("Merge & Sync", (d, w) ->
                                    runSyncAsync(activity, engine, true, refreshDialogStatus, onDataChangedCallback))
                            .setNegativeButton("Keep Offline", null)
                            .show();
                } else {
                    Toast.makeText(activity, summary.message, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
