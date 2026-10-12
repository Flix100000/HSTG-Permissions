package net.bananemdnsa.hstg_permissions.rules;

import java.util.Optional;

/** The admin's own text for a locked command; empty means the translated default. */
public final class LockedMessage {

    private LockedMessage() {}

    public static Optional<String> text(String configured) {
        if (configured == null || configured.isBlank()) return Optional.empty();
        return Optional.of(configured.replace('&', '§'));
    }
}
