package net.bananemdnsa.hstg_permissions.gate;

import java.lang.reflect.Field;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.ArgumentCommandNode;

/** The same reflection as RequirementAccess, for the suggestions of an argument. */
final class SuggestionAccess {

    private static final Field SUGGESTIONS = find();

    private SuggestionAccess() {}

    private static Field find() {
        try {
            Field field = ArgumentCommandNode.class.getDeclaredField("customSuggestions");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new IllegalStateException("Cannot reach ArgumentCommandNode.customSuggestions.", e);
        }
    }

    static <S> void set(ArgumentCommandNode<S, ?> node, SuggestionProvider<S> suggestions) {
        try {
            SUGGESTIONS.set(node, suggestions);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot replace the suggestions of <" + node.getName() + ">", e);
        }
    }
}
