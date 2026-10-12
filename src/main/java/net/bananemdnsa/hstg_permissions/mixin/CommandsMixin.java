package net.bananemdnsa.hstg_permissions.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.bananemdnsa.hstg_permissions.gate.GameModeAccess;
import net.bananemdnsa.hstg_permissions.network.GameModesPayload;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/** Every time a player's command tree goes out – login, op change, reload, stage change – their game modes follow. */
@Mixin(Commands.class)
public abstract class CommandsMixin {

    @Inject(method = "sendCommands", at = @At("TAIL"))
    private void hstgp$sendGameModes(ServerPlayer player, CallbackInfo ci) {
        if (player instanceof FakePlayer || !player.connection.hasChannel(GameModesPayload.TYPE)) return;
        PacketDistributor.sendToPlayer(player, new GameModesPayload(GameModeAccess.allowedFor(player)));
    }
}
