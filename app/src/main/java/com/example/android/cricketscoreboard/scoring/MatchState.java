package com.example.android.cricketscoreboard.scoring;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Deterministic derived state of a cricket match produced by {@link CricketScoringEngine}
 * from an ordered sequence of {@link DeliveryEvent} objects and an immutable {@link CricketRulesProfile}.
 */
public final class MatchState {

    public enum MatchLifecycleStatus {
        /** Live or unverified scoring session; may be edited or undone by the active scorer. */
        PROVISIONAL,
        /** Confirmed by the official scorer; aggregate stats committed to local/cloud storage. */
        CONFIRMED,
        /** Finalized and locked by the competition organizer. */
        FINALIZED
    }

    public static final class BatterInningsStat {
        public final int playerId;
        public String playerName;
        public int runs;
        public int ballsFaced;
        public int fours;
        public int sixes;
        public int dotBalls;
        public DeliveryEvent.DismissalType dismissalType = DeliveryEvent.DismissalType.NONE;
        public String dismissedByBowler = "";
        public String fielderName = "";
        public boolean isOut = false;
        public boolean retiredHurt = false;

        public BatterInningsStat(int playerId, String playerName) {
            this.playerId = playerId;
            this.playerName = playerName == null || playerName.isEmpty() ? ("Batter " + playerId) : playerName;
        }

        public double getStrikeRate() {
            if (ballsFaced <= 0) {
                return 0.0;
            }
            return (runs * 100.0) / ballsFaced;
        }

        public String getDismissalDescription() {
            if (retiredHurt) {
                return "retired hurt";
            }
            if (!isOut || dismissalType == DeliveryEvent.DismissalType.NONE) {
                return "not out";
            }
            switch (dismissalType) {
                case BOWLED:
                    return "b " + dismissedByBowler;
                case CAUGHT:
                    return (fielderName != null && !fielderName.isEmpty()
                            ? "c " + fielderName + " " : "c & ") + "b " + dismissedByBowler;
                case LBW:
                    return "lbw b " + dismissedByBowler;
                case STUMPED:
                    return "st " + (fielderName.isEmpty() ? "wk" : fielderName) + " b " + dismissedByBowler;
                case RUN_OUT:
                    return "run out" + (fielderName.isEmpty() ? "" : " (" + fielderName + ")");
                case HIT_WICKET:
                    return "hit wicket b " + dismissedByBowler;
                case RETIRED_OUT:
                    return "retired out";
                case OBSTRUCTING_FIELD:
                    return "obstructing the field";
                case HIT_BALL_TWICE:
                    return "hit the ball twice";
                case TIMED_OUT:
                    return "timed out";
                default:
                    return "out";
            }
        }
    }

    public static final class BowlerSpellStat {
        public final int playerId;
        public String playerName;
        public final int ballsPerOver;
        public int legalBallsBowled;
        public int maidens;
        public int runsConceded;
        public int wickets;
        public int wides;
        public int noBalls;
        public int dotBalls;

        public BowlerSpellStat(int playerId, String playerName, int ballsPerOver) {
            this.playerId = playerId;
            this.playerName = playerName == null || playerName.isEmpty() ? ("Bowler " + playerId) : playerName;
            this.ballsPerOver = Math.max(1, ballsPerOver);
        }

        public int getCompletedOvers() {
            return legalBallsBowled / ballsPerOver;
        }

        public int getBallsInCurrentOver() {
            return legalBallsBowled % ballsPerOver;
        }

        public String getOversNotation() {
            return getCompletedOvers() + "." + getBallsInCurrentOver();
        }

        public double getEconomyRate() {
            if (legalBallsBowled <= 0) {
                return 0.0;
            }
            double exactOvers = (double) legalBallsBowled / ballsPerOver;
            return runsConceded / exactOvers;
        }
    }

    public static final class FallOfWicket {
        public final int wicketNumber;
        public final int scoreAtFall;
        public final String oversNotationAtFall;
        public final int dismissedPlayerId;
        public final String dismissedPlayerName;
        public final String dismissalText;

