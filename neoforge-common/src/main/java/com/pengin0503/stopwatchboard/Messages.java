package com.pengin0503.stopwatchboard;

import com.pengin0503.stopwatchboard.core.LangCatalog;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Helper for building localized messages that work on both modded and vanilla clients.
 * Uses Component.translatableWithFallback with bundled en_us/ja_jp patterns matched to the
 * player's client locale. Uses Style RGB colors rather than ChatFormatting for 1.21.11..26.3 compatibility.
 */
public final class Messages {
    public static final Style STYLE_INFO = Style.EMPTY.withColor(0xFFFFFF);
    public static final Style STYLE_SUCCESS = Style.EMPTY.withColor(0x55FF55);
    public static final Style STYLE_ERROR = Style.EMPTY.withColor(0xFF5555);
    public static final Style STYLE_GOLD = Style.EMPTY.withColor(0xFFAA00);
    public static final Style STYLE_AQUA = Style.EMPTY.withColor(0x55FFFF);
    public static final Style STYLE_GRAY = Style.EMPTY.withColor(0xAAAAAA);
    public static final Style STYLE_YELLOW = Style.EMPTY.withColor(0xFFFF55);

    private static final LangCatalog CATALOG = LangCatalog.loadFromClasspath(Messages.class.getClassLoader());

    private Messages() {
    }

    public static MutableComponent tr(String locale, String key, Object... args) {
        String fallback = CATALOG.getFallback(locale, key);
        return Component.translatableWithFallback(key, fallback, args);
    }

    public static MutableComponent tr(ServerPlayer player, String key, Object... args) {
        return tr(PlatformHooks.getPlayerLanguage(player), key, args);
    }

    public static MutableComponent tr(CommandSourceStack source, String key, Object... args) {
        ServerPlayer player = source.getPlayer();
        String locale = player != null ? PlatformHooks.getPlayerLanguage(player) : "en_us";
        return tr(locale, key, args);
    }

    public static void sendSuccess(CommandSourceStack source, Style style, String key, Object... args) {
        Component msg = tr(source, key, args).withStyle(style);
        source.sendSuccess(() -> msg, false);
    }

    public static void sendFailure(CommandSourceStack source, String key, Object... args) {
        Component msg = tr(source, key, args).withStyle(STYLE_ERROR);
        source.sendFailure(msg);
    }

    public static void sendToPlayer(ServerPlayer player, Style style, String key, Object... args) {
        player.sendSystemMessage(tr(player, key, args).withStyle(style));
    }

    public static void broadcast(MinecraftServer server, Style style, String key, Object... args) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(tr(player, key, args).withStyle(style));
        }
        server.sendSystemMessage(tr("en_us", key, args));
    }
}
