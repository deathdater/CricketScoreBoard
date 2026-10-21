package com.example.android.cricketscoreboard.scoring;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable ball-by-ball delivery event representing a single scored action in a match.
 *
 * <p>The ordered sequence of {@link DeliveryEvent} objects is the single source of truth
 * for match totals, batting/bowling scorecards, partnerships, fall of wickets, and analytics.
 */
public final class DeliveryEvent {

    public enum ExtraType {
        NONE,
        WIDE,
        NO_BALL,
        BYE,
        LEG_BYE,
        PENALTY
    }

    public enum DismissalType {
        NONE,
        BOWLED,
        CAUGHT,
        LBW,
        RUN_OUT,
        STUMPED,
        HIT_WICKET,
        OBSTRUCTING_FIELD,
        HIT_BALL_TWICE,
        RETIRED_OUT,
        RETIRED_HURT,
        TIMED_OUT
    }

    public final String eventUuid;
    public final String matchUuid;
    public final int inningsNumber;
    public final int sequenceNumber;
    public final int strikerId;
    public final String strikerName;
    public final int nonStrikerId;
    public final String nonStrikerName;
    public final int bowlerId;
    public final String bowlerName;
    public final int runsOffBat;
    public final ExtraType extraType;
    public final int extraRuns;
    public final int overthrowRuns;
    public final boolean isBoundaryFour;
    public final boolean isBoundarySix;
    public final DismissalType dismissalType;
    public final int dismissedPlayerId;
    public final String dismissedPlayerName;
    public final String fielderName;
    public final int newBatterId;
    public final String newBatterName;
    public final boolean crossedBeforeCatch;
    public final boolean wasFreeHit;
    public final long timestampMillis;
    public final String note;

    private DeliveryEvent(Builder b) {
        this.eventUuid = (b.eventUuid == null || b.eventUuid.isEmpty())
                ? UUID.randomUUID().toString() : b.eventUuid;
        this.matchUuid = b.matchUuid == null ? "" : b.matchUuid;
        this.inningsNumber = Math.max(1, b.inningsNumber);
        this.sequenceNumber = Math.max(1, b.sequenceNumber);
        this.strikerId = b.strikerId;
        this.strikerName = b.strikerName == null || b.strikerName.isEmpty() ? "Striker" : b.strikerName;
        this.nonStrikerId = b.nonStrikerId;
        this.nonStrikerName = b.nonStrikerName == null || b.nonStrikerName.isEmpty() ? "Non-Striker" : b.nonStrikerName;
        this.bowlerId = b.bowlerId;
        this.bowlerName = b.bowlerName == null || b.bowlerName.isEmpty() ? "Bowler" : b.bowlerName;
        this.runsOffBat = Math.max(0, b.runsOffBat);
        this.extraType = b.extraType == null ? ExtraType.NONE : b.extraType;
        this.extraRuns = Math.max(0, b.extraRuns);
        this.overthrowRuns = Math.max(0, b.overthrowRuns);
        this.isBoundaryFour = b.isBoundaryFour || (this.runsOffBat == 4 && this.overthrowRuns == 0);
        this.isBoundarySix = b.isBoundarySix || (this.runsOffBat == 6 && this.overthrowRuns == 0);
        this.dismissalType = b.dismissalType == null ? DismissalType.NONE : b.dismissalType;
        this.dismissedPlayerId = b.dismissedPlayerId;
        this.dismissedPlayerName = b.dismissedPlayerName == null ? "" : b.dismissedPlayerName;
        this.fielderName = b.fielderName == null ? "" : b.fielderName;
        this.newBatterId = b.newBatterId;
        this.newBatterName = b.newBatterName == null ? "" : b.newBatterName;
        this.crossedBeforeCatch = b.crossedBeforeCatch;
        this.wasFreeHit = b.wasFreeHit;
        this.timestampMillis = b.timestampMillis <= 0L ? System.currentTimeMillis() : b.timestampMillis;
        this.note = b.note == null ? "" : b.note;
    }

    /**
     * Returns whether this event represents a dismissal that counts as a team wicket
     * (all dismissals except {@link DismissalType#NONE} and {@link DismissalType#RETIRED_HURT}).
     */
    public boolean isTeamWicket() {
        return dismissalType != DismissalType.NONE && dismissalType != DismissalType.RETIRED_HURT;
    }

    /**
     * Returns whether the dismissal is credited to the bowler's wicket column under MCC Laws.
     */
    public boolean isBowlerWicket() {
        switch (dismissalType) {
            case BOWLED:
            case CAUGHT:
            case LBW:
            case STUMPED:
            case HIT_WICKET:
                return true;
            default:
                return false;
        }
    }

