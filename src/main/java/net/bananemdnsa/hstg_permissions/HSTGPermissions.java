package net.bananemdnsa.hstg_permissions;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;

@Mod(HSTGPermissions.MODID)
public class HSTGPermissions {
    public static final String MODID = "hstg_permissions";
    public static final Logger LOGGER = LogUtils.getLogger();

    public HSTGPermissions(IEventBus modEventBus, ModContainer modContainer) {
    }
}
