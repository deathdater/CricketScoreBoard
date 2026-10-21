package com.example.android.cricketscoreboard.views;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.android.cricketscoreboard.DatabaseHandler;
import com.example.android.cricketscoreboard.R;
import com.example.android.cricketscoreboard.ScoreMan;
import com.example.android.cricketscoreboard.UiPreferences;
import com.example.android.cricketscoreboard.UiSettingsDialog;
import com.example.android.cricketscoreboard.scoring.CricketAnalyticsCalculator;
import com.example.android.cricketscoreboard.scoring.CricketRulesProfile;
import com.example.android.cricketscoreboard.scoring.CricketScoringEngine;
import com.example.android.cricketscoreboard.scoring.DeliveryEvent;
import com.example.android.cricketscoreboard.scoring.DeliveryEvent.DismissalType;
import com.example.android.cricketscoreboard.scoring.DeliveryEvent.ExtraType;
import com.example.android.cricketscoreboard.scoring.MatchState;
import com.example.android.cricketscoreboard.scoring.MatchState.BatterInningsStat;
import com.example.android.cricketscoreboard.scoring.MatchState.InningsScorecard;
import com.example.android.cricketscoreboard.scoring.MatchState.MatchLifecycleStatus;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Sunlight-ready, one-handed live cricket scoring interface powered by the deterministic,
 * event-sourced {@link CricketScoringEngine} and persistent SQLite v4 delivery event log.
 */
