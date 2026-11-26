package com.pengin0503.stopwatchboard.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable persisted personal best record for a single player on a board.
 */
public record PersonalBest(
        UUID playerId,
        String playerName,
        long totalMs,
        List<Long> splitsMs,
        long achievedAt
) {
    public PersonalBest {
        Objects.requireNonNull(playerId, "playerId");
        playerName = (playerName == null || playerName.isBlank()) ? playerId.toString() : playerName;
        if (totalMs < 0) {
            throw new IllegalArgumentException("totalMs must be non-negative: " + totalMs);
        }
        Objects.requireNonNull(splitsMs, "splitsMs");
        List<Long> copy = new ArrayList<>(splitsMs.size());
        long previous = 0L;
        for (Long split : splitsMs) {
            if (split == null || split < 0) {
                throw new IllegalArgumentException("Split duration must be non-negative: " + split);
            }
            if (split < previous) {
                throw new IllegalArgumentException("Splits must be non-decreasing: " + split + " < " + previous);
            }
            copy.add(split);
            previous = split;
        }
        splitsMs = Collections.unmodifiableList(copy);
    }

    public PersonalBest withPlayerName(String updatedName) {
        if (updatedName == null || updatedName.isBlank() || updatedName.equals(this.playerName)) {
            return this;
        }
        return new PersonalBest(this.playerId, updatedName, this.totalMs, this.splitsMs, this.achievedAt);
    }
}
