package net.bananemdnsa.hstg_permissions.mixin.client;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.google.common.base.MoreObjects;
import net.bananemdnsa.hstg_permissions.client.ClientGameModes;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.debug.GameModeSwitcherScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.GameType;

/** F3+F4 and F3+N follow the game modes the server allows, instead of the operator level. */
@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private void debugFeedbackTranslated(String message, Object... args) {}

    @Inject(method = "handleDebugKeys", at = @At("HEAD"), cancellable = true)
    private void hstgp$gameModeKeys(int key, CallbackInfoReturnable<Boolean> cir) {
        LocalPlayer player = minecraft.player;
        // Without an answer from the server everything stays vanilla.
        if (player == null || minecraft.gameMode == null || !ClientGameModes.known()) return;
        if (key == GLFW.GLFW_KEY_F4) {
            if (ClientGameModes.anyAllowed()) {
                minecraft.setScreen(new GameModeSwitcherScreen());
            } else {
                debugFeedbackTranslated("debug.gamemodes.error");
            }
            cir.setReturnValue(true);
        } else if (key == GLFW.GLFW_KEY_N) {
            GameType target = player.isSpectator()
                    ? MoreObjects.firstNonNull(minecraft.gameMode.getPreviousPlayerMode(), GameType.CREATIVE)
                    : GameType.SPECTATOR;
            if (ClientGameModes.allows(target.getName())) {
                player.connection.sendUnsignedCommand("gamemode " + target.getName());
            } else {
                debugFeedbackTranslated("debug.creative_spectator.error");
            }
            cir.setReturnValue(true);
        }
    }
}
