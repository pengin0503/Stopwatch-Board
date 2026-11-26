package com.pengin0503.stopwatchboard;

import com.pengin0503.stopwatchboard.core.ActiveRun;
import com.pengin0503.stopwatchboard.core.RunManager;
import com.pengin0503.stopwatchboard.core.TimeFormatter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Optional;

/**
 * Registers NeoForge game bus listeners for commands, live action-bar ticks, disconnect, death, and server stop.
 */
public final class StopwatchEvents {
    private static final int ACTION_BAR_INTERVAL_TICKS = 10;

    private final RunManager runManager;
    private int tickCounter = 0;

    public StopwatchEvents(RunManager runManager) {
        this.runManager = runManager;
    }

    public void register(IEventBus gameBus) {
        gameBus.addListener(this::onRegisterCommands);
        gameBus.addListener(this::onServerTickPost);
        gameBus.addListener(this::onPlayerLoggedOut);
        gameBus.addListener(this::onLivingDeath);
        gameBus.addListener(this::onServerStopping);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        StopwatchCommands.register(event.getDispatcher(), runManager);
    }

    private void onServerTickPost(ServerTickEvent.Post event) {
        tickCounter++;
        if (tickCounter % ACTION_BAR_INTERVAL_TICKS != 0) {
            return;
        }
        if (!StopwatchBoardConfig.actionBarLive()) {
            return;
        }

        MinecraftServer server = event.getServer();
        for (ActiveRun run : runManager.getAllActiveRuns()) {
            ServerPlayer player = server.getPlayerList().getPlayer(run.playerId());
            if (player == null) {
                continue;
            }
            long elapsedMs = runManager.elapsedMs(run);
            Component actionBar = Messages.tr(
                    player,
                    "stopwatchboard.actionbar.live",
                    run.boardId(),
                    TimeFormatter.formatTotal(elapsedMs)
            ).withStyle(Messages.STYLE_AQUA);
            PlatformHooks.sendActionBar(player, actionBar);
        }
    }

    private void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        runManager.cancelRun(event.getEntity().getUUID());
    }

    private void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!StopwatchBoardConfig.cancelOnDeath()) {
            return;
        }
        Optional<ActiveRun> cancelled = runManager.cancelRun(player.getUUID());
        cancelled.ifPresent(run -> Messages.sendToPlayer(
                player,
                Messages.STYLE_YELLOW,
                "stopwatchboard.cancel.death",
                run.boardId()
        ));
    }

    private void onServerStopping(ServerStoppingEvent event) {
        runManager.clearAll();
        tickCounter = 0;
    }
}
