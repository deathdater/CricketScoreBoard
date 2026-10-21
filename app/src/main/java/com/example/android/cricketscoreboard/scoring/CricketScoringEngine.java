package com.example.android.cricketscoreboard.scoring;

import com.example.android.cricketscoreboard.scoring.DeliveryEvent.DismissalType;
import com.example.android.cricketscoreboard.scoring.DeliveryEvent.ExtraType;
import com.example.android.cricketscoreboard.scoring.MatchState.BatterInningsStat;
import com.example.android.cricketscoreboard.scoring.MatchState.BowlerSpellStat;
import com.example.android.cricketscoreboard.scoring.MatchState.FallOfWicket;
import com.example.android.cricketscoreboard.scoring.MatchState.InningsScorecard;
import com.example.android.cricketscoreboard.scoring.MatchState.MatchLifecycleStatus;
import com.example.android.cricketscoreboard.scoring.MatchState.OverSummary;
import com.example.android.cricketscoreboard.scoring.MatchState.Partnership;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pure, deterministic, event-sourced cricket scoring engine independent of Android UI and cloud storage.
 *
 * <p>All totals, batting/bowling scorecards, partnerships, fall of wickets, over summaries, and
 * match results are derived reproducibly from an immutable {@link CricketRulesProfile} and an
 * ordered list of {@link DeliveryEvent} objects.
 */
public final class CricketScoringEngine {

    public static final class ValidationResult {
        public final boolean valid;
        public final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult ok() {
            return new ValidationResult(true, "");
        }

        public static ValidationResult error(String message) {
            return new ValidationResult(false, message);
        }
    }

    private CricketScoringEngine() {
    }

    /**
     * Validates a candidate {@link DeliveryEvent} against the current {@link MatchState}
     * and active {@link CricketRulesProfile}.
     */
    public static ValidationResult validateDelivery(MatchState state, DeliveryEvent event) {
        if (state == null || event == null) {
            return ValidationResult.error("Match state and delivery event are required.");
        }
        if (state.lifecycleStatus == MatchLifecycleStatus.FINALIZED) {
            return ValidationResult.error("Match is finalized and locked against further scoring.");
        }
        if (state.matchCompleted) {
            return ValidationResult.error("Match is already completed (" + state.resultText + ").");
        }
        InningsScorecard innings = state.getOrCreateInnings(event.inningsNumber);
        if (innings.inningsClosed) {
            return ValidationResult.error("Innings " + event.inningsNumber + " is already closed (" + innings.closureReason + ").");
        }

        CricketRulesProfile rules = state.rulesProfile;
        boolean isFreeHit = innings.freeHitArmed || event.wasFreeHit;

        // 1. Validate dismissals on Wide deliveries (MCC Law 22)
        if (event.extraType == ExtraType.WIDE && event.dismissalType != DismissalType.NONE) {
            switch (event.dismissalType) {
                case STUMPED:
                case RUN_OUT:
                case HIT_WICKET:
                case OBSTRUCTING_FIELD:
                case RETIRED_HURT:
                case RETIRED_OUT:
                    break;
                default:
                    return ValidationResult.error(
                            "Invalid dismissal on a Wide ball: " + event.dismissalType + " is not permitted under cricket laws.");
            }
        }

        // 2. Validate dismissals on a No-Ball (MCC Law 21)
        if (event.extraType == ExtraType.NO_BALL && event.dismissalType != DismissalType.NONE) {
            switch (event.dismissalType) {
                case RUN_OUT:
                case OBSTRUCTING_FIELD:
                case HIT_BALL_TWICE:
                case RETIRED_HURT:
                case RETIRED_OUT:
                    break;
                default:
                    return ValidationResult.error(
                            "Invalid dismissal on a No-Ball: " + event.dismissalType + " is not permitted.");
            }
        }

        // 3. Validate dismissals on a Free Hit (MCC / ICC Playing Conditions)
        if (isFreeHit && event.dismissalType != DismissalType.NONE) {
            switch (event.dismissalType) {
                case RUN_OUT:
                case OBSTRUCTING_FIELD:
                case HIT_BALL_TWICE:
                case RETIRED_HURT:
                case RETIRED_OUT:
                    break;
                default:
                    return ValidationResult.error(
                            "Free Hit active: batter cannot be dismissed " + event.dismissalType + " on a Free Hit.");
            }
        }

        // 4. Validate bowler over quota at the start of a new over
        if (rules.maxOversPerBowler > 0 && innings.ballsInCurrentOver == 0 && event.bowlerId > 0) {
            BowlerSpellStat spell = innings.bowlerStats.get(event.bowlerId);
            if (spell != null && spell.getCompletedOvers() >= rules.maxOversPerBowler) {
                return ValidationResult.error("Bowler " + spell.playerName
                        + " has already completed the maximum quota of "
                        + rules.maxOversPerBowler + " overs.");
            }
        }

        return ValidationResult.ok();
    }

