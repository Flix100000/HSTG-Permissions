package net.bananemdnsa.hstg_permissions.client;

import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import net.bananemdnsa.historystages.api.editor.CategoryTab;
import net.bananemdnsa.historystages.api.editor.TabInputContext;
import net.bananemdnsa.historystages.api.editor.TabRenderContext;
import net.bananemdnsa.historystages.api.editor.widget.EditorRowList;
import net.bananemdnsa.historystages.api.editor.widget.PickerOverlay;
import net.bananemdnsa.historystages.data.StageEntry;
import net.bananemdnsa.hstg_permissions.category.CommandsCategory;
import net.bananemdnsa.hstg_permissions.editor.EntryList;
import net.bananemdnsa.hstg_permissions.editor.EntryStatus;
import net.bananemdnsa.hstg_permissions.editor.RowText;
import net.bananemdnsa.hstg_permissions.rules.EntryProblems;
import net.bananemdnsa.hstg_permissions.rules.PermissionEntry;
import net.minecraft.network.chat.Component;

/** The Commands tab: one row per entry, its path written the way the stage file writes it. */
public final class CommandsTab implements CategoryTab {

    private static final int COUNT_LINE_H = 14;

    private final Runnable onChanged;
    private final EntryList list = new EntryList();
    private final EditorRowList rows = new EditorRowList();
    @Nullable
    private CommandPicker picker;

    CommandsTab(Runnable onChanged) {
        this.onChanged = onChanged;
    }

    @Override
    public String categoryId() {
        return CommandsCategory.ID;
    }

    @Override
    public String tabLangKey() {
        return CommandsCategory.get().tabLangKey();
    }

    @Override
    public String tooltipLangKey() {
        return CommandsCategory.get().tooltipLangKey();
    }

    @Override
    public boolean availableForIndividualStages() {
        return true;
    }

    @Override
    public List<String> entries() {
        return list.commands();
    }

    @Override
    public void removeAt(int index) {
        list.removeAt(index);
    }

    @Override
    public void load(StageEntry stage) {
        list.load(CommandsCategory.get().read(stage));
    }

    @Override
    public void store(StageEntry stage) {
        CommandsCategory.get().write(stage, list.toEntries());
    }

    @Override
    public void onShown() {
        rows.resetSlideIn();
    }

    @Override
    public int contentHeight(int width) {
        return list.size() == 0 ? 0 : rows.heightForRows(list.size()) + COUNT_LINE_H;
    }

    @Override
    public int rowAt(TabInputContext ctx) {
        return rows.rowAt(ctx, list.size());
    }

    @Override
    public boolean renderContent(TabRenderContext ctx) {
        var catalog = ClientCatalog.get();
        int bottom = rows.render(ctx, list.size(), (row, i) -> {
            PermissionEntry entry = list.entryAt(i);
            EntryStatus status = EntryStatus.of(entry, catalog);
            row.text(RowText.of(entry.command(), status.level()));
            if (row.isHovered() && status.problem() != null) {
                ctx.tooltip("hstgp.row." + i + "." + status.problem().kind(), describe(entry, status.problem()));
            }
        });
        if (list.size() > 0) {
            String count = Component.translatable("editor.hstg_permissions.count", list.size()).getString();
            ctx.graphics().drawString(ctx.font(), count, ctx.x() + 4, bottom + 3, 0x888888, false);
        }
        return true;
    }

    @Override
    public boolean mouseClicked(TabInputContext ctx, int button) {
        return button == 0 && rows.mouseClicked(ctx);
    }

    @Override
    public void rebuildPicker() {
        picker = new CommandPicker(list, command -> {
            list.pick(command);
            onChanged.run();
        });
    }

    @Override
    @Nullable
    public PickerOverlay activeOverlay() {
        return picker;
    }

    @Override
    public void openPicker(int centerX, int centerY, int parentWidth) {
        if (picker == null) rebuildPicker();
        picker.setFilter("");
        picker.show(centerX, centerY, parentWidth);
    }

    private static String describe(PermissionEntry entry, EntryProblems.Problem problem) {
        String key = "editor.hstg_permissions.problem." + problem.kind().name().toLowerCase(Locale.ROOT);
        return Component.translatable(key, entry.command()).getString();
    }
}
