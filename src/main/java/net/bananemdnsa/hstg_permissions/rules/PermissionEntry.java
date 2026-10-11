package net.bananemdnsa.hstg_permissions.rules;

import java.util.Optional;

/** One row of {@code hstg_permissions:commands} as it sits in the stage file. */
public record PermissionEntry(String command, String mode) {

    public Optional<EntryPattern> pattern() {
        return EntryPattern.parse(command);
    }

    public Optional<Mode> parsedMode() {
        return Mode.parse(mode);
    }
}
