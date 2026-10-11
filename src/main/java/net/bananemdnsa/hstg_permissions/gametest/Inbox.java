package net.bananemdnsa.hstg_permissions.gametest;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.commands.CommandSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

/** Collects what a command says back, so a test can check the message and not just the effect. */
final class Inbox implements CommandSource {

    private final List<Component> messages = new ArrayList<>();

    @Override
    public void sendSystemMessage(Component component) {
        messages.add(component);
    }

    @Override
    public boolean acceptsSuccess() {
        return true;
    }

    @Override
    public boolean acceptsFailure() {
        return true;
    }

    @Override
    public boolean shouldInformAdmins() {
        return false;
    }

    boolean mentions(String langKey) {
        for (Component message : messages) {
            if (mentions(message, langKey)) return true;
        }
        return false;
    }

    // sendFailure wraps the message in an empty component, so the key sits one level down.
    private static boolean mentions(Component component, String langKey) {
        if (component.getContents() instanceof TranslatableContents translatable && translatable.getKey().equals(langKey)) {
            return true;
        }
        for (Component sibling : component.getSiblings()) {
            if (mentions(sibling, langKey)) return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return messages.toString();
    }
}
