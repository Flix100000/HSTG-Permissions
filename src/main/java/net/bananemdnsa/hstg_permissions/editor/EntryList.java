package net.bananemdnsa.hstg_permissions.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

import net.bananemdnsa.hstg_permissions.rules.EntryPattern;
import net.bananemdnsa.hstg_permissions.rules.PermissionEntry;

/**
 * The entries of one stage while the editor has it open.
 *
 * <p>Commands are kept exactly as read: a hand-edited entry the editor cannot read is shown as
 * broken and written back untouched, never dropped or "fixed" on save.
 */
public final class EntryList {

    public enum Change { ADDED, REMOVED }

    /** How an entry reaches a row of the picker: named itself, or covered from above. */
    public enum Mark { DIRECT, INHERITED }

    private final List<String> commands = new ArrayList<>();

    private UnaryOperator<List<String>> canonical = UnaryOperator.identity();

    /** Makes spellings of one command (tp, teleport) compare equal; stored entries are never rewritten. */
    public void setSpellings(UnaryOperator<List<String>> canonical) {
        this.canonical = canonical;
    }

    public void load(List<PermissionEntry> entries) {
        commands.clear();
        for (PermissionEntry entry : entries) commands.add(entry.command());
    }

    public List<PermissionEntry> toEntries() {
        List<PermissionEntry> entries = new ArrayList<>(commands.size());
        for (String command : commands) entries.add(new PermissionEntry(command));
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
        return new PermissionEntry(commands.get(index));
    }

    public void removeAt(int index) {
        if (index < 0 || index >= commands.size()) return;
        commands.remove(index);
    }

    /** What a click in the picker does: add the command, or take it out again if it is already there. */
    public Change pick(String command) {
        int index = indexOfSame(command);
        if (index < 0) {
            commands.add(command);
            return Change.ADDED;
        }
        removeAt(index);
        return Change.REMOVED;
    }

    public Optional<Mark> markFor(String rowEntry) {
        Optional<EntryPattern> row = EntryPattern.parse(rowEntry);
        if (row.isEmpty()) return Optional.empty();
        boolean inherited = false;
        for (String command : commands) {
            Optional<EntryPattern> entry = EntryPattern.parse(command);
            if (entry.isEmpty()) continue;
            if (sameTarget(entry.get(), row.get())) return Optional.of(Mark.DIRECT);
            if (covers(entry.get(), row.get())) inherited = true;
        }
        return inherited ? Optional.of(Mark.INHERITED) : Optional.empty();
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
    private boolean sameTarget(EntryPattern a, EntryPattern b) {
        if (a.kind() != b.kind()) return false;
        return switch (a.kind()) {
            case ALL -> true;
            case MOD -> a.mod().equals(b.mod());
            case PATH -> canonical.apply(a.segments()).equals(canonical.apply(b.segments()));
        };
    }

    private boolean covers(EntryPattern above, EntryPattern below) {
        return switch (above.kind()) {
            case ALL -> below.kind() != EntryPattern.Kind.ALL;
            case MOD -> below.kind() == EntryPattern.Kind.PATH && above.mod().equals(below.mod());
            case PATH -> below.kind() == EntryPattern.Kind.PATH && isBelow(canonical.apply(above.segments()),
                    canonical.apply(below.segments()));
        };
    }

    private static boolean isBelow(List<String> above, List<String> below) {
        return below.size() > above.size() && below.subList(0, above.size()).equals(above);
    }
}
