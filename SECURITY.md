# Security Policy

HSTG: Permissions decides who may run which command, so a bug in it can be a way into someone's server.

## What counts as a security issue

Anything a player could abuse on a server they don't run, for example:

- Running a command that a stage should keep from them
- Getting a command from the protected list (`op`, `stop`, ...) through a stage
- Unlocking their own stages without the rights to do so
- Crashing or hanging a server with a crafted packet

Things that only work in your own singleplayer world, normal bugs and compatibility problems are not security
issues. Those go in the [issue tracker](https://github.com/Flix100000/HSTG-Permissions/issues).

Some gaps are known and documented on the [Limits](https://github.com/Flix100000/HSTG-Permissions/wiki/Limits)
page, like command blocks for players who were given `setblock`. If you found a way around one of those that the
page doesn't mention, that does count.

## Reporting

Please don't open a public issue. Every server running the addon is open to it until the fix reaches them.

Report it privately instead:

- [Report a vulnerability](https://github.com/Flix100000/HSTG-Permissions/security/advisories/new) on GitHub
- A direct message to the maintainer on the [Discord server](https://discord.gg/BeZzxyZ9c4)

Please include the addon, History Stages and Minecraft versions, what a player can do with it, and how to
reproduce it. This is a one person project, so there's no fixed response time, but security reports come first.
Once a fix is out, you're welcome to write about it.
