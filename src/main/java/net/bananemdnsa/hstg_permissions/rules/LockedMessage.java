package net.bananemdnsa.hstg_permissions.rules;

import java.util.Optional;

/**
 * The admin's own text for a locked command, with {stage} and {command} filled in. Empty means the
 * translated default instead.
 */
public final class LockedMessage {

    /** Shipped like History Stages' unlock message: a literal in the config, ready to edit. */
    public static final String DEFAULT = "&cThis command is still locked. &7Needs: &e{stage}";

    /** What {stage} reads as when a stage keeps its name to itself. */
    public static final String HIDDEN = "???";

    private LockedMessage() {}

    public static Optional<String> format(String configured, String stages, String command) {
        if (configured == null || configured.isBlank()) return Optional.empty();
        // Colours first: a stage called "R&D" or a typed "&" must stay as written.
        return Optional.of(configured.replace('&', '§').replace("{stage}", stages).replace("{command}", command));
    }
}
