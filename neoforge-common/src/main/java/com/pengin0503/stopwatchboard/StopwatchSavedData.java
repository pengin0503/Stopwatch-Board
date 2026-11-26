package com.pengin0503.stopwatchboard;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.pengin0503.stopwatchboard.core.BoardDefinition;
import com.pengin0503.stopwatchboard.core.BoardId;
import com.pengin0503.stopwatchboard.core.BoardStore;
import com.pengin0503.stopwatchboard.core.PersonalBest;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Per-world SavedData storing all boards and persisted personal bests.
 * Uses a lenient Dynamic codec so corrupt or unknown entries are skipped with a warning log
 * instead of crashing the server.
 */
public final class StopwatchSavedData extends SavedData {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final Codec<StopwatchSavedData> CODEC = Codec.PASSTHROUGH.xmap(
            StopwatchSavedData::fromDynamic,
            StopwatchSavedData::toDynamic
    );

    private final BoardStore store;

    public StopwatchSavedData() {
        this(new BoardStore());
    }

    private StopwatchSavedData(BoardStore store) {
        this.store = store;
    }

    public BoardStore store() {
        return store;
    }

    private static <T> StopwatchSavedData fromDynamic(Dynamic<T> root) {
        BoardStore store = new BoardStore();
        Optional<List<Dynamic<T>>> maybeBoards = root.get("boards").asListOpt(Function.identity()).result();
        if (maybeBoards.isEmpty()) {
            return new StopwatchSavedData(store);
        }

        for (Dynamic<T> boardDyn : maybeBoards.get()) {
            try {
                Optional<String> idOpt = boardDyn.get("id").asString().result();
                if (idOpt.isEmpty() || !BoardId.isValid(idOpt.get())) {
                    LOGGER.warn("[StopwatchBoard] Skipping corrupt board entry with invalid or missing id: {}", idOpt.orElse("<missing>"));
                    continue;
                }
                String id = idOpt.get();
                String displayName = boardDyn.get("displayName").asString().result().orElse(id);
                String createdBy = boardDyn.get("createdBy").asString().result().orElse("Server");
                long createdAt = boardDyn.get("createdAt").asNumber().result().map(Number::longValue).orElse(0L);

                store.putLoadedBoard(new BoardDefinition(id, displayName, createdBy, createdAt));

                Optional<List<Dynamic<T>>> maybePbs = boardDyn.get("pbs").asListOpt(Function.identity()).result();
                if (maybePbs.isPresent()) {
                    for (Dynamic<T> pbDyn : maybePbs.get()) {
                        parsePersonalBest(id, pbDyn).ifPresent(pb -> store.putLoadedPersonalBest(id, pb));
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("[StopwatchBoard] Skipping corrupt board entry: {}", e.getMessage());
            }
        }

        return new StopwatchSavedData(store);
    }

    private static <T> Optional<PersonalBest> parsePersonalBest(String boardId, Dynamic<T> pbDyn) {
        try {
            Optional<String> uuidStr = pbDyn.get("playerId").asString().result();
            if (uuidStr.isEmpty()) {
                LOGGER.warn("[StopwatchBoard] Skipping corrupt PB entry on board '{}': missing playerId", boardId);
                return Optional.empty();
            }
            UUID playerId = UUID.fromString(uuidStr.get());
            String playerName = pbDyn.get("playerName").asString().result().orElse(playerId.toString());

            Optional<Number> totalNum = pbDyn.get("totalMs").asNumber().result();
            if (totalNum.isEmpty() || totalNum.get().longValue() < 0L) {
                LOGGER.warn("[StopwatchBoard] Skipping corrupt PB entry on board '{}' for {}: invalid totalMs", boardId, playerId);
                return Optional.empty();
            }
            long totalMs = totalNum.get().longValue();
            long achievedAt = pbDyn.get("achievedAt").asNumber().result().map(Number::longValue).orElse(0L);

            List<Long> splits = new ArrayList<>();
            Optional<List<Dynamic<T>>> maybeSplits = pbDyn.get("splitsMs").asListOpt(Function.identity()).result();
            if (maybeSplits.isPresent()) {
                for (Dynamic<T> splitDyn : maybeSplits.get()) {
                    Optional<Number> n = splitDyn.asNumber().result();
                    if (n.isEmpty() || n.get().longValue() < 0L) {
                        LOGGER.warn("[StopwatchBoard] Skipping corrupt PB entry on board '{}' for {}: invalid split value", boardId, playerId);
                        return Optional.empty();
                    }
                    splits.add(n.get().longValue());
                }
            }

            return Optional.of(new PersonalBest(playerId, playerName, totalMs, splits, achievedAt));
        } catch (Exception e) {
            LOGGER.warn("[StopwatchBoard] Skipping corrupt PB entry on board '{}': {}", boardId, e.getMessage());
            return Optional.empty();
        }
    }

    private Dynamic<Tag> toDynamic() {
        NbtOps ops = NbtOps.INSTANCE;
        Stream<Tag> boardTags = store.snapshotBoards().stream().map(board -> {
            Map<Tag, Tag> boardMap = new LinkedHashMap<>();
            boardMap.put(ops.createString("id"), ops.createString(board.id()));
            boardMap.put(ops.createString("displayName"), ops.createString(board.displayName()));
            boardMap.put(ops.createString("createdBy"), ops.createString(board.createdBy()));
            boardMap.put(ops.createString("createdAt"), ops.createLong(board.createdAt()));

            Stream<Tag> pbTags = store.snapshotPersonalBests(board.id()).stream().map(pb -> {
                Map<Tag, Tag> pbMap = new LinkedHashMap<>();
                pbMap.put(ops.createString("playerId"), ops.createString(pb.playerId().toString()));
                pbMap.put(ops.createString("playerName"), ops.createString(pb.playerName()));
                pbMap.put(ops.createString("totalMs"), ops.createLong(pb.totalMs()));
                pbMap.put(ops.createString("achievedAt"), ops.createLong(pb.achievedAt()));
                pbMap.put(ops.createString("splitsMs"), ops.createList(pb.splitsMs().stream().map(ops::createLong)));
                return ops.createMap(pbMap);
            });
            boardMap.put(ops.createString("pbs"), ops.createList(pbTags));
            return ops.createMap(boardMap);
        });

        Map<Tag, Tag> rootMap = new LinkedHashMap<>();
        rootMap.put(ops.createString("version"), ops.createInt(1));
        rootMap.put(ops.createString("boards"), ops.createList(boardTags));
        return new Dynamic<>(ops, ops.createMap(rootMap));
    }
}
