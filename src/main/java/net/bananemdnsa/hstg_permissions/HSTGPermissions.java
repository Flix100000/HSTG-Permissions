package net.bananemdnsa.hstg_permissions;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.bananemdnsa.historystages.api.lock.RegisterLockCategoriesEvent;
import net.bananemdnsa.hstg_permissions.category.CommandsCategory;
import net.bananemdnsa.hstg_permissions.config.PermissionsConfig;
import net.bananemdnsa.hstg_permissions.gate.CommandGuard;
import net.bananemdnsa.hstg_permissions.gate.StageChangeListener;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
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
        modEventBus.addListener(RegisterLockCategoriesEvent.class, CommandsCategory::register);
        // Last, so every other mod's commands are already in the tree.
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, RegisterCommandsEvent.class, CommandGuard::onRegisterCommands);
        StageChangeListener.register(NeoForge.EVENT_BUS);
    }
}