    /**
     * Reconstructs the entire {@link MatchState} deterministically by replaying an ordered list
     * of {@link DeliveryEvent} objects from scratch.
     *
     * <p>Duplicate events sharing the same {@code eventUuid} are ignored so replay and network
     * retries are strictly idempotent.
     */
    public static MatchState replay(
            String matchUuid,
            CricketRulesProfile rulesProfile,
            int teamAId,
            String teamAName,
            int teamBId,
            String teamBName,
            List<DeliveryEvent> events,
            MatchLifecycleStatus lifecycleStatus) {
        MatchState state = new MatchState(
                matchUuid, rulesProfile, teamAId, teamAName, teamBId, teamBName, lifecycleStatus);
        if (events == null || events.isEmpty()) {
            return state;
        }
        Set<String> seenEventUuids = new HashSet<>();
        int currentOverBowlerRuns = 0;
        int currentOverTotalRuns = 0;
        int currentOverWickets = 0;
        int lastInningsNumber = 1;

        for (DeliveryEvent event : events) {
            if (event == null || seenEventUuids.contains(event.eventUuid)) {
                continue;
            }
            if (event.inningsNumber != lastInningsNumber) {
                currentOverBowlerRuns = 0;
                currentOverTotalRuns = 0;
                currentOverWickets = 0;
                lastInningsNumber = event.inningsNumber;
            }
            seenEventUuids.add(event.eventUuid);
            int[] overAccumulators = new int[]{currentOverBowlerRuns, currentOverTotalRuns, currentOverWickets};
            applySingleEventInternal(state, event, overAccumulators);
            currentOverBowlerRuns = overAccumulators[0];
            currentOverTotalRuns = overAccumulators[1];
            currentOverWickets = overAccumulators[2];
        }
        return state;
    }

    /**
     * Validates and appends a new {@link DeliveryEvent}, returning the updated {@link MatchState}.
     * Throws {@link IllegalArgumentException} if the delivery violates active rules.
     */
    public static MatchState appendDeliveryOrThrow(MatchState currentState, DeliveryEvent newEvent) {
        ValidationResult vr = validateDelivery(currentState, newEvent);
        if (!vr.valid) {
            throw new IllegalArgumentException(vr.errorMessage);
        }
        List<DeliveryEvent> nextEvents = new ArrayList<>(currentState.appliedEvents);
        nextEvents.add(newEvent);
        return replay(
                currentState.matchUuid,
                currentState.rulesProfile,
                currentState.teamAId,
                currentState.teamAName,
                currentState.teamBId,
                currentState.teamBName,
                nextEvents,
                currentState.lifecycleStatus);
    }

