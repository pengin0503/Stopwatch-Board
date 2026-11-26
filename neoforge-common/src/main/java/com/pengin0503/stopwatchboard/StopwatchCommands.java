package com.pengin0503.stopwatchboard;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.pengin0503.stopwatchboard.core.ActiveRun;
import com.pengin0503.stopwatchboard.core.BoardDefinition;
import com.pengin0503.stopwatchboard.core.BoardId;
import com.pengin0503.stopwatchboard.core.BoardStore;
import com.pengin0503.stopwatchboard.core.Leaderboard;
import com.pengin0503.stopwatchboard.core.PersonalBest;
import com.pengin0503.stopwatchboard.core.RunManager;
import com.pengin0503.stopwatchboard.core.SplitDelta;
import com.pengin0503.stopwatchboard.core.TimeFormatter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Registers /swb and its alias /stopwatchboard.
 */
public final class StopwatchCommands {
    private static final SuggestionProvider<CommandSourceStack> BOARD_SUGGESTIONS = (context, builder) -> {
        MinecraftServer server = context.getSource().getServer();
        StopwatchSavedData data = PlatformHooks.getSavedData(server);
        List<String> ids = data.store().listBoards().stream().map(BoardDefinition::id).toList();
        return SharedSuggestionProvider.suggest(ids, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> PB_PLAYER_SUGGESTIONS = (context, builder) -> {
        MinecraftServer server = context.getSource().getServer();
        Set<String> names = new LinkedHashSet<>();
        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            names.add(online.getName().getString());
        }
        try {
            String boardId = StringArgumentType.getString(context, "board");
            StopwatchSavedData data = PlatformHooks.getSavedData(server);
            for (PersonalBest pb : data.store().snapshotPersonalBests(boardId)) {
                names.add(pb.playerName());
            }
        } catch (IllegalArgumentException ignored) {
            // board arg not yet available
        }
        return SharedSuggestionProvider.suggest(names, builder);
    };

    private StopwatchCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, RunManager runManager) {
        dispatcher.register(buildRoot("swb", runManager));
        dispatcher.register(buildRoot("stopwatchboard", runManager));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildRoot(String rootName, RunManager runManager) {
        return Commands.literal(rootName)
                .then(Commands.literal("boards")
                        .executes(ctx -> handleBoards(ctx)))
                .then(Commands.literal("start")
                        .then(Commands.argument("board", StringArgumentType.word())
                                .suggests(BOARD_SUGGESTIONS)
                                .executes(ctx -> handleStart(ctx, runManager))))
                .then(Commands.literal("split")
                        .executes(ctx -> handleSplit(ctx, runManager)))
                .then(Commands.literal("finish")
                        .executes(ctx -> handleFinish(ctx, runManager)))
                .then(Commands.literal("cancel")
                        .executes(ctx -> handleCancel(ctx, runManager)))
                .then(Commands.literal("status")
                        .executes(ctx -> handleStatus(ctx, runManager)))
                .then(Commands.literal("top")
                        .then(Commands.argument("board", StringArgumentType.word())
                                .suggests(BOARD_SUGGESTIONS)
                                .executes(ctx -> handleTop(ctx, 1))
                                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                        .executes(ctx -> handleTop(ctx, IntegerArgumentType.getInteger(ctx, "page"))))))
                .then(Commands.literal("pb")
                        .then(Commands.argument("board", StringArgumentType.word())
                                .suggests(BOARD_SUGGESTIONS)
                                .executes(ctx -> handlePb(ctx, null))
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .suggests(PB_PLAYER_SUGGESTIONS)
                                        .executes(ctx -> handlePb(ctx, StringArgumentType.getString(ctx, "player"))))))
                .then(Commands.literal("board")
                        .requires(PlatformHooks::hasAdminPermission)
                        .then(Commands.literal("create")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> handleBoardCreate(ctx, null))
                                        .then(Commands.argument("displayName", StringArgumentType.greedyString())
                                                .executes(ctx -> handleBoardCreate(ctx, StringArgumentType.getString(ctx, "displayName"))))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests(BOARD_SUGGESTIONS)
                                        .executes(ctx -> handleBoardRemove(ctx, runManager))))
                        .then(Commands.literal("reset")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests(BOARD_SUGGESTIONS)
                                        .executes(ctx -> handleBoardReset(ctx))))
                        .then(Commands.literal("list")
                                .executes(ctx -> handleBoardList(ctx))));
    }

    private static ServerPlayer requirePlayer(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            Messages.sendFailure(source, "stopwatchboard.error.player_only");
        }
        return player;
    }

    private static int handleBoards(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = requirePlayer(source);
        if (player == null) {
            return 0;
        }

        StopwatchSavedData data = PlatformHooks.getSavedData(source.getServer());
        List<BoardDefinition> boards = data.store().listBoards();
        if (boards.isEmpty()) {
            Messages.sendSuccess(source, Messages.STYLE_GRAY, "stopwatchboard.boards.empty");
            return 1;
        }

        Messages.sendSuccess(source, Messages.STYLE_GOLD, "stopwatchboard.boards.header");
        for (BoardDefinition board : boards) {
            Component yourPb = data.store().getPersonalBest(board.id(), player.getUUID())
                    .map(pb -> Component.literal(TimeFormatter.formatTotal(pb.totalMs())))
                    .orElseGet(() -> Messages.tr(player, "stopwatchboard.boards.none_value"));

            Component serverBest = data.store().getServerBest(board.id())
                    .map(best -> (Component) Messages.tr(
                            player,
                            "stopwatchboard.boards.server_best_value",
                            TimeFormatter.formatTotal(best.totalMs()),
                            best.playerName()
                    ))
                    .orElseGet(() -> Messages.tr(player, "stopwatchboard.boards.none_value"));

            Messages.sendSuccess(
                    source,
                    Messages.STYLE_INFO,
                    "stopwatchboard.boards.entry",
                    board.effectiveDisplayName(),
                    board.id(),
                    yourPb,
                    serverBest
            );
        }
        return 1;
    }

    private static int handleStart(CommandContext<CommandSourceStack> ctx, RunManager runManager) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = requirePlayer(source);
        if (player == null) {
            return 0;
        }

        String boardId = StringArgumentType.getString(ctx, "board");
        StopwatchSavedData data = PlatformHooks.getSavedData(source.getServer());
        Optional<BoardDefinition> boardOpt = data.store().getBoard(boardId);
        if (boardOpt.isEmpty()) {
            Messages.sendFailure(source, "stopwatchboard.error.board_not_found", boardId);
            return 0;
        }

        RunManager.StartResult result = runManager.startRun(player.getUUID(), boardId);
        if (result.status() == RunManager.StartStatus.ALREADY_RUNNING) {
            Messages.sendFailure(source, "stopwatchboard.error.already_running", result.boardId());
            return 0;
        }

        Messages.sendSuccess(source, Messages.STYLE_SUCCESS, "stopwatchboard.start.success", boardOpt.get().effectiveDisplayName());
        return 1;
    }

    private static int handleSplit(CommandContext<CommandSourceStack> ctx, RunManager runManager) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = requirePlayer(source);
        if (player == null) {
            return 0;
        }

        StopwatchSavedData data = PlatformHooks.getSavedData(source.getServer());
        int maxSplits = StopwatchBoardConfig.maxSplitsPerRun();
        RunManager.SplitResult result = runManager.recordSplit(player.getUUID(), maxSplits, data.store());

        if (result.status() == RunManager.SplitStatus.NO_ACTIVE_RUN) {
            Messages.sendFailure(source, "stopwatchboard.error.no_active_run");
            return 0;
        }
        if (result.status() == RunManager.SplitStatus.MAX_SPLITS_REACHED) {
            Messages.sendFailure(source, "stopwatchboard.error.max_splits_reached", maxSplits);
            return 0;
        }

        String timeText = TimeFormatter.formatTotal(result.cumulativeMs());
        SplitDelta delta = result.delta();
        switch (delta.kind()) {
            case NO_PB -> Messages.sendSuccess(
                    source,
                    Messages.STYLE_AQUA,
                    "stopwatchboard.split.no_pb",
                    result.splitNumber(),
                    timeText
            );
            case NO_PB_SPLIT -> Messages.sendSuccess(
                    source,
                    Messages.STYLE_AQUA,
                    "stopwatchboard.split.no_pb_split",
                    result.splitNumber(),
                    timeText
            );
            case HAS_DELTA -> {
                Style style = delta.deltaMs() < 0 ? Messages.STYLE_SUCCESS
                        : (delta.deltaMs() > 0 ? Messages.STYLE_ERROR : Messages.STYLE_YELLOW);
                Messages.sendSuccess(
                        source,
                        style,
                        "stopwatchboard.split.delta",
                        result.splitNumber(),
                        timeText,
                        TimeFormatter.formatDelta(delta.deltaMs())
                );
            }
        }
        return 1;
    }

    private static int handleFinish(CommandContext<CommandSourceStack> ctx, RunManager runManager) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = requirePlayer(source);
        if (player == null) {
            return 0;
        }

        StopwatchSavedData data = PlatformHooks.getSavedData(source.getServer());
        RunManager.FinishResult result = runManager.finishRun(
                player.getUUID(),
                player.getName().getString(),
                data.store()
        );

        if (result.status() == RunManager.FinishStatus.NO_ACTIVE_RUN) {
            Messages.sendFailure(source, "stopwatchboard.error.no_active_run");
            return 0;
        }
        if (result.status() == RunManager.FinishStatus.BOARD_MISSING) {
            Messages.sendFailure(source, "stopwatchboard.error.board_not_found", result.boardId());
            return 0;
        }

        BoardStore.FinishRecordResult record = result.recordResult();
        if (record.storeMutated()) {
            data.setDirty();
        }

        String boardLabel = data.store().getBoard(result.boardId())
                .map(BoardDefinition::effectiveDisplayName)
                .orElse(result.boardId());
        String totalText = TimeFormatter.formatTotal(record.totalMs());

        if (record.deltaVsPreviousPbMs() != null) {
            Messages.sendSuccess(
                    source,
                    record.newPb() ? Messages.STYLE_SUCCESS : Messages.STYLE_INFO,
                    "stopwatchboard.finish.summary_with_delta",
                    boardLabel,
                    totalText,
                    TimeFormatter.formatDelta(record.deltaVsPreviousPbMs()),
                    record.newRank()
            );
        } else {
            Messages.sendSuccess(
                    source,
                    Messages.STYLE_SUCCESS,
                    "stopwatchboard.finish.summary_first",
                    boardLabel,
                    totalText,
                    record.newRank()
            );
        }

        if (record.newPb()) {
            Messages.sendSuccess(source, Messages.STYLE_GOLD, "stopwatchboard.finish.new_pb");
        }

        if (record.beatOtherPlayerServerRecord() && StopwatchBoardConfig.announceServerRecord()) {
            Messages.broadcast(
                    source.getServer(),
                    Messages.STYLE_GOLD,
                    "stopwatchboard.finish.server_record_broadcast",
                    player.getName().getString(),
                    boardLabel,
                    totalText
            );
        }
        return 1;
    }

    private static int handleCancel(CommandContext<CommandSourceStack> ctx, RunManager runManager) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = requirePlayer(source);
        if (player == null) {
            return 0;
        }

        Optional<ActiveRun> cancelled = runManager.cancelRun(player.getUUID());
        if (cancelled.isEmpty()) {
            Messages.sendFailure(source, "stopwatchboard.error.no_active_run");
            return 0;
        }

        Messages.sendSuccess(source, Messages.STYLE_YELLOW, "stopwatchboard.cancel.success", cancelled.get().boardId());
        return 1;
    }

    private static int handleStatus(CommandContext<CommandSourceStack> ctx, RunManager runManager) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = requirePlayer(source);
        if (player == null) {
            return 0;
        }

        Optional<ActiveRun> activeOpt = runManager.getActiveRun(player.getUUID());
        if (activeOpt.isEmpty()) {
            Messages.sendSuccess(source, Messages.STYLE_GRAY, "stopwatchboard.status.none");
            return 1;
        }

        ActiveRun run = activeOpt.get();
        long elapsedMs = runManager.elapsedMs(run);
        Messages.sendSuccess(
                source,
                Messages.STYLE_AQUA,
                "stopwatchboard.status.active",
                run.boardId(),
                TimeFormatter.formatTotal(elapsedMs),
                run.splitCount()
        );
        return 1;
    }

    private static int handleTop(CommandContext<CommandSourceStack> ctx, int page) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = requirePlayer(source);
        if (player == null) {
            return 0;
        }

        String boardId = StringArgumentType.getString(ctx, "board");
        StopwatchSavedData data = PlatformHooks.getSavedData(source.getServer());
        Optional<BoardDefinition> boardOpt = data.store().getBoard(boardId);
        if (boardOpt.isEmpty()) {
            Messages.sendFailure(source, "stopwatchboard.error.board_not_found", boardId);
            return 0;
        }

        List<PersonalBest> sorted = data.store().getLeaderboard(boardId);
        if (sorted.isEmpty()) {
            Messages.sendSuccess(source, Messages.STYLE_GRAY, "stopwatchboard.top.empty", boardOpt.get().effectiveDisplayName());
            return 1;
        }

        Leaderboard.PageResult pageResult = Leaderboard.page(sorted, page, Leaderboard.DEFAULT_PAGE_SIZE);
        if (!pageResult.validPage()) {
            Messages.sendFailure(source, "stopwatchboard.error.invalid_page", page, pageResult.totalPages());
            return 0;
        }

        Messages.sendSuccess(
                source,
                Messages.STYLE_GOLD,
                "stopwatchboard.top.header",
                boardOpt.get().effectiveDisplayName(),
                pageResult.page(),
                pageResult.totalPages()
        );
        int rank = pageResult.startRank();
        for (PersonalBest pb : pageResult.entries()) {
            Messages.sendSuccess(
                    source,
                    Messages.STYLE_INFO,
                    "stopwatchboard.top.entry",
                    rank++,
                    pb.playerName(),
                    TimeFormatter.formatTotal(pb.totalMs()),
                    TimeFormatter.formatDate(pb.achievedAt())
            );
        }
        return 1;
    }

    private static int handlePb(CommandContext<CommandSourceStack> ctx, String targetPlayerName) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = requirePlayer(source);
        if (player == null) {
            return 0;
        }

        String boardId = StringArgumentType.getString(ctx, "board");
        StopwatchSavedData data = PlatformHooks.getSavedData(source.getServer());
        Optional<BoardDefinition> boardOpt = data.store().getBoard(boardId);
        if (boardOpt.isEmpty()) {
            Messages.sendFailure(source, "stopwatchboard.error.board_not_found", boardId);
            return 0;
        }

        Optional<PersonalBest> pbOpt;
        if (targetPlayerName == null) {
            pbOpt = data.store().getPersonalBest(boardId, player.getUUID());
            if (pbOpt.isEmpty()) {
                Messages.sendFailure(source, "stopwatchboard.error.pb_not_found_self", boardOpt.get().effectiveDisplayName());
                return 0;
            }
        } else {
            ServerPlayer onlineTarget = source.getServer().getPlayerList().getPlayerByName(targetPlayerName);
            if (onlineTarget != null) {
                pbOpt = data.store().getPersonalBest(boardId, onlineTarget.getUUID());
            } else {
                pbOpt = data.store().findPersonalBestByName(boardId, targetPlayerName);
            }
            if (pbOpt.isEmpty()) {
                Messages.sendFailure(source, "stopwatchboard.error.pb_not_found_other", targetPlayerName, boardOpt.get().effectiveDisplayName());
                return 0;
            }
        }

        PersonalBest pb = pbOpt.get();
        Messages.sendSuccess(
                source,
                Messages.STYLE_GOLD,
                "stopwatchboard.pb.header",
                pb.playerName(),
                boardOpt.get().effectiveDisplayName(),
                TimeFormatter.formatTotal(pb.totalMs()),
                TimeFormatter.formatDate(pb.achievedAt())
        );

        if (pb.splitsMs().isEmpty()) {
            Messages.sendSuccess(source, Messages.STYLE_GRAY, "stopwatchboard.pb.no_splits");
        } else {
            for (int i = 0; i < pb.splitsMs().size(); i++) {
                Messages.sendSuccess(
                        source,
                        Messages.STYLE_INFO,
                        "stopwatchboard.pb.split_entry",
                        i + 1,
                        TimeFormatter.formatTotal(pb.splitsMs().get(i))
                );
            }
        }
        return 1;
    }

    private static int handleBoardCreate(CommandContext<CommandSourceStack> ctx, String displayName) {
        CommandSourceStack source = ctx.getSource();
        String id = StringArgumentType.getString(ctx, "id");
        if (!BoardId.isValid(id)) {
            Messages.sendFailure(source, "stopwatchboard.error.invalid_board_id", id);
            return 0;
        }

        StopwatchSavedData data = PlatformHooks.getSavedData(source.getServer());
        String creator = source.getPlayer() != null ? source.getPlayer().getName().getString() : "Server";
        boolean created = data.store().createBoard(id, displayName, creator, System.currentTimeMillis());
        if (!created) {
            Messages.sendFailure(source, "stopwatchboard.error.board_already_exists", id);
            return 0;
        }

        data.setDirty();
        BoardDefinition board = data.store().getBoard(id).orElseThrow();
        Messages.sendSuccess(source, Messages.STYLE_SUCCESS, "stopwatchboard.board.create.success", board.id(), board.effectiveDisplayName());
        return 1;
    }

    private static int handleBoardRemove(CommandContext<CommandSourceStack> ctx, RunManager runManager) {
        CommandSourceStack source = ctx.getSource();
        String id = StringArgumentType.getString(ctx, "id");
        StopwatchSavedData data = PlatformHooks.getSavedData(source.getServer());
        if (!data.store().removeBoard(id)) {
            Messages.sendFailure(source, "stopwatchboard.error.board_not_found", id);
            return 0;
        }

        data.setDirty();
        List<UUID> cancelledPlayers = runManager.cancelRunsForBoard(id);
        for (UUID playerId : cancelledPlayers) {
            ServerPlayer online = source.getServer().getPlayerList().getPlayer(playerId);
            if (online != null) {
                Messages.sendToPlayer(online, Messages.STYLE_YELLOW, "stopwatchboard.cancel.board_removed", id);
            }
        }

        Messages.sendSuccess(source, Messages.STYLE_SUCCESS, "stopwatchboard.board.remove.success", id);
        return 1;
    }

    private static int handleBoardReset(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String id = StringArgumentType.getString(ctx, "id");
        StopwatchSavedData data = PlatformHooks.getSavedData(source.getServer());
        if (!data.store().resetBoard(id)) {
            Messages.sendFailure(source, "stopwatchboard.error.board_not_found", id);
            return 0;
        }

        data.setDirty();
        Messages.sendSuccess(source, Messages.STYLE_SUCCESS, "stopwatchboard.board.reset.success", id);
        return 1;
    }

    private static int handleBoardList(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        StopwatchSavedData data = PlatformHooks.getSavedData(source.getServer());
        List<BoardDefinition> boards = data.store().listBoards();
        if (boards.isEmpty()) {
            Messages.sendSuccess(source, Messages.STYLE_GRAY, "stopwatchboard.board.list.empty");
            return 1;
        }

        Messages.sendSuccess(source, Messages.STYLE_GOLD, "stopwatchboard.board.list.header", boards.size());
        for (BoardDefinition board : boards) {
            Messages.sendSuccess(
                    source,
                    Messages.STYLE_INFO,
                    "stopwatchboard.board.list.entry",
                    board.id(),
                    board.effectiveDisplayName(),
                    data.store().getPersonalBestCount(board.id())
            );
        }
        return 1;
    }
}
