package net.bananemdnsa.hstg_permissions.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import net.bananemdnsa.hstg_permissions.rules.CommandTarget;
import net.bananemdnsa.hstg_permissions.rules.EntryPattern;

/**
 * What a command tree looks like in entry terms: the path of every node, which top-level commands
 * are aliases of each other, and which paths exist at all. Plain Brigadier, so it is testable
 * without the game.
 */
public final class CommandTree<S> {

    private final RootCommandNode<S> root;
    private final Map<String, String> rootMods = new HashMap<>();
    private final Map<String, Set<String>> aliasGroups = new HashMap<>();
    private final Map<CommandNode<S>, List<List<String>>> paths = new IdentityHashMap<>();
    private final Map<CommandNode<S>, List<CommandNode<S>>> chains = new IdentityHashMap<>();
    private final List<CommandNode<S>> order = new ArrayList<>();
    private final Set<List<String>> addressable = new HashSet<>();
    private final Set<List<String>> openValues = new HashSet<>();
    private final List<String> unaddressable = new ArrayList<>();
    private final Map<List<String>, Boolean> listed = new LinkedHashMap<>();
    private final Map<String, String> aliasTargets = new HashMap<>();

    private CommandTree(RootCommandNode<S> root) {
        this.root = root;
    }

    public static <S> CommandTree<S> build(RootCommandNode<S> root, Function<CommandNode<S>, String> modOfRoot,
                                           Function<ArgumentType<?>, Collection<String>> knownValues) {
        CommandTree<S> tree = new CommandTree<>(root);
        for (CommandNode<S> child : root.getChildren()) {
            if (child instanceof LiteralCommandNode<S>) {
                tree.rootMods.put(child.getName().toLowerCase(Locale.ROOT), modOfRoot.apply(child));
            }
        }
        tree.groupAliases();
        tree.walk(root, List.of(), List.of(), knownValues);
        return tree;
    }

    // Only a bare top-level redirect onto another top-level command is an alias (tp, tell, w, xp).
    // execute run also redirects, but to the root, and that starts a new command instead.
    private void groupAliases() {
        for (CommandNode<S> child : root.getChildren()) {
            CommandNode<S> target = child.getRedirect();
            if (!(child instanceof LiteralCommandNode<S>) || target == null || target == root) continue;
            if (!child.getChildren().isEmpty() || root.getChild(target.getName()) != target) continue;
            String alias = child.getName().toLowerCase(Locale.ROOT);
            String main = target.getName().toLowerCase(Locale.ROOT);
            aliasTargets.put(alias, main);
            Set<String> group = new LinkedHashSet<>();
            group.addAll(aliasGroups.getOrDefault(main, Set.of(main)));
            group.addAll(aliasGroups.getOrDefault(alias, Set.of(alias)));
            for (String name : group) aliasGroups.put(name, group);
        }
    }

    private void walk(CommandNode<S> parent, List<String> prefix, List<CommandNode<S>> chain,
                      Function<ArgumentType<?>, Collection<String>> knownValues) {
        for (CommandNode<S> child : parent.getChildren()) {
            if (chain.contains(child)) continue;
            List<CommandNode<S>> childChain = append(chain, child);
            List<String> path;
            if (child instanceof LiteralCommandNode<S>) {
                String name = child.getName().toLowerCase(Locale.ROOT);
                path = append(prefix, name);
                if (name.contains(".")) unaddressable.add(String.join(" ", path));
                if (!hasPlaceholder(path)) {
                    addressable.addAll(spellingsOf(path));
                    // An entry cannot name a word with a dot in it, so the editor must not offer one.
                    if (!hasDot(path)) listed.putIfAbsent(path, false);
                }
            } else {
                if (!prefix.isEmpty() && !hasPlaceholder(prefix) && child instanceof ArgumentCommandNode<S, ?> argument) {
                    Collection<String> values = knownValues.apply(argument.getType());
                    if (values.isEmpty()) {
                        openValues.addAll(spellingsOf(prefix));
                    } else {
                        for (String value : values) {
                            List<String> valuePath = append(prefix, value.toLowerCase(Locale.ROOT));
                            addressable.addAll(spellingsOf(valuePath));
                            if (!hasDot(valuePath)) listed.putIfAbsent(valuePath, true);
                        }
                    }
                }
                path = append(prefix, "<" + child.getName() + ">");
            }
            if (!paths.containsKey(child)) {
                order.add(child);
                chains.put(child, childChain);
            }
            paths.computeIfAbsent(child, node -> new ArrayList<>()).add(path);
            walk(child, path, childChain, knownValues);
        }
    }

    public RootCommandNode<S> root() {
        return root;
    }

    public List<CommandNode<S>> nodes() {
        return List.copyOf(order);
    }

    public List<CommandNode<S>> literals() {
        return order.stream().filter(node -> node instanceof LiteralCommandNode<S>).toList();
    }

