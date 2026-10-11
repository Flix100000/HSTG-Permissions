# HSTG: Permissions

An addon for [History Stages](https://github.com/Flix100000/History-Stages) that locks and grants
commands per stage.

- **Lock** a command until a stage is unlocked, down to a single subcommand: lock
  `minecraft.gamemode.creative` and `/gamemode survival` keeps working.
- **Grant** a command to players who have a stage, even without operator rights. An individual
  stage called "Moderator" can hand out `/kick` and `/tp` and nothing else.
- Locked commands disappear from tab completion right away, without relogging.

Work in progress. Requires History Stages on NeoForge 1.21.1.

## License

All Rights Reserved, see [LICENSE.txt](LICENSE.txt).
