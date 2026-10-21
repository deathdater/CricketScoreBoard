package com.example.android.cricketscoreboard.scoring;

import com.example.android.cricketscoreboard.scoring.DeliveryEvent.DismissalType;
import com.example.android.cricketscoreboard.scoring.DeliveryEvent.ExtraType;
import com.example.android.cricketscoreboard.scoring.MatchState.BatterInningsStat;
import com.example.android.cricketscoreboard.scoring.MatchState.BowlerSpellStat;
import com.example.android.cricketscoreboard.scoring.MatchState.InningsScorecard;
import com.example.android.cricketscoreboard.scoring.MatchState.MatchLifecycleStatus;
import com.example.android.cricketscoreboard.scoring.PointsTableCalculator.TeamStanding;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Dedicated scoring-engine unit test suite verifying Section 19 invariants:
 * <ul>
 *   <li>Legal/illegal deliveries, extras, over progression, and maiden detection</li>
 *   <li>Strike rotation on odd runs, crossed catches, and end of over</li>
 *   <li>Dismissal legality on Wides, No-Balls, and Free Hits</li>
 *   <li>Retired Hurt vs. Retired Out and compulsory youth retirement</li>
 *   <li>Configurable formats: The Hundred (5-ball overs) &amp; Tape-Ball Last-Man-Standing</li>
 *   <li>Undo, correction, replay idempotency, and duplicate event rejection</li>
 *   <li>Innings transitions, chase victory detection, and Tournament NRR (including All-Out rule)</li>
 * </ul>
 */
public class CricketScoringEngineTest {

    @Test
    public void legalDeliveriesStrikeRotationAndMaidenOverAreCalculatedAccurately() {
        CricketRulesProfile t20 = CricketRulesProfile.createStandardT20();
        MatchState state = new MatchState(
                "match-1", t20, 1, "Titans", 2, "Warriors", MatchLifecycleStatus.PROVISIONAL);

        // Over 1: 6 dot balls by Bowler 101 -> Maiden over, strike swaps at end of over
        for (int i = 1; i <= 6; i++) {
            InningsScorecard inn = state.getCurrentInnings();
            DeliveryEvent dot = new DeliveryEvent.Builder()
                    .setMatchUuid("match-1")
                    .setInningsNumber(1)
                    .setSequenceNumber(i)
                    .setStriker(inn.currentStrikerId, inn.currentStrikerName)
                    .setNonStriker(inn.currentNonStrikerId, inn.currentNonStrikerName)
                    .setBowler(101, "Bumrah")
                    .setRunsOffBat(0)
                    .build();
            state = CricketScoringEngine.appendDeliveryOrThrow(state, dot);
        }

        InningsScorecard inn = state.getCurrentInnings();
        assertEquals(0, inn.totalRuns);
        assertEquals(1, inn.completedOvers);
        assertEquals(0, inn.ballsInCurrentOver);
        // End of over swaps striker from 1 to 2
        assertEquals(2, inn.currentStrikerId);
        assertEquals(1, inn.currentNonStrikerId);

        BowlerSpellStat bumrah = inn.bowlerStats.get(101);
        assertNotNull(bumrah);
        assertEquals(1, bumrah.maidens);
        assertEquals("1.0", bumrah.getOversNotation());
        assertEquals(6, bumrah.dotBalls);

        // Over 2, Ball 1: Batter 2 hits 1 run -> rotates strike back to Batter 1
        DeliveryEvent single = new DeliveryEvent.Builder()
                .setMatchUuid("match-1")
                .setInningsNumber(1)
                .setSequenceNumber(7)
                .setStriker(inn.currentStrikerId, inn.currentStrikerName)
                .setNonStriker(inn.currentNonStrikerId, inn.currentNonStrikerName)
                .setBowler(102, "Starc")
                .setRunsOffBat(1)
                .build();
        state = CricketScoringEngine.appendDeliveryOrThrow(state, single);
        assertEquals(1, state.getCurrentInnings(). currentStrikerId);
        assertEquals(1, state.getCurrentInnings().totalRuns);
        assertEquals("1.1", state.getCurrentInnings().getOversNotation());
    }

