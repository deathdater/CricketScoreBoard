package com.example.android.cricketscoreboard.sync;

import android.content.Context;
import android.util.Log;

import com.example.android.cricketscoreboard.sync.SyncRecordModels.PlayerSyncRecord;
import com.example.android.cricketscoreboard.sync.SyncRecordModels.ScheduleSyncRecord;
import com.example.android.cricketscoreboard.sync.SyncRecordModels.TeamSyncRecord;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Production Cloud Firestore + Firebase Authentication implementation of {@link SyncRemoteStore}.
 * Uses {@code /clubs/{clubId}/{teams|players|schedules}/{syncUuid}} and degrades gracefully
 * to offline mode when {@code google-services.json} is not present.
 */
public final class FirestoreRemoteStore implements SyncRemoteStore {

    private static final String TAG = "FirestoreRemoteStore";
    private static final long TIMEOUT_SECONDS = 15L;

    private final Context appContext;

    public FirestoreRemoteStore(Context context) {
        this.appContext = context.getApplicationContext();
    }

    /**
     * Checks whether a {@code FirebaseApp} is configured at runtime without throwing.
     */
    public static boolean isFirebaseConfigured(Context context) {
        try {
            if (!FirebaseApp.getApps(context.getApplicationContext()).isEmpty()) {
                return true;
            }
            FirebaseApp app = FirebaseApp.initializeApp(context.getApplicationContext());
            return app != null;
        } catch (Throwable t) {
            Log.i(TAG, "Firebase not configured in this build; using local/offline mode.");
            return false;
        }
    }

    public static String getAuthenticatedUserEmail(Context context) {
        if (!isFirebaseConfigured(context)) {
            return "";
        }
        try {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            return user != null && user.getEmail() != null ? user.getEmail() : "";
        } catch (Throwable t) {
            return "";
        }
    }

    public static String getAuthenticatedUserUid(Context context) {
        if (!isFirebaseConfigured(context)) {
            return "";
        }
        try {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            return user != null ? user.getUid() : "";
        } catch (Throwable t) {
            return "";
        }
    }

    public static AuthResult signInWithEmailPassword(Context context, String email, String password) throws Exception {
        if (!isFirebaseConfigured(context)) {
            throw new IllegalStateException("Firebase is not configured (missing google-services.json).");
        }
        return Tasks.await(
                FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password),
                TIMEOUT_SECONDS,
                TimeUnit.SECONDS
        );
    }

    public static AuthResult registerWithEmailPassword(Context context, String email, String password) throws Exception {
        if (!isFirebaseConfigured(context)) {
            throw new IllegalStateException("Firebase is not configured (missing google-services.json).");
        }
        return Tasks.await(
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password),
                TIMEOUT_SECONDS,
                TimeUnit.SECONDS
        );
    }

    public static void signOutFirebase(Context context) {
        if (isFirebaseConfigured(context)) {
            try {
                FirebaseAuth.getInstance().signOut();
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public boolean isAvailable() {
        return isFirebaseConfigured(appContext);
    }

    @Override
    public String getBackendName() {
        return "Cloud Firestore";
    }

    @Override
    public void upsertTeam(String clubId, TeamSyncRecord record) throws Exception {
        String safeClub = SyncPreferences.sanitizeClubId(clubId);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Tasks.await(
                db.collection("clubs")
                        .document(safeClub)
                        .collection("teams")
                        .document(record.syncUuid)
                        .set(record.toFirestoreMap(), SetOptions.merge()),
                TIMEOUT_SECONDS,
                TimeUnit.SECONDS
        );
    }

    @Override
    public void upsertPlayer(String clubId, PlayerSyncRecord record) throws Exception {
        String safeClub = SyncPreferences.sanitizeClubId(clubId);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Tasks.await(
                db.collection("clubs")
                        .document(safeClub)
                        .collection("players")
                        .document(record.syncUuid)
                        .set(record.toFirestoreMap(), SetOptions.merge()),
                TIMEOUT_SECONDS,
                TimeUnit.SECONDS
        );
    }

    @Override
    public void upsertSchedule(String clubId, ScheduleSyncRecord record) throws Exception {
        String safeClub = SyncPreferences.sanitizeClubId(clubId);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Tasks.await(
                db.collection("clubs")
                        .document(safeClub)
                        .collection("schedules")
                        .document(record.syncUuid)
                        .set(record.toFirestoreMap(), SetOptions.merge()),
                TIMEOUT_SECONDS,
                TimeUnit.SECONDS
        );
    }

    @Override
    public List<TeamSyncRecord> fetchTeamsUpdatedSince(String clubId, long sinceTimestamp) throws Exception {
        String safeClub = SyncPreferences.sanitizeClubId(clubId);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Query query = db.collection("clubs")
                .document(safeClub)
                .collection("teams")
                .whereGreaterThan("updatedAt", sinceTimestamp)
                .orderBy("updatedAt", Query.Direction.ASCENDING)
                .limit(200);
        QuerySnapshot snap = Tasks.await(query.get(), TIMEOUT_SECONDS, TimeUnit.SECONDS);
        List<TeamSyncRecord> list = new ArrayList<>();
        if (snap != null) {
            for (DocumentSnapshot doc : snap.getDocuments()) {
                Map<String, Object> data = doc.getData();
                if (data != null) {
                    list.add(TeamSyncRecord.fromFirestoreMap(data));
                }
            }
        }
        return list;
    }

    @Override
    public List<PlayerSyncRecord> fetchPlayersUpdatedSince(String clubId, long sinceTimestamp) throws Exception {
        String safeClub = SyncPreferences.sanitizeClubId(clubId);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Query query = db.collection("clubs")
                .document(safeClub)
                .collection("players")
                .whereGreaterThan("updatedAt", sinceTimestamp)
                .orderBy("updatedAt", Query.Direction.ASCENDING)
                .limit(200);
        QuerySnapshot snap = Tasks.await(query.get(), TIMEOUT_SECONDS, TimeUnit.SECONDS);
        List<PlayerSyncRecord> list = new ArrayList<>();
        if (snap != null) {
            for (DocumentSnapshot doc : snap.getDocuments()) {
                Map<String, Object> data = doc.getData();
                if (data != null) {
                    list.add(PlayerSyncRecord.fromFirestoreMap(data));
                }
            }
        }
        return list;
    }

    @Override
    public List<ScheduleSyncRecord> fetchSchedulesUpdatedSince(String clubId, long sinceTimestamp) throws Exception {
        String safeClub = SyncPreferences.sanitizeClubId(clubId);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Query query = db.collection("clubs")
                .document(safeClub)
                .collection("schedules")
                .whereGreaterThan("updatedAt", sinceTimestamp)
                .orderBy("updatedAt", Query.Direction.ASCENDING)
                .limit(200);
        QuerySnapshot snap = Tasks.await(query.get(), TIMEOUT_SECONDS, TimeUnit.SECONDS);
        List<ScheduleSyncRecord> list = new ArrayList<>();
        if (snap != null) {
            for (DocumentSnapshot doc : snap.getDocuments()) {
                Map<String, Object> data = doc.getData();
                if (data != null) {
                    list.add(ScheduleSyncRecord.fromFirestoreMap(data));
                }
            }
        }
        return list;
    }

    @Override
    public int countRemoteRecords(String clubId) throws Exception {
        int teams = fetchTeamsUpdatedSince(clubId, 0L).size();
        int players = fetchPlayersUpdatedSince(clubId, 0L).size();
        int schedules = fetchSchedulesUpdatedSince(clubId, 0L).size();
        return teams + players + schedules;
    }
}
