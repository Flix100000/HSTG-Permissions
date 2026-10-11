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
import net.bananemdnsa.hstg_permissions.category.CommandsCategory;
import net.bananemdnsa.hstg_permissions.gate.PermissionService;
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

    /** @param entries each like {@code "lock minecraft.gamemode"} or {@code "grant minecraft.msg"} */
    static String global(String name, String... entries) {
        return write("global", name, entries);
    }

    static String individual(String name, String... entries) {
        return write("individual", name, entries);
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

    private static String write(String scope, String name, String... entries) {
        String id = PREFIX + name;
        JsonArray list = new JsonArray();
        for (String entry : entries) {
            String[] parts = entry.split(" ", 2);
            JsonObject json = new JsonObject();
            json.addProperty("command", parts[1]);
            json.addProperty("mode", parts[0]);
            list.add(json);
        }
        JsonObject addons = new JsonObject();
        addons.add(CommandsCategory.ID, list);
        JsonObject stage = new JsonObject();
        stage.addProperty("display_name", name);
        stage.add("addons", addons);
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
