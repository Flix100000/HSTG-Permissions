package net.bananemdnsa.hstg_permissions.gate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import net.bananemdnsa.historystages.api.stage.StageDefinitionsChangedEvent;
import net.bananemdnsa.historystages.api.stage.StageEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * A stage change has to reach the player's tab completion at once, so the remembered answers go
 * and the command tree is sent again.
 */
public final class StageChangeListener {

    // Unlocking ten stages in one go would otherwise send the whole tree ten times to everyone.
    private static final AtomicBoolean ALL_PENDING = new AtomicBoolean();
    private static final Set<UUID> PLAYERS_PENDING = ConcurrentHashMap.newKeySet();
    private static final AtomicReference<MinecraftServer> SCHEDULED_ON = new AtomicReference<>();

    private StageChangeListener() {}

    public static void register(IEventBus gameBus) {
        gameBus.addListener(StageEvent.Unlocked.class, event -> refreshAll());
        gameBus.addListener(StageEvent.Locked.class, event -> refreshAll());
        // Fired once per teammate for a shared stage, so a team needs nothing extra.
        gameBus.addListener(StageEvent.IndividualUnlocked.class, event -> refresh(event.getPlayerUUID()));
        gameBus.addListener(StageEvent.IndividualLocked.class, event -> refresh(event.getPlayerUUID()));
        gameBus.addListener(StageDefinitionsChangedEvent.class, event -> refreshAll());
        gameBus.addListener(PlayerEvent.PlayerLoggedOutEvent.class,
                event -> PermissionService.forget(event.getEntity().getUUID()));
    }

    public static void refreshAll() {
        PermissionService.invalidate();
        ALL_PENDING.set(true);
        schedule();
    }

    public static void refresh(UUID playerId) {
        PermissionService.forget(playerId);
        PLAYERS_PENDING.add(playerId);
        schedule();
    }

    private static void schedule() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            ALL_PENDING.set(false);
            PLAYERS_PENDING.clear();
            return;
        }
        // Held per server: a single player world closed before the send ran must not block the next one.
        MinecraftServer scheduled = SCHEDULED_ON.get();
        if (scheduled == server || !SCHEDULED_ON.compareAndSet(scheduled, server)) return;
        // tell, not execute: execute runs at once on the server thread, and nothing would add up.
        // In single player the definitions event can come from the client tick, and the server
        // thread may have put a stale answer back in between, so the cache is cleared again there.
        server.tell(new TickTask(server.getTickCount(), () -> send(server)));
    }

    private static void send(MinecraftServer server) {
        // Cleared first: a request arriving while this runs schedules its own send.
        SCHEDULED_ON.compareAndSet(server, null);
        boolean all = ALL_PENDING.getAndSet(false);
        List<UUID> players = new ArrayList<>(PLAYERS_PENDING);
        PLAYERS_PENDING.removeAll(players);

        if (all) {
            PermissionService.invalidate();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                server.getCommands().sendCommands(player);
            }
            return;
        }
        for (UUID playerId : players) {
            PermissionService.forget(playerId);
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) server.getCommands().sendCommands(player);
        }
    }
}