        public FallOfWicket(
                int wicketNumber,
                int scoreAtFall,
                String oversNotationAtFall,
                int dismissedPlayerId,
                String dismissedPlayerName,
                String dismissalText) {
            this.wicketNumber = wicketNumber;
            this.scoreAtFall = scoreAtFall;
            this.oversNotationAtFall = oversNotationAtFall;
            this.dismissedPlayerId = dismissedPlayerId;
            this.dismissedPlayerName = dismissedPlayerName;
            this.dismissalText = dismissalText;
        }
    }

    public static final class Partnership {
        public int batter1Id;
        public String batter1Name;
        public int batter2Id;
        public String batter2Name;
        public int totalRuns;
        public int totalBalls;
        public int batter1Runs;
        public int batter2Runs;
        public int extrasInPartnership;

        public Partnership(int b1Id, String b1Name, int b2Id, String b2Name) {
            this.batter1Id = b1Id;
            this.batter1Name = b1Name;
            this.batter2Id = b2Id;
            this.batter2Name = b2Name;
        }
    }

    public static final class OverSummary {
        public final int overNumber;
        public final String bowlerName;
        public final int runsInOver;
        public final int wicketsInOver;
        public final boolean isMaiden;
        public final List<String> ballTokens;
        public final int cumulativeRunsAfterOver;
        public final int cumulativeWicketsAfterOver;

        public OverSummary(
                int overNumber,
                String bowlerName,
                int runsInOver,
                int wicketsInOver,
                boolean isMaiden,
                List<String> ballTokens,
                int cumulativeRunsAfterOver,
                int cumulativeWicketsAfterOver) {
            this.overNumber = overNumber;
            this.bowlerName = bowlerName;
            this.runsInOver = runsInOver;
            this.wicketsInOver = wicketsInOver;
            this.isMaiden = isMaiden;
            this.ballTokens = new ArrayList<>(ballTokens);
            this.cumulativeRunsAfterOver = cumulativeRunsAfterOver;
            this.cumulativeWicketsAfterOver = cumulativeWicketsAfterOver;
        }
    }

    public static final class InningsScorecard {
        public final int inningsNumber;
        public final int battingTeamId;
        public final String battingTeamName;
        public final int bowlingTeamId;
        public final String bowlingTeamName;
        public final int ballsPerOver;

        public int totalRuns;
        public int wickets;
        public int completedOvers;
        public int ballsInCurrentOver;
        public int totalLegalBalls;

        public int extrasWide;
        public int extrasNoBall;
        public int extrasBye;
        public int extrasLegBye;
        public int extrasPenalty;

        public int targetRuns;
        public boolean freeHitArmed;
        public boolean inningsClosed;
        public String closureReason = "";

        public int currentStrikerId = 1;
        public String currentStrikerName = "Batter 1";
        public int currentNonStrikerId = 2;
        public String currentNonStrikerName = "Batter 2";
        public int currentBowlerId = 101;
        public String currentBowlerName = "Bowler 1";

        public final Map<Integer, BatterInningsStat> batterStats = new LinkedHashMap<>();
        public final Map<Integer, BowlerSpellStat> bowlerStats = new LinkedHashMap<>();
        public final List<FallOfWicket> fallOfWickets = new ArrayList<>();
        public final List<OverSummary> overSummaries = new ArrayList<>();
        public final List<String> currentOverTokens = new ArrayList<>();
        public final List<String> commentaryLog = new ArrayList<>();
        public Partnership currentPartnership;

        /** Legacy-compatible semicolon-delimited commentary string for {@code ScoreMan}. */
        public final StringBuilder legacyCommentaryBuilder = new StringBuilder(":");

        public InningsScorecard(
                int inningsNumber,
                int battingTeamId,
                String battingTeamName,
                int bowlingTeamId,
                String bowlingTeamName,
                int ballsPerOver) {
            this.inningsNumber = inningsNumber;
            this.battingTeamId = battingTeamId;
            this.battingTeamName = battingTeamName == null || battingTeamName.isEmpty() ? "Team A" : battingTeamName;
            this.bowlingTeamId = bowlingTeamId;
            this.bowlingTeamName = bowlingTeamName == null || bowlingTeamName.isEmpty() ? "Team B" : bowlingTeamName;
            this.ballsPerOver = Math.max(1, ballsPerOver);
            this.currentPartnership = new Partnership(
                    currentStrikerId, currentStrikerName, currentNonStrikerId, currentNonStrikerName);
        }

