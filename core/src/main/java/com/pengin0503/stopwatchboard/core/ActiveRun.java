package com.pengin0503.stopwatchboard.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * In-memory active stopwatch run for a single player.
 */
public final class ActiveRun {
    private static final long NANOS_PER_MILLI = 1_000_000L;

    private final UUID playerId;
    private final String boardId;
    private final long startNanos;
    private final List<Long> splitsMs = new ArrayList<>();

    public ActiveRun(UUID playerId, String boardId, long startNanos) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.boardId = BoardId.requireValid(boardId);
        this.startNanos = startNanos;
    }

    public UUID playerId() {
        return playerId;
    }

    public String boardId() {
        return boardId;
    }

    public long startNanos() {
        return startNanos;
    }

    public long elapsedMs(long currentNanos) {
        long diff = currentNanos - startNanos;
        return diff <= 0L ? 0L : diff / NANOS_PER_MILLI;
    }

    public int splitCount() {
        return splitsMs.size();
    }

    public List<Long> splitsMs() {
        return Collections.unmodifiableList(splitsMs);
    }

    long addSplit(long currentNanos) {
        long elapsed = elapsedMs(currentNanos);
        if (!splitsMs.isEmpty()) {
            long last = splitsMs.get(splitsMs.size() - 1);
            if (elapsed < last) {
                elapsed = last;
            }
        }
        splitsMs.add(elapsed);
        return elapsed;
    }
}
