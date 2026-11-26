package com.pengin0503.stopwatchboard.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Manages in-memory active runs (at most one active run per player across all boards).
 */
public final class RunManager {
    private final Map<UUID, ActiveRun> activeRuns = new ConcurrentHashMap<>();
    private final LongSupplier nanoClock;
    private final LongSupplier epochMillisClock;

    public RunManager() {
        this(System::nanoTime, System::currentTimeMillis);
    }

    public RunManager(LongSupplier nanoClock, LongSupplier epochMillisClock) {
        this.nanoClock = Objects.requireNonNull(nanoClock, "nanoClock");
        this.epochMillisClock = Objects.requireNonNull(epochMillisClock, "epochMillisClock");
    }

    public StartResult startRun(UUID playerId, String boardId) {
        BoardId.requireValid(boardId);
        ActiveRun existing = activeRuns.get(playerId);
        if (existing != null) {
            return new StartResult(StartStatus.ALREADY_RUNNING, existing.boardId());
        }
        ActiveRun run = new ActiveRun(playerId, boardId, nanoClock.getAsLong());
        activeRuns.put(playerId, run);
        return new StartResult(StartStatus.STARTED, boardId);
    }

    public SplitResult recordSplit(UUID playerId, int maxSplitsPerRun, BoardStore store) {
        ActiveRun run = activeRuns.get(playerId);
        if (run == null) {
            return new SplitResult(SplitStatus.NO_ACTIVE_RUN, null, 0, 0L, null);
        }
        if (run.splitCount() >= maxSplitsPerRun) {
            return new SplitResult(SplitStatus.MAX_SPLITS_REACHED, run.boardId(), run.splitCount(), 0L, null);
        }
        int splitIndex = run.splitCount();
        long cumulativeMs = run.addSplit(nanoClock.getAsLong());
        PersonalBest pb = store.getPersonalBest(run.boardId(), playerId).orElse(null);
        SplitDelta delta = SplitDelta.compute(cumulativeMs, splitIndex, pb);
        return new SplitResult(SplitStatus.RECORDED, run.boardId(), splitIndex + 1, cumulativeMs, delta);
    }

    public FinishResult finishRun(UUID playerId, String playerName, BoardStore store) {
        ActiveRun run = activeRuns.remove(playerId);
        if (run == null) {
            return new FinishResult(FinishStatus.NO_ACTIVE_RUN, null, 0, null);
        }
        if (!store.hasBoard(run.boardId())) {
            return new FinishResult(FinishStatus.BOARD_MISSING, run.boardId(), run.splitCount(), null);
        }
        long totalMs = run.elapsedMs(nanoClock.getAsLong());
        List<Long> splits = run.splitsMs();
        if (!splits.isEmpty()) {
            long lastSplit = splits.get(splits.size() - 1);
            if (totalMs < lastSplit) {
                totalMs = lastSplit;
            }
        }
        BoardStore.FinishRecordResult recordResult = store.recordFinish(
                run.boardId(),
                playerId,
                playerName,
                totalMs,
                splits,
                epochMillisClock.getAsLong()
        );
        return new FinishResult(FinishStatus.FINISHED, run.boardId(), splits.size(), recordResult);
    }

    public Optional<ActiveRun> cancelRun(UUID playerId) {
        return Optional.ofNullable(activeRuns.remove(playerId));
    }

    public List<UUID> cancelRunsForBoard(String boardId) {
        List<UUID> cancelled = new ArrayList<>();
        Iterator<Map.Entry<UUID, ActiveRun>> it = activeRuns.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, ActiveRun> entry = it.next();
            if (entry.getValue().boardId().equals(boardId)) {
                cancelled.add(entry.getKey());
                it.remove();
            }
        }
        return cancelled;
    }

    public Optional<ActiveRun> getActiveRun(UUID playerId) {
        return Optional.ofNullable(activeRuns.get(playerId));
    }

    public Collection<ActiveRun> getAllActiveRuns() {
        return Collections.unmodifiableCollection(activeRuns.values());
    }

    public long elapsedMs(ActiveRun run) {
        return run.elapsedMs(nanoClock.getAsLong());
    }

    public void clearAll() {
        activeRuns.clear();
    }

    public enum StartStatus {
        STARTED,
        ALREADY_RUNNING
    }

    public record StartResult(StartStatus status, String boardId) {
    }

    public enum SplitStatus {
        RECORDED,
        NO_ACTIVE_RUN,
        MAX_SPLITS_REACHED
    }

    public record SplitResult(
            SplitStatus status,
            String boardId,
            int splitNumber,
            long cumulativeMs,
            SplitDelta delta
    ) {
    }

    public enum FinishStatus {
        FINISHED,
        NO_ACTIVE_RUN,
        BOARD_MISSING
    }

    public record FinishResult(
            FinishStatus status,
            String boardId,
            int splitCount,
            BoardStore.FinishRecordResult recordResult
    ) {
    }
}
