package net.bananemdnsa.hstg_permissions.command;

import java.util.Optional;
import java.util.UUID;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.bananemdnsa.hstg_permissions.gate.PermissionCheck;
import net.bananemdnsa.hstg_permissions.gate.VerboseWatch;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.jetbrains.annotations.Nullable;

/** /hstgperms: look up a player's answer for a command, or watch the answers as they happen. */
public final class PermsCommand {

    private PermsCommand() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("hstgperms")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("check")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("command", StringArgumentType.greedyString())
                                        .executes(PermsCommand::check))))
                .then(Commands.literal("verbose")
                        .then(Commands.literal("on")
                                .executes(context -> watch(context, null))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> watch(context, EntityArgument.getPlayer(context, "player")))))
                        .then(Commands.literal("off").executes(PermsCommand::stop))));
    }

    private static int check(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        PermissionCheck.Result result = PermissionCheck.of(player, StringArgumentType.getString(context, "command"));
        context.getSource().sendSuccess(result::line, false);
        return result.outcome() == PermissionCheck.Outcome.ALLOWED_BY_STAGE
                || result.outcome() == PermissionCheck.Outcome.MINECRAFT_ALLOWS ? 1 : 0;
    }

    private static int watch(CommandContext<CommandSourceStack> context, @Nullable ServerPlayer only) {
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();
        ServerPlayer watcher = source.getPlayer();
        UUID id = watcher != null ? watcher.getUUID() : VerboseWatch.CONSOLE;
        // Looked up each time: a respawn makes a new player object.
        VerboseWatch.watch(id, Optional.ofNullable(only).map(ServerPlayer::getUUID), line -> {
            ServerPlayer current = server.getPlayerList().getPlayer(id);
            if (current != null) current.sendSystemMessage(line);
        });
        Component answer = only == null
                ? Component.translatable("command.hstg_permissions.verbose.on.all")
                : Component.translatable("command.hstg_permissions.verbose.on.player", only.getDisplayName());
        source.sendSuccess(() -> answer, false);
        return 1;
    }

    private static int stop(CommandContext<CommandSourceStack> context) {
        ServerPlayer watcher = context.getSource().getPlayer();
        VerboseWatch.stop(watcher != null ? watcher.getUUID() : VerboseWatch.CONSOLE);
        context.getSource().sendSuccess(() -> Component.translatable("command.hstg_permissions.verbose.off"), false);
        return 1;
    }
}