        public int getTotalExtras() {
            return extrasWide + extrasNoBall + extrasBye + extrasLegBye + extrasPenalty;
        }

        public String getOversNotation() {
            return completedOvers + "." + ballsInCurrentOver;
        }

        public String getThisOverDisplayString() {
            if (currentOverTokens.isEmpty()) {
                if (!overSummaries.isEmpty()) {
                    OverSummary last = overSummaries.get(overSummaries.size() - 1);
                    return ";" + String.join(";", last.ballTokens) + "||";
                }
                return "";
            }
            return ";" + String.join(";", currentOverTokens);
        }

        public double getCurrentRunRate() {
            if (totalLegalBalls <= 0) {
                return 0.0;
            }
            double exactOvers = (double) totalLegalBalls / ballsPerOver;
            return totalRuns / exactOvers;
        }

        public double getRequiredRunRate(int maxLegalBalls) {
            if (targetRuns <= 0 || maxLegalBalls <= 0) {
                return 0.0;
            }
            int runsNeeded = Math.max(0, targetRuns - totalRuns);
            int ballsRemaining = maxLegalBalls - totalLegalBalls;
            if (ballsRemaining <= 0) {
                return runsNeeded > 0 ? 99.99 : 0.0;
            }
            double oversRemaining = (double) ballsRemaining / ballsPerOver;
            return runsNeeded / oversRemaining;
        }

        public String getScoreSlashWickets() {
            return totalRuns + "/" + wickets;
        }

        public String getFormattedSummaryLine() {
            return String.format(Locale.US, "%s %d/%d (%s ov, CRR %.2f)",
                    battingTeamName, totalRuns, wickets, getOversNotation(), getCurrentRunRate());
        }
    }

    public final String matchUuid;
    public final CricketRulesProfile rulesProfile;
    public final int teamAId;
    public final String teamAName;
    public final int teamBId;
    public final String teamBName;
    public MatchLifecycleStatus lifecycleStatus;
    public int currentInningsNumber;
    public final List<InningsScorecard> inningsList = new ArrayList<>();
    public final List<DeliveryEvent> appliedEvents = new ArrayList<>();
    public boolean matchCompleted;
    public int winnerTeamId;
    public boolean isTied;
    public String resultText = "Match in progress";

    public MatchState(
            String matchUuid,
            CricketRulesProfile rulesProfile,
            int teamAId,
            String teamAName,
            int teamBId,
            String teamBName,
            MatchLifecycleStatus lifecycleStatus) {
        this.matchUuid = matchUuid;
        this.rulesProfile = rulesProfile == null ? CricketRulesProfile.createStandardT20() : rulesProfile;
        this.teamAId = teamAId;
        this.teamAName = teamAName == null || teamAName.isEmpty() ? "Team A" : teamAName;
        this.teamBId = teamBId;
        this.teamBName = teamBName == null || teamBName.isEmpty() ? "Team B" : teamBName;
        this.lifecycleStatus = lifecycleStatus == null ? MatchLifecycleStatus.PROVISIONAL : lifecycleStatus;
        this.currentInningsNumber = 1;
        this.inningsList.add(new InningsScorecard(
                1, this.teamAId, this.teamAName, this.teamBId, this.teamBName, this.rulesProfile.ballsPerOver));
    }

    public InningsScorecard getCurrentInnings() {
        int idx = Math.max(0, Math.min(currentInningsNumber - 1, inningsList.size() - 1));
        return inningsList.get(idx);
    }

    public InningsScorecard getOrCreateInnings(int inningsNumber) {
        while (inningsList.size() < inningsNumber) {
            int nextNum = inningsList.size() + 1;
            boolean oddInnings = (nextNum % 2 == 1);
            int batId = oddInnings ? teamAId : teamBId;
            String batName = oddInnings ? teamAName : teamBName;
            int bowlId = oddInnings ? teamBId : teamAId;
            String bowlName = oddInnings ? teamBName : teamAName;
            InningsScorecard next = new InningsScorecard(
                    nextNum, batId, batName, bowlId, bowlName, rulesProfile.ballsPerOver);
            if (nextNum == 2 && !inningsList.isEmpty()) {
                next.targetRuns = inningsList.get(0).totalRuns + 1;
            }
            inningsList.add(next);
        }
        return inningsList.get(inningsNumber - 1);
    }
}
