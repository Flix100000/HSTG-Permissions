package net.bananemdnsa.hstg_permissions.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import net.bananemdnsa.hstg_permissions.catalog.CommandCatalog;
import net.bananemdnsa.hstg_permissions.rules.EntryPattern;

/**
 * Turns what someone typed into an entry. The mod name may be left out; when the server knows the
 * command, it is put in front. Anything the server does not know stays exactly as typed, so a path
 * for a mod that is missing right now can still be entered.
 */
public final class TypedPath {

    private TypedPath() {}

    public static String resolve(String raw, CommandCatalog catalog) {
        String text = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        // The stage file names a command without a trailing ".*"; only a whole mod keeps it.
        if (text.length() > 2 && text.endsWith(".*")) {
            String stripped = text.substring(0, text.length() - 2);
            if (!stripped.contains(".") && catalog.mods().contains(stripped)) return text;
            String resolved = resolvePlain(stripped, catalog);
            return names(resolved, catalog) ? resolved : text;
        }
        return resolvePlain(text, catalog);
    }

    /**
     * Whether a search should also be offered as an entry to add. A plain word is a search; a path
     * (with a dot) or a star is something to add. An "@" is the mod filter, never an entry.
     */
    public static boolean looksLikeEntry(String query) {
        String text = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (text.isEmpty() || text.startsWith("@")) return false;
        if (!text.contains(".") && !text.contains("*")) return false;
        return EntryPattern.parse(text).isPresent();
    }

    private static boolean names(String entry, CommandCatalog catalog) {
        return EntryPattern.parse(entry)
                .filter(p -> p.kind() == EntryPattern.Kind.PATH)
                .map(p -> catalog.exists(p) && catalog.modOfRoot(p.segments().get(0)).equals(p.mod()))
                .orElse(false);
    }

    private static String resolvePlain(String text, CommandCatalog catalog) {
        Optional<EntryPattern> parsed = EntryPattern.parse(text);
        if (parsed.isEmpty()) return text;
        EntryPattern pattern = parsed.get();
        return switch (pattern.kind()) {
            case ALL -> "*";
            case MOD -> {
                if (text.endsWith(".*")) yield text;
                String word = pattern.mod();
                if (catalog.rootMods().containsKey(word)) yield catalog.modOfRoot(word) + "." + word;
                if (catalog.mods().contains(word)) yield word + ".*";
                yield text;
            }
            case PATH -> {
                if (catalog.exists(pattern) && catalog.modOfRoot(pattern.segments().get(0)).equals(pattern.mod())) {
                    yield text;
                }
                // Read the first word as a command instead of a mod name.
                List<String> whole = new ArrayList<>();
                whole.add(pattern.mod());
                whole.addAll(pattern.segments());
                EntryPattern asCommand = new EntryPattern(EntryPattern.Kind.PATH, "", List.copyOf(whole));
                if (catalog.rootMods().containsKey(pattern.mod()) && catalog.exists(asCommand)) {
                    yield catalog.modOfRoot(pattern.mod()) + "." + String.join(".", whole);
                }
                yield text;
            }
        };
    }
}