    /**
     * Undoes the most recent delivery event and deterministically recomputes the {@link MatchState}.
     */
    public static MatchState undoLastDelivery(MatchState currentState) {
        if (currentState == null || currentState.appliedEvents.isEmpty()) {
            return currentState;
        }
        List<DeliveryEvent> remaining = new ArrayList<>(
                currentState.appliedEvents.subList(0, currentState.appliedEvents.size() - 1));
        return replay(
                currentState.matchUuid,
                currentState.rulesProfile,
                currentState.teamAId,
                currentState.teamAName,
                currentState.teamBId,
                currentState.teamBName,
                remaining,
                currentState.lifecycleStatus);
    }

    /**
     * Transitions the match to the next innings (e.g., after declaration or innings break).
     */
    public static void startNextInnings(MatchState state) {
        if (state == null || state.matchCompleted) {
            return;
        }
        InningsScorecard current = state.getCurrentInnings();
        if (!current.inningsClosed) {
            current.inningsClosed = true;
            current.closureReason = "Declared / Innings Break";
        }
        int maxInnings = state.rulesProfile.inningsPerTeam * 2;
        if (state.currentInningsNumber < maxInnings) {
            state.currentInningsNumber++;
            state.getOrCreateInnings(state.currentInningsNumber);
        }
    }

