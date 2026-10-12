package net.bananemdnsa.hstg_permissions.rank;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.bananemdnsa.hstg_permissions.config.PermissionsConfig;
import net.bananemdnsa.hstg_permissions.gate.StageChangeListener;
import net.bananemdnsa.hstg_permissions.network.RanksPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Brings names up to date after a stage change, and tells clients with the addon what to put on
 * the name tags. Remembers what each online player shows, so a newcomer gets the whole list.
 */
public final class RankUpdates {

    private static final Map<UUID, Rank> APPLIED = new ConcurrentHashMap<>();
    private static volatile boolean nameTagsShown = true;

    private RankUpdates() {}

    public static void register(IEventBus gameBus) {
        gameBus.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) onLogin(player);
        });
        gameBus.addListener(PlayerEvent.PlayerLoggedOutEvent.class, event -> APPLIED.remove(event.getEntity().getUUID()));
        gameBus.addListener(ServerStoppedEvent.class, event -> APPLIED.clear());
    }

    private static void onLogin(ServerPlayer player) {
        if (player instanceof FakePlayer) return;
        send(player, payload(true, APPLIED));
        // A tick later, once History Stages has read the player's stages; this also sends theirs to everyone.
        StageChangeListener.refresh(player.getUUID());
    }

    public static void apply(MinecraftServer server, Collection<ServerPlayer> players) {
        Map<UUID, Rank> changed = new LinkedHashMap<>();
        for (ServerPlayer player : players) {
            Rank rank = RankService.rankOf(player);
            if (!rank.equals(APPLIED.put(player.getUUID(), rank))) changed.put(player.getUUID(), rank);
            player.refreshDisplayName();
            player.refreshTabListName();
        }
        // Turning name tags on or off changes no rank, but every client has to hear of it.
        boolean shown = PermissionsConfig.rankInNameTag();
        boolean switched = shown != nameTagsShown;
        nameTagsShown = shown;
        if (changed.isEmpty() && !switched) return;
        RanksPayload payload = switched ? payload(true, APPLIED) : payload(false, changed);
        for (ServerPlayer online : server.getPlayerList().getPlayers()) send(online, payload);
    }

    /** With name tags turned off every rank goes out empty, so clients drop what they had. */
    public static RanksPayload payload(boolean replace, Map<UUID, Rank> ranks) {
        boolean shown = PermissionsConfig.rankInNameTag();
        List<RanksPayload.Entry> entries = new ArrayList<>();
        ranks.forEach((player, rank) -> entries.add(shown
                ? new RanksPayload.Entry(player, rank.prefix(), rank.suffix())
                : new RanksPayload.Entry(player, "", "")));
        return new RanksPayload(replace, entries);
    }

    private static void send(ServerPlayer player, RanksPayload payload) {
        if (player instanceof FakePlayer || !player.connection.hasChannel(RanksPayload.TYPE)) return;
        PacketDistributor.sendToPlayer(player, payload);
    }
}
