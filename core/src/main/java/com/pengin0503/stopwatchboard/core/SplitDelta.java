package com.pengin0503.stopwatchboard.core;

/**
 * Result of comparing a recorded split against the player's personal best split at the same index.
 */
public record SplitDelta(Kind kind, long deltaMs) {
    public enum Kind {
        NO_PB,
        NO_PB_SPLIT,
        HAS_DELTA
    }

    public static SplitDelta noPb() {
        return new SplitDelta(Kind.NO_PB, 0L);
    }

    public static SplitDelta noPbSplit() {
        return new SplitDelta(Kind.NO_PB_SPLIT, 0L);
    }

    public static SplitDelta ofDelta(long deltaMs) {
        return new SplitDelta(Kind.HAS_DELTA, deltaMs);
    }

    /**
     * Computes the split delta for the 0-based {@code splitIndex} against {@code personalBest}.
     */
    public static SplitDelta compute(long currentSplitMs, int splitIndex, PersonalBest personalBest) {
        if (splitIndex < 0) {
            throw new IllegalArgumentException("splitIndex must be non-negative: " + splitIndex);
        }
        if (personalBest == null) {
            return noPb();
        }
        if (splitIndex >= personalBest.splitsMs().size()) {
            return noPbSplit();
        }
        long pbSplitMs = personalBest.splitsMs().get(splitIndex);
        return ofDelta(currentSplitMs - pbSplitMs);
    }
}
