package net.bananemdnsa.hstg_permissions.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import net.bananemdnsa.historystages.api.editor.TabInputContext;
import net.bananemdnsa.historystages.api.editor.TabRenderContext;
import net.bananemdnsa.historystages.api.editor.widget.AbstractModalScreen;
import net.bananemdnsa.historystages.api.editor.widget.Checkbox;
import net.bananemdnsa.historystages.api.editor.widget.EditorRowList;
import net.bananemdnsa.historystages.api.editor.widget.SearchBar;
import net.bananemdnsa.hstg_permissions.catalog.CommandCatalog;
import net.bananemdnsa.hstg_permissions.editor.PickerRow;
import net.bananemdnsa.hstg_permissions.editor.PickerTree;
import net.bananemdnsa.hstg_permissions.editor.ProtectedSelection;
import net.bananemdnsa.hstg_permissions.editor.TypedPath;
import net.bananemdnsa.hstg_permissions.rules.EntryPattern;
import net.bananemdnsa.hstg_permissions.rules.ProtectedListValue;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The protected list as the server's command tree with a tick box on every row. Opened from the
 * config editor; the list is only stored when "Done" hands it back.
 */
public final class ProtectedListScreen extends AbstractModalScreen {

    private static final int VISIBLE_ROWS = 9;
    private static final int MIN_ROWS = 3;
    private static final int GAP = 4;
    private static final int INDENT = 10;
    private static final int SCROLLBAR_W = 4;
    private static final String OPEN = "▾ ";
    private static final String CLOSED = "▸ ";

    private enum Kind { MOD, NODE, LOOSE, INFO }

    /**
     * One drawn row. {@code path} is the stored form without a mod; a loose row is an entry the
     * tree cannot place – unknown to the server, or no tree at all.
     */
    private record Line(Kind kind, String key, String label, int depth, boolean hasChildren, boolean open,
                        @Nullable List<String> path, @Nullable String mod) {}

    private final Consumer<String> onDone;
    private final ProtectedSelection selection;
    private final SearchBar searchBar;
    private final EditorRowList toolbar = new EditorRowList().flat();
    private final EditorRowList list = new EditorRowList();
    private final Set<String> expanded = new HashSet<>();
    private final Map<String, Checkbox.State> checks = new HashMap<>();
    // Once shown, a loose entry keeps its row after being unticked, so it can be ticked again.
    private final Set<String> loose = new LinkedHashSet<>();

    private List<Line> lines = List.of();
    private boolean dirty = true;
    private long seenVersion = -1;
    private PickerTree tree;
    private CommandCatalog treeOf;
    private Map<String, List<List<String>>> rootsByMod = Map.of();
    private int visibleRows = VISIBLE_ROWS;
    private int scroll;

    public ProtectedListScreen(Screen parent, String currentValue, Consumer<String> onDone) {
        super(parent, Component.translatable("config.hstg_permissions.protected.title"));
        this.onDone = onDone;
        this.selection = new ProtectedSelection(ProtectedListValue.split(currentValue));
        this.searchBar = new SearchBar(Component.translatable("editor.hstg_permissions.picker.search").getString())
                .setEscapeReleasesFocus(true)
                .onChange(text -> {
                    scroll = 0;
                    dirty = true;
                });
        CatalogRequester.request();
    }

    @Override
    protected Component titleText() {
        return Component.translatable("config.hstg_permissions.protected.title");
    }

    @Override
    protected Component subtitle() {
        return Component.translatable("config.hstg_permissions.protected.subtitle");
    }

    @Override
    protected int dialogWidth() {
        return 320;
    }

    @Override
    protected int contentHeight() {
        // Nine rows do not fit a small window at a large GUI scale; fewer rows beat a cut-off dialog.
        int chrome = TITLE_H + SUBTITLE_H + PAD * 4 + BUTTON_H + topHeight();
        int fit = (this.height - chrome) / rowStep();
        visibleRows = Math.max(MIN_ROWS, Math.min(VISIBLE_ROWS, fit));
        return topHeight() + visibleRows * rowStep();
    }

    @Override
    protected void onConfirm() {
        onDone.accept(ProtectedListValue.join(selection.entries()));
        this.minecraft.setScreen(parent);
    }

    // ============ Layout ============

    private int topHeight() {
        return SearchBar.HEIGHT + GAP + toolbar.rowHeight() + GAP;
    }

    private int rowStep() {
        return list.rowHeight() + EditorRowList.CARD_GAP;
    }

    private int contentX() {
        return boxX + PAD;
    }

