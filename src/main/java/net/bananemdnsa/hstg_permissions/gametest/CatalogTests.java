package net.bananemdnsa.hstg_permissions.gametest;

import java.util.List;
import java.util.Optional;

import net.bananemdnsa.hstg_permissions.HSTGPermissions;
import net.bananemdnsa.hstg_permissions.catalog.CatalogCodec;
import net.bananemdnsa.hstg_permissions.catalog.CommandCatalog;
import net.bananemdnsa.hstg_permissions.network.CatalogAnswer;
import net.bananemdnsa.hstg_permissions.rules.EntryPattern;
import net.bananemdnsa.hstg_permissions.rules.EntryProblems;
import net.bananemdnsa.hstg_permissions.rules.PermissionEntry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(HSTGPermissions.MODID)
@PrefixGameTestTemplate(false)
public final class CatalogTests {

    private CatalogTests() {}

    @GameTest(template = "empty", batch = "hstgp_catalog")
    public static void anOpGetsTheServersCommands(GameTestHelper helper) {
        ServerPlayer op = TestPlayers.createOp(helper);
        try {
            Optional<CommandCatalog> answer = CatalogAnswer.forPlayer(op);
            if (answer.isEmpty()) {
                helper.fail("an operator must get the catalog");
                return;
            }
            // Through the codec, as the client would see it.
            CommandCatalog catalog = CatalogCodec.decode(CatalogCodec.encode(answer.get()));
            if (!catalog.exists(EntryPattern.parse("minecraft.gamemode.creative").orElseThrow())) {
                helper.fail("the catalog must know /gamemode creative");
                return;
            }
            if (!catalog.nodes().contains(new CommandCatalog.Node(List.of("gamemode", "creative"), true))) {
                helper.fail("creative must be listed as a value of /gamemode");
                return;
            }
            if (!"teleport".equals(catalog.aliasTargets().get("tp"))) {
                helper.fail("tp must be known as an alias of teleport, got " + catalog.aliasTargets().get("tp"));
                return;
            }
            if (catalog.problems(new PermissionEntry("minecraft.op", "grant")).stream()
                    .noneMatch(problem -> problem.kind() == EntryProblems.Kind.PROTECTED_GRANT)) {
                helper.fail("a grant on /op must be flagged from the server's protected list");
                return;
            }
            helper.succeed();
        } finally {
            TestPlayers.cleanUp(op);
        }
    }

    @GameTest(template = "empty", batch = "hstgp_catalog")
    public static void aNonOpGetsNoCatalog(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.create(helper);
        try {
            if (CatalogAnswer.forPlayer(player).isPresent()) {
                helper.fail("a player who may not open the editor must not get the command catalog");
                return;
            }
            helper.succeed();
        } finally {
            TestPlayers.cleanUp(player);
        }
    }
}
