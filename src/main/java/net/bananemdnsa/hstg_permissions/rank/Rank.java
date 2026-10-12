package net.bananemdnsa.hstg_permissions.rank;

/** What goes before and after a player's name. The space between is added when shown, so the ends are trimmed. */
public record Rank(String prefix, String suffix) {

    public static final Rank NONE = new Rank("", "");

    public Rank {
        prefix = prefix == null ? "" : prefix.strip();
        suffix = suffix == null ? "" : suffix.strip();
    }

    public boolean isEmpty() {
        return prefix.isEmpty() && suffix.isEmpty();
    }
}
