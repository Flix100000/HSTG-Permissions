package net.bananemdnsa.hstg_permissions.rank;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class RankDisplay {

    private RankDisplay() {}

    /**
     * The name with the rank around it. Prefix and suffix are literals of their own, so a colour
     * code in them ends where they end and never tints the name.
     */
    public static MutableComponent decorate(Component name, Rank rank) {
        if (rank.isEmpty()) return name.copy();
        MutableComponent decorated = Component.empty();
        if (!rank.prefix().isEmpty()) {
            decorated.append(Component.literal(RankText.colour(rank.prefix()))).append(" ");
        }
        decorated.append(name);
        if (!rank.suffix().isEmpty()) {
            decorated.append(" ").append(Component.literal(RankText.colour(rank.suffix())));
        }
        return decorated;
    }
}
