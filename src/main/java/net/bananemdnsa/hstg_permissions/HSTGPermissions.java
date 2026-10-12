package net.bananemdnsa.hstg_permissions;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.bananemdnsa.historystages.api.config.RegisterConfigSectionsEvent;
import net.bananemdnsa.historystages.api.diagnostics.RegisterReportSectionsEvent;
import net.bananemdnsa.historystages.api.diagnostics.RegisterStageChecksEvent;
import net.bananemdnsa.historystages.api.lock.RegisterLockCategoriesEvent;
import net.bananemdnsa.historystages.api.settings.RegisterStageSettingsGroupsEvent;
import net.bananemdnsa.hstg_permissions.category.CommandsCategory;
import net.bananemdnsa.hstg_permissions.command.PermsCommand;
import net.bananemdnsa.hstg_permissions.config.PermissionsConfig;
import net.bananemdnsa.hstg_permissions.config.PermissionsSettings;
import net.bananemdnsa.hstg_permissions.diagnostics.PermissionDiagnostics;
import net.bananemdnsa.hstg_permissions.gate.CommandEventGate;
import net.bananemdnsa.hstg_permissions.gate.CommandGuard;
import net.bananemdnsa.hstg_permissions.gate.StageChangeListener;
import net.bananemdnsa.hstg_permissions.gate.VerboseWatch;
import net.bananemdnsa.hstg_permissions.network.PermissionsNetwork;
import net.bananemdnsa.hstg_permissions.rank.RankService;
import net.bananemdnsa.hstg_permissions.rank.RankSettings;
import net.bananemdnsa.hstg_permissions.rank.RankUpdates;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;

@Mod(HSTGPermissions.MODID)
public class HSTGPermissions {
    public static final String MODID = "hstg_permissions";
    public static final Logger LOGGER = LogUtils.getLogger();

    public HSTGPermissions(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, PermissionsConfig.SPEC);
        modEventBus.addListener(ModConfigEvent.Loading.class, PermissionsConfig::onConfigChanged);
        modEventBus.addListener(ModConfigEvent.Reloading.class, PermissionsConfig::onConfigChanged);
        modEventBus.addListener(RegisterConfigSectionsEvent.class, PermissionsSettings::register);
        modEventBus.addListener(RegisterLockCategoriesEvent.class, CommandsCategory::register);
        modEventBus.addListener(RegisterStageChecksEvent.class, PermissionDiagnostics::registerChecks);
        modEventBus.addListener(RegisterReportSectionsEvent.class, PermissionDiagnostics::registerReport);
        modEventBus.addListener(RegisterPayloadHandlersEvent.class, PermissionsNetwork::register);
        modEventBus.addListener(RegisterStageSettingsGroupsEvent.class, RankSettings::register);
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, PermsCommand::register);
        // Last, so every other mod's commands are already in the tree.
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, RegisterCommandsEvent.class, CommandGuard::onRegisterCommands);
        // Last, so no later listener can swap the parse after it was checked.
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, CommandEvent.class, CommandEventGate::onCommand);
        StageChangeListener.register(NeoForge.EVENT_BUS);
        VerboseWatch.register(NeoForge.EVENT_BUS);
        // Low, so a nickname another mod sets is already there to wrap.
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, PlayerEvent.NameFormat.class, RankService::onNameFormat);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, PlayerEvent.TabListNameFormat.class, RankService::onTabListNameFormat);
        RankUpdates.register(NeoForge.EVENT_BUS);
    }
}
