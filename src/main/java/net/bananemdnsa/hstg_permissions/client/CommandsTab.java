package net.bananemdnsa.hstg_permissions.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.UnaryOperator;

import org.jetbrains.annotations.Nullable;

import net.bananemdnsa.historystages.api.editor.CategoryTab;
import net.bananemdnsa.historystages.api.editor.TabInputContext;
import net.bananemdnsa.historystages.api.editor.TabRenderContext;
import net.bananemdnsa.historystages.api.editor.widget.EditorRowList;
import net.bananemdnsa.historystages.api.editor.widget.PickerOverlay;
import net.bananemdnsa.historystages.api.editor.widget.SearchBar;
import net.bananemdnsa.historystages.data.StageEntry;
import net.bananemdnsa.hstg_permissions.catalog.CommandCatalog;
import net.bananemdnsa.hstg_permissions.category.CommandsCategory;
import net.bananemdnsa.hstg_permissions.editor.EntryList;
import net.bananemdnsa.hstg_permissions.editor.EntryStatus;
import net.bananemdnsa.hstg_permissions.editor.RowText;
import net.bananemdnsa.hstg_permissions.rules.EntryProblems;
import net.bananemdnsa.hstg_permissions.rules.PermissionEntry;
import net.minecraft.network.chat.Component;

/** The Commands tab: one row per entry, its path written the way the stage file writes it. */
public final class CommandsTab implements CategoryTab {

    private final Runnable onChanged;
    private final EntryList list = new EntryList();
    private final EditorRowList rows = new EditorRowList();
    private final SearchBar filter = new SearchBar("")
            .setLightStyle(true)
            .setEscapeReleasesFocus(true);
    @Nullable
    private CommandPicker picker;

    /** Gap between the filter field and the first row. */
    private static final int FILTER_GAP = 4;

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
        if (list.size() == 0) clearFilter();
    }

    @Override
    public void load(StageEntry stage) {
        list.load(CommandsCategory.get().read(stage));
        clearFilter();
    }

    @Override
    public void store(StageEntry stage) {
        CommandsCategory.get().write(stage, list.toEntries());
    }

    @Override
    public void onShown() {
        rows.resetSlideIn();
        clearFilter();
    }

    @Override
    public int contentHeight(int width) {
        return filterHeight() + rows.heightForRows(visibleRows().size());
    }

    @Override
    public int rowAt(TabInputContext ctx) {
        List<Integer> visible = visibleRows();
        int shown = rows.rowAt(below(ctx), visible.size());
        return shown < 0 ? -1 : visible.get(shown);
    }

    @Override
    public boolean renderContent(TabRenderContext ctx) {
        var catalog = ClientCatalog.get();
        useSpellingsOf(catalog);
        if (list.size() > 0) {
            filter.setPlaceholder(Component.translatable("editor.hstg_permissions.filter").getString());
            filter.setPosition(ctx.x(), ctx.y(), ctx.width());
            filter.render(ctx.graphics(), ctx.font(),
                    ctx.inputBlocked() ? -1 : ctx.mouseX(), ctx.inputBlocked() ? -1 : ctx.mouseY());
        }
        List<Integer> visible = visibleRows();
        TabRenderContext rowsCtx = new TabRenderContext(ctx.graphics(), ctx.font(), ctx.x(),
                ctx.y() + filterHeight(), ctx.width(), ctx.clipTop(), ctx.clipBottom(),
                ctx.mouseX(), ctx.mouseY(), ctx.inputBlocked(), ctx.tooltipSink());
        rows.render(rowsCtx, visible.size(), (row, shown) -> {
            int i = visible.get(shown);
            PermissionEntry entry = list.entryAt(i);
            EntryStatus status = EntryStatus.of(entry, catalog);
            row.text(RowText.of(entry.command(), status.level()));
            if (row.isHovered() && status.problem() != null) {
                ctx.tooltip("hstgp.row." + i + "." + status.problem().kind(), describe(entry, status.problem()));
            }
        });
        return true;
    }

    @Override
    public boolean mouseClicked(TabInputContext ctx, int button) {
        if (button != 0) return false;
        if (list.size() > 0) {
            filter.setPosition(ctx.x(), ctx.y(), ctx.width());
            if (filter.mouseClicked(ctx.mouseX(), ctx.mouseY())) return true;
            filter.setFocused(false);
        }
        return rows.mouseClicked(below(ctx));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return list.size() > 0 && filter.keyPressed(keyCode);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return list.size() > 0 && filter.charTyped(codePoint);
    }

    @Override
    public void rebuildPicker() {
        picker = new CommandPicker(list, command -> {
            useSpellingsOf(ClientCatalog.get());
            list.pick(command);
            clearFilter();
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

    private void clearFilter() {
        filter.setText("");
        filter.setFocused(false);
    }

    private int filterHeight() {
        return list.size() > 0 ? SearchBar.HEIGHT + FILTER_GAP : 0;
    }

    private TabInputContext below(TabInputContext ctx) {
        return new TabInputContext(ctx.x(), ctx.y() + filterHeight(), ctx.width(), ctx.clipTop(),
                ctx.clipBottom(), ctx.mouseX(), ctx.mouseY());
    }

    /** Indices into the real list of the entries the filter lets through. */
    private List<Integer> visibleRows() {
        String needle = filter.getText();
        List<Integer> visible = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            if (needle.isEmpty() || list.entryAt(i).command().toLowerCase(Locale.ROOT).contains(needle)) {
                visible.add(i);
            }
        }
        return visible;
    }

    private void useSpellingsOf(CommandCatalog catalog) {
        list.setSpellings(catalog == null ? UnaryOperator.identity() : catalog::canonical);
    }

    private static String describe(PermissionEntry entry, EntryProblems.Problem problem) {
        String key = "editor.hstg_permissions.problem." + problem.kind().name().toLowerCase(Locale.ROOT);
        return Component.translatable(key, entry.command()).getString();
    }
}
