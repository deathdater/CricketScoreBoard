package com.example.android.cricketscoreboard.scoring;

import com.example.android.cricketscoreboard.scoring.MatchState.InningsScorecard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Computes tournament and league points tables, Net Run Rate (NRR) with the official ICC
 * all-out overs rule, and deterministic tie-breaking from verified {@link MatchState} results.
 */
public final class PointsTableCalculator {

    public static final class TeamStanding {
        public final int teamId;
        public final String teamName;
        public int played;
        public int won;
        public int lost;
        public int tied;
        public int noResult;
        public int points;
        public int totalRunsScored;
        public double totalOversFaced;
        public int totalRunsConceded;
        public double totalOversBowled;

        public TeamStanding(int teamId, String teamName) {
            this.teamId = teamId;
            this.teamName = teamName == null ? ("Team " + teamId) : teamName;
        }

        public double getNetRunRate() {
            double forRate = totalOversFaced > 0.0 ? (totalRunsScored / totalOversFaced) : 0.0;
            double againstRate = totalOversBowled > 0.0 ? (totalRunsConceded / totalOversBowled) : 0.0;
            return forRate - againstRate;
        }

        public String formatRow(int rank) {
            return String.format(Locale.US,
                    "%d. %-14s  P:%d  W:%d  L:%d  T:%d  Pts:%2d  NRR:%+.3f",
                    rank, teamName, played, won, lost, tied, points, getNetRunRate());
        }
    }

    private PointsTableCalculator() {
    }

    /**
     * Calculates sorted tournament standings from a list of completed matches.
     *
     * <p>Official NRR All-Out Rule: If a team is bowled out in fewer than {@code maxOversPerInnings},
     * the full quota of {@code maxOversPerInnings} is used as the overs denominator for that innings.
     */
    public static List<TeamStanding> computeStandings(
            List<MatchState> matches,
            CricketRulesProfile tournamentRules) {
        CricketRulesProfile rules = tournamentRules == null
                ? CricketRulesProfile.createStandardT20() : tournamentRules;
        Map<String, TeamStanding> byKey = new LinkedHashMap<>();

        if (matches != null) {
            for (MatchState match : matches) {
                if (match == null || !match.matchCompleted || match.inningsList.size() < 2) {
                    continue;
                }
                InningsScorecard inn1 = match.inningsList.get(0);
                InningsScorecard inn2 = match.inningsList.get(1);

                String key1 = inn1.battingTeamName.trim().toLowerCase(Locale.ROOT);
                String key2 = inn2.battingTeamName.trim().toLowerCase(Locale.ROOT);

                TeamStanding s1 = byKey.computeIfAbsent(
                        key1, k -> new TeamStanding(inn1.battingTeamId, inn1.battingTeamName));
                TeamStanding s2 = byKey.computeIfAbsent(
                        key2, k -> new TeamStanding(inn2.battingTeamId, inn2.battingTeamName));

                s1.played++;
                s2.played++;

                double overs1 = computeEffectiveOversForNrr(inn1, match.rulesProfile);
                double overs2 = computeEffectiveOversForNrr(inn2, match.rulesProfile);

                s1.totalRunsScored += inn1.totalRuns;
                s1.totalOversFaced += overs1;
                s1.totalRunsConceded += inn2.totalRuns;
                s1.totalOversBowled += overs2;

                s2.totalRunsScored += inn2.totalRuns;
                s2.totalOversFaced += overs2;
                s2.totalRunsConceded += inn1.totalRuns;
                s2.totalOversBowled += overs1;

                if (match.isTied) {
                    s1.tied++;
                    s2.tied++;
                    s1.points += rules.pointsTie;
                    s2.points += rules.pointsTie;
                } else if (inn1.totalRuns > inn2.totalRuns) {
                    s1.won++;
                    s2.lost++;
                    s1.points += rules.pointsWin;
                } else if (inn2.totalRuns > inn1.totalRuns) {
                    s2.won++;
                    s1.lost++;
                    s2.points += rules.pointsWin;
                } else {
                    s1.noResult++;
                    s2.noResult++;
                    s1.points += rules.pointsNoResult;
                    s2.points += rules.pointsNoResult;
                }
            }
        }

        List<TeamStanding> standings = new ArrayList<>(byKey.values());
        Collections.sort(standings, (a, b) -> {
            if (b.points != a.points) {
                return Integer.compare(b.points, a.points);
            }
            if (b.won != a.won) {
                return Integer.compare(b.won, a.won);
            }
            int nrrCmp = Double.compare(b.getNetRunRate(), a.getNetRunRate());
            if (nrrCmp != 0) {
                return nrrCmp;
            }
            return a.teamName.compareToIgnoreCase(b.teamName);
        });
        return standings;
    }

    public static double computeEffectiveOversForNrr(
            InningsScorecard innings,
            CricketRulesProfile rules) {
        if (innings == null || rules == null) {
            return 0.0;
        }
        int maxWickets = rules.getMaxWicketsPerInnings();
        if (rules.maxOversPerInnings > 0 && innings.wickets >= maxWickets) {
            return rules.maxOversPerInnings;
        }
        return (double) innings.totalLegalBalls / Math.max(1, rules.ballsPerOver);
    }
}
