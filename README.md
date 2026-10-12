# HSTG: Permissions

**HSTG: Permissions** is an addon for [History Stages](https://github.com/Flix100000/History-Stages) that
ties commands to stages. Put `/gamemode creative` into a stage, and only players who have that stage can
use it. Operators included. Put `/kick` and `/tp` into a "Moderator" stage, and whoever gets that stage
can use them without being an operator.

It started on a small server running LuckPerms, where all we wanted was to lock creative mode. That took
far more nodes than it should have. Here an entry is written the way you type the command
(`minecraft.gamemode.creative`), and it covers everything after it.

## What it does

- **One rule.** A command entry belongs to its stage. Whoever has the stage may use the command, operator
  or not. Everyone else may not, operators included. For a global stage that means once the world has
  it. A command that no stage names follows Minecraft's own rules.
- **Down to a subcommand.** `minecraft.gamemode.creative` leaves `/gamemode survival` alone,
  `minecraft.*` covers all vanilla commands, `ftbquests.*` everything from FTB Quests.
- **An everything stage.** A stage with `*` gives its holders every command and takes nothing from anyone
  else. Handy for an admin role.
- **Safe by default.** Commands on the protected list (`op`, `stop`, `execute` and so on, editable in
  the config) can only be taken away, never handed out. The commands of History Stages and this addon can
  never be taken away, and the console, command blocks and functions are never checked.
- **Tab completion that fits.** Players only see suggestions for commands they may use, and `/gamemode`
  only suggests the game modes they may switch to. F3+F4 and F3+N grey out and skip the modes they may
  not use.
- **Ranks.** Individual stages can carry a prefix and suffix, like `[Admin]`, shown in chat, the tab list
  and on name tags.
- **Built into the editor.** Every stage gets a Commands tab with a searchable tree of all commands on the
  server, grouped by mod. Typos and missing mods show up in yellow.
- **Debugging.** `/hstgperms check <player> <command>` tells you whether a player may run something and
  which stage decides. `/hstgperms verbose` shows every command players run, live.
- **Instant.** Unlocking or losing a stage changes what a player may use right away, no relogging.

## Supported versions

| Minecraft | Loader   | Status                 |
| --------- | -------- | ---------------------- |
| 1.21.1    | NeoForge | In development         |

Requires History Stages 6.1.0 or newer. The addon goes on the server. On the client it is optional, but
you need it there for the editor tab, the F3+F4 menu and ranks on name tags.

## Documentation

How entries are written, the editor, ranks, commands, config and known limits are all in the
[Wiki](https://github.com/Flix100000/HSTG-Permissions/wiki).

## Community

Questions or support: join the [History Stages Discord](https://discord.gg/BeZzxyZ9c4).

## Issues & bug reports

Found a bug or have an idea? Open an issue on the
[Issue Tracker](https://github.com/Flix100000/HSTG-Permissions/issues).

## Authors

- Flix100000 (Developer)

## License

**All Rights Reserved**: the source is public, but the addon is not open source.

Quick rules:
- Use in modpacks (public, private, commercial): **allowed**
- Use in Let's Plays, streams, servers: **allowed**
- Fork to prepare a PR: **allowed**
- Port to another Minecraft version or mod loader: **allowed under conditions** (see Section 3 of the license)
- Publish forks / modified versions / re-uploads: **not allowed**

Full terms: [LICENSE.txt](LICENSE.txt).
