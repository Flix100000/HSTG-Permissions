package net.bananemdnsa.hstg_permissions.gate;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.bananemdnsa.hstg_permissions.HSTGPermissions;
import net.bananemdnsa.hstg_permissions.tree.CommandTree;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Lays the guard around every literal of a freshly built dispatcher: at startup and on every /reload. */
public final class CommandGuard {

    private CommandGuard() {}

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        install(event.getDispatcher());
    }

    /** Safe to run twice: an already guarded node is wrapped again from its original. */
    public static CommandTree<CommandSourceStack> install(CommandDispatcher<CommandSourceStack> dispatcher) {
        CommandTree<CommandSourceStack> tree =
                CommandTree.build(dispatcher.getRoot(), ModAttribution::modOf, KnownValues::of);
        Map<CommandNode<CommandSourceStack>, Predicate<CommandSourceStack>> originals = new IdentityHashMap<>();
        for (CommandNode<CommandSourceStack> node : tree.nodes()) originals.put(node, originalOf(node));

        for (CommandNode<CommandSourceStack> literal : tree.literals()) {
            List<Predicate<CommandSourceStack>> chain = new ArrayList<>();
            for (CommandNode<CommandSourceStack> step : tree.chainTo(literal)) chain.add(originals.get(step));
            RequirementAccess.set(literal, new GuardedRequirement(originals.get(literal), tree.targetOf(literal), chain));
        }
        HSTGPermissions.LOGGER.info("Guarding {} command words", tree.literalCount());
        return tree;
    }

    public static Predicate<CommandSourceStack> originalOf(CommandNode<CommandSourceStack> node) {
        Predicate<CommandSourceStack> requirement = node.getRequirement();
        return requirement instanceof GuardedRequirement guarded ? guarded.original() : requirement;
    }
}
