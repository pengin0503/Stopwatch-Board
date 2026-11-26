package com.pengin0503.stopwatchboard;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Version-specific NeoForge/Minecraft hooks for Minecraft 1.21.11.
 */
public final class PlatformHooks {
    private static final SavedDataType<StopwatchSavedData> SAVED_DATA_TYPE = new SavedDataType<>(
            StopwatchBoardMod.MOD_ID,
            StopwatchSavedData::new,
            StopwatchSavedData.CODEC,
            null
    );

    private PlatformHooks() {
    }

    public static void registerServerConfig(ModContainer modContainer, ModConfigSpec spec) {
        modContainer.registerConfig(ModConfig.Type.SERVER, spec);
    }

    public static StopwatchSavedData getSavedData(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(SAVED_DATA_TYPE);
    }

    public static boolean hasAdminPermission(CommandSourceStack source) {
        return Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(source);
    }

    public static void sendActionBar(ServerPlayer player, Component message) {
        player.displayClientMessage(message, true);
    }

    public static String getPlayerLanguage(ServerPlayer player) {
        return player.clientInformation().language();
    }
}
