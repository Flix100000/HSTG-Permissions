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
import net.bananemdnsa.hstg_permissions.rules.EntryPattern;
import net.bananemdnsa.hstg_permissions.rules.Mode;
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
    private Mode mode = Mode.LOCK;
    private long seenVersion = -1;
    private PickerTree tree;
    private CommandCatalog treeOf;

    public CommandPicker(EntryList list, Consumer<String> onSelect) {
        super(Component.translatable("editor.hstg_permissions.picker.search").getString(), onSelect, list::commands);
        this.list = list;
        setMultiSelect(true);
    }

    /** What the confirmed selection becomes: the last of the two own tabs that was open. */
    public Mode mode() {
        return mode;
    }

    @Override
    protected List<String> ownTabLabels() {
        return List.of(Component.translatable("editor.hstg_permissions.mode.lock").getString(),
                Component.translatable("editor.hstg_permissions.mode.grant").getString());
    }

    @Override
    protected void onOwnTabChanged(int index) {
        mode = index == 1 ? Mode.GRANT : Mode.LOCK;
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
        if (current == null) return List.of(PickerRow.info(waitingText()));
        return current.rows(expanded, "");
    }

    @Override
    protected boolean matchesQuery(PickerRow entry, String lowerCaseQuery) {
        return true; // the tree decides in afterFilter, because a match needs its parents shown too
    }

    @Override
    protected void afterFilter(List<PickerRow> filtered) {
        PickerTree current = currentTree();
        String query = searchBar.getText() == null ? "" : searchBar.getText();
        filtered.clear();
        if (current == null) {
            String typed = query.trim().toLowerCase(Locale.ROOT);
            if (EntryPattern.parse(typed).isPresent()) {
                // Nothing to check it against, so it is not marked as missing either.
                filtered.add(new PickerRow(PickerRow.Kind.TYPED, typed, typed, 0, false, false, null, 0, true));
            }
            filtered.add(PickerRow.info(waitingText()));
            return;
        }
        filtered.addAll(current.rows(expanded, query));
    }

    @Override
    protected boolean isHeaderRow(int index) {
        return index < filteredEntries.size() && filteredEntries.get(index).kind() == PickerRow.Kind.INFO;
    }

    @Override
    protected void renderHeaderRow(GuiGraphics g, Font font, PickerRow entry, int x, int y, int w, int h) {
        drawRowText(g, font, entry.label(), x, y, w, false);
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

    // Drawn by the addon, so red and green are fine here: they say lock and grant, not "wrong".
    private static void drawChip(GuiGraphics g, Font font, EntryList.Mark mark, int x, int y, int w, int h) {
        boolean lock = mark.mode() == Mode.LOCK;
        int line = lock ? 0xFFE05555 : 0xFF55C060;
        String letter = Component.translatable(lock ? "editor.hstg_permissions.mode.lock.short"
                : "editor.hstg_permissions.mode.grant.short").getString();
        if (mark.inherited()) {
            for (int dx = 0; dx < w; dx += 2) g.fill(x + dx, y + h - 1, x + dx + 1, y + h, line);
            g.drawString(font, letter, x + (w - font.width(letter)) / 2, y + (h - 8) / 2, line, false);
            return;
        }
        g.fill(x, y, x + w, y + h, lock ? 0xFF4A2222 : 0xFF22402A);
        g.fill(x, y + h - 1, x + w, y + h, line);
        g.drawString(font, letter, x + (w - font.width(letter)) / 2, y + (h - 8) / 2, 0xFFFFFFFF, false);
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

    private static String waitingText() {
        return Component.translatable(ClientCatalog.state() == ClientCatalog.State.UNAVAILABLE
                ? "editor.hstg_permissions.picker.unavailable"
                : "editor.hstg_permissions.picker.loading").getString();
    }
}
