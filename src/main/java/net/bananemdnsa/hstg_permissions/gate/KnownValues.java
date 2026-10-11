package net.bananemdnsa.hstg_permissions.gate;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.mojang.brigadier.arguments.ArgumentType;
import net.minecraft.commands.arguments.GameModeArgument;
import net.minecraft.world.level.GameType;

/** Values worth naming in a path even though the tree only has a typed argument there. */
final class KnownValues {

    private KnownValues() {}

    static Collection<String> of(ArgumentType<?> type) {
        // /gamemode takes its mode as a typed word since 1.20.3, so the tree alone never names one.
        if (type instanceof GameModeArgument) {
            return Arrays.stream(GameType.values()).map(GameType::getName).toList();
        }
        return List.of();
    }
}