    public int literalCount() {
        return literals().size();
    }

    public List<CommandNode<S>> chainTo(CommandNode<S> node) {
        return chains.getOrDefault(node, List.of(node));
    }

    public Optional<List<String>> firstPath(CommandNode<S> node) {
        List<List<String>> known = paths.get(node);
        return known == null ? Optional.empty() : Optional.of(known.get(0));
    }

    public CommandTarget targetOf(CommandNode<S> node) {
        List<List<String>> known = paths.get(node);
        if (known == null) throw new IllegalArgumentException("Node /" + node.getName() + " is not part of this tree.");
        Set<List<String>> spellings = new LinkedHashSet<>();
        for (List<String> path : known) spellings.addAll(spellingsOf(path));
        return new CommandTarget(modOf(known.get(0).get(0)), List.copyOf(spellings));
    }

    public CommandTarget targetFor(List<String> path) {
        List<String> lower = path.stream().map(word -> word.toLowerCase(Locale.ROOT)).toList();
        return new CommandTarget(modOf(lower.get(0)), List.copyOf(spellingsOf(lower)));
    }

    /**
     * The commands in a typed line, read word by word without asking anyone's permission. A
     * redirect to the root ({@code execute … run}) starts the next command.
     */
    public List<CommandTarget> targetsForInput(String input) {
        String text = input.startsWith("/") ? input.substring(1) : input;
        List<CommandTarget> targets = new ArrayList<>();
        List<String> path = new ArrayList<>();
        CommandNode<S> node = root;
        for (String word : text.trim().split("\\s+")) {
            if (word.isEmpty()) break;
            if (node.getRedirect() != null && node.getChildren().isEmpty()) {
                if (node.getRedirect() == root && !path.isEmpty()) {
                    targets.add(targetFor(path));
                    path = new ArrayList<>();
                }
                node = node.getRedirect();
            }
            CommandNode<S> literal = node.getChild(word);
            if (literal instanceof LiteralCommandNode<S>) {
                path.add(word);
                node = literal;
                continue;
            }
            CommandNode<S> argument = firstArgument(node);
            if (argument == null || path.isEmpty()) break;
            path.add(word);
            node = argument;
        }
        if (!path.isEmpty()) targets.add(targetFor(path));
        return targets;
    }

    public boolean exists(EntryPattern pattern) {
        return switch (pattern.kind()) {
            case ALL -> true;
            case MOD -> rootMods.containsValue(pattern.mod());
            case PATH -> addressable.contains(pattern.segments())
                    || (pattern.segments().size() > 1
                        && openValues.contains(pattern.segments().subList(0, pattern.segments().size() - 1)));
        };
    }

    /** Every path an entry can name, in tree order, mapped to whether its last word is a typed value. */
    public Map<List<String>, Boolean> listed() {
        return Collections.unmodifiableMap(listed);
    }

    public Map<String, String> rootMods() {
        return Map.copyOf(rootMods);
    }

    /** Bare top-level aliases and the command they stand for, such as tp → teleport. */
    public Map<String, String> aliasTargets() {
        return Map.copyOf(aliasTargets);
    }

    /** Paths that take any typed value after them, so every value below them exists. */
    public Set<List<String>> openValues() {
        return Set.copyOf(openValues);
    }

    public Set<String> mods() {
        return new TreeSet<>(rootMods.values());
    }

    public List<String> unaddressable() {
        return List.copyOf(unaddressable);
    }

    private String modOf(String rootName) {
        return rootMods.getOrDefault(rootName, "unknown");
    }

    private Set<List<String>> spellingsOf(List<String> path) {
        Set<List<String>> spellings = new LinkedHashSet<>();
        spellings.add(path);
        for (String alias : aliasGroups.getOrDefault(path.get(0), Set.of())) {
            spellings.add(replaceFirst(path, alias));
        }
        return spellings;
    }

    private CommandNode<S> firstArgument(CommandNode<S> node) {
        for (CommandNode<S> child : node.getChildren()) {
            if (child instanceof ArgumentCommandNode<S, ?>) return child;
        }
        return null;
    }

    private static boolean hasDot(List<String> path) {
        for (String word : path) {
            if (word.contains(".")) return true;
        }
        return false;
    }

    private static boolean hasPlaceholder(List<String> path) {
        for (String word : path) {
            if (word.startsWith("<")) return true;
        }
        return false;
    }

    private static <T> List<T> append(List<T> list, T item) {
        List<T> copy = new ArrayList<>(list.size() + 1);
        copy.addAll(list);
        copy.add(item);
        return List.copyOf(copy);
    }

    private static List<String> replaceFirst(List<String> path, String first) {
        List<String> copy = new ArrayList<>(path);
        copy.set(0, first);
        return List.copyOf(copy);
    }
}