    private static void applySingleEventInternal(
            MatchState state,
            DeliveryEvent event,
            int[] overAcc) {
        ValidationResult vr = validateDelivery(state, event);
        if (!vr.valid) {
            return;
        }

        CricketRulesProfile rules = state.rulesProfile;
        state.currentInningsNumber = event.inningsNumber;
        InningsScorecard innings = state.getOrCreateInnings(event.inningsNumber);
        state.appliedEvents.add(event);

        // Bind active striker, non-striker, and bowler identities if supplied
        if (event.strikerId > 0) {
            innings.currentStrikerId = event.strikerId;
            innings.currentStrikerName = event.strikerName;
        }
        if (event.nonStrikerId > 0) {
            innings.currentNonStrikerId = event.nonStrikerId;
            innings.currentNonStrikerName = event.nonStrikerName;
        }
        if (event.bowlerId > 0) {
            innings.currentBowlerId = event.bowlerId;
            innings.currentBowlerName = event.bowlerName;
        }

        BatterInningsStat strikerStat = innings.batterStats.computeIfAbsent(
                innings.currentStrikerId,
                id -> new BatterInningsStat(id, innings.currentStrikerName));
        strikerStat.playerName = innings.currentStrikerName;

        BatterInningsStat nonStrikerStat = innings.batterStats.computeIfAbsent(
                innings.currentNonStrikerId,
                id -> new BatterInningsStat(id, innings.currentNonStrikerName));
        nonStrikerStat.playerName = innings.currentNonStrikerName;

        BowlerSpellStat bowlerStat = innings.bowlerStats.computeIfAbsent(
                innings.currentBowlerId,
                id -> new BowlerSpellStat(id, innings.currentBowlerName, rules.ballsPerOver));
        bowlerStat.playerName = innings.currentBowlerName;

        int batterRuns = 0;
        int bowlerRuns = 0;
        int totalDeliveryRuns = 0;
        int physicalRunsBetweenWickets = 0;
        boolean isLegalDelivery = true;
        boolean batterFacedBall = true;

        switch (event.extraType) {
            case WIDE: {
                int penalty = rules.wideBallPenaltyRuns;
                int additional = event.runsOffBat + event.extraRuns + event.overthrowRuns;
                totalDeliveryRuns = penalty + additional;
                bowlerRuns = totalDeliveryRuns;
                innings.extrasWide += totalDeliveryRuns;
                bowlerStat.wides++;
                physicalRunsBetweenWickets = additional;
                isLegalDelivery = !rules.rebowlWideAndNoBall;
                batterFacedBall = false;
                break;
            }
            case NO_BALL: {
                int penalty = rules.noBallPenaltyRuns;
                batterRuns = event.runsOffBat + event.overthrowRuns;
                totalDeliveryRuns = penalty + batterRuns + event.extraRuns;
                bowlerRuns = totalDeliveryRuns;
                innings.extrasNoBall += penalty + event.extraRuns;
                bowlerStat.noBalls++;
                physicalRunsBetweenWickets = batterRuns + event.extraRuns;
                isLegalDelivery = !rules.rebowlWideAndNoBall;
                batterFacedBall = true;
                break;
            }
            case BYE: {
                int byes = (event.extraRuns > 0 ? event.extraRuns : event.runsOffBat) + event.overthrowRuns;
                totalDeliveryRuns = byes;
                innings.extrasBye += byes;
                physicalRunsBetweenWickets = byes;
                isLegalDelivery = true;
                batterFacedBall = true;
                break;
            }
            case LEG_BYE: {
                int legByes = (event.extraRuns > 0 ? event.extraRuns : event.runsOffBat) + event.overthrowRuns;
                totalDeliveryRuns = legByes;
                innings.extrasLegBye += legByes;
                physicalRunsBetweenWickets = legByes;
                isLegalDelivery = true;
                batterFacedBall = true;
                break;
            }
            case PENALTY: {
                int pen = event.extraRuns > 0 ? event.extraRuns : 5;
                totalDeliveryRuns = pen;
                innings.extrasPenalty += pen;
                physicalRunsBetweenWickets = 0;
                isLegalDelivery = false;
                batterFacedBall = false;
                break;
            }
            case NONE:
            default: {
                batterRuns = event.runsOffBat + event.overthrowRuns;
                totalDeliveryRuns = batterRuns;
                bowlerRuns = batterRuns;
                physicalRunsBetweenWickets = batterRuns;
                isLegalDelivery = true;
                batterFacedBall = true;
                break;
            }
        }

        // Update team, batter, bowler, and partnership totals
        innings.totalRuns += totalDeliveryRuns;
        strikerStat.runs += batterRuns;
        if (event.isBoundaryFour && batterRuns >= 4) {
            strikerStat.fours++;
        }
        if (event.isBoundarySix && batterRuns >= 6) {
            strikerStat.sixes++;
        }
        if (batterFacedBall) {
            strikerStat.ballsFaced++;
            if (batterRuns == 0) {
                strikerStat.dotBalls++;
            }
            innings.currentPartnership.totalBalls++;
        }
        innings.currentPartnership.totalRuns += totalDeliveryRuns;
        if (innings.currentStrikerId == innings.currentPartnership.batter1Id) {
            innings.currentPartnership.batter1Runs += batterRuns;
        } else {
            innings.currentPartnership.batter2Runs += batterRuns;
        }
        innings.currentPartnership.extrasInPartnership += (totalDeliveryRuns - batterRuns);

        bowlerStat.runsConceded += bowlerRuns;
        overAcc[0] += bowlerRuns;
        overAcc[1] += totalDeliveryRuns;

        if (isLegalDelivery) {
            innings.totalLegalBalls++;
            innings.ballsInCurrentOver++;
            bowlerStat.legalBallsBowled++;
            if (bowlerRuns == 0) {
                bowlerStat.dotBalls++;
            }
        }

        // Update Free Hit state machine
        if (event.extraType == ExtraType.NO_BALL && rules.freeHitAfterNoBall) {
            innings.freeHitArmed = true;
        } else if (isLegalDelivery) {
            innings.freeHitArmed = false;
        }

        // Record ball token and commentary
        String token = event.toBallToken();
        innings.currentOverTokens.add(token);
        innings.legacyCommentaryBuilder.append(";").append(token);
        String overBallLabel = innings.completedOvers + "." + innings.ballsInCurrentOver;
        String commLine = overBallLabel + " " + innings.currentBowlerName + " to "
                + innings.currentStrikerName + ": " + token
                + (event.note.isEmpty() ? "" : " (" + event.note + ")");
        innings.commentaryLog.add(commLine);

        // Strike rotation from physical runs or batters crossing before catch
        boolean swapEnds = ((physicalRunsBetweenWickets % 2) != 0) || event.crossedBeforeCatch;
        if (swapEnds) {
            swapBattersAtCrease(innings);
        }

        // Handle dismissals or compulsory retirement
        if (event.dismissalType == DismissalType.RETIRED_HURT) {
            int hurtId = event.dismissedPlayerId > 0 ? event.dismissedPlayerId : event.strikerId;
            BatterInningsStat hurtStat = innings.batterStats.getOrDefault(hurtId, strikerStat);
            hurtStat.retiredHurt = true;
            hurtStat.isOut = false;
            bringInNextBatter(innings, hurtId, event.newBatterId, event.newBatterName);
        } else if (event.isTeamWicket()) {
            innings.wickets++;
            overAcc[2]++;
            int outId = event.dismissedPlayerId > 0 ? event.dismissedPlayerId : event.strikerId;
            BatterInningsStat outStat = innings.batterStats.getOrDefault(outId, strikerStat);
            outStat.isOut = true;
            outStat.retiredHurt = false;
            outStat.dismissalType = event.dismissalType;
            outStat.dismissedByBowler = innings.currentBowlerName;
            outStat.fielderName = event.fielderName;

            if (event.isBowlerWicket()) {
                bowlerStat.wickets++;
            }

            innings.fallOfWickets.add(new FallOfWicket(
                    innings.wickets,
                    innings.totalRuns,
                    innings.getOversNotation(),
                    outStat.playerId,
                    outStat.playerName,
                    outStat.getDismissalDescription()));

            if (innings.wickets < rules.getMaxWicketsPerInnings()) {
                bringInNextBatter(innings, outId, event.newBatterId, event.newBatterName);
            }
        } else if (rules.retireAtScore > 0 && strikerStat.runs >= rules.retireAtScore) {
            strikerStat.retiredHurt = true;
            strikerStat.isOut = false;
            bringInNextBatter(innings, strikerStat.playerId, event.newBatterId, event.newBatterName);
        }

        // Check End of Over progression
        if (innings.ballsInCurrentOver >= rules.ballsPerOver) {
            boolean isMaiden = (overAcc[0] == 0);
            if (isMaiden) {
                bowlerStat.maidens++;
            }
            innings.overSummaries.add(new OverSummary(
                    innings.completedOvers + 1,
                    innings.currentBowlerName,
                    overAcc[1],
                    overAcc[2],
                    isMaiden,
                    Collections.unmodifiableList(new ArrayList<>(innings.currentOverTokens)),
                    innings.totalRuns,
                    innings.wickets));

            innings.completedOvers++;
            innings.ballsInCurrentOver = 0;
            innings.currentOverTokens.clear();
            innings.legacyCommentaryBuilder.append("||");
            overAcc[0] = 0;
            overAcc[1] = 0;
            overAcc[2] = 0;

            // Swap ends at end of over
            swapBattersAtCrease(innings);
        }

        // Evaluate Innings Completion & Match Result
        evaluateInningsAndMatchStatus(state, innings);
    }