public class ScorerPadActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static final String EXTRA_MATCH_UUID = "extra_match_uuid";
    private static final String STATE_MATCH_UUID = "state_match_uuid";

    private static final String[] DISMISSAL_OPTIONS = new String[]{
            "Bowled",
            "Caught",
            "LBW",
            "Run Out",
            "Stumped",
            "Hit Wicket",
            "Retired Hurt",
            "Retired Out"
    };

    public ScoreMan scoreMan = new ScoreMan();
    ArrayList al;
    TextView batting_score, wickets, overCount, ballCount, thisOver;
    TextView strikerNameValueView, nonStrikerNameValueView, matchStatusSummaryText;
    RadioGroup runs, wideNoBall, byesGroup, strikerNonStriker;
    RadioButton runsInBall, wideNoBallInBall, zeroRun, wideBall, noBall, byesRadio, legByesRadio;
    Spinner bowlerSpinner, outSpinner;
    public int score = 0, balls = 0, overs = 0, runSelection, wideNoBallSelection, count = 0;
    public int wicketDown = 0;
    int endOfOver, a, b, c;
    String runText, wideNoBallInBallText, thisOverStr;
    char runCheck;
    CheckBox freeHit, outCheckBox, undoBall;

    private DatabaseHandler dbHandler;
    private MatchState matchState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scorer_pad);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        dbHandler = new DatabaseHandler(this);
        bindViews();
        initMatchSession(savedInstanceState);
        wireControls();
        renderMatchState();

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        final OnBackPressedCallback drawerBackCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                drawer.closeDrawer(GravityCompat.START);
            }
        };
        getOnBackPressedDispatcher().addCallback(this, drawerBackCallback);
        drawer.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerOpened(View drawerView) {
                drawerBackCallback.setEnabled(true);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                drawerBackCallback.setEnabled(false);
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (matchState != null && matchState.matchUuid != null) {
            outState.putString(STATE_MATCH_UUID, matchState.matchUuid);
        }
    }

    private void bindViews() {
        batting_score = findViewById(R.id.Batting_score_editText);
        wickets = findViewById(R.id.Wickets_editText);
        overCount = findViewById(R.id.Bowling_overs_editText);
        ballCount = findViewById(R.id.Bowling_oversBall_editText);
        thisOver = findViewById(R.id.thisOverValue_text_view);

        runs = findViewById(R.id.runsScored_radioGrp);
        wideNoBall = findViewById(R.id.wideBall_NoBall_radioGrp);
        byesGroup = findViewById(R.id.byes_radioGrp);
        strikerNonStriker = findViewById(R.id.Striker_NonStriker_radioGrp);

        zeroRun = findViewById(R.id.radioBtn_ZeroRun);
        wideBall = findViewById(R.id.radioBtn_WideBall);
        noBall = findViewById(R.id.radioBtn_NoBall);
        byesRadio = findViewById(R.id.radioBtn_byes);
        legByesRadio = findViewById(R.id.radioBtn_LegByes);

        freeHit = findViewById(R.id.freeHit_checkBox);
        outCheckBox = findViewById(R.id.out_CheckBox);
        undoBall = findViewById(R.id.undo_checkBox);

        bowlerSpinner = findViewById(R.id.bowlerName_spinner);
        outSpinner = findViewById(R.id.out_spinner);
        strikerNameValueView = findViewById(R.id.strikerNameValue);
        nonStrikerNameValueView = findViewById(R.id.nonStrikerNameValue);
        matchStatusSummaryText = findViewById(R.id.matchStatusSummaryText);

        if (outSpinner != null) {
            ArrayAdapter<String> outAdapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_item, Arrays.asList(DISMISSAL_OPTIONS));
            outAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            outSpinner.setAdapter(outAdapter);
        }

        if (bowlerSpinner != null) {
            List<String> players = dbHandler.viewAllPlayers();
            if (players == null || players.isEmpty()) {
                players = Arrays.asList("Bowler 1", "Bowler 2", "Bowler 3", "Bowler 4");
            }
            ArrayAdapter<String> bowlerAdapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_item, players);
            bowlerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            bowlerSpinner.setAdapter(bowlerAdapter);
        }
    }

    private void initMatchSession(Bundle savedInstanceState) {
        String resumeUuid = null;
        if (savedInstanceState != null) {
            resumeUuid = savedInstanceState.getString(STATE_MATCH_UUID);
        }
        if (resumeUuid == null && getIntent() != null) {
            resumeUuid = getIntent().getStringExtra(EXTRA_MATCH_UUID);
        }
        if (resumeUuid != null && !resumeUuid.isEmpty()) {
            matchState = dbHandler.loadMatchState(resumeUuid);
        }
        if (matchState == null) {
            List<String> teams = dbHandler.getAllTeamNames();
            String teamAName = (teams != null && teams.size() >= 1) ? teams.get(0) : "Home XI";
            String teamBName = (teams != null && teams.size() >= 2) ? teams.get(1) : "Visitors XI";
            int teamAId = dbHandler.getTeamId(teamAName);
            int teamBId = dbHandler.getTeamId(teamBName);
            if (teamAId <= 0) {
                teamAId = 1;
            }
            if (teamBId <= 0) {
                teamBId = 2;
            }
            matchState = new MatchState(
                    UUID.randomUUID().toString(),
                    CricketRulesProfile.createStandardT20(),
                    teamAId,
                    teamAName,
                    teamBId,
                    teamBName,
                    MatchLifecycleStatus.PROVISIONAL);
            dbHandler.saveMatchSessionHeader(matchState, 0);
        }
    }

    private void wireControls() {
        Button setBowlerBtn = findViewById(R.id.StartOver_btn);
        if (setBowlerBtn != null) {
            setBowlerBtn.setOnClickListener(v -> {
                if (bowlerSpinner != null && bowlerSpinner.getSelectedItem() != null) {
                    String selectedBowler = bowlerSpinner.getSelectedItem().toString();
                    InningsScorecard inn = matchState.getCurrentInnings();
                    inn.currentBowlerName = selectedBowler;
                    int pid = dbHandler.getPlayerId(selectedBowler);
                    inn.currentBowlerId = pid > 0 ? pid : (100 + bowlerSpinner.getSelectedItemPosition() + 1);
                    Snackbar.make(findViewById(R.id.activity_scorer_pad),
                            "Active bowler set to " + selectedBowler, Snackbar.LENGTH_SHORT).show();
                    renderMatchState();
                }
            });
        }

        Button rulesBtn = findViewById(R.id.btn_select_rules_profile);
        if (rulesBtn != null) {
            rulesBtn.setOnClickListener(v -> showRulesProfileDialog());
        }

        Button nextInningsBtn = findViewById(R.id.btn_next_innings_or_confirm);
        if (nextInningsBtn != null) {
            nextInningsBtn.setOnClickListener(v -> handleNextInningsOrConfirm());
        }

        Button openCenterBtn = findViewById(R.id.btn_open_match_center);
        if (openCenterBtn != null) {
            openCenterBtn.setOnClickListener(v -> {
                Intent intent = new Intent(this, ScorerTabActivity.class);
                intent.putExtra(EXTRA_MATCH_UUID, matchState.matchUuid);
                startActivity(intent);
            });
        }

        Button shareBtn = findViewById(R.id.btn_share_live_score);
        if (shareBtn != null) {
            shareBtn.setOnClickListener(v -> shareLiveScorecard());
        }
    }

    private void showRulesProfileDialog() {
        List<CricketRulesProfile> templates = dbHandler.getAllRulesProfiles();
        String[] labels = new String[templates.size()];
        for (int i = 0; i < templates.size(); i++) {
            CricketRulesProfile p = templates.get(i);
            labels[i] = p.profileName + " (" + p.ballsPerOver + "b/ov)";
        }
        new AlertDialog.Builder(this)
                .setTitle("Select Match Rules Profile")
                .setItems(labels, (dialog, which) -> {
                    CricketRulesProfile chosen = templates.get(which);
                    matchState = CricketScoringEngine.replay(
                            matchState.matchUuid,
                            chosen,
                            matchState.teamAId,
                            matchState.teamAName,
                            matchState.teamBId,
                            matchState.teamBName,
                            matchState.appliedEvents,
                            matchState.lifecycleStatus);
                    dbHandler.saveMatchSessionHeader(matchState, 0);
                    renderMatchState();
                    Snackbar.make(findViewById(R.id.activity_scorer_pad),
                            "Applied rules: " + chosen.profileName, Snackbar.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void handleNextInningsOrConfirm() {
        int maxInnings = matchState.rulesProfile.inningsPerTeam * 2;
        if (!matchState.matchCompleted && matchState.currentInningsNumber < maxInnings) {
            CricketScoringEngine.startNextInnings(matchState);
            dbHandler.saveMatchSessionHeader(matchState, 0);
            renderMatchState();
            Snackbar.make(findViewById(R.id.activity_scorer_pad),
                    "Started Innings " + matchState.currentInningsNumber + " • Target: "
                            + matchState.getCurrentInnings().targetRuns,
                    Snackbar.LENGTH_SHORT).show();
        } else {
            matchState = dbHandler.confirmAndCommitMatchStats(matchState.matchUuid, false);
            renderMatchState();
            Snackbar.make(findViewById(R.id.activity_scorer_pad),
                    "Match Confirmed & Career Stats Saved!", Snackbar.LENGTH_SHORT).show();
        }
    }

    private void shareLiveScorecard() {
        String sharePayload = CricketAnalyticsCalculator.buildShareableScorecardText(matchState);
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_SUBJECT,
                matchState.teamAName + " vs " + matchState.teamBName + " Live Score");
        sendIntent.putExtra(Intent.EXTRA_TEXT, sharePayload);
        startActivity(Intent.createChooser(sendIntent, "Share Live Cricket Scorecard"));
    }

    public MatchState getActiveMatchState() {
        return matchState;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.scorer_pad, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            UiSettingsDialog.show(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            Intent intent = new Intent(this, ScorerTabActivity.class);
            if (matchState != null) {
                intent.putExtra(EXTRA_MATCH_UUID, matchState.matchUuid);
            }
            startActivity(intent);
        } else if (id == R.id.nav_gallery) {
            startActivity(new Intent(this, TeamDetails.class));
        } else if (id == R.id.nav_slideshow) {
            startActivity(new Intent(this, PlayerDetailsActivity.class));
        } else if (id == R.id.nav_manage) {
            UiSettingsDialog.show(this);
        } else if (id == R.id.nav_share) {
            startActivity(new Intent(this, ScheduleMatches.class));
        } else if (id == R.id.nav_send) {
            finish();
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void onBallPress(View view) {
        if (view != null && UiPreferences.get(this).isScorerHapticsEnabled()) {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        }

        undoBall = findViewById(R.id.undo_checkBox);
        wickets = findViewById(R.id.Wickets_editText);
        thisOver = findViewById(R.id.thisOverValue_text_view);
        wideBall = findViewById(R.id.radioBtn_WideBall);
        noBall = findViewById(R.id.radioBtn_NoBall);
        byesRadio = findViewById(R.id.radioBtn_byes);
        legByesRadio = findViewById(R.id.radioBtn_LegByes);
        batting_score = findViewById(R.id.Batting_score_editText);
        overCount = findViewById(R.id.Bowling_overs_editText);
        ballCount = findViewById(R.id.Bowling_oversBall_editText);
        runs = findViewById(R.id.runsScored_radioGrp);
        outCheckBox = findViewById(R.id.out_CheckBox);
        freeHit = findViewById(R.id.freeHit_checkBox);

        if (undoBall != null && undoBall.isChecked()) {
            if (matchState == null || matchState.appliedEvents.isEmpty()) {
                Snackbar.make(findViewById(R.id.activity_scorer_pad),
                        "No Records to Undo !!", Snackbar.LENGTH_SHORT).show();
                undoBall.setChecked(false);
                return;
            }
            MatchState undone = dbHandler.undoLastMatchDelivery(matchState.matchUuid);
            if (undone != null) {
                matchState = undone;
            } else {
                matchState = CricketScoringEngine.undoLastDelivery(matchState);
            }
            undoBall.setChecked(false);
            renderMatchState();
            Snackbar.make(findViewById(R.id.activity_scorer_pad),
                    "Undo is completed", Snackbar.LENGTH_SHORT).show();
            return;
        }

        // Parse selected runs (0..6)
        runSelection = 0;
        if (runs != null) {
            int checkedId = runs.getCheckedRadioButtonId();
            runsInBall = findViewById(checkedId);
            if (runsInBall != null && runsInBall.getText() != null && runsInBall.getText().length() > 0) {
                runCheck = runsInBall.getText().charAt(0);
                if (runCheck >= '0' && runCheck <= '6') {
                    runSelection = runCheck - '0';
                }
            }
        }

        // Determine ExtraType
        ExtraType extraType = ExtraType.NONE;
        int runsOffBat = runSelection;
        int extraRuns = 0;
        if (wideBall != null && wideBall.isChecked()) {
            extraType = ExtraType.WIDE;
            runsOffBat = 0;
            extraRuns = runSelection;
        } else if (noBall != null && noBall.isChecked()) {
            extraType = ExtraType.NO_BALL;
            runsOffBat = runSelection;
            extraRuns = 0;
        } else if (byesRadio != null && byesRadio.isChecked()) {
            extraType = ExtraType.BYE;
            runsOffBat = 0;
            extraRuns = runSelection;
        } else if (legByesRadio != null && legByesRadio.isChecked()) {
            extraType = ExtraType.LEG_BYE;
            runsOffBat = 0;
            extraRuns = runSelection;
        }

        InningsScorecard inn = matchState.getCurrentInnings();

        // Determine DismissalType if Out checkbox is checked
        DismissalType dismissalType = DismissalType.NONE;
        int dismissedPlayerId = 0;
        String dismissedPlayerName = "";
        if (outCheckBox != null && outCheckBox.isChecked()) {
            dismissalType = resolveSelectedDismissalType();
            RadioButton nonStrikerOutBtn = findViewById(R.id.radioBtn_NonStriker);
            if (nonStrikerOutBtn != null && nonStrikerOutBtn.isChecked()) {
                dismissedPlayerId = inn.currentNonStrikerId;
                dismissedPlayerName = inn.currentNonStrikerName;
            } else {
                dismissedPlayerId = inn.currentStrikerId;
                dismissedPlayerName = inn.currentStrikerName;
            }
        }

        DeliveryEvent candidate = new DeliveryEvent.Builder()
                .setMatchUuid(matchState.matchUuid)
                .setInningsNumber(matchState.currentInningsNumber)
                .setSequenceNumber(matchState.appliedEvents.size() + 1)
                .setStriker(inn.currentStrikerId, inn.currentStrikerName)
                .setNonStriker(inn.currentNonStrikerId, inn.currentNonStrikerName)
                .setBowler(inn.currentBowlerId, inn.currentBowlerName)
                .setRunsOffBat(runsOffBat)
                .setExtraType(extraType)
                .setExtraRuns(extraRuns)
                .setDismissal(dismissalType, dismissedPlayerId, dismissedPlayerName, "", 0, "")
                .setWasFreeHit(inn.freeHitArmed || (freeHit != null && freeHit.isChecked()))
                .build();

        CricketScoringEngine.ValidationResult vr = CricketScoringEngine.validateDelivery(matchState, candidate);
        if (!vr.valid) {
            Snackbar.make(findViewById(R.id.activity_scorer_pad),
                    vr.errorMessage, Snackbar.LENGTH_LONG).show();
            return;
        }

        matchState = dbHandler.recordMatchDelivery(matchState, candidate);
        renderMatchState();
        reset(view);

        String message = "The Score is " + score + "/" + wicketDown + " (" + overs + "." + balls + " ov)";
        Snackbar.make(findViewById(R.id.activity_scorer_pad), message, Snackbar.LENGTH_SHORT).show();
    }

    private DismissalType resolveSelectedDismissalType() {
        if (outSpinner == null || outSpinner.getSelectedItem() == null) {
            return DismissalType.BOWLED;
        }
        String label = outSpinner.getSelectedItem().toString();
        switch (label) {
            case "Caught":
                return DismissalType.CAUGHT;
            case "LBW":
                return DismissalType.LBW;
            case "Run Out":
                return DismissalType.RUN_OUT;
            case "Stumped":
                return DismissalType.STUMPED;
            case "Hit Wicket":
                return DismissalType.HIT_WICKET;
            case "Retired Hurt":
                return DismissalType.RETIRED_HURT;
            case "Retired Out":
                return DismissalType.RETIRED_OUT;
            case "Bowled":
            default:
                return DismissalType.BOWLED;
        }
    }

    private void renderMatchState() {
        if (matchState == null) {
            return;
        }
        InningsScorecard inn = matchState.getCurrentInnings();
        score = inn.totalRuns;
        wicketDown = inn.wickets;
        overs = inn.completedOvers;
        balls = inn.ballsInCurrentOver;

        // Keep legacy ScoreMan synchronized
        scoreMan.battingScore = score;
        scoreMan.oversBowled = overs;
        scoreMan.overBalls = balls;
        scoreMan.scoreCommentry = inn.legacyCommentaryBuilder.toString();

        if (batting_score != null) {
            batting_score.setText(String.valueOf(score));
        }
        if (wickets != null) {
            wickets.setText(String.valueOf(wicketDown));
        }
        if (overCount != null) {
            overCount.setText(String.valueOf(overs));
        }
        if (ballCount != null) {
            ballCount.setText(String.valueOf(balls));
        }
        if (thisOver != null) {
            thisOver.setText(inn.getThisOverDisplayString());
        }

        if (strikerNameValueView != null) {
            BatterInningsStat sStat = inn.batterStats.get(inn.currentStrikerId);
            String sLine = sStat != null
                    ? String.format(Locale.US, "%s*  %d (%d)", inn.currentStrikerName, sStat.runs, sStat.ballsFaced)
                    : inn.currentStrikerName + "*  0 (0)";
            strikerNameValueView.setText(sLine);
        }
        if (nonStrikerNameValueView != null) {
            BatterInningsStat nsStat = inn.batterStats.get(inn.currentNonStrikerId);
            String nsLine = nsStat != null
                    ? String.format(Locale.US, "%s  %d (%d)", inn.currentNonStrikerName, nsStat.runs, nsStat.ballsFaced)
                    : inn.currentNonStrikerName + "  0 (0)";
            nonStrikerNameValueView.setText(nsLine);
        }
        if (matchStatusSummaryText != null) {
            matchStatusSummaryText.setText(String.format(Locale.US,
                    "%s • Inn %d (%s) • %s • [%s]",
                    matchState.rulesProfile.profileName,
                    inn.inningsNumber,
                    inn.battingTeamName,
                    matchState.resultText,
                    matchState.lifecycleStatus.name()));
        }
        if (freeHit != null) {
            freeHit.setChecked(inn.freeHitArmed);
            freeHit.setEnabled(inn.freeHitArmed);
        }
    }

    public void onNoBallCheck(View view) {
        freeHit = findViewById(R.id.freeHit_checkBox);
        if (freeHit != null) {
            freeHit.setEnabled(true);
        }
    }

    public void onWideBallCheck(View view) {
        freeHit = findViewById(R.id.freeHit_checkBox);
        if (freeHit != null) {
            freeHit.setChecked(false);
            freeHit.setEnabled(false);
        }
    }

    public void onOutCheckBoxChecked(View v) {
        outCheckBox = findViewById(R.id.out_CheckBox);
        strikerNonStriker = findViewById(R.id.Striker_NonStriker_radioGrp);
        if (outCheckBox != null && strikerNonStriker != null) {
            strikerNonStriker.setVisibility(outCheckBox.isChecked() ? View.VISIBLE : View.GONE);
        }
    }

    public void reset(View v) {
        outCheckBox = findViewById(R.id.out_CheckBox);
        wideBall = findViewById(R.id.radioBtn_WideBall);
        noBall = findViewById(R.id.radioBtn_NoBall);
        byesGroup = findViewById(R.id.byes_radioGrp);
        freeHit = findViewById(R.id.freeHit_checkBox);
        zeroRun = findViewById(R.id.radioBtn_ZeroRun);
        strikerNonStriker = findViewById(R.id.Striker_NonStriker_radioGrp);

        if (zeroRun != null) {
            zeroRun.setChecked(true);
        }
        if (wideNoBall != null && ((wideBall != null && wideBall.isChecked())
                || (noBall != null && noBall.isChecked()))) {
            wideNoBall.clearCheck();
        }
        if (byesGroup != null) {
            byesGroup.clearCheck();
        }
        if (outCheckBox != null && outCheckBox.isChecked()) {
            outCheckBox.setChecked(false);
        }
        if (strikerNonStriker != null) {
            strikerNonStriker.setVisibility(View.GONE);
        }
        if (freeHit != null && matchState != null) {
            boolean armed = matchState.getCurrentInnings().freeHitArmed;
            freeHit.setChecked(armed);
            freeHit.setEnabled(armed);
        }
    }
}