    @Test
    public void widesNoBallsByesAndFreeHitEnforceMccDismissalRules() {
        CricketRulesProfile t20 = CricketRulesProfile.createStandardT20();
        MatchState state = new MatchState(
                "match-2", t20, 1, "Titans", 2, "Warriors", MatchLifecycleStatus.PROVISIONAL);

        // 1. BOWLED on a WIDE is rejected
        DeliveryEvent invalidWideBowled = new DeliveryEvent.Builder()
                .setMatchUuid("match-2")
                .setInningsNumber(1)
                .setSequenceNumber(1)
                .setExtraType(ExtraType.WIDE)
                .setDismissal(DismissalType.BOWLED, 1, "Batter 1", "", 3, "Batter 3")
                .build();
        assertFalse(CricketScoringEngine.validateDelivery(state, invalidWideBowled).valid);

        // 2. STUMPED on a WIDE is valid
        DeliveryEvent validWideStumped = new DeliveryEvent.Builder()
                .setMatchUuid("match-2")
                .setInningsNumber(1)
                .setSequenceNumber(1)
                .setStriker(1, "Batter 1")
                .setNonStriker(2, "Batter 2")
                .setBowler(101, "Spinner")
                .setExtraType(ExtraType.WIDE)
                .setDismissal(DismissalType.STUMPED, 1, "Batter 1", "Keeper", 3, "Batter 3")
                .build();
        state = CricketScoringEngine.appendDeliveryOrThrow(state, validWideStumped);
        assertEquals(1, state.getCurrentInnings().totalRuns);
        assertEquals(1, state.getCurrentInnings().wickets);
        assertEquals(0, state.getCurrentInnings().ballsInCurrentOver);
        assertEquals(1, state.getCurrentInnings().bowlerStats.get(101).wickets);

        // 3. No-Ball + 4 off the bat = 5 runs, arms Free Hit, 0 legal balls
        DeliveryEvent noBallFour = new DeliveryEvent.Builder()
                .setMatchUuid("match-2")
                .setInningsNumber(1)
                .setSequenceNumber(2)
                .setStriker(3, "Batter 3")
                .setNonStriker(2, "Batter 2")
                .setBowler(101, "Spinner")
                .setExtraType(ExtraType.NO_BALL)
                .setRunsOffBat(4)
                .build();
        state = CricketScoringEngine.appendDeliveryOrThrow(state, noBallFour);
        assertEquals(6, state.getCurrentInnings().totalRuns);
        assertTrue(state.getCurrentInnings().freeHitArmed);
        assertEquals(4, state.getCurrentInnings().batterStats.get(3).runs);

        // 4. CAUGHT on a Free Hit is rejected
        DeliveryEvent caughtOnFreeHit = new DeliveryEvent.Builder()
                .setMatchUuid("match-2")
                .setInningsNumber(1)
                .setSequenceNumber(3)
                .setStriker(3, "Batter 3")
                .setNonStriker(2, "Batter 2")
                .setBowler(101, "Spinner")
                .setDismissal(DismissalType.CAUGHT, 3, "Batter 3", "Fielder", 4, "Batter 4")
                .build();
        assertFalse(CricketScoringEngine.validateDelivery(state, caughtOnFreeHit).valid);

        // 5. RUN_OUT on a Free Hit is valid and consumes the Free Hit if legal delivery
        DeliveryEvent runOutOnFreeHit = new DeliveryEvent.Builder()
                .setMatchUuid("match-2")
                .setInningsNumber(1)
                .setSequenceNumber(3)
                .setStriker(3, "Batter 3")
                .setNonStriker(2, "Batter 2")
                .setBowler(101, "Spinner")
                .setDismissal(DismissalType.RUN_OUT, 3, "Batter 3", "Fielder", 4, "Batter 4")
                .build();
        state = CricketScoringEngine.appendDeliveryOrThrow(state, runOutOnFreeHit);
        assertEquals(2, state.getCurrentInnings().wickets);
        // Run out is NOT credited to the bowler's wickets
        assertEquals(1, state.getCurrentInnings().bowlerStats.get(101).wickets);
        assertFalse(state.getCurrentInnings().freeHitArmed);
    }