    private static void swapBattersAtCrease(InningsScorecard innings) {
        int tmpId = innings.currentStrikerId;
        String tmpName = innings.currentStrikerName;
        innings.currentStrikerId = innings.currentNonStrikerId;
        innings.currentStrikerName = innings.currentNonStrikerName;
        innings.currentNonStrikerId = tmpId;
        innings.currentNonStrikerName = tmpName;
    }

    private static void bringInNextBatter(
            InningsScorecard innings,
            int outgoingPlayerId,
            int explicitNewBatterId,
            String explicitNewBatterName) {
        int nextId = explicitNewBatterId > 0 ? explicitNewBatterId : (innings.batterStats.size() + 1);
        while (innings.batterStats.containsKey(nextId) && explicitNewBatterId <= 0) {
            nextId++;
        }
        String nextName = (explicitNewBatterName != null && !explicitNewBatterName.isEmpty())
                ? explicitNewBatterName : ("Batter " + nextId);

        innings.batterStats.computeIfAbsent(nextId, id -> new BatterInningsStat(id, nextName));

        if (innings.currentStrikerId == outgoingPlayerId) {
            innings.currentStrikerId = nextId;
            innings.currentStrikerName = nextName;
        } else if (innings.currentNonStrikerId == outgoingPlayerId) {
            innings.currentNonStrikerId = nextId;
            innings.currentNonStrikerName = nextName;
        } else {
            innings.currentStrikerId = nextId;
            innings.currentStrikerName = nextName;
        }

        innings.currentPartnership = new Partnership(
                innings.currentStrikerId,
                innings.currentStrikerName,
                innings.currentNonStrikerId,
                innings.currentNonStrikerName);
    }

