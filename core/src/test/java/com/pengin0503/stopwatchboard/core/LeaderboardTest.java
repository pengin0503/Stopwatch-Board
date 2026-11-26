package com.pengin0503.stopwatchboard.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeaderboardTest {

    @Test
    void sortsByTotalMsAscendingAndEarlierAchievedAtOnTie() {
        UUID u1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID u2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID u3 = UUID.fromString("00000000-0000-0000-0000-000000000003");

        PersonalBest laterTie = new PersonalBest(u1, "Later", 10_000L, List.of(), 2_000L);
        PersonalBest earlierTie = new PersonalBest(u2, "Earlier", 10_000L, List.of(), 1_000L);
        PersonalBest fastest = new PersonalBest(u3, "Fastest", 9_500L, List.of(), 3_000L);

        List<PersonalBest> sorted = Leaderboard.sort(List.of(laterTie, earlierTie, fastest));

        assertEquals(List.of(fastest, earlierTie, laterTie), sorted);
        assertEquals(OptionalInt.of(1), Leaderboard.rankOf(sorted, u3));
        assertEquals(OptionalInt.of(2), Leaderboard.rankOf(sorted, u2));
        assertEquals(OptionalInt.of(3), Leaderboard.rankOf(sorted, u1));
        assertEquals(OptionalInt.empty(), Leaderboard.rankOf(sorted, UUID.randomUUID()));
    }

    @Test
    void paginationSlicesTenPerPage() {
        List<PersonalBest> entries = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            entries.add(new PersonalBest(
                    UUID.randomUUID(),
                    "Player" + i,
                    i * 1_000L,
                    List.of(),
                    i * 100L
            ));
        }
        List<PersonalBest> sorted = Leaderboard.sort(entries);

        Leaderboard.PageResult page1 = Leaderboard.page(sorted, 1, Leaderboard.DEFAULT_PAGE_SIZE);
        assertTrue(page1.validPage());
        assertEquals(1, page1.page());
        assertEquals(2, page1.totalPages());
        assertEquals(1, page1.startRank());
        assertEquals(10, page1.entries().size());
        assertEquals("Player1", page1.entries().get(0).playerName());

        Leaderboard.PageResult page2 = Leaderboard.page(sorted, 2, Leaderboard.DEFAULT_PAGE_SIZE);
        assertTrue(page2.validPage());
        assertEquals(2, page2.page());
        assertEquals(11, page2.startRank());
        assertEquals(5, page2.entries().size());
        assertEquals("Player15", page2.entries().get(4).playerName());

        Leaderboard.PageResult outOfRange = Leaderboard.page(sorted, 3, Leaderboard.DEFAULT_PAGE_SIZE);
        assertFalse(outOfRange.validPage());
        assertEquals(2, outOfRange.totalPages());
    }
}
