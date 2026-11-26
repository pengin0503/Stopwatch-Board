# Stopwatch Board

A server-side NeoForge mod that turns any challenge into a timed board: personal bests, split times, and a server-wide leaderboard for every course or contest you define.

Vanilla Minecraft 1.21.11 added a `/stopwatch` command, but it is operator-only, shared across the whole server, and forgets everything. Stopwatch Board gives every player their own real-time timer — no client mod required.

- **Boards for anything**: create a named board for a parkour course, an elytra race, a boss kill, a mining contest — the board is just a name, the timing machinery is the same
- **Splits with PB comparison**: every split shows how you are doing against your own personal best, live
- **Personal bests and leaderboards**: PBs (with their splits) are saved per board, and `/swb top` shows the server ranking
- **Anyone can run**: players start their own runs with `/swb start` — no operator permission needed, unlike the vanilla stopwatch
- **Real time, millisecond precision**: timing does not depend on ticks, so server lag does not change results
- **Server-side only**: vanilla clients can join; nothing to install for players
- **Minecraft 1.21.11 and 26.x** on NeoForge, with English and Japanese language files

## Status

Design phase. No downloads yet.

## How it differs from existing mods

Client stopwatch and HUD timer mods are personal displays that every player has to install, and they keep no server records. Speedrun timers measure a whole game run, not per-course times. Parkour timer plugins with leaderboards exist for Paper servers, but not for NeoForge mod servers. Stopwatch Board brings per-player splits, personal bests, and per-board leaderboards to NeoForge, for vanilla clients.
