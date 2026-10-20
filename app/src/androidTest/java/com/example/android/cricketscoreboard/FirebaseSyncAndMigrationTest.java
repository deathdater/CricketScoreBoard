package com.example.android.cricketscoreboard;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.android.cricketscoreboard.sync.DatabaseBackupManager;
import com.example.android.cricketscoreboard.sync.InMemoryRemoteStore;
import com.example.android.cricketscoreboard.sync.SyncEngine;
import com.example.android.cricketscoreboard.sync.SyncPreferences;
import com.example.android.cricketscoreboard.sync.SyncRecordModels.ConflictResolutionPolicy;
import com.example.android.cricketscoreboard.sync.SyncRecordModels.PlayerSyncRecord;
import com.example.android.cricketscoreboard.sync.SyncRecordModels.ScheduleSyncRecord;
import com.example.android.cricketscoreboard.sync.SyncRecordModels.SyncSummary;
import com.example.android.cricketscoreboard.sync.SyncRecordModels.TeamSyncRecord;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * End-to-end instrumentation tests verifying Phase 3–7 Firebase synchronization requirements:
 * <ul>
 *   <li>Offline-first local persistence and {@code SYNC_OUTBOX} tracking</li>
 *   <li>Topological dependency ordering ({@code TEAM -> PLAYER -> SCHEDULE}) and FK UUID translation</li>
 *   <li>Idempotent retries after transient network interruptions without duplication</li>
 *   <li>First-time sync merge confirmation, natural-key deduplication, and pre-sync backup rollback</li>
 *   <li>Deterministic domain-specific conflict resolution and tombstone soft-deletes</li>
 *   <li>Multi-club account switching and data isolation</li>
 * </ul>
 */
@RunWith(AndroidJUnit4.class)
public class FirebaseSyncAndMigrationTest {

