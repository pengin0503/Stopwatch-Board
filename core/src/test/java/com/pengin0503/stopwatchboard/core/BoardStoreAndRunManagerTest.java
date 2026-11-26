package com.pengin0503.stopwatchboard.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoardStoreAndRunManagerTest {

    @Test
    void pbReplacementRuleStrictlyFasterOnlyAndEqualKeepsEarlierSplits() {
        BoardStore store = new BoardStore();
        assertTrue(store.createBoard("parkour", "Parkour Course", "Admin", 100L));

        UUID alice = UUID.randomUUID();
        BoardStore.FinishRecordResult first = store.recordFinish(
                "parkour", alice, "Alice", 15_000L, List.of(5_000L, 10_000L), 1_000L
        );
        assertTrue(first.newPb());
        assertNull(first.previousPbTotalMs());
        assertNull(first.deltaVsPreviousPbMs());
        assertEquals(1, first.newRank());
        assertFalse(first.beatOtherPlayerServerRecord());

        // Equal time: must NOT replace PB or its splits
        BoardStore.FinishRecordResult tied = store.recordFinish(
                "parkour", alice, "Alice", 15_000L, List.of(4_000L, 9_000L), 2_000L
        );
        assertFalse(tied.newPb());
        assertEquals(15_000L, tied.previousPbTotalMs());
        assertEquals(0L, tied.deltaVsPreviousPbMs());
        PersonalBest storedAfterTie = store.getPersonalBest("parkour", alice).orElseThrow();
        assertEquals(1_000L, storedAfterTie.achievedAt());
        assertEquals(List.of(5_000L, 10_000L), storedAfterTie.splitsMs());

        // Strictly faster: replaces PB and splits
        BoardStore.FinishRecordResult faster = store.recordFinish(
                "parkour", alice, "Alice", 14_200L, List.of(4_500L, 9_100L), 3_000L
        );
        assertTrue(faster.newPb());
        assertEquals(-800L, faster.deltaVsPreviousPbMs());
        assertFalse(faster.beatOtherPlayerServerRecord()); // beating own #1 does not announce
        PersonalBest storedAfterFast = store.getPersonalBest("parkour", alice).orElseThrow();
        assertEquals(14_200L, storedAfterFast.totalMs());
        assertEquals(List.of(4_500L, 9_100L), storedAfterFast.splitsMs());
        assertEquals(3_000L, storedAfterFast.achievedAt());
    }

    @Test
    void serverRecordAnnouncementOnlyWhenBeatingAnotherPlayersFirstPlace() {
        BoardStore store = new BoardStore();
        store.createBoard("race", "Elytra Race", "Admin", 100L);

        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();

        // Alice sets initial #1 (no previous record) -> no announcement
        BoardStore.FinishRecordResult r1 = store.recordFinish("race", alice, "Alice", 20_000L, List.of(), 1_000L);
        assertFalse(r1.beatOtherPlayerServerRecord());

        // Bob ties Alice -> Bob is rank #2 (Alice achieved earlier), no announcement
        BoardStore.FinishRecordResult r2 = store.recordFinish("race", bob, "Bob", 20_000L, List.of(), 2_000L);
        assertEquals(2, r2.newRank());
        assertFalse(r2.beatOtherPlayerServerRecord());

        // Bob beats Alice -> rank #1, announcement triggered
        BoardStore.FinishRecordResult r3 = store.recordFinish("race", bob, "Bob", 19_500L, List.of(), 3_000L);
        assertTrue(r3.newPb());
        assertEquals(1, r3.newRank());
        assertTrue(r3.beatOtherPlayerServerRecord());
        assertEquals("Alice", r3.previousServerRecordHolder());
        assertEquals(20_000L, r3.previousServerRecordMs());

        // Bob beats his own #1 -> no announcement
        BoardStore.FinishRecordResult r4 = store.recordFinish("race", bob, "Bob", 19_000L, List.of(), 4_000L);
        assertTrue(r4.newPb());
        assertEquals(1, r4.newRank());
        assertFalse(r4.beatOtherPlayerServerRecord());
    }

    @Test
    void concurrentRunsAndSplitLimitsWorkWithoutInterference() {
        AtomicLong nanoClock = new AtomicLong(1_000_000_000L);
        AtomicLong epochClock = new AtomicLong(10_000L);
        RunManager runs = new RunManager(nanoClock::get, epochClock::get);
        BoardStore store = new BoardStore();
        store.createBoard("course", "Course", "Admin", 100L);

        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();

        assertEquals(RunManager.StartStatus.STARTED, runs.startRun(alice, "course").status());
        // Cannot start second run while active
        assertEquals(RunManager.StartStatus.ALREADY_RUNNING, runs.startRun(alice, "course").status());

        nanoClock.addAndGet(2_000_000_000L); // +2.000s
        assertEquals(RunManager.StartStatus.STARTED, runs.startRun(bob, "course").status());

        nanoClock.addAndGet(3_000_000_000L); // Alice at 5.000s, Bob at 3.000s
        RunManager.SplitResult aliceSplit1 = runs.recordSplit(alice, 2, store);
        assertEquals(RunManager.SplitStatus.RECORDED, aliceSplit1.status());
        assertEquals(1, aliceSplit1.splitNumber());
        assertEquals(5_000L, aliceSplit1.cumulativeMs());
        assertEquals(SplitDelta.Kind.NO_PB, aliceSplit1.delta().kind());

        nanoClock.addAndGet(1_000_000_000L); // Alice at 6.000s, Bob at 4.000s
        assertEquals(RunManager.SplitStatus.RECORDED, runs.recordSplit(alice, 2, store).status());
        // Max splits = 2 -> 3rd split rejected
        assertEquals(RunManager.SplitStatus.MAX_SPLITS_REACHED, runs.recordSplit(alice, 2, store).status());

        nanoClock.addAndGet(4_000_000_000L); // Alice at 10.000s, Bob at 8.000s
        RunManager.FinishResult aliceFinish = runs.finishRun(alice, "Alice", store);
        assertEquals(RunManager.FinishStatus.FINISHED, aliceFinish.status());
        assertEquals(10_000L, aliceFinish.recordResult().totalMs());
        assertTrue(runs.getActiveRun(alice).isEmpty());
        assertTrue(runs.getActiveRun(bob).isPresent());

        // Bob finishes independently
        RunManager.FinishResult bobFinish = runs.finishRun(bob, "Bob", store);
        assertEquals(RunManager.FinishStatus.FINISHED, bobFinish.status());
        assertEquals(8_000L, bobFinish.recordResult().totalMs());
        assertEquals(1, bobFinish.recordResult().newRank());
        assertTrue(bobFinish.recordResult().beatOtherPlayerServerRecord());

        // Alice runs a second time and splits compare against her PB
        nanoClock.addAndGet(1_000_000_000L);
        runs.startRun(alice, "course");
        nanoClock.addAndGet(4_500_000_000L); // 4.500s vs PB split 1 of 5.000s -> -500ms
        RunManager.SplitResult aliceSecondRunSplit1 = runs.recordSplit(alice, 10, store);
        assertEquals(SplitDelta.Kind.HAS_DELTA, aliceSecondRunSplit1.delta().kind());
        assertEquals(-500L, aliceSecondRunSplit1.delta().deltaMs());
    }
}
