package com.pengin0503.stopwatchboard.core;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Pure Java data store for boards and their persisted personal bests.
 * Minecraft's SavedData delegates storage and business rules to this class.
 */
public final class BoardStore {
    private final Map<String, BoardDefinition> boards = new LinkedHashMap<>();
    private final Map<String, Map<UUID, PersonalBest>> personalBestsByBoard = new HashMap<>();

    public boolean createBoard(String id, String displayName, String createdBy, long createdAt) {
        BoardId.requireValid(id);
        if (boards.containsKey(id)) {
            return false;
        }
        boards.put(id, new BoardDefinition(id, displayName, createdBy, createdAt));
        personalBestsByBoard.put(id, new HashMap<>());
        return true;
    }

    public boolean removeBoard(String id) {
        if (!boards.containsKey(id)) {
            return false;
        }
        boards.remove(id);
        personalBestsByBoard.remove(id);
        return true;
    }

    public boolean resetBoard(String id) {
        if (!boards.containsKey(id)) {
            return false;
        }
        personalBestsByBoard.computeIfAbsent(id, k -> new HashMap<>()).clear();
        return true;
    }

    public Optional<BoardDefinition> getBoard(String id) {
        return Optional.ofNullable(boards.get(id));
    }

    public boolean hasBoard(String id) {
        return boards.containsKey(id);
    }

    public List<BoardDefinition> listBoards() {
        return boards.values().stream()
                .sorted(Comparator.comparing(BoardDefinition::id))
                .toList();
    }

    public Optional<PersonalBest> getPersonalBest(String boardId, UUID playerId) {
        Map<UUID, PersonalBest> pbs = personalBestsByBoard.get(boardId);
        if (pbs == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(pbs.get(playerId));
    }

    public Optional<PersonalBest> findPersonalBestByName(String boardId, String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return Optional.empty();
        }
        Map<UUID, PersonalBest> pbs = personalBestsByBoard.get(boardId);
        if (pbs == null) {
            return Optional.empty();
        }
        String target = playerName.toLowerCase(Locale.ROOT);
        return pbs.values().stream()
                .filter(pb -> pb.playerName().toLowerCase(Locale.ROOT).equals(target))
                .findFirst();
    }

    public List<PersonalBest> getLeaderboard(String boardId) {
        Map<UUID, PersonalBest> pbs = personalBestsByBoard.get(boardId);
        if (pbs == null || pbs.isEmpty()) {
            return List.of();
        }
        return Leaderboard.sort(pbs.values());
    }

    public Optional<PersonalBest> getServerBest(String boardId) {
        List<PersonalBest> sorted = getLeaderboard(boardId);
        return sorted.isEmpty() ? Optional.empty() : Optional.of(sorted.get(0));
    }

    public int getPersonalBestCount(String boardId) {
        Map<UUID, PersonalBest> pbs = personalBestsByBoard.get(boardId);
        return pbs == null ? 0 : pbs.size();
    }

    /**
     * Records a completed run on {@code boardId}.
     * PB replacement rule: replaces existing PB only when strictly faster ({@code totalMs < existing.totalMs}).
     * Equal time keeps the earlier record and its splits.
     * Server record beat rule: true only if there was a previous #1 held by a different player and this run is strictly faster.
     */
    public FinishRecordResult recordFinish(
            String boardId,
            UUID playerId,
            String playerName,
            long totalMs,
            List<Long> splitsMs,
            long achievedAt
    ) {
        if (!boards.containsKey(boardId)) {
            throw new IllegalArgumentException("Board does not exist: " + boardId);
        }
        Map<UUID, PersonalBest> pbs = personalBestsByBoard.computeIfAbsent(boardId, k -> new HashMap<>());

        PersonalBest previousLeader = getServerBest(boardId).orElse(null);
        PersonalBest existingPb = pbs.get(playerId);

        Long previousPbTotalMs = existingPb != null ? existingPb.totalMs() : null;
        Long deltaVsPreviousPbMs = existingPb != null ? (totalMs - existingPb.totalMs()) : null;

        boolean newPb;
        boolean mutated = false;

        if (existingPb == null || totalMs < existingPb.totalMs()) {
            PersonalBest updated = new PersonalBest(playerId, playerName, totalMs, splitsMs, achievedAt);
            pbs.put(playerId, updated);
            newPb = true;
            mutated = true;
        } else {
            newPb = false;
            PersonalBest refreshed = existingPb.withPlayerName(playerName);
            if (refreshed != existingPb) {
                pbs.put(playerId, refreshed);
                mutated = true;
            }
        }

        int newRank = Leaderboard.rankOf(pbs.values(), playerId).orElseThrow();
        boolean beatOtherPlayerServerRecord = newPb
                && previousLeader != null
                && !previousLeader.playerId().equals(playerId)
                && totalMs < previousLeader.totalMs();

        return new FinishRecordResult(
                totalMs,
                previousPbTotalMs,
                deltaVsPreviousPbMs,
                newPb,
                newRank,
                beatOtherPlayerServerRecord,
                previousLeader != null ? previousLeader.playerName() : null,
                previousLeader != null ? previousLeader.totalMs() : null,
                mutated
        );
    }

    /**
     * Loads a validated board entry (used during deserialization).
     */
    public void putLoadedBoard(BoardDefinition board) {
        boards.put(board.id(), board);
        personalBestsByBoard.computeIfAbsent(board.id(), k -> new HashMap<>());
    }

    /**
     * Loads a validated personal best entry for an existing board (used during deserialization).
     * If duplicate entries exist for the same player, keeps the faster one (earlier achievedAt on tie).
     */
    public void putLoadedPersonalBest(String boardId, PersonalBest pb) {
        if (!boards.containsKey(boardId)) {
            return;
        }
        Map<UUID, PersonalBest> pbs = personalBestsByBoard.computeIfAbsent(boardId, k -> new HashMap<>());
        PersonalBest existing = pbs.get(pb.playerId());
        if (existing == null || Leaderboard.COMPARATOR.compare(pb, existing) < 0) {
            pbs.put(pb.playerId(), pb);
        }
    }

    public Collection<BoardDefinition> snapshotBoards() {
        return Collections.unmodifiableCollection(boards.values());
    }

    public Collection<PersonalBest> snapshotPersonalBests(String boardId) {
        Map<UUID, PersonalBest> pbs = personalBestsByBoard.get(boardId);
        if (pbs == null) {
            return List.of();
        }
        return Collections.unmodifiableCollection(pbs.values());
    }

    public record FinishRecordResult(
            long totalMs,
            Long previousPbTotalMs,
            Long deltaVsPreviousPbMs,
            boolean newPb,
            int newRank,
            boolean beatOtherPlayerServerRecord,
            String previousServerRecordHolder,
            Long previousServerRecordMs,
            boolean storeMutated
    ) {
    }
}
