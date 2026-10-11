package net.bananemdnsa.hstg_permissions.tree;

import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;
import static com.mojang.brigadier.builder.RequiredArgumentBuilder.argument;

import java.util.Collection;
import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.bananemdnsa.hstg_permissions.catalog.CommandCatalog;
import net.bananemdnsa.hstg_permissions.rules.ProtectionRules;

/** A small server: vanilla-like commands with an alias, a value, a redirect to the root, and one mod command. */
public final class SampleTree {

    public static final StringArgumentType GAMEMODE = StringArgumentType.word();

    private SampleTree() {}

    public static CommandTree<Object> build() {
        CommandDispatcher<Object> dispatcher = new CommandDispatcher<>();
        dispatcher.register(literal("gamemode")
                .then(argument("gamemode", GAMEMODE).executes(c -> 1)
                        .then(argument("target", StringArgumentType.word()).executes(c -> 1))));
        LiteralCommandNode<Object> teleport = dispatcher.register(literal("teleport")
                .then(argument("targets", StringArgumentType.word()).executes(c -> 1)));
        dispatcher.register(literal("tp").redirect(teleport));
        dispatcher.register(literal("time")
                .then(literal("set").then(literal("day").executes(c -> 1)))
                .then(literal("query").executes(c -> 1)));
        dispatcher.register(literal("execute").then(literal("run").redirect(dispatcher.getRoot())));
        dispatcher.register(literal("weird.name").executes(c -> 1));
        dispatcher.register(literal("quests").then(literal("open_book").executes(c -> 1)));
        return CommandTree.build(dispatcher.getRoot(),
                node -> node.getName().equals("quests") ? "ftbquests" : "minecraft",
                SampleTree::knownValues);
    }

    public static CommandCatalog catalog(List<String> protectedPaths) {
        return CommandCatalog.of(build(), protectedPaths, ProtectionRules.NEVER_LOCKABLE_MODS);
    }

    private static Collection<String> knownValues(ArgumentType<?> type) {
        return type == GAMEMODE ? List.of("survival", "creative", "adventure", "spectator") : List.of();
    }
}
