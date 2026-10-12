package net.bananemdnsa.hstg_permissions.client;

import org.jetbrains.annotations.Nullable;

import net.bananemdnsa.hstg_permissions.catalog.CommandCatalog;

/**
 * The server's last answer, for the editor.
 *
 * <p>Asking again keeps the last answer on show until the new one arrives, so reopening a stage
 * does not flash "loading". The version lets a picker that is already open notice a new answer.
 */
public final class ClientCatalog {

    public enum State { NONE, LOADING, READY, UNAVAILABLE }

    private static State state = State.NONE;
    @Nullable
    private static CommandCatalog catalog;
    private static long version;

    private ClientCatalog() {}

    public static synchronized void loading() {
        state = State.LOADING;
        version++;
    }

    public static synchronized void received(CommandCatalog answer) {
        catalog = answer;
        state = State.READY;
        version++;
    }

    public static synchronized void unavailable() {
        catalog = null;
        state = State.UNAVAILABLE;
        version++;
    }

    public static synchronized void reset() {
        catalog = null;
        state = State.NONE;
        version++;
    }

    public static synchronized State state() {
        return state;
    }

    @Nullable
    public static synchronized CommandCatalog get() {
        return catalog;
    }

    public static synchronized long version() {
        return version;
    }
}
