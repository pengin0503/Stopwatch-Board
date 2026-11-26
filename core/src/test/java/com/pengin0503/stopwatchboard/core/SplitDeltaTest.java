package com.pengin0503.stopwatchboard.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SplitDeltaTest {

    @Test
    void missingPersonalBestReturnsNoPb() {
        SplitDelta delta = SplitDelta.compute(5_000L, 0, null);
        assertEquals(SplitDelta.Kind.NO_PB, delta.kind());
    }

    @Test
    void fewerPbSplitsReturnsNoPbSplit() {
        PersonalBest pb = new PersonalBest(
                UUID.randomUUID(),
                "Alice",
                30_000L,
                List.of(10_000L, 20_000L),
                1_000L
        );
        SplitDelta delta = SplitDelta.compute(25_000L, 2, pb);
        assertEquals(SplitDelta.Kind.NO_PB_SPLIT, delta.kind());
    }

    @Test
    void computesFasterSlowerAndEqualDeltas() {
        PersonalBest pb = new PersonalBest(
                UUID.randomUUID(),
                "Alice",
                30_000L,
                List.of(10_000L, 20_000L),
                1_000L
        );

        SplitDelta faster = SplitDelta.compute(9_500L, 0, pb);
        assertEquals(SplitDelta.Kind.HAS_DELTA, faster.kind());
        assertEquals(-500L, faster.deltaMs());

        SplitDelta equal = SplitDelta.compute(10_000L, 0, pb);
        assertEquals(SplitDelta.Kind.HAS_DELTA, equal.kind());
        assertEquals(0L, equal.deltaMs());

        SplitDelta slower = SplitDelta.compute(21_250L, 1, pb);
        assertEquals(SplitDelta.Kind.HAS_DELTA, slower.kind());
        assertEquals(1_250L, slower.deltaMs());
    }
}
