package com.example.android.cricketscoreboard.scoring;

import com.example.android.cricketscoreboard.scoring.MatchState.BatterInningsStat;
import com.example.android.cricketscoreboard.scoring.MatchState.BowlerSpellStat;
import com.example.android.cricketscoreboard.scoring.MatchState.FallOfWicket;
import com.example.android.cricketscoreboard.scoring.MatchState.InningsScorecard;
import com.example.android.cricketscoreboard.scoring.MatchState.OverSummary;

import java.util.Locale;

/**
 * Generates standard cricket statistics, visual chart series (Manhattan & Worm),
 * explicitly labeled model-based chase projections, and shareable live scorecard payloads
 * from a validated {@link MatchState}.
 */
public final class CricketAnalyticsCalculator {

    private CricketAnalyticsCalculator() {
    }

    /**
     * Computes percentage of total runs scored via boundaries (4s and 6s).
     */
    public static double computeBoundaryPercentage(InningsScorecard innings) {
        if (innings == null || innings.totalRuns <= 0) {
            return 0.0;
        }
        int boundaryRuns = 0;
        for (BatterInningsStat b : innings.batterStats.values()) {
            boundaryRuns += (b.fours * 4) + (b.sixes * 6);
        }
        return Math.min(100.0, (boundaryRuns * 100.0) / innings.totalRuns);
    }

    /**
     * Computes percentage of legal deliveries that yielded 0 bowler-attributable runs.
     */
    public static double computeDotBallPercentage(InningsScorecard innings) {
        if (innings == null || innings.totalLegalBalls <= 0) {
            return 0.0;
        }
        int dots = 0;
        for (BowlerSpellStat bw : innings.bowlerStats.values()) {
            dots += bw.dotBalls;
        }
        return Math.min(100.0, (dots * 100.0) / innings.totalLegalBalls);
    }

    /**
     * Computes model-projected first-innings total at current run rate, adjusted for wickets in hand.
     * Note: This is a model projection, not a guaranteed outcome.
     */
    public static int computeProjectedScore(InningsScorecard innings, CricketRulesProfile rules) {
        if (innings == null || rules == null || rules.maxOversPerInnings <= 0) {
            return innings == null ? 0 : innings.totalRuns;
        }
        if (innings.inningsClosed || innings.totalLegalBalls <= 0) {
            return innings.totalRuns;
        }
        double crr = innings.getCurrentRunRate();
        int maxWickets = rules.getMaxWicketsPerInnings();
        double wicketRetentionFactor = Math.max(0.55, 1.0 - (0.04 * innings.wickets / Math.max(1.0, maxWickets / 10.0)));
        double remainingOvers = Math.max(0.0,
                (rules.getMaxLegalBallsPerInnings() - innings.totalLegalBalls) / (double) rules.ballsPerOver);
        int projectedAdditional = (int) Math.round(crr * remainingOvers * wicketRetentionFactor);
        return innings.totalRuns + Math.max(0, projectedAdditional);
    }

    /**
     * Computes an estimated batting-side win probability (0..100%) during a chase.
     * Clearly labeled as a statistical model projection in all UI outputs.
     */
    public static int computeBattingWinProbabilityModel(MatchState state) {
        if (state == null) {
            return 50;
        }
        InningsScorecard current = state.getCurrentInnings();
        if (state.matchCompleted) {
            if (state.isTied) {
                return 50;
            }
            return state.winnerTeamId == current.battingTeamId ? 100 : 0;
        }
        if (current.targetRuns <= 0) {
            return 50;
        }
        int maxBalls = state.rulesProfile.getMaxLegalBallsPerInnings();
        double rrr = current.getRequiredRunRate(maxBalls);
        double crr = current.getCurrentRunRate() <= 0.1 ? 6.0 : current.getCurrentRunRate();
        int maxWickets = state.rulesProfile.getMaxWicketsPerInnings();
        double wicketRatio = (maxWickets - current.wickets) / (double) maxWickets;
        double rateRatio = crr / Math.max(0.5, rrr);
        double raw = 50.0 * rateRatio * (0.5 + 0.5 * wicketRatio);
        return (int) Math.max(5, Math.min(95, Math.round(raw)));
    }

