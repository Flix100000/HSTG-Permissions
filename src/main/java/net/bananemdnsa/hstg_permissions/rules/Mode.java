package net.bananemdnsa.hstg_permissions.rules;

import java.util.Locale;
import java.util.Optional;

public enum Mode {
    LOCK,
    GRANT;

    /** How the mode is written in the stage file. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Stage files are edited by hand, so case and stray spaces are forgiven. */
    public static Optional<Mode> parse(String raw) {
        if (raw == null) return Optional.empty();
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "lock" -> Optional.of(LOCK);
            case "grant" -> Optional.of(GRANT);
            default -> Optional.empty();
        };
    }
}