    /**
     * Produces the compact ball token used in over summaries and the Scorer Pad (e.g., {@code "0"},
     * {@code "4"}, {@code "0wd"}, {@code "4nb"}, {@code "W"}, {@code "2b"}).
     */
    public String toBallToken() {
        StringBuilder sb = new StringBuilder();
        if (extraType == ExtraType.WIDE) {
            sb.append(runsOffBat + extraRuns).append("wd");
        } else if (extraType == ExtraType.NO_BALL) {
            sb.append(runsOffBat + extraRuns).append("nb");
        } else if (extraType == ExtraType.BYE) {
            sb.append(extraRuns).append("b");
        } else if (extraType == ExtraType.LEG_BYE) {
            sb.append(extraRuns).append("lb");
        } else if (extraType == ExtraType.PENALTY) {
            sb.append(extraRuns).append("p");
        } else {
            sb.append(runsOffBat + overthrowRuns);
        }
        if (isTeamWicket()) {
            sb.append("W");
        } else if (dismissalType == DismissalType.RETIRED_HURT) {
            sb.append("RH");
        }
        return sb.toString();
    }

    public String serialize() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("eventUuid", eventUuid);
        m.put("matchUuid", matchUuid);
        m.put("inningsNumber", String.valueOf(inningsNumber));
        m.put("sequenceNumber", String.valueOf(sequenceNumber));
        m.put("strikerId", String.valueOf(strikerId));
        m.put("strikerName", strikerName);
        m.put("nonStrikerId", String.valueOf(nonStrikerId));
        m.put("nonStrikerName", nonStrikerName);
        m.put("bowlerId", String.valueOf(bowlerId));
        m.put("bowlerName", bowlerName);
        m.put("runsOffBat", String.valueOf(runsOffBat));
        m.put("extraType", extraType.name());
        m.put("extraRuns", String.valueOf(extraRuns));
        m.put("overthrowRuns", String.valueOf(overthrowRuns));
        m.put("isBoundaryFour", String.valueOf(isBoundaryFour));
        m.put("isBoundarySix", String.valueOf(isBoundarySix));
        m.put("dismissalType", dismissalType.name());
        m.put("dismissedPlayerId", String.valueOf(dismissedPlayerId));
        m.put("dismissedPlayerName", dismissedPlayerName);
        m.put("fielderName", fielderName);
        m.put("newBatterId", String.valueOf(newBatterId));
        m.put("newBatterName", newBatterName);
        m.put("crossedBeforeCatch", String.valueOf(crossedBeforeCatch));
        m.put("wasFreeHit", String.valueOf(wasFreeHit));
        m.put("timestampMillis", String.valueOf(timestampMillis));
        m.put("note", note);
        return SimpleKvSerializer.encode(m);
    }

    public static DeliveryEvent deserialize(String encoded) {
        Map<String, String> m = SimpleKvSerializer.decode(encoded);
        ExtraType extra = ExtraType.NONE;
        try {
            extra = ExtraType.valueOf(m.getOrDefault("extraType", "NONE"));
        } catch (IllegalArgumentException ignored) {
        }
        DismissalType dismissal = DismissalType.NONE;
        try {
            dismissal = DismissalType.valueOf(m.getOrDefault("dismissalType", "NONE"));
        } catch (IllegalArgumentException ignored) {
        }
        return new Builder()
                .setEventUuid(m.get("eventUuid"))
                .setMatchUuid(m.get("matchUuid"))
                .setInningsNumber(parseInt(m.get("inningsNumber"), 1))
                .setSequenceNumber(parseInt(m.get("sequenceNumber"), 1))
                .setStriker(parseInt(m.get("strikerId"), 1), m.getOrDefault("strikerName", "Striker"))
                .setNonStriker(parseInt(m.get("nonStrikerId"), 2), m.getOrDefault("nonStrikerName", "Non-Striker"))
                .setBowler(parseInt(m.get("bowlerId"), 101), m.getOrDefault("bowlerName", "Bowler"))
                .setRunsOffBat(parseInt(m.get("runsOffBat"), 0))
                .setExtraType(extra)
                .setExtraRuns(parseInt(m.get("extraRuns"), 0))
                .setOverthrowRuns(parseInt(m.get("overthrowRuns"), 0))
                .setBoundaryFour(Boolean.parseBoolean(m.getOrDefault("isBoundaryFour", "false")))
                .setBoundarySix(Boolean.parseBoolean(m.getOrDefault("isBoundarySix", "false")))
                .setDismissal(
                        dismissal,
                        parseInt(m.get("dismissedPlayerId"), 0),
                        m.getOrDefault("dismissedPlayerName", ""),
                        m.getOrDefault("fielderName", ""),
                        parseInt(m.get("newBatterId"), 0),
                        m.getOrDefault("newBatterName", "")
                )
                .setCrossedBeforeCatch(Boolean.parseBoolean(m.getOrDefault("crossedBeforeCatch", "false")))
                .setWasFreeHit(Boolean.parseBoolean(m.getOrDefault("wasFreeHit", "false")))
                .setTimestampMillis(parseLong(m.get("timestampMillis"), 0L))
                .setNote(m.getOrDefault("note", ""))
                .build();
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

    private static long parseLong(String val, long fallback) {
        if (val == null || val.isEmpty()) {
            return fallback;
        }
        try {
            return Long.parseLong(val.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static final class Builder {
        private String eventUuid;
        private String matchUuid = "";
        private int inningsNumber = 1;
        private int sequenceNumber = 1;
        private int strikerId = 1;
        private String strikerName = "Batter 1";
        private int nonStrikerId = 2;
        private String nonStrikerName = "Batter 2";
        private int bowlerId = 101;
        private String bowlerName = "Bowler 1";
        private int runsOffBat = 0;
        private ExtraType extraType = ExtraType.NONE;
        private int extraRuns = 0;
        private int overthrowRuns = 0;
        private boolean isBoundaryFour = false;
        private boolean isBoundarySix = false;
        private DismissalType dismissalType = DismissalType.NONE;
        private int dismissedPlayerId = 0;
        private String dismissedPlayerName = "";
        private String fielderName = "";
        private int newBatterId = 0;
        private String newBatterName = "";
        private boolean crossedBeforeCatch = false;
        private boolean wasFreeHit = false;
        private long timestampMillis = 0L;
        private String note = "";

        public Builder setEventUuid(String eventUuid) {
            this.eventUuid = eventUuid;
            return this;
        }

        public Builder setMatchUuid(String matchUuid) {
            this.matchUuid = matchUuid;
            return this;
        }

        public Builder setInningsNumber(int inningsNumber) {
            this.inningsNumber = inningsNumber;
            return this;
        }

        public Builder setSequenceNumber(int sequenceNumber) {
            this.sequenceNumber = sequenceNumber;
            return this;
        }

        public Builder setStriker(int strikerId, String strikerName) {
            this.strikerId = strikerId;
            this.strikerName = strikerName;
            return this;
        }

        public Builder setNonStriker(int nonStrikerId, String nonStrikerName) {
            this.nonStrikerId = nonStrikerId;
            this.nonStrikerName = nonStrikerName;
            return this;
        }

        public Builder setBowler(int bowlerId, String bowlerName) {
            this.bowlerId = bowlerId;
            this.bowlerName = bowlerName;
            return this;
        }

        public Builder setRunsOffBat(int runsOffBat) {
            this.runsOffBat = runsOffBat;
            return this;
        }

        public Builder setExtraType(ExtraType extraType) {
            this.extraType = extraType;
            return this;
        }

        public Builder setExtraRuns(int extraRuns) {
            this.extraRuns = extraRuns;
            return this;
        }

        public Builder setOverthrowRuns(int overthrowRuns) {
            this.overthrowRuns = overthrowRuns;
            return this;
        }

        public Builder setBoundaryFour(boolean isBoundaryFour) {
            this.isBoundaryFour = isBoundaryFour;
            return this;
        }

        public Builder setBoundarySix(boolean isBoundarySix) {
            this.isBoundarySix = isBoundarySix;
            return this;
        }

        public Builder setDismissal(
                DismissalType dismissalType,
                int dismissedPlayerId,
                String dismissedPlayerName,
                String fielderName,
                int newBatterId,
                String newBatterName) {
            this.dismissalType = dismissalType;
            this.dismissedPlayerId = dismissedPlayerId;
            this.dismissedPlayerName = dismissedPlayerName;
            this.fielderName = fielderName;
            this.newBatterId = newBatterId;
            this.newBatterName = newBatterName;
            return this;
        }

        public Builder setCrossedBeforeCatch(boolean crossedBeforeCatch) {
            this.crossedBeforeCatch = crossedBeforeCatch;
            return this;
        }

        public Builder setWasFreeHit(boolean wasFreeHit) {
            this.wasFreeHit = wasFreeHit;
            return this;
        }

        public Builder setTimestampMillis(long timestampMillis) {
            this.timestampMillis = timestampMillis;
            return this;
        }

        public Builder setNote(String note) {
            this.note = note;
            return this;
        }

        public DeliveryEvent build() {
            return new DeliveryEvent(this);
        }
    }
}
