package com.pengin0503.stopwatchboard.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * Pure Java leaderboard sorting, ranking, and pagination derived from personal bests.
 */
public final class Leaderboard {
    public static final int DEFAULT_PAGE_SIZE = 10;

    /**
     * Ordering: faster totalMs first; ties broken by earlier achievedAt; then UUID for determinism.
     */
    public static final Comparator<PersonalBest> COMPARATOR = Comparator
            .comparingLong(PersonalBest::totalMs)
            .thenComparingLong(PersonalBest::achievedAt)
            .thenComparing(pb -> pb.playerId().toString());

    private Leaderboard() {
    }

    public static List<PersonalBest> sort(Collection<PersonalBest> personalBests) {
        if (personalBests == null || personalBests.isEmpty()) {
            return List.of();
        }
        List<PersonalBest> sorted = new ArrayList<>(personalBests);
        sorted.sort(COMPARATOR);
        return Collections.unmodifiableList(sorted);
    }

    /**
     * Returns the 1-based rank of {@code playerId} in the sorted leaderboard, or empty if absent.
     */
    public static OptionalInt rankOf(Collection<PersonalBest> personalBests, UUID playerId) {
        List<PersonalBest> sorted = sort(personalBests);
        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).playerId().equals(playerId)) {
                return OptionalInt.of(i + 1);
            }
        }
        return OptionalInt.empty();
    }

    /**
     * Returns a 1-based page slice from an already-sorted leaderboard.
     */
    public static PageResult page(List<PersonalBest> sorted, int pageNumber, int pageSize) {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize must be positive: " + pageSize);
        }
        int totalEntries = sorted.size();
        int totalPages = totalEntries == 0 ? 1 : (totalEntries + pageSize - 1) / pageSize;
        if (pageNumber < 1 || pageNumber > totalPages) {
            return new PageResult(List.of(), pageNumber, totalPages, totalEntries, 0, false);
        }
        int fromIndex = (pageNumber - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, totalEntries);
        List<PersonalBest> slice = List.copyOf(sorted.subList(fromIndex, toIndex));
        return new PageResult(slice, pageNumber, totalPages, totalEntries, fromIndex + 1, true);
    }

    public record PageResult(
            List<PersonalBest> entries,
            int page,
            int totalPages,
            int totalEntries,
            int startRank,
            boolean validPage
    ) {
    }
}
