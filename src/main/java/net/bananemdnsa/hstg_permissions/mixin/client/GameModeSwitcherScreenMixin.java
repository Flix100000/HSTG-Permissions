package net.bananemdnsa.hstg_permissions.mixin.client;

import java.util.Locale;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.bananemdnsa.hstg_permissions.client.ClientGameModes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.debug.GameModeSwitcherScreen;
import net.minecraft.client.player.LocalPlayer;

/** The selection skips modes the server does not allow, and a disallowed mode is never sent. */
@Mixin(GameModeSwitcherScreen.class)
public abstract class GameModeSwitcherScreenMixin {

    @Shadow
    private GameModeSwitcherScreen.GameModeIcon currentlyHovered;

    @Inject(method = "init", at = @At("TAIL"))
    private void hstgp$skipOnOpen(CallbackInfo ci) {
        hstgp$skipDisallowed();
    }

    @Inject(method = "keyPressed", at = @At("RETURN"))
    private void hstgp$skipOnCycle(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (keyCode == GLFW.GLFW_KEY_F4) hstgp$skipDisallowed();
    }

    @Redirect(method = "switchToHoveredGameMode(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/gui/screens/debug/GameModeSwitcherScreen$GameModeIcon;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;hasPermissions(I)Z"))
    private static boolean hstgp$mayUse(LocalPlayer player, int level, Minecraft minecraft, GameModeSwitcherScreen.GameModeIcon icon) {
        return ClientGameModes.known() ? ClientGameModes.allows(hstgp$name(icon)) : player.hasPermissions(level);
    }

    private void hstgp$skipDisallowed() {
        GameModeSwitcherScreen.GameModeIcon[] icons = GameModeSwitcherScreen.GameModeIcon.values();
        // Same order as GameModeIcon#getNext; at most one full round, in case nothing is allowed.
        for (int i = 0; i < icons.length && ClientGameModes.known() && !ClientGameModes.allows(hstgp$name(currentlyHovered)); i++) {
            currentlyHovered = icons[(currentlyHovered.ordinal() + 1) % icons.length];
        }
    }

    // The icon names match GameType#getName.
    private static String hstgp$name(GameModeSwitcherScreen.GameModeIcon icon) {
        return icon.name().toLowerCase(Locale.ROOT);
    }
}
