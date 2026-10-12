package net.bananemdnsa.hstg_permissions.rank;

import net.bananemdnsa.historystages.api.settings.RegisterStageSettingsGroupsEvent;
import net.bananemdnsa.historystages.api.settings.Setting;
import net.bananemdnsa.historystages.api.settings.StageSettingsGroup;
import net.bananemdnsa.historystages.api.stage.StageScope;

/**
 * The rank card in History Stages' stage editor. Individual stages only: a global stage is held by
 * everyone at once, so its rank would set nobody apart.
 */
public final class RankSettings {

    public static final Setting<String> PREFIX = Setting.longText("prefix")
            .langKey("settings.hstg_permissions.rank.prefix")
            .defaultValue("")
            .supportedScopes(StageScope.INDIVIDUAL)
            .build();

    public static final Setting<String> SUFFIX = Setting.longText("suffix")
            .langKey("settings.hstg_permissions.rank.suffix")
            .defaultValue("")
            .supportedScopes(StageScope.INDIVIDUAL)
            .build();

    public static final Setting<Integer> PRIORITY = Setting.integer("priority")
            .langKey("settings.hstg_permissions.rank.priority")
            .range(0, 1000)
            .defaultValue(0)
            .supportedScopes(StageScope.INDIVIDUAL)
            .build();

    public static final StageSettingsGroup GROUP = StageSettingsGroup.builder("hstg_permissions:rank")
            .titleLangKey("settings.hstg_permissions.rank.title")
            .supportedScopes(StageScope.INDIVIDUAL)
            .field(PREFIX)
            .field(SUFFIX)
            .field(PRIORITY)
            .build();

    private RankSettings() {}

    public static void register(RegisterStageSettingsGroupsEvent event) {
        event.register(GROUP);
    }
}