    /**
     * Formats a complete, human-readable scorecard and commentary report for the Scorecard tab.
     */
    public static String buildFullScorecardReport(MatchState state) {
        if (state == null) {
            return "No active match session.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Format: ").append(state.rulesProfile.profileName)
                .append(" (v").append(state.rulesProfile.version).append(")\n");
        sb.append("Status: [").append(state.lifecycleStatus.name()).append("] ")
                .append(state.resultText).append("\n\n");

        for (InningsScorecard inn : state.inningsList) {
            sb.append("═══ Innings ").append(inn.inningsNumber).append(": ")
                    .append(inn.battingTeamName).append(" ")
                    .append(inn.totalRuns).append("/").append(inn.wickets)
                    .append(" (").append(inn.getOversNotation()).append(" ov) ═══\n");
            sb.append(String.format(Locale.US,
                    "CRR: %.2f  •  Extras: %d (wd %d, nb %d, b %d, lb %d, pen %d)\n",
                    inn.getCurrentRunRate(), inn.getTotalExtras(),
                    inn.extrasWide, inn.extrasNoBall, inn.extrasBye, inn.extrasLegBye, inn.extrasPenalty));
            if (inn.targetRuns > 0) {
                sb.append(String.format(Locale.US, "Target: %d  •  RRR: %.2f\n",
                        inn.targetRuns, inn.getRequiredRunRate(state.rulesProfile.getMaxLegalBallsPerInnings())));
            }
            sb.append("\nBATTING:\n");
            if (inn.batterStats.isEmpty()) {
                sb.append("  No deliveries recorded yet.\n");
            } else {
                for (BatterInningsStat b : inn.batterStats.values()) {
                    boolean atCrease = !b.isOut && !b.retiredHurt
                            && (b.playerId == inn.currentStrikerId || b.playerId == inn.currentNonStrikerId);
                    String marker = (b.playerId == inn.currentStrikerId && !inn.inningsClosed) ? "*" : (atCrease ? "" : "");
                    sb.append(String.format(Locale.US,
                            "  • %-14s %-18s %3d (%2db, 4s:%d, 6s:%d, SR:%.1f)\n",
                            b.playerName + marker,
                            b.getDismissalDescription(),
                            b.runs, b.ballsFaced, b.fours, b.sixes, b.getStrikeRate()));
                }
            }

            sb.append("\nBOWLING:\n");
            if (inn.bowlerStats.isEmpty()) {
                sb.append("  No overs bowled yet.\n");
            } else {
                for (BowlerSpellStat bw : inn.bowlerStats.values()) {
                    sb.append(String.format(Locale.US,
                            "  • %-14s %s ov  M:%d  R:%d  W:%d  Econ:%.2f (wd:%d, nb:%d)\n",
                            bw.playerName, bw.getOversNotation(), bw.maidens,
                            bw.runsConceded, bw.wickets, bw.getEconomyRate(), bw.wides, bw.noBalls));
                }
            }

            if (!inn.fallOfWickets.isEmpty()) {
                sb.append("\nFALL OF WICKETS:\n  ");
                for (int i = 0; i < inn.fallOfWickets.size(); i++) {
                    FallOfWicket fow = inn.fallOfWickets.get(i);
                    if (i > 0) {
                        sb.append(", ");
                    }
                    sb.append(fow.wicketNumber).append("-").append(fow.scoreAtFall)
                            .append(" (").append(fow.dismissedPlayerName)
                            .append(", ").append(fow.oversNotationAtFall).append(" ov)");
                }
                sb.append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Builds the visual Manhattan (runs per over) & Worm (cumulative progression) analytics report
     * with explicit separation between measured stats and model projections.
     */
    public static String buildOverByOverChartReport(MatchState state) {
        if (state == null) {
            return "No match analytics available.";
        }
        StringBuilder sb = new StringBuilder();
        InningsScorecard inn = state.getCurrentInnings();
        sb.append("MEASURED PERFORMANCE METRICS (").append(inn.battingTeamName).append(")\n");
        sb.append(String.format(Locale.US,
                "• Current Run Rate: %.2f rpo\n• Boundary Runs Share: %.1f%%\n• Dot Ball Pressure: %.1f%%\n• Active Partnership: %d runs (%d balls)\n\n",
                inn.getCurrentRunRate(),
                computeBoundaryPercentage(inn),
                computeDotBallPercentage(inn),
                inn.currentPartnership.totalRuns,
                inn.currentPartnership.totalBalls));

        sb.append("MODEL PROJECTIONS (Statistical Estimate — Not Guaranteed)\n");
        if (inn.targetRuns > 0) {
            sb.append(String.format(Locale.US,
                    "• Chase Win Probability Estimate: %d%% (%s)\n• Required Run Rate: %.2f rpo\n\n",
                    computeBattingWinProbabilityModel(state),
                    inn.battingTeamName,
                    inn.getRequiredRunRate(state.rulesProfile.getMaxLegalBallsPerInnings())));
        } else if (state.rulesProfile.maxOversPerInnings > 0) {
            sb.append(String.format(Locale.US,
                    "• Projected Innings Score: %d runs (at CRR %.2f adjusted for wickets)\n\n",
                    computeProjectedScore(inn, state.rulesProfile),
                    inn.getCurrentRunRate()));
        }

        sb.append("MANHATTAN & WORM OVER-BY-OVER PROGRESSION\n");
        if (inn.overSummaries.isEmpty()) {
            sb.append("Current Over (In Progress): ")
                    .append(inn.getThisOverDisplayString().isEmpty() ? "No balls yet" : inn.getThisOverDisplayString())
                    .append("\n");
        } else {
            for (OverSummary ov : inn.overSummaries) {
                int barBlocks = Math.min(20, Math.max(1, ov.runsInOver));
                StringBuilder bar = new StringBuilder();
                for (int i = 0; i < barBlocks; i++) {
                    bar.append('█');
                }
                if (ov.runsInOver == 0) {
                    bar.setLength(0);
                    bar.append('▏');
                }
                sb.append(String.format(Locale.US,
                        "Ov %2d (%-10s): %-12s %2d runs, %dW  [Worm: %d/%d]\n",
                        ov.overNumber, ov.bowlerName, bar.toString(),
                        ov.runsInOver, ov.wicketsInOver,
                        ov.cumulativeRunsAfterOver, ov.cumulativeWicketsAfterOver));
            }
        }
        return sb.toString();
    }

    /**
     * Generates a compact, zero-account shareable live match summary for WhatsApp, SMS, or social sharing.
     */
    public static String buildShareableScorecardText(MatchState state) {
        if (state == null) {
            return "CricketScoreBoard Live Match";
        }
        InningsScorecard inn = state.getCurrentInnings();
        String statusBadge = state.lifecycleStatus == MatchState.MatchLifecycleStatus.PROVISIONAL
                ? "[LIVE - PROVISIONAL]"
                : "[" + state.lifecycleStatus.name() + " OFFICIAL]";
        StringBuilder sb = new StringBuilder();
        sb.append("🏏 ").append(statusBadge).append(" ")
                .append(state.teamAName).append(" vs ").append(state.teamBName).append("\n");
        sb.append("Format: ").append(state.rulesProfile.profileName).append("\n");
        for (InningsScorecard item : state.inningsList) {
            sb.append("• ").append(item.getFormattedSummaryLine()).append("\n");
        }
        sb.append("Status: ").append(state.resultText).append("\n");
        sb.append("Partnership: ").append(inn.currentPartnership.totalRuns)
                .append(" (").append(inn.currentPartnership.totalBalls).append("b)\n");
        sb.append("Shared via CricketScoreBoard");
        return sb.toString();
    }
}