    private int contentW() {
        return boxW - PAD * 2;
    }

    private int toolbarY() {
        return contentY + SearchBar.HEIGHT + GAP;
    }

    private int listY() {
        return contentY + topHeight();
    }

    private int listH() {
        return visibleRows * rowStep();
    }

    private int listW() {
        return contentW() - SCROLLBAR_W - 2;
    }

    private int maxScroll() {
        return Math.max(0, list.heightForRows(lines.size()) - listH());
    }

    private TabInputContext toolbarInput(double mouseX, double mouseY) {
        int y = toolbarY();
        return new TabInputContext(contentX(), y, contentW(), y, y + toolbar.rowHeight(), mouseX, mouseY);
    }

    private TabInputContext listInput(double mouseX, double mouseY) {
        return new TabInputContext(contentX(), listY() - scroll, listW(), listY(), listY() + listH(), mouseX, mouseY);
    }

    // ============ Rows ============

    private void refresh() {
        if (seenVersion != ClientCatalog.version()) {
            seenVersion = ClientCatalog.version();
            dirty = true;
        }
        if (!dirty) return;
        dirty = false;
        lines = buildLines();
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
    }

    private List<Line> buildLines() {
        List<Line> out = new ArrayList<>();
        String query = TypedPath.normalizeQuery(searchBar.getText());
        CommandCatalog catalog = ClientCatalog.get();
        if (catalog == null) {
            String info = ClientCatalog.state() == ClientCatalog.State.UNAVAILABLE
                    ? "editor.hstg_permissions.picker.unavailable"
                    : "editor.hstg_permissions.picker.loading";
            out.add(new Line(Kind.INFO, "info", Component.translatable(info).getString(), 0, false, false, null, null));
            loose.addAll(selection.entries());
            addLoose(out, query, entry -> true);
            return out;
        }

        PickerTree current = currentTree(catalog);
        for (PickerRow row : current.rows(expanded, searchBar.getText())) {
            if (row.context()) continue;
            switch (row.kind()) {
                case MOD -> out.add(new Line(Kind.MOD, row.entry(), row.label(), 0, row.hasChildren(),
                        row.expanded(), null, row.label()));
                case COMMAND, VALUE -> {
                    String stored = row.entry().substring(row.entry().indexOf('.') + 1);
                    out.add(new Line(Kind.NODE, row.entry(), label(row), row.depth(), row.hasChildren(),
                            row.expanded(), List.of(stored.split("\\.")), null));
                }
                default -> { }
            }
        }
        Predicate<List<String>> exists = path -> catalog.exists(new EntryPattern(EntryPattern.Kind.PATH, null, path));
        loose.addAll(selection.missing(exists));
        addLoose(out, query, entry -> !exists.test(List.of(entry.split("\\."))));
        return out;
    }

    private void addLoose(List<Line> out, String query, Predicate<String> show) {
        // A mod filter cannot place an entry the server does not know, so it hides them.
        if (query.startsWith("@")) return;
        for (String entry : loose) {
            if (!show.test(entry) || !entry.contains(query)) continue;
            out.add(new Line(Kind.LOOSE, "loose:" + entry, "§e⚠ " + entry, 0, false, false,
                    List.of(entry.split("\\.")), null));
        }
    }

    private static String label(PickerRow row) {
        if (row.kind() == PickerRow.Kind.VALUE) return "§o" + row.label();
        return row.aliasOf() == null ? row.label() : row.label() + " §8→ " + row.aliasOf();
    }

