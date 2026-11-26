package com.pengin0503.stopwatchboard;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server configuration for Stopwatch Board (NeoForge ModConfigSpec, SERVER type).
 */
public final class StopwatchBoardConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ACTION_BAR_LIVE;
    public static final ModConfigSpec.BooleanValue ANNOUNCE_SERVER_RECORD;
    public static final ModConfigSpec.BooleanValue CANCEL_ON_DEATH;
    public static final ModConfigSpec.IntValue MAX_SPLITS_PER_RUN;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        ACTION_BAR_LIVE = builder
                .comment("Send live elapsed time updates to the player's action bar every 10 ticks while a run is active.")
                .define("actionBarLive", true);

        ANNOUNCE_SERVER_RECORD = builder
                .comment("Broadcast a server-wide announcement when a player beats another player's #1 record on a board.")
                .define("announceServerRecord", true);

        CANCEL_ON_DEATH = builder
                .comment("Cancel a player's active run if they die. Default is false (run continues on death).")
                .define("cancelOnDeath", false);

        MAX_SPLITS_PER_RUN = builder
                .comment("Maximum number of splits allowed in a single run (1..1000).")
                .defineInRange("maxSplitsPerRun", 100, 1, 1000);

        SPEC = builder.build();
    }

    private StopwatchBoardConfig() {
    }

    public static boolean actionBarLive() {
        return ACTION_BAR_LIVE.get();
    }

    public static boolean announceServerRecord() {
        return ANNOUNCE_SERVER_RECORD.get();
    }

    public static boolean cancelOnDeath() {
        return CANCEL_ON_DEATH.get();
    }

    public static int maxSplitsPerRun() {
        return MAX_SPLITS_PER_RUN.get();
    }
}
