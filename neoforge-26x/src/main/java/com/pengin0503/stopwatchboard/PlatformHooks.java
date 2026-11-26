package com.pengin0503.stopwatchboard;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Version-specific NeoForge/Minecraft hooks for Minecraft 26.x (26.1.2 / 26.2 / 26.3).
 */
public final class PlatformHooks {
    private static final SavedDataType<StopwatchSavedData> SAVED_DATA_TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(StopwatchBoardMod.MOD_ID, "boards"),
            StopwatchSavedData::new,
            StopwatchSavedData.CODEC,
            null
    );

    private PlatformHooks() {
    }

    /**
     * Resolves ModConfig.Type.SERVER on NeoForge 26.1.2/26.2 (FML 11) and ModConfig.Type.SYNCED
     * on NeoForge 26.3+ (FML 12) so a single 26.x JAR runs cleanly across all 26.x releases.
     */
    public static void registerServerConfig(ModContainer modContainer, ModConfigSpec spec) {
        ModConfig.Type configType;
        try {
            configType = ModConfig.Type.valueOf("SERVER");
        } catch (IllegalArgumentException e) {
            configType = ModConfig.Type.valueOf("SYNCED");
        }
        modContainer.registerConfig(configType, spec);
    }

    public static StopwatchSavedData getSavedData(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(SAVED_DATA_TYPE);
    }

    public static boolean hasAdminPermission(CommandSourceStack source) {
        return Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(source);
    }

    public static void sendActionBar(ServerPlayer player, Component message) {
        player.sendOverlayMessage(message);
    }

    public static String getPlayerLanguage(ServerPlayer player) {
        return player.clientInformation().language();
    }
}
