package net.bananemdnsa.hstg_permissions.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import net.bananemdnsa.historystages.api.editor.widget.AbstractSearchableList;
import net.bananemdnsa.historystages.api.editor.widget.Checkbox;
import net.bananemdnsa.historystages.api.editor.widget.SearchBar;
import net.bananemdnsa.hstg_permissions.catalog.CommandCatalog;
import net.bananemdnsa.hstg_permissions.editor.PickerRow;
import net.bananemdnsa.hstg_permissions.editor.PickerTree;
import net.bananemdnsa.hstg_permissions.editor.ProtectedSelection;
import net.bananemdnsa.hstg_permissions.editor.TypedPath;
import net.bananemdnsa.hstg_permissions.rules.EntryPattern;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * The protected list in the same window as the "+" picker, with a tick box on every row. Rows
 * never pick anything: a click ticks, and the list only leaves through "Done".
 *
 * <p>Entries the tree has no row for are listed as {@link PickerRow.Kind#TYPED} rows, keyed by
 * their stored form, so they can still be seen and unticked.
 */
public final class ProtectedListPicker extends AbstractSearchableList<PickerRow> {

    private static final int INDENT = 10;
    private static final int CARET_W = 10;

    private final ProtectedSelection selection;
    private final Runnable onDone;
    private final Set<String> expanded = new HashSet<>();
    private final Map<String, Checkbox.State> checks = new HashMap<>();
    // Once shown, a loose entry keeps its row after being unticked, so it can be ticked again.
    private final Set<String> loose = new LinkedHashSet<>();

    private long seenVersion = -1;
    private PickerTree tree;
    private CommandCatalog treeOf;
    private Map<String, List<List<String>>> rootsByMod = Map.of();
    private Set<List<String>> nodePaths = Set.of();

    public ProtectedListPicker(ProtectedSelection selection, Runnable onDone) {
        super(Component.translatable("config.hstg_permissions.protected.search").getString(), value -> { }, null);
        this.selection = selection;
        this.onDone = onDone;
        // Esc after a search should only leave the search, not throw away every tick.
        searchBar.setEscapeReleasesFocus(true);
    }

    @Override
    protected List<FooterButton> footerButtons() {
        return List.of(
                new FooterButton(Component.translatable("config.hstg_permissions.protected.default").getString(), () -> {
                    selection.reset();
                    reloadEntriesKeepingScroll();
                }),
                new FooterButton(Component.translatable("config.hstg_permissions.protected.done").getString(), onDone));
    }

    @Override
    protected void configureFilters(SearchBar bar) {
    }

    @Override
    protected int getPanelWidth() {
        return 300;
    }

    // ============ Rows ============

    @Override
    protected List<PickerRow> loadEntries() {
        seenVersion = ClientCatalog.version();
        return List.of();
    }

    @Override
    protected boolean matchesQuery(PickerRow entry, String lowerCaseQuery) {
        return entry.entry().contains(lowerCaseQuery)
                || entry.label().toLowerCase(Locale.ROOT).contains(lowerCaseQuery);
    }

    @Override
    protected void afterFilter(List<PickerRow> filtered) {
        filtered.clear();
        String raw = searchBar.getText() == null ? "" : searchBar.getText();
        String query = TypedPath.normalizeQuery(raw);
        CommandCatalog catalog = ClientCatalog.get();
        if (catalog == null) {
            String info = ClientCatalog.state() == ClientCatalog.State.UNAVAILABLE
                    ? "editor.hstg_permissions.picker.unavailable"
                    : "editor.hstg_permissions.picker.loading";
            filtered.add(PickerRow.info(Component.translatable(info).getString()));
            loose.addAll(selection.entries());
            // Nothing to check them against, so none is marked as missing either.
            addLoose(filtered, query, entry -> true, entry -> true);
            return;
        }

        PickerTree current = currentTree(catalog);
        // Context rows stay tickable: here each one is a real command or mod, and without them a
        // search for "add" would show bare "add" rows with nothing to tell them apart.
        for (PickerRow row : current.rows(expanded, raw)) {
            if (row.kind() == PickerRow.Kind.MOD || row.kind() == PickerRow.Kind.COMMAND
                    || row.kind() == PickerRow.Kind.VALUE) {
                filtered.add(row);
            }
        }
        // Every entry the tree has no row for still gets one. Only what the server does not know
        // at all is marked as missing.
        Predicate<List<String>> exists = path -> catalog.exists(new EntryPattern(EntryPattern.Kind.PATH, null, path));
        loose.addAll(selection.missing(nodePaths::contains));
        addLoose(filtered, query, entry -> !nodePaths.contains(pathOf(entry)), entry -> exists.test(pathOf(entry)));
    }

    private void addLoose(List<PickerRow> out, String query, Predicate<String> show, Predicate<String> known) {
        // A mod filter cannot place an entry the tree has no row for, so it hides them.
        if (query.startsWith("@")) return;
        for (String entry : loose) {
            if (!show.test(entry) || !entry.contains(query)) continue;
            out.add(new PickerRow(PickerRow.Kind.TYPED, entry, entry, 0, false, false, null, 0, known.test(entry), false));
        }
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

    private PickerTree currentTree(CommandCatalog catalog) {
        if (catalog != treeOf) {
            tree = new PickerTree(catalog);
            treeOf = catalog;
            Map<String, List<List<String>>> roots = new HashMap<>();
            Set<List<String>> paths = new HashSet<>();
            for (CommandCatalog.Node node : catalog.nodes()) {
                paths.add(node.path());
                if (node.path().size() != 1) continue;
                roots.computeIfAbsent(catalog.modOfRoot(node.path().get(0)), mod -> new ArrayList<>()).add(node.path());
            }
            rootsByMod = roots;
            nodePaths = paths;
        }
        return tree;
    }

    private static List<String> pathOf(String entry) {
        return List.of(entry.split("\\."));
    }

    /** The stored form of a row: tree rows carry their mod in front, which the list leaves off. */
    private static List<String> pathOf(PickerRow row) {
        if (row.kind() == PickerRow.Kind.TYPED) return pathOf(row.entry());
        return pathOf(row.entry().substring(row.entry().indexOf('.') + 1));
    }

    private static String keyOf(PickerRow row) {
        // A loose entry and a tree row could spell the same string, and must not share a box.
        return row.kind() == PickerRow.Kind.TYPED ? "loose:" + row.entry() : row.entry();
    }

    // ============ Ticks ============

    private List<List<String>> rootsOf(PickerRow modRow) {
        return rootsByMod.getOrDefault(modRow.label(), List.of());
    }

    private int protectedIn(String mod) {
        CommandCatalog catalog = treeOf;
        if (catalog == null) return 0;
        int count = 0;
        for (String entry : selection.entries()) {
            if (catalog.modOfRoot(entry.split("\\.")[0]).equals(mod)) count++;
        }
        return count;
    }

    private Checkbox.Mark markOf(PickerRow row) {
        if (row.kind() == PickerRow.Kind.MOD) {
            return switch (selection.modState(rootsOf(row))) {
                case ALL -> Checkbox.Mark.ON;
                case SOME -> Checkbox.Mark.PARTIAL;
                case NONE -> Checkbox.Mark.OFF;
            };
        }
        return selection.state(pathOf(row)) == ProtectedSelection.State.OFF ? Checkbox.Mark.OFF : Checkbox.Mark.ON;
    }

    private boolean covered(PickerRow row) {
        return row.kind() != PickerRow.Kind.MOD && selection.state(pathOf(row)) == ProtectedSelection.State.COVERED;
    }

    @Override
    protected boolean onRowClicked(PickerRow entry, int index, double mouseX, int rowX) {
        int caretRight = rowX + 3 + entry.depth() * INDENT + CARET_W;
        if (entry.hasChildren() && mouseX < caretRight) {
            if (!expanded.remove(entry.entry())) expanded.add(entry.entry());
        } else if (entry.kind() == PickerRow.Kind.MOD) {
            selection.toggleMod(rootsOf(entry));
        } else {
            selection.toggle(pathOf(entry));
        }
        reloadEntriesKeepingScroll();
        // Never falls through: a row here is ticked, not picked, and picking would close the window.
        return true;
    }

    // ============ Rendering ============

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
        int boxX = x + w - Checkbox.SIZE - 4;
        int textRight = boxX - 6;
        if (row.kind() == PickerRow.Kind.MOD) {
            String count = String.valueOf(protectedIn(row.label()));
            textRight -= font.width(count) + 4;
            g.drawString(font, count, boxX - 6 - font.width(count), y + 4, 0x777777, false);
        }
        drawRowMarqueeText(g, font, label(row), textX, y, textRight - textX, h, hovered, rowIndex);

        Checkbox.State state = checks.computeIfAbsent(keyOf(row), key -> new Checkbox.State());
        Checkbox.Mark mark = markOf(row);
        state.update(mark);
        Checkbox.draw(g, boxX, y + (h - Checkbox.SIZE) / 2, mark, state, covered(row));
    }

    private static String label(PickerRow row) {
        return switch (row.kind()) {
            case VALUE -> "§o" + row.label();
            case COMMAND -> row.aliasOf() == null ? row.label() : row.label() + " §8→ " + row.aliasOf();
            case TYPED -> row.found() ? row.entry() : "§e⚠ " + row.entry();
            default -> row.label();
        };
    }

    // ============ Input ============

    @Override
    public boolean keyPressed(int keyCode) {
        // ENTER after typing a search ends the search, not the window.
        if (isVisible() && (keyCode == 257 || keyCode == 335)
                && searchBar.isFocused() && searchBar.getText() != null && !searchBar.getText().isEmpty()) {
            searchBar.setFocused(false);
            return true;
        }
        return super.keyPressed(keyCode);
    }
}
