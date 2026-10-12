package net.bananemdnsa.hstg_permissions.rank;

import java.util.ArrayList;
import java.util.List;

import net.bananemdnsa.historystages.api.settings.SettingsValues;
import net.bananemdnsa.historystages.api.stage.StageScope;
import net.bananemdnsa.historystages.api.stage.StageStates;
import net.bananemdnsa.hstg_permissions.config.PermissionsConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Reads a player's rank off their stages and puts it around their name. */
public final class RankService {

    private RankService() {}

    public static Rank rankOf(ServerPlayer player) {
        List<RankSource> sources = new ArrayList<>();
        for (String stageId : StageStates.unlockedIndividual(player)) {
            SettingsValues values = RankSettings.GROUP.valuesFor(stageId, StageScope.INDIVIDUAL);
            sources.add(new RankSource(stageId, values.get(RankSettings.PREFIX), values.get(RankSettings.SUFFIX),
                    values.get(RankSettings.PRIORITY)));
        }
        return RankChoice.pick(sources);
    }

    // Built on the name the event already carries, so a nickname another mod set stays inside the rank.
    public static void onNameFormat(PlayerEvent.NameFormat event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer) return;
        if (!PermissionsConfig.rankInChat()) return;
        Rank rank = rankOf(player);
        if (!rank.isEmpty()) event.setDisplayname(RankDisplay.decorate(event.getDisplayname(), rank));
    }

    public static void onTabListNameFormat(PlayerEvent.TabListNameFormat event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer) return;
        if (!PermissionsConfig.rankInTabList()) return;
        Rank rank = rankOf(player);
        if (rank.isEmpty()) return;
        // No name set means the client shows the plain name in its team's colours, so that is the base.
        Component base = event.getDisplayName() != null
                ? event.getDisplayName()
                : PlayerTeam.formatNameForTeam(player.getTeam(), player.getName());
        event.setDisplayName(RankDisplay.decorate(base, rank));
    }
}
