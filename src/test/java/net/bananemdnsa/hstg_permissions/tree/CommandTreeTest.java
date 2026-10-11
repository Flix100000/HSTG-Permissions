package net.bananemdnsa.hstg_permissions.tree;

import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;
import static com.mojang.brigadier.builder.RequiredArgumentBuilder.argument;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.bananemdnsa.hstg_permissions.rules.CommandTarget;
import net.bananemdnsa.hstg_permissions.rules.EntryPattern;
import org.junit.jupiter.api.Test;

class CommandTreeTest {

    private static final StringArgumentType GAMEMODE = StringArgumentType.word();

    private final CommandDispatcher<Object> dispatcher = new CommandDispatcher<>();
    private final CommandTree<Object> tree;

    CommandTreeTest() {
        dispatcher.register(literal("gamemode").requires(source -> false)
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
        dispatcher.register(literal("quests").executes(c -> 1));
        tree = CommandTree.build(dispatcher.getRoot(),
                node -> node.getName().equals("quests") ? "ftbquests" : "minecraft",
                CommandTreeTest::knownValues);
    }

    private static Collection<String> knownValues(ArgumentType<?> type) {
        return type == GAMEMODE ? List.of("survival", "creative", "adventure", "spectator") : List.of();
    }

    private CommandNode<Object> node(String... path) {
        CommandNode<Object> node = dispatcher.getRoot();
        for (String word : path) node = node.getChild(word);
        return node;
    }

    private static boolean matches(String pattern, CommandTarget target) {
        return EntryPattern.parse(pattern).orElseThrow().matches(target);
    }

    @Test
    void aLiteralKnowsItsPathAndMod() {
        CommandTarget day = tree.targetOf(node("time", "set", "day"));
        assertEquals("minecraft", day.mod());
        assertEquals(List.of("time", "set", "day"), day.spellings().get(0));
        assertEquals("ftbquests", tree.targetOf(node("quests")).mod());
    }

    @Test
    void anAliasAndItsTargetShareTheirSpellings() {
        CommandTarget tp = tree.targetOf(node("tp"));
        assertTrue(matches("minecraft.teleport", tp));
        CommandTarget teleport = tree.targetOf(node("teleport"));
        assertTrue(matches("minecraft.tp", teleport));
        assertTrue(matches("minecraft.tp", tree.targetFor(List.of("teleport", "steve"))));
    }

    @Test
    void executeRunIsNotAnAliasOfEverything() {
        assertFalse(matches("minecraft.execute", tree.targetOf(node("time"))));
    }

    @Test
    void theChainRunsFromTheTopWordDown() {
        assertEquals(List.of(node("time"), node("time", "set"), node("time", "set", "day")),
                tree.chainTo(node("time", "set", "day")));
    }

    @Test
    void typedInputBecomesTargets() {
        List<CommandTarget> gamemode = tree.targetsForInput("/gamemode Creative steve");
        assertEquals(1, gamemode.size());
        assertEquals(List.of("gamemode", "creative", "steve"), gamemode.get(0).spellings().get(0));

        assertTrue(matches("minecraft.teleport", tree.targetsForInput("tp steve").get(0)));

        List<CommandTarget> chained = tree.targetsForInput("execute run time set day");
        assertEquals(2, chained.size());
        assertEquals(List.of("execute", "run"), chained.get(0).spellings().get(0));
        assertEquals(List.of("time", "set", "day"), chained.get(1).spellings().get(0));

        assertTrue(tree.targetsForInput("nosuchcommand").isEmpty());
    }

    @Test
    void existsKnowsWordsValuesAndMods() {
        assertTrue(tree.exists(EntryPattern.parse("*").orElseThrow()));
        assertTrue(tree.exists(EntryPattern.parse("minecraft.*").orElseThrow()));
        assertTrue(tree.exists(EntryPattern.parse("ftbquests.*").orElseThrow()));
        assertFalse(tree.exists(EntryPattern.parse("kubejs.*").orElseThrow()));
        assertTrue(tree.exists(EntryPattern.parse("minecraft.time.set.day").orElseThrow()));
        assertFalse(tree.exists(EntryPattern.parse("minecraft.time.set.night").orElseThrow()));
        assertTrue(tree.exists(EntryPattern.parse("minecraft.gamemode.creative").orElseThrow()));
        assertFalse(tree.exists(EntryPattern.parse("minecraft.gamemode.creatve").orElseThrow()));
        // No known values for a free word, so any value counts as existing.
        assertTrue(tree.exists(EntryPattern.parse("minecraft.teleport.steve").orElseThrow()));
        assertTrue(tree.exists(EntryPattern.parse("minecraft.tp").orElseThrow()));
        // The mod name does not count for a concrete path.
        assertTrue(tree.exists(EntryPattern.parse("wrongmod.time").orElseThrow()));
    }

    @Test
    void namesWithADotCannotBeAddressed() {
        assertEquals(List.of("weird.name"), tree.unaddressable());
    }

    @Test
    void everyLiteralIsCountedOnce() {
        // gamemode, teleport, tp, time, set, day, query, execute, run, weird.name, quests
        assertEquals(11, tree.literalCount());
        assertEquals(11, tree.literals().size());
    }
}
