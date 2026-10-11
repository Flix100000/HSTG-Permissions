package net.bananemdnsa.hstg_permissions.catalog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import net.bananemdnsa.hstg_permissions.rules.EntryPattern;
import net.bananemdnsa.hstg_permissions.rules.EntryProblems;
import net.bananemdnsa.hstg_permissions.rules.PermissionEntry;
import net.bananemdnsa.hstg_permissions.rules.ProtectionRules;
import net.bananemdnsa.hstg_permissions.tree.CommandTree;

/**
 * What the editor knows about the server's commands: the paths it may offer, the mod of every
 * top-level command, and the protection rules. Built from the server's tree and sent to the
 * client, so the editor judges an entry by the same rules as the debug report.
 */
public final class CommandCatalog {

    public record Node(List<String> path, boolean value) {
        public Node {
            path = List.copyOf(path);
        }
    }

    private final List<Node> nodes;
    private final Map<String, String> rootMods;
    private final Map<String, String> aliasTargets;
    private final Set<List<String>> openValues;
    private final List<String> protectedPaths;
    private final Set<String> neverLockableMods;
    private final Set<List<String>> addressable = new HashSet<>();
    private final ProtectionRules protection;

    /**
     * @param protectedPaths protected commands without a mod name ({@code op}, {@code whitelist.add}),
     *                       already stripped of one on the server
     */
    public CommandCatalog(List<Node> nodes, Map<String, String> rootMods, Map<String, String> aliasTargets,
                          Set<List<String>> openValues, List<String> protectedPaths, Set<String> neverLockableMods) {
        this.nodes = List.copyOf(nodes);
        this.rootMods = Map.copyOf(rootMods);
        this.aliasTargets = Map.copyOf(aliasTargets);
        this.openValues = Set.copyOf(openValues);
        this.protectedPaths = List.copyOf(protectedPaths);
        this.neverLockableMods = Set.copyOf(neverLockableMods);
        Map<String, Set<String>> groups = aliasGroups(aliasTargets);
        for (Node node : nodes) addressable.addAll(spellings(node.path(), groups));
        this.protection = ProtectionRules.of(protectedPaths, neverLockableMods, Set.of());
    }

    public static CommandCatalog of(CommandTree<?> tree, List<String> protectedPaths, Set<String> neverLockableMods) {
        List<Node> nodes = new ArrayList<>();
        tree.listed().forEach((path, value) -> nodes.add(new Node(path, value)));
        return new CommandCatalog(nodes, tree.rootMods(), tree.aliasTargets(), tree.openValues(),
                protectedPaths, neverLockableMods);
    }

    public List<Node> nodes() { return nodes; }
    public Map<String, String> rootMods() { return rootMods; }
    public Map<String, String> aliasTargets() { return aliasTargets; }
    public Set<List<String>> openValues() { return openValues; }
    public List<String> protectedPaths() { return protectedPaths; }
    public Set<String> neverLockableMods() { return neverLockableMods; }

    /** The same answer {@link CommandTree#exists} gives on the server. */
    public boolean exists(EntryPattern pattern) {
        return switch (pattern.kind()) {
            case ALL -> true;
            case MOD -> rootMods.containsValue(pattern.mod());
            case PATH -> addressable.contains(pattern.segments())
                    || (pattern.segments().size() > 1
                        && openValues.contains(pattern.segments().subList(0, pattern.segments().size() - 1)));
        };
    }

    public String modOfRoot(String rootName) {
        return rootMods.getOrDefault(rootName, "unknown");
    }

    public SortedSet<String> mods() {
        return new TreeSet<>(rootMods.values());
    }

    public List<EntryProblems.Problem> problems(PermissionEntry entry) {
        return EntryProblems.check(entry, protection, this::exists,
                pattern -> modOfRoot(pattern.segments().get(0)));
    }

    // The same grouping CommandTree does; a union of sets, so the order of the pairs does not matter.
    private static Map<String, Set<String>> aliasGroups(Map<String, String> aliasTargets) {
        Map<String, Set<String>> groups = new HashMap<>();
        aliasTargets.forEach((alias, main) -> {
            Set<String> group = new LinkedHashSet<>();
            group.addAll(groups.getOrDefault(main, Set.of(main)));
            group.addAll(groups.getOrDefault(alias, Set.of(alias)));
            for (String name : group) groups.put(name, group);
        });
        return groups;
    }

    private static Set<List<String>> spellings(List<String> path, Map<String, Set<String>> groups) {
        Set<List<String>> spellings = new LinkedHashSet<>();
        spellings.add(path);
        for (String alias : groups.getOrDefault(path.get(0), Set.of())) {
            List<String> copy = new ArrayList<>(path);
            copy.set(0, alias);
            spellings.add(List.copyOf(copy));
        }
        return spellings;
    }
}
