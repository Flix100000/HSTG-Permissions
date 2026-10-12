package net.bananemdnsa.hstg_permissions.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.bananemdnsa.hstg_permissions.rules.EntryPattern;
import net.bananemdnsa.hstg_permissions.rules.Mode;
import net.bananemdnsa.hstg_permissions.rules.PermissionEntry;

/**
 * The entries of one stage while the editor has it open.
 *
 * <p>Command and mode are kept exactly as read: a hand-edited entry the editor cannot read is shown
 * as broken and written back untouched, never dropped or "fixed" on save.
 */
public final class EntryList {

    public enum Change { ADDED, REMOVED, SWITCHED }

    /** How an entry reaches a row of the picker: named itself, or covered from above. */
    public record Mark(Mode mode, boolean inherited) {}

    private final List<String> commands = new ArrayList<>();
    private final List<String> modes = new ArrayList<>();

    public void load(List<PermissionEntry> entries) {
        commands.clear();
        modes.clear();
        for (PermissionEntry entry : entries) {
            commands.add(entry.command());
            modes.add(entry.mode());
        }
    }

    public List<PermissionEntry> toEntries() {
        List<PermissionEntry> entries = new ArrayList<>(commands.size());
        for (int i = 0; i < commands.size(); i++) entries.add(entryAt(i));
        return entries;
    }

    /** Live: the editor indexes into it and copies from it. */
    public List<String> commands() {
        return commands;
    }

    public int size() {
        return commands.size();
    }

    public PermissionEntry entryAt(int index) {
        return new PermissionEntry(commands.get(index), modes.get(index));
    }

    public Optional<Mode> mode(int index) {
        return Mode.parse(modes.get(index));
    }

    public void setMode(int index, Mode mode) {
        modes.set(index, mode.id());
    }

    /**
     * Sets the mode a click asked for. False when nothing changed, so a click on the half that is
     * already chosen does not mark the stage dirty; an unreadable mode always counts as a change.
     */
    public boolean switchTo(int index, Mode mode) {
        if (mode(index).orElse(null) == mode) return false;
        setMode(index, mode);
        return true;
    }

    public void removeAt(int index) {
        if (index < 0 || index >= commands.size()) return;
        commands.remove(index);
        modes.remove(index);
    }

    /** What a click in the picker does: add, take out again in the same mode, or switch the mode. */
    public Change pick(String command, Mode mode) {
        int index = indexOfSame(command);
        if (index < 0) {
            commands.add(command);
            modes.add(mode.id());
            return Change.ADDED;
        }
        if (mode(index).orElse(null) == mode) {
            removeAt(index);
            return Change.REMOVED;
        }
        setMode(index, mode);
        return Change.SWITCHED;
    }

    public Optional<Mark> markFor(String rowEntry) {
        Optional<EntryPattern> row = EntryPattern.parse(rowEntry);
        if (row.isEmpty()) return Optional.empty();
        Mark inherited = null;
        int inheritedSpecificity = -1;
        for (int i = 0; i < commands.size(); i++) {
            Optional<EntryPattern> entry = EntryPattern.parse(commands.get(i));
            Optional<Mode> mode = mode(i);
            if (entry.isEmpty() || mode.isEmpty()) continue;
            if (sameTarget(entry.get(), row.get())) return Optional.of(new Mark(mode.get(), false));
            if (covers(entry.get(), row.get()) && entry.get().specificity() > inheritedSpecificity) {
                inherited = new Mark(mode.get(), true);
                inheritedSpecificity = entry.get().specificity();
            }
        }
        return Optional.ofNullable(inherited);
    }

    public int count(Mode mode) {
        int count = 0;
        for (int i = 0; i < commands.size(); i++) {
            if (mode(i).orElse(null) == mode) count++;
        }
        return count;
    }

    private int indexOfSame(String command) {
        Optional<EntryPattern> wanted = EntryPattern.parse(command);
        for (int i = 0; i < commands.size(); i++) {
            Optional<EntryPattern> have = EntryPattern.parse(commands.get(i));
            if (wanted.isPresent() ? have.isPresent() && sameTarget(have.get(), wanted.get())
                    : commands.get(i).equals(command)) {
                return i;
            }
        }
        return -1;
    }

    // A concrete path ignores its mod when checked, so two paths with the same words are one entry.
    private static boolean sameTarget(EntryPattern a, EntryPattern b) {
        if (a.kind() != b.kind()) return false;
        return switch (a.kind()) {
            case ALL -> true;
            case MOD -> a.mod().equals(b.mod());
            case PATH -> a.segments().equals(b.segments());
        };
    }

    private static boolean covers(EntryPattern above, EntryPattern below) {
        return switch (above.kind()) {
            case ALL -> below.kind() != EntryPattern.Kind.ALL;
            case MOD -> below.kind() == EntryPattern.Kind.PATH && above.mod().equals(below.mod());
            case PATH -> below.kind() == EntryPattern.Kind.PATH
                    && below.segments().size() > above.segments().size()
                    && below.segments().subList(0, above.segments().size()).equals(above.segments());
        };
    }
}
