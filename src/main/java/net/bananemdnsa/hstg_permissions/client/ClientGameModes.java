package net.bananemdnsa.hstg_permissions.client;

import java.util.Collection;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

/** The game modes the server says this player may switch to; unknown means the server does not run the addon. */
public final class ClientGameModes {

    @Nullable
    private static volatile Set<String> allowed;

    private ClientGameModes() {}

    public static void set(Collection<String> modes) {
        allowed = Set.copyOf(modes);
    }

    public static void reset() {
        allowed = null;
    }

    public static boolean known() {
        return allowed != null;
    }

    public static boolean allows(String mode) {
        Set<String> current = allowed;
        return current != null && current.contains(mode);
    }

    public static boolean anyAllowed() {
        Set<String> current = allowed;
        return current != null && !current.isEmpty();
    }
}
