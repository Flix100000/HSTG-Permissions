package net.bananemdnsa.hstg_permissions.category;

import net.bananemdnsa.historystages.api.lock.AddonLockCategory;
import net.bananemdnsa.historystages.api.lock.CategoryStorage;
import net.bananemdnsa.historystages.api.lock.RegisterLockCategoriesEvent;
import net.bananemdnsa.hstg_permissions.rules.CommandTarget;
import net.bananemdnsa.hstg_permissions.rules.PermissionEntry;

public final class CommandsCategory {

    public static final String ID = "hstg_permissions:commands";

    private static final AddonLockCategory<PermissionEntry> CATEGORY = AddonLockCategory.<PermissionEntry>builder(ID)
            .tabLangKey("editor.hstg_permissions.tab.commands")
            .tooltipLangKey("editor.hstg_permissions.tooltip.commands")
            .storage(CategoryStorage.gson(PermissionEntry.class))
            .matcher(CommandTarget.class, (entry, target) ->
                    entry.pattern().map(pattern -> pattern.matches(target)).orElse(false))
            .build();

    private CommandsCategory() {}

    public static AddonLockCategory<PermissionEntry> get() {
        return CATEGORY;
    }

    public static void register(RegisterLockCategoriesEvent event) {
        event.register(CATEGORY);
    }
}
