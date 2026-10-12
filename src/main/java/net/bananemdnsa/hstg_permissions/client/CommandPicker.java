package net.bananemdnsa.hstg_permissions.client;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

import net.bananemdnsa.historystages.api.editor.widget.AbstractSearchableList;
import net.bananemdnsa.historystages.api.editor.widget.SearchBar;
import net.bananemdnsa.hstg_permissions.catalog.CommandCatalog;
import net.bananemdnsa.hstg_permissions.editor.EntryList;
import net.bananemdnsa.hstg_permissions.editor.PickerRow;
import net.bananemdnsa.hstg_permissions.editor.PickerTree;
import net.bananemdnsa.hstg_permissions.editor.TypedPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * The window behind "+": the server's commands as a tree. The two tabs pick what a confirmed
 * selection becomes; picking something that is already there takes it out or switches it, the way
 * {@link EntryList#pick} says.
 */
public final class CommandPicker extends AbstractSearchableList<PickerRow> {

    private static final int INDENT = 10;
    private static final int CARET_W = 10;

    private final EntryList list;
    private final Set<String> expanded = new HashSet<>();
    private long seenVersion = -1;
    private PickerTree tree;
    private CommandCatalog treeOf;

    public CommandPicker(EntryList list, Consumer<String> onSelect) {
        super(Component.translatable("editor.hstg_permissions.picker.search").getString(), onSelect, null);
        this.list = list;
        setMultiSelect(true);
    }

    @Override
    protected void configureFilters(SearchBar bar) {
        // No vanilla/modded pair: the tree is already grouped by mod, and @mod narrows it.
    }

    @Override
    protected int getPanelWidth() {
        return 300;
    }

    @Override
    protected List<PickerRow> loadEntries() {
        seenVersion = ClientCatalog.version();
        PickerTree current = currentTree();
        if (current == null) return waitingRows();
        return current.rows(expanded, "");
    }

    @Override
    protected boolean matchesQuery(PickerRow entry, String lowerCaseQuery) {
        // Only the own tabs rely on this; the tree rebuilds the main list in afterFilter, because a
        // match needs its parents shown too.
        return entry.entry().contains(lowerCaseQuery)
                || entry.label().toLowerCase(Locale.ROOT).contains(lowerCaseQuery);
    }

    @Override
    protected void afterFilter(List<PickerRow> filtered) {
        PickerTree current = currentTree();
        String query = searchBar.getText() == null ? "" : searchBar.getText();
        filtered.clear();
        if (current == null) {
            String typed = TypedPath.normalizeQuery(query);
            if (TypedPath.looksLikeEntry(typed)) {
                // Nothing to check it against, so it is not marked as missing either.
                filtered.add(new PickerRow(PickerRow.Kind.TYPED, typed, typed, 0, false, false, null, 0, true, false));
            }
            filtered.addAll(waitingRows());
            return;
        }
        filtered.addAll(current.rows(expanded, query));
    }

    @Override
    protected boolean isHeaderRow(int index) {
        if (index >= filteredEntries.size()) return false;
        PickerRow row = filteredEntries.get(index);
        return row.kind() == PickerRow.Kind.INFO || row.context();
    }

    @Override
    protected void renderHeaderRow(GuiGraphics g, Font font, PickerRow entry, int x, int y, int w, int h) {
        if (!entry.context()) {
            drawRowText(g, font, entry.label(), x, y, w, false);
            return;
        }
        // No caret: a header row cannot be clicked, so it could not open or close anything.
        int textX = x + entry.depth() * INDENT + CARET_W;
        String text = font.plainSubstrByWidth(label(entry), w - (textX - x) - 6);
        g.drawString(font, text, textX + 3, y + 4, 0xFF777777, false);
    }

    @Override
    protected void renderSelectedRow(GuiGraphics g, Font font, String value, PickerRow entry,
                                     int x, int y, int w, int h, boolean hovered, int rowIndex) {
        // The tree around a row is gone here, so the stored value is the only thing that places it.
        int chipW = 12;
        drawRowMarqueeText(g, font, value, x + 4, y, w - 4 - chipW - 22, h, hovered, rowIndex);
        list.markFor(entry.entry()).ifPresent(mark -> drawChip(g, font, mark, x + w - chipW - 2, y + 2, chipW, h - 4));
    }

    @Override
    protected String getIdForFilter(PickerRow entry) {
        return entry.entry();
    }

    @Override
    protected String selectionValueOf(PickerRow entry) {
        return entry.entry();
    }

    @Override
    protected boolean onRowClicked(PickerRow entry, int index, double mouseX, int rowX) {
        if (!entry.hasChildren()) return false;
        int caretRight = rowX + 3 + entry.depth() * INDENT + CARET_W;
        if (mouseX >= caretRight) return false;
        if (!expanded.remove(entry.entry())) expanded.add(entry.entry());
        reloadEntriesKeepingScroll();
        return true;
    }

    @Override
    public void render(GuiGraphics g, Font font, int mouseX, int mouseY) {
        if (isVisible() && seenVersion != ClientCatalog.version()) reloadEntriesKeepingScroll();
        super.render(g, font, mouseX, mouseY);
    }

    @Override
    protected void renderRow(GuiGraphics g, Font font, PickerRow row, int x, int y, int w, int h,
                             boolean hovered, int rowIndex) {
        int textX = x + row.depth() * INDENT;
        if (row.hasChildren()) {
            g.drawString(font, row.expanded() ? "▾" : "▸", textX + 3, y + 4, 0x888888, false);
        }
        textX += CARET_W;
        int chipW = 12;
        drawRowMarqueeText(g, font, label(row), textX, y, w - (textX - x) - chipW - 22, h, hovered, rowIndex);
        if (row.kind() == PickerRow.Kind.MOD) {
            String count = String.valueOf(row.count());
            g.drawString(font, count, x + w - chipW - 6 - font.width(count), y + 4, 0x777777, false);
        }
        list.markFor(row.entry()).ifPresent(mark -> drawChip(g, font, mark, x + w - chipW - 2, y + 2, chipW, h - 4));
    }

    private String label(PickerRow row) {
        return switch (row.kind()) {
            case ALL -> "§6★§r " + Component.translatable("editor.hstg_permissions.picker.all").getString();
            case TYPED -> Component.translatable("editor.hstg_permissions.picker.typed", row.entry()).getString()
                    + (row.found() ? "" : " §e(" + Component.translatable("editor.hstg_permissions.picker.not_found").getString() + ")");
            case VALUE -> "§o" + row.label();
            case COMMAND -> row.aliasOf() == null ? row.label() : row.label() + " §8→ " + row.aliasOf();
            default -> row.label();
        };
    }

    private static void drawChip(GuiGraphics g, Font font, EntryList.Mark mark, int x, int y, int w, int h) {
        String tick = "✓";
        int tickX = x + (w - font.width(tick)) / 2;
        int tickY = y + (h - 8) / 2;
        if (mark == EntryList.Mark.INHERITED) {
            for (int dx = 0; dx < w; dx += 2) g.fill(x + dx, y + h - 1, x + dx + 1, y + h, 0xFF888888);
            g.drawString(font, tick, tickX, tickY, 0xFF888888, false);
            return;
        }
        g.fill(x, y, x + w, y + h, 0xFF2A2A2A);
        g.fill(x, y + h - 1, x + w, y + h, 0xFFFFCC00);
        g.drawString(font, tick, tickX, tickY, 0xFFFFCC00, false);
    }

    private PickerTree currentTree() {
        CommandCatalog catalog = ClientCatalog.get();
        if (catalog == null) return null;
        if (catalog != treeOf) {
            tree = new PickerTree(catalog);
            treeOf = catalog;
        }
        return tree;
    }

    private static List<PickerRow> waitingRows() {
        if (ClientCatalog.state() != ClientCatalog.State.UNAVAILABLE) {
            return List.of(PickerRow.info(Component.translatable("editor.hstg_permissions.picker.loading").getString()));
        }
        return List.of(
                PickerRow.info(Component.translatable("editor.hstg_permissions.picker.unavailable").getString()),
                PickerRow.info(Component.translatable("editor.hstg_permissions.picker.unavailable.hint").getString()));
    }
}
