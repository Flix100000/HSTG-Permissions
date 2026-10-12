package net.bananemdnsa.hstg_permissions.gate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import net.bananemdnsa.hstg_permissions.rules.Decision;
import net.bananemdnsa.hstg_permissions.tree.CommandTree;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;

/**
 * Suggests only the values a player may use. Having any provider at all makes the client ask the
 * server instead of listing every value of the argument type itself.
 */
final class ValueSuggestions implements SuggestionProvider<CommandSourceStack> {

    @Nullable
    private final SuggestionProvider<CommandSourceStack> original;
    private final ArgumentCommandNode<CommandSourceStack, ?> node;
    private final List<String> prefix;
    private final Collection<String> values;
    private final List<Predicate<CommandSourceStack>> vanillaChain;

    ValueSuggestions(@Nullable SuggestionProvider<CommandSourceStack> original, ArgumentCommandNode<CommandSourceStack, ?> node,
                     List<String> prefix, Collection<String> values, List<Predicate<CommandSourceStack>> vanillaChain) {
        this.original = original;
        this.node = node;
        this.prefix = List.copyOf(prefix);
        this.values = List.copyOf(values);
        this.vanillaChain = List.copyOf(vanillaChain);
    }

    @Nullable
    SuggestionProvider<CommandSourceStack> original() {
        return original;
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context,
                                                         SuggestionsBuilder builder) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        CommandTree<CommandSourceStack> tree = PermissionService.tree();
        if (player == null || player instanceof FakePlayer || tree == null) {
            return original != null ? original.getSuggestions(context, builder) : node.getType().listSuggestions(context, builder);
        }
        String typed = builder.getRemaining().toLowerCase(Locale.ROOT);
        for (String value : values) {
            if (!value.startsWith(typed)) continue;
            List<String> path = new ArrayList<>(prefix);
            path.add(value);
            Decision decision = PermissionService.decide(player, tree.targetFor(path));
            boolean allowed = switch (decision.verdict()) {
                case GRANT -> true;
                case LOCK -> false;
                case DEFAULT -> vanillaChain.stream().allMatch(step -> step.test(source));
            };
            if (allowed) builder.suggest(value);
        }
        return builder.buildFuture();
    }
}
