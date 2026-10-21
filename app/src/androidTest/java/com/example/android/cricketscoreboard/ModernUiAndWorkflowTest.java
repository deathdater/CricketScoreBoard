package com.example.android.cricketscoreboard;

import android.content.Context;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.android.cricketscoreboard.scoring.CricketAnalyticsCalculator;
import com.example.android.cricketscoreboard.scoring.CricketRulesProfile;
import com.example.android.cricketscoreboard.scoring.DeliveryEvent;
import com.example.android.cricketscoreboard.scoring.MatchState;
import com.example.android.cricketscoreboard.views.LandingPageActivity;
import com.example.android.cricketscoreboard.views.PlayerDetailsActivity;
import com.example.android.cricketscoreboard.views.ScheduleMatches;
import com.example.android.cricketscoreboard.views.ScorerPadActivity;
import com.example.android.cricketscoreboard.views.ScorerTabActivity;
import com.example.android.cricketscoreboard.views.TeamDetails;
import com.example.android.cricketscoreboard.views.ViewPlayers;
import com.example.android.cricketscoreboard.views.ViewSchedules;
import com.example.android.cricketscoreboard.views.ViewTeams;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * End-to-end verification of UI personalization preferences ({@link UiPreferences}) and critical
 * operational workflows (Team creation, Player registration, Squad assignment, Match scheduling,
 * and Live ball-by-ball scoring + undo).
 */
