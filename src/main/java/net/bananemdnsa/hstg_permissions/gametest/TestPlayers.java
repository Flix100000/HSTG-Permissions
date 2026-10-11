package net.bananemdnsa.hstg_permissions.gametest;

import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.tree.CommandNode;
import net.bananemdnsa.hstg_permissions.gate.PermissionService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.jetbrains.annotations.Nullable;

/**
 * Players built directly rather than through the login path, which would drag every other mod's
 * login handling into the test. The connection swallows whatever is sent to it.
 */
final class TestPlayers {

    private TestPlayers() {}

    static ServerPlayer create(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        GameProfile profile = new GameProfile(UUID.randomUUID(), "hstgp_test");
        ServerPlayer player = new ServerPlayer(level.getServer(), level, profile, ClientInformation.createDefault());
        new SilentListener(player);
        return player;
    }

    static ServerPlayer createOp(GameTestHelper helper) {
        ServerPlayer player = create(helper);
        // PlayerList.op() uses the server's default level, which is 0 on the game test server.
        player.getServer().getPlayerList().getOps().add(new ServerOpListEntry(player.getGameProfile(), 4, false));
        return player;
    }

    static void cleanUp(ServerPlayer player) {
        player.getServer().getPlayerList().deop(player.getGameProfile());
        PermissionService.forget(player.getUUID());
    }

    /** Whether Brigadier would let this player see and use the top-level command. */
    static boolean canUse(ServerPlayer player, String rootCommand) {
        MinecraftServer server = player.getServer();
        CommandNode<CommandSourceStack> node = server.getCommands().getDispatcher().getRoot().getChild(rootCommand);
        if (node == null) throw new IllegalArgumentException("No command /" + rootCommand + " on this server.");
        return node.canUse(player.createCommandSourceStack());
    }

    /** Runs a command the way a typed one runs, and returns what it said. */
    static Inbox run(ServerPlayer player, String command) {
        Inbox inbox = new Inbox();
        player.getServer().getCommands().performPrefixedCommand(player.createCommandSourceStack().withSource(inbox), command);
        return inbox;
    }

    // NeoForge checks mod payloads against negotiated channels on the listener, so the listener is
    // what has to swallow them; a fake connection alone is not enough.
    private static final class SilentListener extends ServerGamePacketListenerImpl {
        SilentListener(ServerPlayer player) {
            super(player.getServer(), new Connection(PacketFlow.SERVERBOUND), player,
                    CommonListenerCookie.createInitial(player.getGameProfile(), false));
        }

        @Override
        public void send(Packet<?> packet) {}

        @Override
        public void send(Packet<?> packet, @Nullable PacketSendListener listener) {}
    }
}
