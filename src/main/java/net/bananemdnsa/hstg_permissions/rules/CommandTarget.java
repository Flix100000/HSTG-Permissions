package net.bananemdnsa.hstg_permissions.rules;

import java.util.List;
import java.util.Locale;

/**
 * The command being checked: which mod it belongs to and every way of writing it. An alias such
 * as {@code tp} for {@code teleport} gives one target two spellings.
 */
public record CommandTarget(String mod, List<List<String>> spellings) {

    public CommandTarget {
        mod = mod == null ? "" : mod.toLowerCase(Locale.ROOT);
        if (spellings.isEmpty()) throw new IllegalArgumentException("A command target needs a spelling.");
        spellings = spellings.stream()
                .map(spelling -> spelling.stream().map(word -> word.toLowerCase(Locale.ROOT)).toList())
                .toList();
        for (List<String> spelling : spellings) {
            if (spelling.isEmpty()) throw new IllegalArgumentException("A command target needs at least one word.");
        }
    }

    public static CommandTarget of(String mod, String... path) {
        return new CommandTarget(mod, List.of(List.of(path)));
    }

    /** Stable cache key; the first spelling is the one the tree found first. */
    public String key() {
        return mod + ":" + String.join(".", spellings.get(0));
    }
}
