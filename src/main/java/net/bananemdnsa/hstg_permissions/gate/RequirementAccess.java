package net.bananemdnsa.hstg_permissions.gate;

import java.lang.reflect.Field;
import java.util.function.Predicate;

import com.mojang.brigadier.tree.CommandNode;

/**
 * Brigadier sits on the plain classpath, outside what Mixin can transform, so the final
 * {@code requirement} field is set by reflection instead of through an accessor.
 */
final class RequirementAccess {

    private static final Field REQUIREMENT = find();

    private RequirementAccess() {}

    private static Field find() {
        try {
            Field field = CommandNode.class.getDeclaredField("requirement");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new IllegalStateException("Cannot reach CommandNode.requirement, so no command can be guarded.", e);
        }
    }

    static <S> void set(CommandNode<S> node, Predicate<S> requirement) {
        try {
            REQUIREMENT.set(node, requirement);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot replace the requirement of /" + node.getName(), e);
        }
    }
}
