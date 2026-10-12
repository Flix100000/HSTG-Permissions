package net.bananemdnsa.hstg_permissions.gate;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import net.bananemdnsa.hstg_permissions.HSTGPermissions;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * Who asked to see the commands players run. Kept in memory only, so a restart or the watcher
 * leaving ends it and nothing is left switched on by accident.
 */
public final class VerboseWatch {

    public static final UUID CONSOLE = new UUID(0, 0);

    private record Watch(Optional<UUID> only, Consumer<Component> sink) {}

    private static final Map<UUID, Watch> WATCHERS = new ConcurrentHashMap<>();

    private VerboseWatch() {}

    public static void register(IEventBus gameBus) {
        gameBus.addListener(PlayerEvent.PlayerLoggedOutEvent.class, event -> stop(event.getEntity().getUUID()));
        gameBus.addListener(ServerStoppedEvent.class, event -> WATCHERS.clear());
    }

    public static void watch(UUID watcher, Optional<UUID> only, Consumer<Component> sink) {
        WATCHERS.put(watcher, new Watch(only, sink));
    }

    public static void stop(UUID watcher) {
        WATCHERS.remove(watcher);
    }

    public static boolean isWatching(UUID watcher) {
        return WATCHERS.containsKey(watcher);
    }

    public static void onCommand(ServerPlayer player, String input) {
        if (WATCHERS.isEmpty()) return;
        Component line = null;
        for (Map.Entry<UUID, Watch> entry : WATCHERS.entrySet()) {
            Optional<UUID> only = entry.getValue().only();
            if (only.isPresent() && !only.get().equals(player.getUUID())) continue;
            if (line == null) {
                line = Component.translatable("command.hstg_permissions.verbose.prefix")
                        .withStyle(ChatFormatting.GRAY)
                        .append(PermissionCheck.of(player, input).line());
                // Once per command, however many are watching; the console has nothing but this.
                HSTGPermissions.LOGGER.info(line.getString());
            }
            if (!entry.getKey().equals(CONSOLE)) entry.getValue().sink().accept(line);
        }
    }
}
