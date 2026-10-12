package net.bananemdnsa.hstg_permissions.mixin.client;

import java.util.Locale;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.bananemdnsa.hstg_permissions.client.ClientGameModes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.debug.GameModeSwitcherScreen;

/** Greys out the modes the server does not allow. */
@Mixin(GameModeSwitcherScreen.GameModeSlot.class)
public abstract class GameModeSlotMixin {

    @Shadow
    @Final
    GameModeSwitcherScreen.GameModeIcon icon;

    @Inject(method = "renderWidget", at = @At("TAIL"))
    private void hstgp$greyOut(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!ClientGameModes.known() || ClientGameModes.allows(icon.name().toLowerCase(Locale.ROOT))) return;
        AbstractWidget self = (AbstractWidget) (Object) this;
        graphics.fill(self.getX(), self.getY(), self.getX() + 26, self.getY() + 26, 0xB0000000);
    }
}
