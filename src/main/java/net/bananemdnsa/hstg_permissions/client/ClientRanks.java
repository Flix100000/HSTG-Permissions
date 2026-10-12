package net.bananemdnsa.hstg_permissions.client;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.bananemdnsa.hstg_permissions.rank.Rank;

/** The ranks the server sent for the name tags; empty on a server without the addon. */
public final class ClientRanks {

    private static final Map<UUID, Rank> RANKS = new ConcurrentHashMap<>();

    private ClientRanks() {}

    public static void apply(boolean replace, Map<UUID, Rank> ranks) {
        if (replace) RANKS.clear();
        ranks.forEach((player, rank) -> {
            if (rank.isEmpty()) {
                RANKS.remove(player);
            } else {
                RANKS.put(player, rank);
            }
        });
    }

    public static Optional<Rank> get(UUID player) {
        return Optional.ofNullable(RANKS.get(player));
    }

    public static void reset() {
        RANKS.clear();
    }
}