    private PickerTree currentTree(CommandCatalog catalog) {
        if (catalog != treeOf) {
            tree = new PickerTree(catalog);
            treeOf = catalog;
            Map<String, List<List<String>>> roots = new HashMap<>();
            for (CommandCatalog.Node node : catalog.nodes()) {
                if (node.path().size() != 1) continue;
                roots.computeIfAbsent(catalog.modOfRoot(node.path().get(0)), mod -> new ArrayList<>()).add(node.path());
            }
            rootsByMod = roots;
        }
        return tree;
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

    private Checkbox.Mark markOf(Line line) {
        if (line.kind() == Kind.MOD) {
            return switch (selection.modState(rootsByMod.getOrDefault(line.mod(), List.of()))) {
                case ALL -> Checkbox.Mark.ON;
                case SOME -> Checkbox.Mark.PARTIAL;
                case NONE -> Checkbox.Mark.OFF;
            };
        }
        return selection.state(line.path()) == ProtectedSelection.State.OFF ? Checkbox.Mark.OFF : Checkbox.Mark.ON;
    }

    private boolean covered(Line line) {
        return line.path() != null && selection.state(line.path()) == ProtectedSelection.State.COVERED;
    }

    private String text(Line line) {
        String caret = line.hasChildren() ? (line.open() ? OPEN : CLOSED) : "";
        if (line.kind() == Kind.MOD) return caret + line.label() + " §7" + protectedIn(line.mod());
        return caret + line.label();
    }

    private int indentOf(Line line) {
        // Rows without a caret move over by its width, so names line up under their siblings.
        return line.depth() * INDENT + (line.hasChildren() || line.kind() == Kind.INFO ? 0 : this.font.width(CLOSED));
    }

    // ============ Rendering ============

    @Override
    protected void renderContent(GuiGraphics g, int x, int y, int w, int mouseX, int mouseY) {
        refresh();

        searchBar.setPosition(x, y, w);
        searchBar.render(g, this.font, mouseX, mouseY);

        int barY = toolbarY();
        String count = Component.translatable("config.hstg_permissions.protected.count",
                selection.entries().size()).getString();
        String reset = Component.translatable("config.hstg_permissions.protected.default").getString();
        toolbar.render(context(g, x, barY, w, barY, barY + toolbar.rowHeight(), mouseX, mouseY), 1,
                (row, index) -> row.text(count).button(reset, () -> {
                    selection.reset();
                    dirty = true;
                }));

        int top = listY();
        int bottom = top + listH();
        g.enableScissor(x, top, x + w, bottom);
        list.render(context(g, x, top - scroll, listW(), top, bottom, mouseX, mouseY), lines.size(), (row, index) -> {
            Line line = lines.get(index);
            row.indent(indentOf(line)).text(text(line));
            if (line.kind() == Kind.INFO) return;
            Checkbox.State state = checks.computeIfAbsent(line.key(), key -> new Checkbox.State());
            Checkbox.Mark mark = markOf(line);
            state.update(mark);
            boolean dimmed = covered(line);
            row.leading(Checkbox.SIZE + 2, (gg, px, py, pw, ph) ->
                    Checkbox.draw(gg, px, py + (ph - Checkbox.SIZE) / 2, mark, state, dimmed));
        });
        g.disableScissor();

        int max = maxScroll();
        if (max > 0) {
            int trackX = x + w - SCROLLBAR_W;
            int thumbH = Math.max(12, listH() * listH() / (listH() + max));
            int thumbY = top + (listH() - thumbH) * scroll / max;
            g.fill(trackX, top, trackX + SCROLLBAR_W, bottom, 0x40FFFFFF);
            g.fill(trackX, thumbY, trackX + SCROLLBAR_W, thumbY + thumbH, 0xFF888888);
        }
    }

    private TabRenderContext context(GuiGraphics g, int x, int y, int w, int clipTop, int clipBottom,
                                     int mouseX, int mouseY) {
        // No tooltips here, and setTooltipForNextRenderPass is not ours to call, so the sink stays empty.
        return new TabRenderContext(g, this.font, x, y, w, clipTop, clipBottom, mouseX, mouseY, false, (key, text) -> { });
    }

    // ============ Input ============

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isOpenSettled()) return true;
        if (searchBar.mouseClicked(mouseX, mouseY)) return true;
        if (button == 0) {
            if (toolbar.mouseClicked(toolbarInput(mouseX, mouseY))) return true;
            int index = list.rowAt(listInput(mouseX, mouseY), lines.size());
            if (index >= 0 && index < lines.size()) {
                clickRow(lines.get(index), mouseX);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void clickRow(Line line, double mouseX) {
        if (line.kind() == Kind.INFO) return;
        if (line.hasChildren()) {
            // Where EditorRowList puts the text: after the leading box and its 6px gap.
            int caretX = contentX() + indentOf(line) + Checkbox.SIZE + 2 + 6;
            if (mouseX >= caretX - 2 && mouseX < caretX + this.font.width(CLOSED)) {
                if (!expanded.remove(line.key())) expanded.add(line.key());
                dirty = true;
                return;
            }
        }
        if (line.kind() == Kind.MOD) selection.toggleMod(rootsByMod.getOrDefault(line.mod(), List.of()));
        else selection.toggle(line.path());
        dirty = true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        refresh();
        scroll = (int) Math.max(0, Math.min(maxScroll(), scroll - scrollY * rowStep()));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // ESC on a focused bar only drops the focus; the second ESC closes the window.
        if (searchBar.keyPressed(keyCode)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (searchBar.charTyped(codePoint)) return true;
        return super.charTyped(codePoint, modifiers);
    }
}
