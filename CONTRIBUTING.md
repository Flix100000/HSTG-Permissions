# Contributing

Help is welcome: bug fixes, features, translations, wiki pages, testing.

For anything bigger than a small fix, open an [issue](https://github.com/Flix100000/HSTG-Permissions/issues)
first so we can agree on the approach before you put time into it. Typos and one line fixes can go straight to a
pull request.

## Setup

You need Git, JDK 21 and an IDE (IntelliJ IDEA works well).

```bash
git clone https://github.com/Flix100000/HSTG-Permissions.git
cd HSTG-Permissions
./gradlew build
```

History Stages is pulled in as a released jar, the same way any addon would get it. Until History Stages 6.1.0
is out, the build needs a locally built jar of it instead (`hstg_local_jar` in `gradle.properties`).
`./gradlew runClient` starts a dev client, `./gradlew runGameTestServer` runs the game tests.

There's only one branch, `main`. Branch off it and open your pull request against it.

## A few rules

- The addon only uses the public History Stages API (`net.bananemdnsa.historystages.api.*`). If something is
  missing there, it gets added to History Stages, not worked around here.
- Player facing text goes into the lang files, never straight into the code.
- The permission rule is covered by game tests. If you change how commands are checked, add or adjust a test
  that fails without your change.
- Keep the one rule: an entry means "this command belongs to this stage". See the
  [wiki](https://github.com/Flix100000/HSTG-Permissions/wiki/How-Entries-Work).

## Translations

Translations are welcome as a pull request. Copy `en_us.json` from
`src/main/resources/assets/hstg_permissions/lang/` and translate the values.

## License

HSTG: Permissions is All Rights Reserved (see [LICENSE.txt](LICENSE.txt)). By opening a pull request you agree
that your contribution is licensed to the project owner under those terms.
