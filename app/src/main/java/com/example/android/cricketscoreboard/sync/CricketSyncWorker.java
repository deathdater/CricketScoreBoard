package com.example.android.cricketscoreboard.sync;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.android.cricketscoreboard.sync.SyncRecordModels.SyncSummary;

import java.util.concurrent.TimeUnit;

/**
 * Persistent background {@link Worker} that runs {@link SyncEngine#syncNow(boolean)}
 * when network connectivity is available, with exponential backoff on transient failures.
 */
public final class CricketSyncWorker extends Worker {

    public static final String UNIQUE_WORK_NAME = "cricket_cloud_sync_work";

    public CricketSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        SyncPreferences prefs = SyncPreferences.get(context);
        if (!prefs.isSyncEnabled()) {
            return Result.success();
        }
        SyncSummary summary = SyncEngine.getInstance(context).syncNow(false);
        if (summary.success || summary.requiresMergeConfirmation) {
            return Result.success();
        }
        return Result.retry();
    }

    /**
     * Enqueues a background synchronization pass via {@link WorkManager} if Cloud Sync is enabled.
     */
    public static void enqueueIfEnabled(Context context) {
        if (context == null || !SyncPreferences.get(context).isSyncEnabled()) {
            return;
        }
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(CricketSyncWorker.class)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build();

        WorkManager.getInstance(context.getApplicationContext())
                .enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.KEEP, request);
    }
}
