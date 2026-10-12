package net.bananemdnsa.hstg_permissions.editor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import net.bananemdnsa.hstg_permissions.catalog.CommandCatalog;
import net.bananemdnsa.hstg_permissions.rules.EntryPattern;

/**
 * The picker's rows: everything, then one row per mod, then its commands as a tree. Built from the
 * catalog once; asked for the visible rows whenever a branch opens or the search changes.
 */
public final class PickerTree {

    private final CommandCatalog catalog;
    private final Map<List<String>, List<List<String>>> children = new HashMap<>();
    private final Map<String, List<List<String>>> rootsByMod = new LinkedHashMap<>();
    private final Set<List<String>> values = new HashSet<>();

    public PickerTree(CommandCatalog catalog) {
        this.catalog = catalog;
        Map<String, List<List<String>>> roots = new HashMap<>();
        for (CommandCatalog.Node node : catalog.nodes()) {
            List<String> path = node.path();
            if (node.value()) values.add(path);
            if (path.size() == 1) {
                roots.computeIfAbsent(catalog.modOfRoot(path.get(0)), mod -> new ArrayList<>()).add(path);
            } else {
                children.computeIfAbsent(path.subList(0, path.size() - 1), parent -> new ArrayList<>()).add(path);
            }
        }
        // Vanilla first, the rest alphabetically; commands alphabetically within a mod.
        roots.keySet().stream()
                .sorted(Comparator.comparing((String mod) -> !mod.equals("minecraft")).thenComparing(mod -> mod))
                .forEach(mod -> {
                    List<List<String>> list = new ArrayList<>(roots.get(mod));
                    list.sort(Comparator.comparing(path -> path.get(0)));
                    rootsByMod.put(mod, list);
                });
    }

    public List<PickerRow> rows(Set<String> expanded, String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<PickerRow> out = new ArrayList<>();
        if (q.isEmpty() || q.startsWith("@")) {
            String modFilter = q.isEmpty() ? "" : q.substring(1);
            if (modFilter.isEmpty()) out.add(new PickerRow(PickerRow.Kind.ALL, "*", "*", 0, false, false, null, 0, true));
            rootsByMod.forEach((mod, roots) -> {
                if (!mod.contains(modFilter)) return;
                boolean open = expanded.contains(mod + ".*");
                out.add(new PickerRow(PickerRow.Kind.MOD, mod + ".*", mod, 0, true, open, null, roots.size(), true));
                if (open) for (List<String> root : roots) addBranch(out, mod, root, 1, expanded, false, path -> true);
            });
            return out;
        }

        if (EntryPattern.parse(q).isPresent()) {
            String resolved = TypedPath.resolve(q, catalog);
            boolean found = EntryPattern.parse(resolved).map(catalog::exists).orElse(false);
            // A bare unknown word is just a search, not an entry worth adding.
            if (found || q.contains(".") || q.contains("*")) {
                out.add(new PickerRow(PickerRow.Kind.TYPED, resolved, resolved, 0, false, false, null, 0, found));
            }
        }
        // A match shows with every row above it, all open, so it is clear where it lives.
        Set<List<String>> shown = new HashSet<>();
        for (CommandCatalog.Node node : catalog.nodes()) {
            if (!String.join(".", node.path()).contains(q)) continue;
            for (int size = 1; size <= node.path().size(); size++) shown.add(node.path().subList(0, size));
        }
        rootsByMod.forEach((mod, roots) -> {
            List<List<String>> hits = roots.stream().filter(shown::contains).toList();
            if (hits.isEmpty()) return;
            out.add(new PickerRow(PickerRow.Kind.MOD, mod + ".*", mod, 0, true, true, null, roots.size(), true));
            for (List<String> root : hits) addBranch(out, mod, root, 1, expanded, true, shown::contains);
        });
        return out;
    }

    private void addBranch(List<PickerRow> out, String mod, List<String> path, int depth, Set<String> expanded,
                           boolean forceOpen, Predicate<List<String>> keep) {
        List<List<String>> kids = children.getOrDefault(path, List.of());
        String entry = mod + "." + String.join(".", path);
        boolean open = !kids.isEmpty() && (forceOpen || expanded.contains(entry));
        PickerRow.Kind kind = values.contains(path) ? PickerRow.Kind.VALUE : PickerRow.Kind.COMMAND;
        String aliasOf = path.size() == 1 ? catalog.aliasTargets().get(path.get(0)) : null;
        out.add(new PickerRow(kind, entry, path.get(path.size() - 1), depth, !kids.isEmpty(), open, aliasOf, 0, true));
        if (!open) return;
        for (List<String> kid : kids) {
            if (keep.test(kid)) addBranch(out, mod, kid, depth + 1, expanded, forceOpen, keep);
        }
    }
}