    private Context context;
    private DatabaseHandler db;
    private SyncPreferences syncPrefs;
    private SyncEngine syncEngine;
    private InMemoryRemoteStore remoteStore;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        db = new DatabaseHandler(context);
        db.clearLocalDataForClubSwitch();
        DatabaseBackupManager.clearPreSyncBackup(context);
        syncPrefs = SyncPreferences.get(context);
        syncPrefs.clearAll();
        remoteStore = InMemoryRemoteStore.getInstance();
        remoteStore.clearAll();
        syncEngine = SyncEngine.getInstance(context);
        syncEngine.setRemoteStoreOverride(remoteStore);
    }

    @After
    public void tearDown() {
        remoteStore.clearAll();
        syncPrefs.clearAll();
    }

    @Test
    public void offlineWrites_enqueueOutboxAndSyncEndToEndInDependencyOrder() throws Exception {
        String clubId = "test_club_e2e";
        syncPrefs.setClubId(clubId);
        syncPrefs.setSyncEnabled(false); // Start offline

        // 1. Perform local offline writes (Teams, Players, Roster Assignment, Match Schedule)
        db.insertTeamBasicDetails("Alpha Warriors");
        db.insertTeamBasicDetails("Beta Knights");
        int teamAId = db.getTeamId("Alpha Warriors");
        int teamBId = db.getTeamId("Beta Knights");
        assertTrue(teamAId > 0);
        assertTrue(teamBId > 0);

        db.addPlayerDetails("aditya.juet@gmail.com", "Rohit Opener", 1, 0, 0);
        int playerId = db.getPlayerId("Rohit Opener");
        assertTrue(playerId > 0);
        db.addPlayerInTeam(playerId, teamAId);

        db.addSchedule("aditya.juet@gmail.com", "2026/11/15", teamAId, teamBId, 20);

        // Verify outbox tracked all 4 distinct entities (2 teams, 1 player coalesced, 1 schedule)
        assertEquals(4, db.getPendingOutboxCount());

        // 2. Enable sync and run SyncEngine
        syncPrefs.setSyncEnabled(true);
        SyncSummary pushSummary = syncEngine.syncNow(true);
        assertTrue(pushSummary.message, pushSummary.success);
        assertEquals(0, db.getPendingOutboxCount());
        assertEquals(4, remoteStore.countRemoteRecords(clubId));

        // 3. Simulate a second device pulling from the same club into a fresh local SQLite DB
        db.clearLocalDataForClubSwitch();
        syncPrefs.setLastPullCursor(clubId, 0L);
        assertEquals(0, db.getAllTeamNames().size());

        SyncSummary pullSummary = syncEngine.syncNow(true);
        assertTrue(pullSummary.message, pullSummary.success);
        assertEquals(2, db.getAllTeamNames().size());
        assertTrue(db.getAllTeamNames().contains("Alpha Warriors"));
        assertTrue(db.getAllTeamNames().contains("Beta Knights"));

        // Verify foreign key PLAYER_TEAM_ID was accurately resolved to the new local TEAM_ID
        int pulledTeamAId = db.getTeamId("Alpha Warriors");
        List<String> teamAPlayers = db.viewAllPlayers(pulledTeamAId);
        assertNotNull(teamAPlayers);
        assertTrue(teamAPlayers.contains("Rohit Opener"));

        // Verify schedule foreign keys (SCHEDULE_TEAMA_ID, SCHEDULE_TEAMB_ID) resolved accurately
        assertEquals(1, db.getNumberOfSchedules());
        String[][] schedules = db.viewAllSchedules();
        assertEquals("2026/11/15", schedules[0][0]);
        assertEquals(String.valueOf(pulledTeamAId), schedules[0][1]);
        assertEquals(String.valueOf(db.getTeamId("Beta Knights")), schedules[0][2]);
        assertEquals("20", schedules[0][3]);
    }

    @Test
    public void transientFailuresAndRetries_areIdempotentWithoutDuplicates() {
        String clubId = "test_club_retry";
        syncPrefs.setClubId(clubId);
        syncPrefs.setSyncEnabled(true);
        syncPrefs.setInitialMigrationCompleted(clubId, true);

        db.insertTeamBasicDetails("Resilient XI");
        assertEquals(1, db.getPendingOutboxCount());

        // Inject 1 transient network failure
        remoteStore.setSimulateTransientFailureCount(1);
        SyncSummary failedAttempt = syncEngine.syncNow(false);
        assertFalse(failedAttempt.success);
        // Record must still be pending in outbox (never marked synced before remote ack)
        assertEquals(1, db.getPendingOutboxCount());
        assertEquals(1, db.getPendingOutboxItems().get(0).retryCount);

        // Retry when connection recovers
        SyncSummary retryAttempt = syncEngine.syncNow(false);
        assertTrue(retryAttempt.message, retryAttempt.success);
        assertEquals(0, db.getPendingOutboxCount());
        assertEquals(1, remoteStore.countRemoteRecords(clubId));

        // Repeated sync must be idempotent and never create duplicates
        SyncSummary thirdAttempt = syncEngine.syncNow(false);
        assertTrue(thirdAttempt.success);
        assertEquals(1, db.getAllTeamNames().size());
        assertEquals(1, remoteStore.countRemoteRecords(clubId));
    }

    @Test
    public void firstTimeSync_detectsConflictAndDeduplicatesNaturalKeysWithBackupRollback() throws Exception {
        String clubId = "test_club_migration";
        syncPrefs.setClubId(clubId);

        // 1. Pre-existing local offline data
        db.insertTeamBasicDetails("Royal Strikers");
        db.insertTeamBasicDetails("Local Only XI");
        assertEquals(2, db.getAllTeamNames().size());

        // 2. Pre-existing cloud data with overlapping natural key ("Royal Strikers") + 1 cloud-only team
        TeamSyncRecord cloudStrikers = new TeamSyncRecord();
        cloudStrikers.syncUuid = UUID.randomUUID().toString();
        cloudStrikers.clubId = clubId;
        cloudStrikers.teamName = "Royal Strikers";
        cloudStrikers.matchWon = 5;
        cloudStrikers.matchLost = 1;
        cloudStrikers.matchDraw = 0;
        cloudStrikers.updatedAt = System.currentTimeMillis() + 1000L;
        cloudStrikers.remoteVersion = 2L;
        remoteStore.upsertTeam(clubId, cloudStrikers);

        TeamSyncRecord cloudTitans = new TeamSyncRecord();
        cloudTitans.syncUuid = UUID.randomUUID().toString();
        cloudTitans.clubId = clubId;
        cloudTitans.teamName = "Cloud Titans";
        cloudTitans.matchWon = 3;
        cloudTitans.updatedAt = System.currentTimeMillis() + 2000L;
        cloudTitans.remoteVersion = 1L;
        remoteStore.upsertTeam(clubId, cloudTitans);

        // 3. First sync without confirmation must pause and request merge confirmation
        syncPrefs.setSyncEnabled(true);
        SyncSummary unconfirmed = syncEngine.syncNow(false);
        assertFalse(unconfirmed.success);
        assertTrue(unconfirmed.requiresMergeConfirmation);
        assertTrue(DatabaseBackupManager.hasPreSyncBackup(context));

        // 4. Confirm merge -> natural-key deduplication merges "Royal Strikers" without duplicate
        SyncSummary confirmed = syncEngine.syncNow(true);
        assertTrue(confirmed.message, confirmed.success);

        List<String> mergedTeams = db.getAllTeamNames();
        assertEquals(3, mergedTeams.size());
        assertTrue(mergedTeams.contains("Royal Strikers"));
        assertTrue(mergedTeams.contains("Local Only XI"));
        assertTrue(mergedTeams.contains("Cloud Titans"));

        // Verify "Royal Strikers" preserved the 5 wins from cloud and bound to canonical UUID
        int strikersId = db.getTeamId("Royal Strikers");
        assertEquals(5, db.getTeamDetails(strikersId).matchWon);
        assertEquals(cloudStrikers.syncUuid, db.getTeamSyncUuid(strikersId));

        // 5. Verify one-tap pre-sync rollback restores the exact pre-sync local state (2 teams)
        assertTrue(DatabaseBackupManager.restorePreSyncBackup(context));
        DatabaseHandler restoredDb = new DatabaseHandler(context);
        List<String> restoredTeams = restoredDb.getAllTeamNames();
        assertEquals(2, restoredTeams.size());
        assertTrue(restoredTeams.contains("Royal Strikers"));
        assertTrue(restoredTeams.contains("Local Only XI"));
        assertFalse(restoredTeams.contains("Cloud Titans"));
    }

    @Test
    public void concurrentEditsAndTombstoneDeletions_followDeterministicConflictPolicies() {
        // 1. Domain conflict policies unit verification
        TeamSyncRecord localTeam = new TeamSyncRecord();
        localTeam.syncUuid = "team-uuid-1";
        localTeam.teamName = "Local Name";
        localTeam.matchWon = 8;
        localTeam.matchLost = 2;
        localTeam.updatedAt = 1000L;

        TeamSyncRecord remoteTeam = new TeamSyncRecord();
        remoteTeam.syncUuid = "team-uuid-1";
        remoteTeam.teamName = "Updated Remote Name";
        remoteTeam.matchWon = 6;
        remoteTeam.matchLost = 4;
        remoteTeam.updatedAt = 2000L;

        TeamSyncRecord resolvedTeam = ConflictResolutionPolicy.resolveTeamConflict(localTeam, remoteTeam);
        assertEquals("Updated Remote Name", resolvedTeam.teamName);
        assertEquals(8, resolvedTeam.matchWon); // Math.max preserved
        assertEquals(4, resolvedTeam.matchLost); // Math.max preserved

        PlayerSyncRecord localPlayer = new PlayerSyncRecord();
        localPlayer.syncUuid = "player-uuid-1";
        localPlayer.playerName = "Virat";
        localPlayer.totalRuns = 450;
        localPlayer.wicketsTaken = 2;
        localPlayer.updatedAt = 1500L;

        PlayerSyncRecord remotePlayer = new PlayerSyncRecord();
        remotePlayer.syncUuid = "player-uuid-1";
        remotePlayer.playerName = "Virat K";
        remotePlayer.totalRuns = 320;
        remotePlayer.wicketsTaken = 5;
        remotePlayer.updatedAt = 2500L;

        PlayerSyncRecord resolvedPlayer = ConflictResolutionPolicy.resolvePlayerConflict(localPlayer, remotePlayer);
        assertEquals("Virat K", resolvedPlayer.playerName);
        assertEquals(450, resolvedPlayer.totalRuns);
        assertEquals(5, resolvedPlayer.wicketsTaken);

        ScheduleSyncRecord localSched = new ScheduleSyncRecord();
        localSched.syncUuid = "sched-uuid-1";
        localSched.matchDate = "2026/12/01";
        localSched.matchStatus = "COMPLETED";
        localSched.updatedAt = 1000L;

        ScheduleSyncRecord remoteSched = new ScheduleSyncRecord();
        remoteSched.syncUuid = "sched-uuid-1";
        remoteSched.matchDate = "2026/12/01";
        remoteSched.matchStatus = "SCHEDULED";
        remoteSched.updatedAt = 2000L;

        ScheduleSyncRecord resolvedSched = ConflictResolutionPolicy.resolveScheduleConflict(localSched, remoteSched);
        assertEquals("COMPLETED", resolvedSched.matchStatus); // Never regresses COMPLETED -> SCHEDULED

        // 2. Tombstone soft-delete verification in SQLite
        db.insertTeamBasicDetails("Temporary XI");
        int tempId = db.getTeamId("Temporary XI");
        assertTrue(db.getAllTeamNames().contains("Temporary XI"));

        db.softDeleteTeam(tempId);
        assertFalse(db.getAllTeamNames().contains("Temporary XI"));
    }

    @Test
    public void clubSwitching_isolatesDataAndPreventsCrossClubLeakage() {
        syncPrefs.setClubId("club_alpha");
        syncPrefs.setSyncEnabled(true);
        syncPrefs.setInitialMigrationCompleted("club_alpha", true);

        db.insertTeamBasicDetails("Alpha Secret Team");
        assertTrue(syncEngine.syncNow(false).success);
        assertEquals(1, remoteStore.countRemoteRecords("club_alpha"));

        // Switch to club_beta -> local data from club_alpha must be cleared and isolated
        syncEngine.switchClubIsolated("club_beta");
        assertEquals("club_beta", syncPrefs.getClubId());
        assertEquals(0, db.getAllTeamNames().size());
        assertEquals(0, db.getPendingOutboxCount());

        // Syncing on club_beta must not leak Alpha Secret Team into club_beta
        assertTrue(syncEngine.syncNow(true).success);
        assertEquals(0, remoteStore.countRemoteRecords("club_beta"));
    }
}
