package com.example.android.cricketscoreboard.scoring;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Explicit, versioned, and immutable cricket rules configuration profile.
 *
 * <p>A format name never implicitly hardcodes every rule. Instead, each match captures an
 * immutable snapshot of {@link CricketRulesProfile} at creation time so subsequent edits to
 * club or tournament templates never alter ongoing or historical matches.
 */
public final class CricketRulesProfile {

    public static final String FORMAT_T20 = "T20";
    public static final String FORMAT_ODI = "ODI";
    public static final String FORMAT_T10 = "T10";
    public static final String FORMAT_THE_HUNDRED = "THE_HUNDRED";
    public static final String FORMAT_TEST = "TEST";
    public static final String FORMAT_TAPE_BALL = "TAPE_BALL";
    public static final String FORMAT_YOUTH_ACADEMY = "YOUTH_ACADEMY";
    public static final String FORMAT_CUSTOM = "CUSTOM";

    public final String profileId;
    public final String profileName;
    public final int version;
    public final String formatCode;
    public final int inningsPerTeam;
    public final int maxOversPerInnings;
    public final int ballsPerOver;
    public final int playersPerTeam;
    public final boolean lastManStanding;
    public final int wideBallPenaltyRuns;
    public final int noBallPenaltyRuns;
    public final boolean rebowlWideAndNoBall;
    public final boolean freeHitAfterNoBall;
    public final int maxOversPerBowler;
    public final int retireAtScore;
    public final int powerplayOvers;
    public final int pointsWin;
    public final int pointsTie;
    public final int pointsNoResult;
    public final boolean allowSuperOverOnTie;

    private CricketRulesProfile(Builder b) {
        this.profileId = b.profileId;
        this.profileName = b.profileName;
        this.version = b.version;
        this.formatCode = b.formatCode;
        this.inningsPerTeam = b.inningsPerTeam;
        this.maxOversPerInnings = b.maxOversPerInnings;
        this.ballsPerOver = b.ballsPerOver;
        this.playersPerTeam = b.playersPerTeam;
        this.lastManStanding = b.lastManStanding;
        this.wideBallPenaltyRuns = b.wideBallPenaltyRuns;
        this.noBallPenaltyRuns = b.noBallPenaltyRuns;
        this.rebowlWideAndNoBall = b.rebowlWideAndNoBall;
        this.freeHitAfterNoBall = b.freeHitAfterNoBall;
        this.maxOversPerBowler = b.maxOversPerBowler;
        this.retireAtScore = b.retireAtScore;
        this.powerplayOvers = b.powerplayOvers;
        this.pointsWin = b.pointsWin;
        this.pointsTie = b.pointsTie;
        this.pointsNoResult = b.pointsNoResult;
        this.allowSuperOverOnTie = b.allowSuperOverOnTie;
    }

    /**
     * Maximum wickets before an innings is all-out.
     * In standard cricket, 11 players = 10 wickets; in Last-Man-Standing, the final batter can bat alone.
     */
    public int getMaxWicketsPerInnings() {
        return lastManStanding ? playersPerTeam : Math.max(1, playersPerTeam - 1);
    }

    /**
     * Total legal balls in a full innings quota, or {@code 0} for unlimited-over matches (Test/First-Class).
     */
    public int getMaxLegalBallsPerInnings() {
        return maxOversPerInnings <= 0 ? 0 : maxOversPerInnings * ballsPerOver;
    }

