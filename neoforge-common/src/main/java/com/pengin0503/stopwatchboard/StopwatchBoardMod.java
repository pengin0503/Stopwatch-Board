package com.pengin0503.stopwatchboard;

import com.pengin0503.stopwatchboard.core.RunManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Main entrypoint for Stopwatch Board (server-side only NeoForge mod, vanilla client compatible).
 */
@Mod(StopwatchBoardMod.MOD_ID)
public final class StopwatchBoardMod {
    public static final String MOD_ID = "stopwatchboard";

    public StopwatchBoardMod(IEventBus modEventBus, ModContainer modContainer) {
        PlatformHooks.registerServerConfig(modContainer, StopwatchBoardConfig.SPEC);

        RunManager runManager = new RunManager();
        new StopwatchEvents(runManager).register(NeoForge.EVENT_BUS);
    }
}
