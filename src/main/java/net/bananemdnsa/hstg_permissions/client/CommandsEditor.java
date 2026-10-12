package net.bananemdnsa.hstg_permissions.client;

import net.bananemdnsa.historystages.api.editor.CategoryEditor;
import net.bananemdnsa.historystages.api.editor.CategoryTab;
import net.bananemdnsa.historystages.api.editor.RegisterCategoryEditorsEvent;
import net.bananemdnsa.historystages.api.stage.StageScope;
import net.bananemdnsa.hstg_permissions.HSTGPermissions;
import net.bananemdnsa.hstg_permissions.category.CommandsCategory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = HSTGPermissions.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class CommandsEditor {

    private CommandsEditor() {}

    @SubscribeEvent
    public static void onRegisterEditors(RegisterCategoryEditorsEvent event) {
        event.register(new CategoryEditor() {
            @Override
            public String categoryId() {
                return CommandsCategory.ID;
            }

            @Override
            public CategoryTab createTab(Runnable onChanged, StageScope scope) {
                // Asked whenever a stage opens, so a /reload or a new mod shows up without relogging.
                CatalogRequester.request();
                return new CommandsTab(onChanged);
            }
        });
    }
}