@RunWith(AndroidJUnit4.class)
public class ModernUiAndWorkflowTest {

    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        UiPreferences.get(context).resetToDefaults();
    }

    @After
    public void tearDown() {
        UiPreferences.get(context).resetToDefaults();
    }

    @Test
    public void uiPreferencesPersistAndResetCleanly() {
        UiPreferences prefs = UiPreferences.get(context);
        assertEquals(UiPreferences.ThemeMode.SYSTEM, prefs.getThemeMode());
        assertEquals(UiPreferences.AccentPalette.TURF_GREEN, prefs.getAccentPalette());
        assertEquals(UiPreferences.LayoutDensity.STANDARD, prefs.getLayoutDensity());
        assertTrue(prefs.isScorerHapticsEnabled());
        assertFalse(prefs.isSkipSplashEnabled());

        prefs.setAccentPalette(UiPreferences.AccentPalette.STADIUM_BLUE);
        prefs.setLayoutDensity(UiPreferences.LayoutDensity.COMFORTABLE);
        prefs.setScorerHapticsEnabled(false);
        prefs.setSkipSplashEnabled(true);

        UiPreferences reloaded = UiPreferences.get(context);
        assertEquals(UiPreferences.AccentPalette.STADIUM_BLUE, reloaded.getAccentPalette());
        assertEquals(UiPreferences.LayoutDensity.COMFORTABLE, reloaded.getLayoutDensity());
        assertFalse(reloaded.isScorerHapticsEnabled());
        assertTrue(reloaded.isSkipSplashEnabled());

        reloaded.resetToDefaults();
        assertEquals(UiPreferences.AccentPalette.TURF_GREEN, reloaded.getAccentPalette());
        assertEquals(UiPreferences.LayoutDensity.STANDARD, reloaded.getLayoutDensity());
        assertTrue(reloaded.isScorerHapticsEnabled());
        assertFalse(reloaded.isSkipSplashEnabled());
    }

    @Test
    public void teamPlayerScheduleWorkflowPreservesDatabaseLogic() {
        String suffix = String.valueOf(System.currentTimeMillis() % 100000);
        String teamA = "Titans_" + suffix;
        String teamB = "Warriors_" + suffix;
        String playerName = "Virat_" + suffix;

        // 1. Create two teams via TeamDetails activity
        try (ActivityScenario<TeamDetails> scenario = ActivityScenario.launch(TeamDetails.class)) {
            scenario.onActivity(activity -> {
                EditText nameInput = activity.findViewById(R.id.teamName_edit_text);
                nameInput.setText(teamA);
                activity.onSaveClick(activity.findViewById(R.id.teamDetails_save_btn));

                nameInput.setText(teamB);
                activity.onSaveClick(activity.findViewById(R.id.teamDetails_save_btn));
            });
        }

        DatabaseHandler db = new DatabaseHandler(context);
        assertTrue(db.getAllTeamNames().contains(teamA));
        assertTrue(db.getAllTeamNames().contains(teamB));

        // 2. Create a player via PlayerDetailsActivity
        try (ActivityScenario<PlayerDetailsActivity> scenario =
                     ActivityScenario.launch(PlayerDetailsActivity.class)) {
            scenario.onActivity(activity -> {
                EditText nameInput = activity.findViewById(R.id.playerName_edit_text);
                EditText emailInput = activity.findViewById(R.id.playerEmail_edit_text);
                CheckBox batsmanBox = activity.findViewById(R.id.checkbox_batsmen);

                nameInput.setText(playerName);
                emailInput.setText("virat@example.com");
                batsmanBox.setChecked(true);
                activity.onCheckBatsmen(batsmanBox);
                assertEquals(View.VISIBLE,
                        activity.findViewById(R.id.batsmenStyle_radioGrp).getVisibility());

                activity.saveNewPlayer(activity.findViewById(R.id.playerDetails_save_btn));
            });
        }
        assertTrue(db.viewAllPlayers().contains(playerName));

        // 3. Verify ViewTeams and ViewPlayers launch and bind data cleanly
        try (ActivityScenario<ViewTeams> scenario = ActivityScenario.launch(ViewTeams.class)) {
            scenario.onActivity(activity -> {
                assertNotNull(activity.findViewById(R.id.teamNameSpinner));
                assertNotNull(activity.findViewById(R.id.teamMatchPlayedValue));
            });
        }

        try (ActivityScenario<ViewPlayers> scenario = ActivityScenario.launch(ViewPlayers.class)) {
            scenario.onActivity(activity -> {
                RadioGroup statsGroup = activity.findViewById(R.id.playerStats_radioGrp_edit);
                statsGroup.check(R.id.radioBtn_Bowler_edit);
                assertEquals(View.VISIBLE,
                        activity.findViewById(R.id.player_details_Bowler_Table).getVisibility());
                statsGroup.check(R.id.radioBtn_Batsmen_edit);
                assertEquals(View.VISIBLE,
                        activity.findViewById(R.id.player_details_Batsmen_Table).getVisibility());
            });
        }

        // 4. Schedule a match and verify ViewSchedules table population
        int beforeSchedules = db.getNumberOfSchedules();
        db.addSchedule("aditya.juet@gmail.com", "2026/10/21",
                db.getTeamId(teamA), db.getTeamId(teamB), 20);
        assertEquals(beforeSchedules + 1, db.getNumberOfSchedules());

        try (ActivityScenario<ViewSchedules> scenario = ActivityScenario.launch(ViewSchedules.class)) {
            scenario.onActivity(activity -> {
                TableLayout table = activity.findViewById(R.id.scheduleMatchTableView);
                assertNotNull(table);
                assertTrue(table.getChildCount() >= 2);
            });
        }

        // 5. Verify LandingPageActivity club summary banner reflects DB records
        try (ActivityScenario<LandingPageActivity> scenario =
                     ActivityScenario.launch(LandingPageActivity.class)) {
            scenario.onActivity(activity -> {
                TextView summary = activity.findViewById(R.id.dashboard_summary_subtitle);
                assertNotNull(summary);
                assertTrue(summary.getText().toString().contains("Teams"));
            });
        }
    }

    @Test
    public void scorerPadRecordsDeliveriesExtrasAndUndoAccurately() {
        try (ActivityScenario<ScorerPadActivity> scenario =
                     ActivityScenario.launch(ScorerPadActivity.class)) {
            scenario.onActivity(activity -> {
                TextView scoreText = activity.findViewById(R.id.Batting_score_editText);
                TextView oversText = activity.findViewById(R.id.Bowling_overs_editText);
                TextView ballsText = activity.findViewById(R.id.Bowling_oversBall_editText);
                RadioGroup runsGroup = activity.findViewById(R.id.runsScored_radioGrp);
                RadioButton wideBtn = activity.findViewById(R.id.radioBtn_WideBall);
                CheckBox undoBox = activity.findViewById(R.id.undo_checkBox);
                View ballBtn = activity.findViewById(R.id.postBall_imageButton);

                // Ball 1: 4 runs
                runsGroup.check(R.id.radioBtn_FourRun);
                activity.onBallPress(ballBtn);
                assertEquals("4", scoreText.getText().toString());
                assertEquals("0", oversText.getText().toString());
                assertEquals("1", ballsText.getText().toString());

                // Ball 2 (Extra): Wide + 1 run = +2 runs, ball count stays 1
                runsGroup.check(R.id.radioBtn_OneRun);
                wideBtn.setChecked(true);
                activity.onWideBallCheck(wideBtn);
                activity.onBallPress(ballBtn);
                assertEquals("6", scoreText.getText().toString());
                assertEquals("1", ballsText.getText().toString());

                // Undo the wide delivery -> back to 4 runs, 1 ball
                undoBox.setChecked(true);
                activity.onBallPress(ballBtn);
                assertEquals("4", scoreText.getText().toString());
                assertEquals("1", ballsText.getText().toString());

                // Verify SQLite v4 persisted the match session and delivery event log
                DatabaseHandler db = new DatabaseHandler(context);
                MatchState reloaded = db.loadMatchState(activity.getActiveMatchState().matchUuid);
                assertNotNull(reloaded);
                assertEquals(4, reloaded.getCurrentInnings().totalRuns);
                assertEquals(1, reloaded.getCurrentInnings().totalLegalBalls);
            });
        }
    }

    @Test
    public void configurableRulesPersistenceAndFourTabMatchCenterWorkEndToEnd() {
        DatabaseHandler db = new DatabaseHandler(context);
        assertTrue(db.getAllRulesProfiles().size() >= 7);

        CricketRulesProfile hundred = CricketRulesProfile.createTheHundred();
        MatchState match = new MatchState(
                "match-sqlite-v4-" + System.currentTimeMillis(),
                hundred, 1, "Invincibles", 2, "Superchargers",
                MatchState.MatchLifecycleStatus.PROVISIONAL);
        db.saveMatchSessionHeader(match, 0);

        match = db.recordMatchDelivery(match, new DeliveryEvent.Builder()
                .setMatchUuid(match.matchUuid)
                .setInningsNumber(1)
                .setSequenceNumber(1)
                .setStriker(1, "Buttler")
                .setNonStriker(2, "Salt")
                .setBowler(101, "Rashid")
                .setRunsOffBat(6)
                .build());

        MatchState reloaded = db.loadMatchState(match.matchUuid);
        assertNotNull(reloaded);
        assertEquals(5, reloaded.rulesProfile.ballsPerOver);
        assertEquals(6, reloaded.getCurrentInnings().totalRuns);
        assertTrue(CricketAnalyticsCalculator.buildShareableScorecardText(reloaded).contains("Invincibles"));

        try (ActivityScenario<ScorerTabActivity> scenario =
                     ActivityScenario.launch(ScorerTabActivity.class)) {
            scenario.onActivity(activity -> {
                assertNotNull(activity.getOrLoadMatchState());
                assertNotNull(activity.findViewById(R.id.tabs));
            });
        }
    }
}
