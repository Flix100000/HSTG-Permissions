package net.bananemdnsa.hstg_permissions.gametest;

import java.util.List;
import java.util.UUID;

import net.bananemdnsa.historystages.api.lock.CategoryLocks;
import net.bananemdnsa.historystages.api.lock.LockHit;
import net.bananemdnsa.hstg_permissions.HSTGPermissions;
import net.bananemdnsa.hstg_permissions.category.CommandsCategory;
import net.bananemdnsa.hstg_permissions.rules.CommandTarget;
import net.bananemdnsa.hstg_permissions.rules.PermissionEntry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(HSTGPermissions.MODID)
@PrefixGameTestTemplate(false)
public final class CategoryTests {

    private CategoryTests() {}

    @GameTest(template = "empty", batch = "hstgp_category_file")
    public static void aStageFileReachesTheCategory(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        try {
            TestStages.global("category_file", "minecraft.gamemode");
            TestStages.reload(server);
            UUID player = UUID.randomUUID();

            List<LockHit<PermissionEntry>> hits = CategoryLocks.hits(CommandsCategory.get(),
                    CommandTarget.of("minecraft", "gamemode", "creative"), player);
            if (hits.size() != 1 || !hits.get(0).locks() || !"minecraft.gamemode".equals(hits.get(0).entry().command())) {
                helper.fail("an entry in a stage file must come back as one locking hit, got " + hits);
                return;
            }
            // The control: a category matching everything would pass the check above.
            if (!CategoryLocks.hits(CommandsCategory.get(), CommandTarget.of("minecraft", "time"), player).isEmpty()) {
                helper.fail("an entry must not match a command it does not name");
                return;
            }
            helper.succeed();
        } finally {
            TestStages.removeAll(server);
        }
    }
}
