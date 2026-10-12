package net.bananemdnsa.hstg_permissions.rules;

import java.util.Optional;

/** One row of {@code hstg_permissions:commands} as it sits in the stage file: a command that belongs to the stage. */
public record PermissionEntry(String command) {

    public Optional<EntryPattern> pattern() {
        return EntryPattern.parse(command);
    }
}