    /**
     * Validates that all rule parameters satisfy cricket domain constraints.
     *
     * @return empty list if valid, or a list of human-readable validation errors.
     */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (profileId == null || profileId.trim().isEmpty()) {
            errors.add("profileId must not be empty");
        }
        if (profileName == null || profileName.trim().isEmpty()) {
            errors.add("profileName must not be empty");
        }
        if (version < 1) {
            errors.add("version must be >= 1");
        }
        if (inningsPerTeam < 1 || inningsPerTeam > 2) {
            errors.add("inningsPerTeam must be 1 or 2");
        }
        if (maxOversPerInnings < 0 || maxOversPerInnings > 500) {
            errors.add("maxOversPerInnings must be between 0 (unlimited) and 500");
        }
        if (ballsPerOver < 4 || ballsPerOver > 10) {
            errors.add("ballsPerOver must be between 4 and 10");
        }
        if (playersPerTeam < 2 || playersPerTeam > 15) {
            errors.add("playersPerTeam must be between 2 and 15");
        }
        if (wideBallPenaltyRuns < 1 || wideBallPenaltyRuns > 5) {
            errors.add("wideBallPenaltyRuns must be between 1 and 5");
        }
        if (noBallPenaltyRuns < 1 || noBallPenaltyRuns > 5) {
            errors.add("noBallPenaltyRuns must be between 1 and 5");
        }
        if (maxOversPerBowler < 0 || (maxOversPerInnings > 0 && maxOversPerBowler > maxOversPerInnings)) {
            errors.add("maxOversPerBowler cannot exceed maxOversPerInnings");
        }
        if (retireAtScore < 0) {
            errors.add("retireAtScore must be >= 0");
        }
        if (powerplayOvers < 0 || (maxOversPerInnings > 0 && powerplayOvers > maxOversPerInnings)) {
            errors.add("powerplayOvers cannot exceed maxOversPerInnings");
        }
        return errors;
    }

    /**
     * Returns a human-readable summary of nonstandard or notable rules active in this profile.
     */
    public List<String> describeActiveRules() {
        List<String> notes = new ArrayList<>();
        if (maxOversPerInnings > 0) {
            notes.add(maxOversPerInnings + " overs (" + ballsPerOver + " balls/over, "
                    + getMaxLegalBallsPerInnings() + " total balls)");
        } else {
            notes.add("Unlimited overs (" + inningsPerTeam + " innings per side)");
        }
        notes.add(playersPerTeam + " players/side ("
                + (lastManStanding ? "Last-Man-Standing: " + playersPerTeam + " wickets"
                : getMaxWicketsPerInnings() + " wickets all-out") + ")");
        if (!rebowlWideAndNoBall) {
            notes.add("Wides/No-balls count as legal balls (no re-bowl)");
        }
        if (wideBallPenaltyRuns > 1 || noBallPenaltyRuns > 1) {
            notes.add("Wide penalty: +" + wideBallPenaltyRuns + ", No-ball penalty: +" + noBallPenaltyRuns);
        }
        if (freeHitAfterNoBall) {
            notes.add("Free Hit after every No-ball");
        }
        if (maxOversPerBowler > 0) {
            notes.add("Max " + maxOversPerBowler + " overs per bowler");
        }
        if (retireAtScore > 0) {
            notes.add("Compulsory batter retirement at " + retireAtScore + "* runs");
        }
        return Collections.unmodifiableList(notes);
    }

    public Builder toBuilder() {
        return new Builder()
                .setProfileId(profileId)
                .setProfileName(profileName)
                .setVersion(version)
                .setFormatCode(formatCode)
                .setInningsPerTeam(inningsPerTeam)
                .setMaxOversPerInnings(maxOversPerInnings)
                .setBallsPerOver(ballsPerOver)
                .setPlayersPerTeam(playersPerTeam)
                .setLastManStanding(lastManStanding)
                .setWideBallPenaltyRuns(wideBallPenaltyRuns)
                .setNoBallPenaltyRuns(noBallPenaltyRuns)
                .setRebowlWideAndNoBall(rebowlWideAndNoBall)
                .setFreeHitAfterNoBall(freeHitAfterNoBall)
                .setMaxOversPerBowler(maxOversPerBowler)
                .setRetireAtScore(retireAtScore)
                .setPowerplayOvers(powerplayOvers)
                .setPointsWin(pointsWin)
                .setPointsTie(pointsTie)
                .setPointsNoResult(pointsNoResult)
                .setAllowSuperOverOnTie(allowSuperOverOnTie);
    }

    /**
     * Creates a new immutable version of this rules profile without mutating existing snapshots.
     */
    public CricketRulesProfile withIncrementedVersion() {
        return toBuilder().setVersion(version + 1).build();
    }

    /**
     * Serializes this profile into a deterministic key-value string safe for both SQLite and JVM unit tests.
     */
    public String serialize() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("profileId", safeStr(profileId));
        map.put("profileName", safeStr(profileName));
        map.put("version", String.valueOf(version));
        map.put("formatCode", safeStr(formatCode));
        map.put("inningsPerTeam", String.valueOf(inningsPerTeam));
        map.put("maxOversPerInnings", String.valueOf(maxOversPerInnings));
        map.put("ballsPerOver", String.valueOf(ballsPerOver));
        map.put("playersPerTeam", String.valueOf(playersPerTeam));
        map.put("lastManStanding", String.valueOf(lastManStanding));
        map.put("wideBallPenaltyRuns", String.valueOf(wideBallPenaltyRuns));
        map.put("noBallPenaltyRuns", String.valueOf(noBallPenaltyRuns));
        map.put("rebowlWideAndNoBall", String.valueOf(rebowlWideAndNoBall));
        map.put("freeHitAfterNoBall", String.valueOf(freeHitAfterNoBall));
        map.put("maxOversPerBowler", String.valueOf(maxOversPerBowler));
        map.put("retireAtScore", String.valueOf(retireAtScore));
        map.put("powerplayOvers", String.valueOf(powerplayOvers));
        map.put("pointsWin", String.valueOf(pointsWin));
        map.put("pointsTie", String.valueOf(pointsTie));
        map.put("pointsNoResult", String.valueOf(pointsNoResult));
        map.put("allowSuperOverOnTie", String.valueOf(allowSuperOverOnTie));
        return SimpleKvSerializer.encode(map);
    }

    public static CricketRulesProfile deserialize(String encoded) {
        if (encoded == null || encoded.trim().isEmpty()) {
            return createStandardT20();
        }
        Map<String, String> m = SimpleKvSerializer.decode(encoded);
        return new Builder()
                .setProfileId(m.getOrDefault("profileId", "ICC_T20_STD"))
                .setProfileName(m.getOrDefault("profileName", "Standard T20 (20 Overs)"))
                .setVersion(parseInt(m.get("version"), 1))
                .setFormatCode(m.getOrDefault("formatCode", FORMAT_T20))
                .setInningsPerTeam(parseInt(m.get("inningsPerTeam"), 1))
                .setMaxOversPerInnings(parseInt(m.get("maxOversPerInnings"), 20))
                .setBallsPerOver(parseInt(m.get("ballsPerOver"), 6))
                .setPlayersPerTeam(parseInt(m.get("playersPerTeam"), 11))
                .setLastManStanding(Boolean.parseBoolean(m.getOrDefault("lastManStanding", "false")))
                .setWideBallPenaltyRuns(parseInt(m.get("wideBallPenaltyRuns"), 1))
                .setNoBallPenaltyRuns(parseInt(m.get("noBallPenaltyRuns"), 1))
                .setRebowlWideAndNoBall(Boolean.parseBoolean(m.getOrDefault("rebowlWideAndNoBall", "true")))
                .setFreeHitAfterNoBall(Boolean.parseBoolean(m.getOrDefault("freeHitAfterNoBall", "true")))
                .setMaxOversPerBowler(parseInt(m.get("maxOversPerBowler"), 4))
                .setRetireAtScore(parseInt(m.get("retireAtScore"), 0))
                .setPowerplayOvers(parseInt(m.get("powerplayOvers"), 6))
                .setPointsWin(parseInt(m.get("pointsWin"), 2))
                .setPointsTie(parseInt(m.get("pointsTie"), 1))
                .setPointsNoResult(parseInt(m.get("pointsNoResult"), 1))
                .setAllowSuperOverOnTie(Boolean.parseBoolean(m.getOrDefault("allowSuperOverOnTie", "true")))
                .build();
    }

    private static String safeStr(String s) {
        return s == null ? "" : s;
    }

    private static int parseInt(String val, int fallback) {
        if (val == null || val.isEmpty()) {
            return fallback;
        }
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // =========================================================================
    // Official Versioned Templates
    // =========================================================================

    public static CricketRulesProfile createStandardT20() {
        return new Builder()
                .setProfileId("ICC_T20_STD")
                .setProfileName("Standard T20 (20 Overs)")
                .setVersion(1)
                .setFormatCode(FORMAT_T20)
                .setInningsPerTeam(1)
                .setMaxOversPerInnings(20)
                .setBallsPerOver(6)
                .setPlayersPerTeam(11)
                .setLastManStanding(false)
                .setWideBallPenaltyRuns(1)
                .setNoBallPenaltyRuns(1)
                .setRebowlWideAndNoBall(true)
                .setFreeHitAfterNoBall(true)
                .setMaxOversPerBowler(4)
                .setPowerplayOvers(6)
                .build();
    }

    public static CricketRulesProfile createStandardOdi() {
        return createStandardT20().toBuilder()
                .setProfileId("ICC_ODI_50")
                .setProfileName("One-Day 50 Overs (ODI)")
                .setFormatCode(FORMAT_ODI)
                .setMaxOversPerInnings(50)
                .setMaxOversPerBowler(10)
                .setPowerplayOvers(10)
                .build();
    }

    public static CricketRulesProfile createStandardT10() {
        return createStandardT20().toBuilder()
                .setProfileId("CLUB_T10_STD")
                .setProfileName("Express T10 (10 Overs)")
                .setFormatCode(FORMAT_T10)
                .setMaxOversPerInnings(10)
                .setMaxOversPerBowler(2)
                .setPowerplayOvers(3)
                .build();
    }

    public static CricketRulesProfile createTheHundred() {
        return createStandardT20().toBuilder()
                .setProfileId("ECB_THE_HUNDRED")
                .setProfileName("The Hundred (100 Balls / 5-Ball Sets)")
                .setFormatCode(FORMAT_THE_HUNDRED)
                .setMaxOversPerInnings(20)
                .setBallsPerOver(5)
                .setMaxOversPerBowler(4)
                .setPowerplayOvers(5)
                .build();
    }

    public static CricketRulesProfile createStandardTest() {
        return createStandardT20().toBuilder()
                .setProfileId("ICC_TEST_MULTI")
                .setProfileName("Multi-Day / Test Cricket")
                .setFormatCode(FORMAT_TEST)
                .setInningsPerTeam(2)
                .setMaxOversPerInnings(0)
                .setMaxOversPerBowler(0)
                .setFreeHitAfterNoBall(false)
                .setPowerplayOvers(0)
                .setPointsWin(12)
                .setPointsTie(6)
                .setPointsNoResult(4)
                .setAllowSuperOverOnTie(false)
                .build();
    }

    public static CricketRulesProfile createTapeBallLastManStanding() {
        return createStandardT20().toBuilder()
                .setProfileId("STREET_TAPE_LMS_8")
                .setProfileName("Tape-Ball / Street LMS (8 Overs, 8 Players)")
                .setFormatCode(FORMAT_TAPE_BALL)
                .setMaxOversPerInnings(8)
                .setPlayersPerTeam(8)
                .setLastManStanding(true)
                .setMaxOversPerBowler(2)
                .setPowerplayOvers(2)
                .build();
    }

    public static CricketRulesProfile createYouthAcademyRetireAt30() {
        return createStandardT20().toBuilder()
                .setProfileId("YOUTH_ACADEMY_U15")
                .setProfileName("Youth Academy (15 Overs, Retire at 30*, No Re-bowl)")
                .setFormatCode(FORMAT_YOUTH_ACADEMY)
                .setMaxOversPerInnings(15)
                .setRebowlWideAndNoBall(false)
                .setRetireAtScore(30)
                .setMaxOversPerBowler(3)
                .setPowerplayOvers(4)
                .build();
    }

    public static List<CricketRulesProfile> getOfficialTemplates() {
        List<CricketRulesProfile> list = new ArrayList<>();
        list.add(createStandardT20());
        list.add(createStandardT10());
        list.add(createStandardOdi());
        list.add(createTheHundred());
        list.add(createTapeBallLastManStanding());
        list.add(createYouthAcademyRetireAt30());
        list.add(createStandardTest());
        return list;
    }

    public static final class Builder {
        private String profileId = "ICC_T20_STD";
        private String profileName = "Standard T20 (20 Overs)";
        private int version = 1;
        private String formatCode = FORMAT_T20;
        private int inningsPerTeam = 1;
        private int maxOversPerInnings = 20;
        private int ballsPerOver = 6;
        private int playersPerTeam = 11;
        private boolean lastManStanding = false;
        private int wideBallPenaltyRuns = 1;
        private int noBallPenaltyRuns = 1;
        private boolean rebowlWideAndNoBall = true;
        private boolean freeHitAfterNoBall = true;
        private int maxOversPerBowler = 4;
        private int retireAtScore = 0;
        private int powerplayOvers = 6;
        private int pointsWin = 2;
        private int pointsTie = 1;
        private int pointsNoResult = 1;
        private boolean allowSuperOverOnTie = true;

        public Builder setProfileId(String profileId) {
            this.profileId = profileId;
            return this;
        }

        public Builder setProfileName(String profileName) {
            this.profileName = profileName;
            return this;
        }

        public Builder setVersion(int version) {
            this.version = version;
            return this;
        }

        public Builder setFormatCode(String formatCode) {
            this.formatCode = formatCode;
            return this;
        }

        public Builder setInningsPerTeam(int inningsPerTeam) {
            this.inningsPerTeam = inningsPerTeam;
            return this;
        }

        public Builder setMaxOversPerInnings(int maxOversPerInnings) {
            this.maxOversPerInnings = maxOversPerInnings;
            return this;
        }

        public Builder setBallsPerOver(int ballsPerOver) {
            this.ballsPerOver = ballsPerOver;
            return this;
        }

        public Builder setPlayersPerTeam(int playersPerTeam) {
            this.playersPerTeam = playersPerTeam;
            return this;
        }

        public Builder setLastManStanding(boolean lastManStanding) {
            this.lastManStanding = lastManStanding;
            return this;
        }

        public Builder setWideBallPenaltyRuns(int wideBallPenaltyRuns) {
            this.wideBallPenaltyRuns = wideBallPenaltyRuns;
            return this;
        }

        public Builder setNoBallPenaltyRuns(int noBallPenaltyRuns) {
            this.noBallPenaltyRuns = noBallPenaltyRuns;
            return this;
        }

        public Builder setRebowlWideAndNoBall(boolean rebowlWideAndNoBall) {
            this.rebowlWideAndNoBall = rebowlWideAndNoBall;
            return this;
        }

        public Builder setFreeHitAfterNoBall(boolean freeHitAfterNoBall) {
            this.freeHitAfterNoBall = freeHitAfterNoBall;
            return this;
        }

        public Builder setMaxOversPerBowler(int maxOversPerBowler) {
            this.maxOversPerBowler = maxOversPerBowler;
            return this;
        }

        public Builder setRetireAtScore(int retireAtScore) {
            this.retireAtScore = retireAtScore;
            return this;
        }

        public Builder setPowerplayOvers(int powerplayOvers) {
            this.powerplayOvers = powerplayOvers;
            return this;
        }

        public Builder setPointsWin(int pointsWin) {
            this.pointsWin = pointsWin;
            return this;
        }

        public Builder setPointsTie(int pointsTie) {
            this.pointsTie = pointsTie;
            return this;
        }

        public Builder setPointsNoResult(int pointsNoResult) {
            this.pointsNoResult = pointsNoResult;
            return this;
        }

        public Builder setAllowSuperOverOnTie(boolean allowSuperOverOnTie) {
            this.allowSuperOverOnTie = allowSuperOverOnTie;
            return this;
        }

        public CricketRulesProfile build() {
            return new CricketRulesProfile(this);
        }
    }
}