    private static void evaluateInningsAndMatchStatus(MatchState state, InningsScorecard innings) {
        CricketRulesProfile rules = state.rulesProfile;
        int maxWickets = rules.getMaxWicketsPerInnings();

        // Check if chasing team reached target
        if (innings.targetRuns > 0 && innings.totalRuns >= innings.targetRuns) {
            innings.inningsClosed = true;
            innings.closureReason = "Target reached";
            state.matchCompleted = true;
            state.winnerTeamId = innings.battingTeamId;
            state.isTied = false;
            int wicketsRemaining = Math.max(1, maxWickets - innings.wickets);
            state.resultText = innings.battingTeamName + " won by " + wicketsRemaining
                    + (wicketsRemaining == 1 ? " wicket" : " wickets");
            return;
        }

        if (innings.wickets >= maxWickets) {
            innings.inningsClosed = true;
            innings.closureReason = "All out";
        } else if (rules.maxOversPerInnings > 0 && innings.completedOvers >= rules.maxOversPerInnings) {
            innings.inningsClosed = true;
            innings.closureReason = "Overs completed";
        }

        if (!innings.inningsClosed) {
            if (innings.targetRuns > 0) {
                int needed = innings.targetRuns - innings.totalRuns;
                int ballsLeft = Math.max(0, rules.getMaxLegalBallsPerInnings() - innings.totalLegalBalls);
                state.resultText = innings.battingTeamName + " need " + needed + " runs in " + ballsLeft + " balls";
            } else {
                state.resultText = innings.getFormattedSummaryLine();
            }
            return;
        }

        // If 1st innings of a 1-innings-per-side match just closed, set target for 2nd innings
        int totalMatchInnings = rules.inningsPerTeam * 2;
        if (innings.inningsNumber == 1 && totalMatchInnings == 2) {
            int target = innings.totalRuns + 1;
            state.resultText = innings.battingTeamName + " scored " + innings.totalRuns + "/" + innings.wickets
                    + ". Target for " + innings.bowlingTeamName + ": " + target;
            return;
        }

        // If final innings closed without reaching target, determine winner or tie
        if (innings.inningsNumber == totalMatchInnings && state.inningsList.size() >= 2) {
            InningsScorecard first = state.inningsList.get(0);
            state.matchCompleted = true;
            if (innings.totalRuns > first.totalRuns) {
                state.winnerTeamId = innings.battingTeamId;
                state.isTied = false;
                int wicketsRemaining = Math.max(1, maxWickets - innings.wickets);
                state.resultText = innings.battingTeamName + " won by " + wicketsRemaining
                        + (wicketsRemaining == 1 ? " wicket" : " wickets");
            } else if (first.totalRuns > innings.totalRuns) {
                state.winnerTeamId = first.battingTeamId;
                state.isTied = false;
                int margin = first.totalRuns - innings.totalRuns;
                state.resultText = first.battingTeamName + " won by " + margin
                        + (margin == 1 ? " run" : " runs");
            } else {
                state.winnerTeamId = 0;
                state.isTied = true;
                state.resultText = "Match Tied (" + first.totalRuns + " - " + innings.totalRuns + ")"
                        + (rules.allowSuperOverOnTie ? " • Super Over Eligible" : "");
            }
        }
    }
}
