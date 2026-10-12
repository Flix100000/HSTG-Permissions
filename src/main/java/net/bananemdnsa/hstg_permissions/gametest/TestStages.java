package net.bananemdnsa.hstg_permissions.gametest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.bananemdnsa.hstg_permissions.category.CommandsCategory;
import net.bananemdnsa.hstg_permissions.gate.PermissionService;
import net.bananemdnsa.hstg_permissions.rank.RankSettings;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.loading.FMLPaths;

/**
 * Test stages as real stage files, loaded through /history reload. The API has no way to put a
 * stage in from code, and a file is the honest test anyway: it goes through the addons block and
 * the category's storage exactly as a pack's stage would.
 */
final class TestStages {

    static final String PREFIX = "hstgp_test_";

    private TestStages() {}

    /** @param commands the command paths the stage owns, like {@code "minecraft.gamemode"} */
    static String global(String name, String... commands) {
        return write("global", name, commands);
    }

    static String individual(String name, String... commands) {
        return write("individual", name, commands);
    }

    /** A global stage that asks for its lock hints to be hidden. */
    static String globalWithoutHints(String name, String... commands) {
        return write("global", name, false, commands);
    }

    /** An individual stage that owns no commands and gives a rank. */
    static String individualWithRank(String name, String prefix, String suffix, int priority) {
        return writeRank("individual", name, prefix, suffix, priority);
    }

    /** The same as a global stage, which must give nobody a rank. */
    static String globalWithRank(String name, String prefix, String suffix, int priority) {
        return writeRank("global", name, prefix, suffix, priority);
    }

    private static String writeRank(String scope, String name, String prefix, String suffix, int priority) {
        String id = write(scope, name);
        JsonObject rank = new JsonObject();
        rank.addProperty("prefix", prefix);
        rank.addProperty("suffix", suffix);
        rank.addProperty("priority", priority);
        JsonObject settings = new JsonObject();
        settings.add(RankSettings.GROUP.id(), rank);
        try {
            Path file = dir(scope).resolve(id + ".json");
            JsonObject stage = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            stage.add("addon_settings", settings);
            Files.writeString(file, stage.toString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return id;
    }

    static void reload(MinecraftServer server) {
        server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack().withSuppressedOutput(), "history reload");
        // StageDefinitionsChangedEvent only arrives at the end of the tick, after the test has asked.
        PermissionService.invalidate();
    }

    /** Call from a finally, so a failed test does not leave its stages to the next one. */
    static void removeAll(MinecraftServer server) {
        for (String scope : List.of("global", "individual")) {
            try (Stream<Path> files = Files.list(dir(scope))) {
                for (Path file : files.filter(f -> f.getFileName().toString().startsWith(PREFIX)).toList()) {
                    Files.delete(file);
                }
            } catch (NoSuchFileException ignored) {
                // Nothing was ever written there.
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        reload(server);
    }

    private static String write(String scope, String name, String... commands) {
        return write(scope, name, true, commands);
    }

    private static String write(String scope, String name, boolean hints, String... commands) {
        String id = PREFIX + name;
        JsonArray list = new JsonArray();
        for (String command : commands) {
            JsonObject json = new JsonObject();
            json.addProperty("command", command);
            list.add(json);
        }
        JsonObject addons = new JsonObject();
        addons.add(CommandsCategory.ID, list);
        JsonObject stage = new JsonObject();
        stage.addProperty("display_name", name);
        stage.add("addons", addons);
        if (!hints) {
            JsonObject hidden = new JsonObject();
            hidden.addProperty("show_lock_hints", false);
            stage.add("hidden_display", hidden);
        }
        try {
            Path dir = dir(scope);
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(id + ".json"), stage.toString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return id;
    }

    private static Path dir(String scope) {
        return FMLPaths.CONFIGDIR.get().resolve("historystages").resolve(scope);
    }
}
