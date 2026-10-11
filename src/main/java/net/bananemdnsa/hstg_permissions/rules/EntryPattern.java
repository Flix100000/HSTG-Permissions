package net.bananemdnsa.hstg_permissions.rules;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A stored entry, read: {@code *}, a whole mod ({@code ftbquests.*}), or a path
 * ({@code minecraft.gamemode.creative}). A path always covers itself and everything below it.
 */
public record EntryPattern(Kind kind, String mod, List<String> segments) {

    public enum Kind { ALL, MOD, PATH }

    private static final Map<String, Optional<EntryPattern>> PARSED = new ConcurrentHashMap<>();

    public static Optional<EntryPattern> parse(String raw) {
        if (raw == null) return Optional.empty();
        return PARSED.computeIfAbsent(raw, EntryPattern::read);
    }

    private static Optional<EntryPattern> read(String raw) {
        String text = raw.trim().toLowerCase(Locale.ROOT);
        if (text.equals("*")) return Optional.of(new EntryPattern(Kind.ALL, null, List.of()));
        if (text.endsWith(".*")) text = text.substring(0, text.length() - 2);
        if (text.isEmpty()) return Optional.empty();
        List<String> parts = List.of(text.split("\\.", -1));
        for (String part : parts) {
            if (part.isEmpty() || part.contains("*") || part.contains(" ")) return Optional.empty();
        }
        if (parts.size() == 1) return Optional.of(new EntryPattern(Kind.MOD, parts.get(0), List.of()));
        return Optional.of(new EntryPattern(Kind.PATH, parts.get(0), parts.subList(1, parts.size())));
    }

    /** Longer wins. {@code *} is the weakest, a whole mod the next. */
    public int specificity() {
        return switch (kind) {
            case ALL -> 0;
            case MOD -> 1;
            case PATH -> 1 + segments.size();
        };
    }

    public boolean matches(CommandTarget target) {
        return switch (kind) {
            case ALL -> true;
            case MOD -> mod.equals(target.mod());
            // Top-level command names are unique on a server, so a concrete path ignores the mod.
            // A wrong mod guess can then never switch a lock off.
            case PATH -> target.spellings().stream().anyMatch(this::isPrefixOf);
        };
    }

    /** True for a path that names something strictly underneath the target, such as a value of it. */
    public boolean isStrictlyBelow(CommandTarget target) {
        if (kind != Kind.PATH) return false;
        return target.spellings().stream().anyMatch(spelling ->
                segments.size() > spelling.size() && segments.subList(0, spelling.size()).equals(spelling));
    }

    private boolean isPrefixOf(List<String> spelling) {
        return spelling.size() >= segments.size() && spelling.subList(0, segments.size()).equals(segments);
    }
}
