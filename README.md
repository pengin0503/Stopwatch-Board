# Stopwatch Board

A server-side NeoForge mod that turns any challenge into a timed board: personal bests, split times, and a server-wide leaderboard for every course or contest you define.

Vanilla Minecraft 1.21.11 added a `/stopwatch` command, but it is operator-only (permission level 2), shared globally across the server, and has no splits or saved records. Stopwatch Board implements its own real-time per-player timer — **no client mod required**.

## Features

- **Boards for anything**: Create a named board for a parkour course, an elytra race, a boss kill, or a mining contest. Every board has its own personal bests and derived leaderboard.
- **Per-player real-time runs**: Each player can run one active stopwatch at a time, measured with a monotonic clock (`System.nanoTime`) at millisecond precision — unaffected by server ticks or lag.
- **Live split comparison**: Record splits mid-run with `/swb split` and immediately see your cumulative time and `+/-` delta against your personal best's corresponding split.
- **Personal bests & leaderboards**: PBs (including their split times) are saved per world in `SavedData`. Leaderboards are derived directly from PBs (fastest time first; ties broken by earlier achievement date).
- **Live action bar**: While a run is active, the server sends a live action-bar timer every 10 ticks, visible on vanilla clients.
- **Server record announcements**: Optionally broadcasts a server-wide message when a player beats another player's `#1` record.
- **Server-side only & localized**: Vanilla clients join with nothing to install. Ships with English (`en_us`) and Japanese (`ja_jp`) language files, with server-side locale fallbacks so vanilla clients also see localized messages.
- **Multi-version NeoForge support**:
  - Minecraft `1.21.11` (Java 21)
  - Minecraft `26.1.2`, `26.2`, `26.3` (Java 25, single universal 26.x JAR)

## Commands

Root command: `/swb` (alias: `/stopwatchboard`).

### Player Commands (Permission Level 0, Players Only)

| Command | Description |
| --- | --- |
| `/swb boards` | List all boards with your PB and the current server best for each. |
| `/swb start <board>` | Start a timed run on `<board>`. Errors if you already have an active run. |
| `/swb split` | Record a split on your active run and show delta vs your PB split at the same index. |
| `/swb finish` | Finish your active run, show total time, PB delta, rank, and update PB if faster. |
| `/swb cancel` | Discard your active run. |
| `/swb status` | Show your active run's board, elapsed time, and split count. |
| `/swb top <board> [page]` | Show the board's leaderboard (10 entries per page: rank, player, time, UTC date). |
| `/swb pb <board> [player]` | Show your own or another player's PB (total time + splits) on `<board>`. |

### Admin Commands (Permission Level 2 / OP)

| Command | Description |
| --- | --- |
| `/swb board create <id> [display name...]` | Create a board. `id` must match `[a-z0-9][a-z0-9_-]*` (max 32 characters). |
| `/swb board remove <id>` | Delete a board and all of its PBs (cancels any active runs on it). |
| `/swb board reset <id>` | Delete all PBs on `<id>` while keeping the board definition. |
| `/swb board list` | List all boards with their display names and PB counts. |

## Configuration

Stored in the world's server config (`serverconfig/stopwatchboard-server.toml` on 1.21.11 / 26.1.2 / 26.2, or `serverconfig/stopwatchboard-synced.toml` on 26.3+):

| Option | Type | Default | Description |
| --- | --- | --- | --- |
| `actionBarLive` | boolean | `true` | Send live elapsed time updates to the action bar every 10 ticks during an active run. |
| `announceServerRecord` | boolean | `true` | Broadcast when a player beats another player's `#1` record on a board. |
| `cancelOnDeath` | boolean | `false` | Cancel a player's active run if they die (by default runs continue on death). |
| `maxSplitsPerRun` | integer (`1..1000`) | `100` | Maximum number of splits allowed in a single run. |

## Time Format

- Under 1 hour: `m:ss.mmm` (e.g. `3:07.042`)
- 1 hour or more: `h:mm:ss.mmm` (e.g. `1:02:03.004`)
- Deltas: sign always shown (`+` / `-`), seconds to 3 decimals under 60s (e.g. `+1.234s`, `-0.087s`, `+0.000s`), switching to total format at 60s and above (e.g. `+1:02.345`).

## How It Differs from Existing Timer Mods

- **Vanilla `/stopwatch` (1.21.11+)**: Requires OP (permission level 2), shares IDs globally across the server, and does not support splits, personal bests, or leaderboards.
- **Client HUD stopwatch mods**: Must be installed by every player on the client and only show a local timer with no server-side records or rankings.
- **Speedrun timer mods**: Measure a full game playthrough rather than arbitrary named courses or challenges on a multiplayer server.
- **Paper/Spigot parkour timer plugins**: Not available for NeoForge modded servers.

Stopwatch Board brings per-player splits, personal bests, and per-board leaderboards directly to NeoForge servers while remaining 100% compatible with vanilla clients.

## Building from Source

```bash
# Run pure-Java unit tests
./gradlew :core:test

# Build for Minecraft 1.21.11 (requires JDK 21)
./gradlew :mc-1.21.11:build

# Build universal 26.x JAR for Minecraft 26.1.2 / 26.2 / 26.3 (requires JDK 25)
./gradlew :mc-26.1.2:build

# Run full build + compatibility checks across all versions
./gradlew buildAll
```

## License

Custom License (All Rights Reserved) — see [LICENSE](LICENSE) for the full terms.

Personal use and free modpack inclusion (with credit and a link to an official page) are allowed. Redistribution, re-uploading, selling, and distributing modified versions require the author's permission.
