package net.bananemdnsa.hstg_permissions;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.bananemdnsa.historystages.api.lock.RegisterLockCategoriesEvent;
import net.bananemdnsa.hstg_permissions.category.CommandsCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(HSTGPermissions.MODID)
public class HSTGPermissions {
    public static final String MODID = "hstg_permissions";
    public static final Logger LOGGER = LogUtils.getLogger();

    public HSTGPermissions(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(RegisterLockCategoriesEvent.class, CommandsCategory::register);
    }
}