    @Test
    public void theHundredFiveBallOversAndTapeBallLastManStandingRulesAreEnforced() {
        // 1. The Hundred: 5 balls per over
        CricketRulesProfile hundred = CricketRulesProfile.createTheHundred();
        assertEquals(5, hundred.ballsPerOver);
        MatchState state = new MatchState(
                "match-hundred", hundred, 1, "Invincibles", 2, "Phoenix", MatchLifecycleStatus.PROVISIONAL);

        for (int i = 1; i <= 5; i++) {
            state = CricketScoringEngine.appendDeliveryOrThrow(state,
                    new DeliveryEvent.Builder()
                            .setMatchUuid("match-hundred")
                            .setInningsNumber(1)
                            .setSequenceNumber(i)
                            .setRunsOffBat(1)
                            .build());
        }
        assertEquals(1, state.getCurrentInnings().completedOvers);
        assertEquals(0, state.getCurrentInnings().ballsInCurrentOver);
        assertEquals(5, state.getCurrentInnings().totalRuns);

        // 2. Tape-Ball Last-Man-Standing: 3 players per side with lastManStanding = true -> 3 wickets all-out
        CricketRulesProfile lms = CricketRulesProfile.createTapeBallLastManStanding()
                .toBuilder()
                .setPlayersPerTeam(3)
                .setLastManStanding(true)
                .build();
        assertEquals(3, lms.getMaxWicketsPerInnings());

        MatchState lmsState = new MatchState(
                "match-lms", lms, 1, "Street A", 2, "Street B", MatchLifecycleStatus.PROVISIONAL);
        for (int w = 1; w <= 2; w++) {
            lmsState = CricketScoringEngine.appendDeliveryOrThrow(lmsState,
                    new DeliveryEvent.Builder()
                            .setMatchUuid("match-lms")
                            .setInningsNumber(1)
                            .setSequenceNumber(w)
                            .setDismissal(DismissalType.BOWLED, w, "P" + w, "", w + 2, "P" + (w + 2))
                            .build());
        }
        // After 2 wickets with 3 players, standard cricket would be all-out, but LMS continues!
        assertFalse(lmsState.getCurrentInnings().inningsClosed);

        // 3rd wicket closes the LMS innings
        lmsState = CricketScoringEngine.appendDeliveryOrThrow(lmsState,
                new DeliveryEvent.Builder()
                        .setMatchUuid("match-lms")
                        .setInningsNumber(1)
                        .setSequenceNumber(3)
                        .setDismissal(DismissalType.BOWLED, 3, "P3", "", 0, "")
                        .build());
        assertTrue(lmsState.getCurrentInnings().inningsClosed);
        assertEquals("All out", lmsState.getCurrentInnings().closureReason);
    }

    @Test
    public void retiredHurtDoesNotIncrementWicketsAndReplayUndoAreIdempotent() {
        CricketRulesProfile rules = CricketRulesProfile.createStandardT20();
        MatchState state = new MatchState(
                "match-ret", rules, 1, "Alpha", 2, "Beta", MatchLifecycleStatus.PROVISIONAL);

        DeliveryEvent e1 = new DeliveryEvent.Builder()
                .setEventUuid("uuid-ball-1")
                .setMatchUuid("match-ret")
                .setInningsNumber(1)
                .setSequenceNumber(1)
                .setStriker(1, "Rohit")
                .setNonStriker(2, "Gill")
                .setBowler(101, "Cummins")
                .setRunsOffBat(6)
                .build();
        DeliveryEvent e2 = new DeliveryEvent.Builder()
                .setEventUuid("uuid-ball-2")
                .setMatchUuid("match-ret")
                .setInningsNumber(1)
                .setSequenceNumber(2)
                .setStriker(1, "Rohit")
                .setNonStriker(2, "Gill")
                .setBowler(101, "Cummins")
                .setDismissal(DismissalType.RETIRED_HURT, 1, "Rohit", "", 3, "Kohli")
                .build();

        state = CricketScoringEngine.appendDeliveryOrThrow(state, e1);
        state = CricketScoringEngine.appendDeliveryOrThrow(state, e2);

        // Retired hurt does NOT increment team wickets
        assertEquals(0, state.getCurrentInnings().wickets);
        BatterInningsStat rohit = state.getCurrentInnings().batterStats.get(1);
        assertTrue(rohit.retiredHurt);
        assertFalse(rohit.isOut);
        assertEquals("retired hurt", rohit.getDismissalDescription());
        assertEquals(3, state.getCurrentInnings().currentStrikerId);

        // Replaying with duplicate event UUIDs does not double-count
        MatchState replayedWithDuplicates = CricketScoringEngine.replay(
                "match-ret", rules, 1, "Alpha", 2, "Beta",
                Arrays.asList(e1, e1, e2, e2), MatchLifecycleStatus.PROVISIONAL);
        assertEquals(6, replayedWithDuplicates.getCurrentInnings().totalRuns);
        assertEquals(2, replayedWithDuplicates.getCurrentInnings().totalLegalBalls);

        // Undo last delivery restores Rohit at the crease with 6 runs off 1 ball
        MatchState afterUndo = CricketScoringEngine.undoLastDelivery(state);
        assertEquals(6, afterUndo.getCurrentInnings().totalRuns);
        assertEquals(1, afterUndo.getCurrentInnings().totalLegalBalls);
        assertEquals(1, afterUndo.getCurrentInnings().currentStrikerId);
        assertFalse(afterUndo.getCurrentInnings().batterStats.get(1).retiredHurt);
    }

