package net.bananemdnsa.hstg_permissions.gate;

import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.bananemdnsa.hstg_permissions.rules.CommandTarget;
import net.bananemdnsa.hstg_permissions.tree.CommandTree;
import net.minecraft.commands.CommandSourceStack;

/** One command inside a parsed line; {@code execute … run} makes a line hold several. */
record CommandStep(CommandTarget target, List<CommandNode<CommandSourceStack>> nodes) {

    boolean vanillaAllows(CommandSourceStack source) {
        for (CommandNode<CommandSourceStack> node : nodes) {
            if (!CommandGuard.originalOf(node).test(source)) return false;
        }
        return true;
    }

    static List<CommandStep> of(ParseResults<CommandSourceStack> parse, CommandTree<CommandSourceStack> tree) {
        String input = parse.getReader().getString();
        List<CommandStep> steps = new ArrayList<>();
        for (CommandContextBuilder<CommandSourceStack> context = parse.getContext(); context != null;
             context = context.getChild()) {
            // A redirect to the root starts a new command; any other redirect (tp -> teleport)
            // starts again from the redirect target's own path.
            CommandNode<CommandSourceStack> start = context.getRootNode();
            List<String> path = new ArrayList<>(start == tree.root() ? List.of() : tree.firstPath(start).orElse(List.of()));
            List<CommandNode<CommandSourceStack>> nodes = new ArrayList<>();
            for (ParsedCommandNode<CommandSourceStack> parsed : context.getNodes()) {
                CommandNode<CommandSourceStack> node = parsed.getNode();
                nodes.add(node);
                path.add(node instanceof LiteralCommandNode<?> ? node.getName() : parsed.getRange().get(input));
            }
            if (!path.isEmpty()) steps.add(new CommandStep(tree.targetFor(path), nodes));
        }
        return steps;
    }
}
