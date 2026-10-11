package net.bananemdnsa.hstg_permissions.gate;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModInfo;
import org.jetbrains.annotations.Nullable;

/** Minecraft does not record which mod made a command, so it is read off the command's code. */
final class ModAttribution {

    private ModAttribution() {}

    static String modOf(CommandNode<CommandSourceStack> rootChild) {
        for (Class<?> code : codeOf(rootChild)) {
            String mod = modOfClass(code);
            if (mod != null) return mod;
        }
        return "unknown";
    }

    // The requirement lambda is the best clue: mods write their own, Brigadier's default lives in
    // Brigadier. After that, the first command that actually runs.
    private static List<Class<?>> codeOf(CommandNode<CommandSourceStack> rootChild) {
        List<Class<?>> classes = new ArrayList<>();
        classes.add(CommandGuard.originalOf(rootChild).getClass());
        Deque<CommandNode<CommandSourceStack>> queue = new ArrayDeque<>();
        queue.add(rootChild);
        if (rootChild.getRedirect() != null) queue.add(rootChild.getRedirect());
        Set<CommandNode<CommandSourceStack>> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        while (!queue.isEmpty()) {
            CommandNode<CommandSourceStack> node = queue.poll();
            if (!seen.add(node)) continue;
            if (node.getCommand() != null) {
                classes.add(node.getCommand().getClass());
                break;
            }
            queue.addAll(node.getChildren());
        }
        return classes;
    }

    @Nullable
    private static String modOfClass(Class<?> code) {
        String name = code.getName();
        if (name.startsWith("com.mojang.brigadier.")) return null;
        if (name.startsWith("net.minecraft.")) return "minecraft";
        // In a dev run NeoForge shares Minecraft's jar, so the package is the only reliable clue.
        if (name.startsWith("net.neoforged.")) return "neoforge";
        String module = code.getModule().getName();
        if (module == null) return null;
        for (IModInfo mod : ModList.get().getMods()) {
            if (module.equals(mod.getOwningFile().getFile().getSecureJar().name())) return mod.getModId();
        }
        return null;
    }
}