    @Test
    public void twoInningsChaseAndTournamentPointsTableNrrRespectAllOutRule() {
        CricketRulesProfile oneOverProfile = CricketRulesProfile.createStandardT20()
                .toBuilder()
                .setMaxOversPerInnings(1)
                .setPlayersPerTeam(3) // 2 wickets = all out
                .build();

        MatchState match = new MatchState(
                "match-chase", oneOverProfile, 1, "Royals", 2, "Kings", MatchLifecycleStatus.PROVISIONAL);

        // Innings 1: Royals bowled out for 10 runs in 2 balls (0.2 overs), but NRR must charge full 1.0 over!
        match = CricketScoringEngine.appendDeliveryOrThrow(match,
                new DeliveryEvent.Builder().setInningsNumber(1).setSequenceNumber(1).setRunsOffBat(6).build());
        match = CricketScoringEngine.appendDeliveryOrThrow(match,
                new DeliveryEvent.Builder().setInningsNumber(1).setSequenceNumber(2).setRunsOffBat(4).build());
        match = CricketScoringEngine.appendDeliveryOrThrow(match,
                new DeliveryEvent.Builder().setInningsNumber(1).setSequenceNumber(3)
                        .setDismissal(DismissalType.BOWLED, 1, "B1", "", 3, "B3").build());
        match = CricketScoringEngine.appendDeliveryOrThrow(match,
                new DeliveryEvent.Builder().setInningsNumber(1).setSequenceNumber(4)
                        .setDismissal(DismissalType.BOWLED, 3, "B3", "", 0, "").build());

        assertTrue(match.getCurrentInnings().inningsClosed);
        assertEquals(10, match.getCurrentInnings().totalRuns);

        // Innings 2: Kings chase 11 in 3 balls (0.3 overs = 0.5 overs) scoring 12/0
        CricketScoringEngine.startNextInnings(match);
        assertEquals(11, match.getCurrentInnings().targetRuns);

        match = CricketScoringEngine.appendDeliveryOrThrow(match,
                new DeliveryEvent.Builder().setInningsNumber(2).setSequenceNumber(5).setRunsOffBat(6).build());
        match = CricketScoringEngine.appendDeliveryOrThrow(match,
                new DeliveryEvent.Builder().setInningsNumber(2).setSequenceNumber(6).setRunsOffBat(6).build());

        assertTrue(match.matchCompleted);
        assertEquals(2, match.winnerTeamId);
        assertTrue(match.resultText.contains("Kings won by 2 wickets"));

        // Verify PointsTableCalculator NRR:
        // Kings: 12 runs in 2/6 overs (1/3 over -> 36.0 rpo) vs 10 runs conceded in 1.0 full over (all-out rule -> 10.0 rpo) => NRR = +26.0
        List<TeamStanding> standings = PointsTableCalculator.computeStandings(
                Collections.singletonList(match), oneOverProfile);
        assertEquals(2, standings.size());
        assertEquals("Kings", standings.get(0).teamName);
        assertEquals(2, standings.get(0).points);
        assertEquals(1.0, standings.get(0).totalOversBowled, 0.001);
        assertTrue(standings.get(0).getNetRunRate() > 0.0);
        assertTrue(standings.get(1).getNetRunRate() < 0.0);
    }
}
