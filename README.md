# HSTG: Permissions

An addon for [History Stages](https://github.com/Flix100000/History-Stages) that ties commands to
stages.

- A command entry belongs to its stage. Whoever has the stage may use the command, operator or not.
  For a global stage that means once the world has it. Everyone else may not, operators included.
- Entries can go down to a single subcommand: `minecraft.gamemode.creative` leaves
  `/gamemode survival` alone.
- A command that no stage names follows Minecraft's own rules.
- A stage with `*` gives its holders every command and takes nothing from anyone else.
- The commands on the protected list (`op`, `stop` and so on, set in the server config) can only be
  taken away, never handed out.
- The commands of History Stages itself can never be taken away, and the console is never checked.
- Tab completion only suggests what the player may use, and `/gamemode` only suggests the game modes
  they may switch to. F3+F4 and F3+N follow the allowed game modes too, which needs the addon on the
  client as well.
- Changes show up right away, without relogging.

Work in progress. Requires History Stages on NeoForge 1.21.1.

## License

All Rights Reserved, see [LICENSE.txt](LICENSE.txt).
