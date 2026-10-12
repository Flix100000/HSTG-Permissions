package net.bananemdnsa.hstg_permissions.editor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import net.bananemdnsa.hstg_permissions.rules.ProtectionRules;

/**
 * The protected list while its window is open. An entry protects itself and everything below it,
 * so a row under a protected command shows as covered and cannot be ticked on its own.
 */
public final class ProtectedSelection {

    public enum State { ON, COVERED, OFF }

    public enum ModState { ALL, SOME, NONE }

    private final List<String> entries = new ArrayList<>();

    public ProtectedSelection(Collection<String> entries) {
        this.entries.addAll(entries);
    }

    public List<String> entries() {
        return List.copyOf(entries);
    }

    private UnaryOperator<List<String>> canonical = UnaryOperator.identity();

    /** Makes spellings of one command (tp, teleport) compare equal; stored entries are never rewritten. */
    public void setSpellings(UnaryOperator<List<String>> canonical) {
        this.canonical = canonical;
    }

    public State state(List<String> path) {
        List<String> wanted = canonical.apply(path);
        if (indexOf(wanted) >= 0) return State.ON;
        for (int size = 1; size < wanted.size(); size++) {
            if (indexOf(wanted.subList(0, size)) >= 0) return State.COVERED;
        }
        return State.OFF;
    }

    public void toggle(List<String> path) {
        switch (state(path)) {
            case ON -> entries.remove(indexOf(canonical.apply(path)));
            case OFF -> entries.add(String.join(".", path));
            case COVERED -> { }
        }
    }

    public ModState modState(List<List<String>> roots) {
        int on = 0;
        for (List<String> root : roots) if (state(root) == State.ON) on++;
        if (on == 0) return ModState.NONE;
        return on == roots.size() ? ModState.ALL : ModState.SOME;
    }

    public void toggleMod(List<List<String>> roots) {
        boolean all = modState(roots) == ModState.ALL;
        for (List<String> root : roots) {
            int index = indexOf(canonical.apply(root));
            if (all) entries.remove(index);
            else if (index < 0) entries.add(String.join(".", root));
        }
    }

    // The stored entry whose canonical form is this path, or -1.
    private int indexOf(List<String> canonicalPath) {
        for (int i = 0; i < entries.size(); i++) {
            if (canonical.apply(Arrays.asList(entries.get(i).split("\\."))).equals(canonicalPath)) return i;
        }
        return -1;
    }

    /** Entries the server does not know right now – a mod may be missing – so they are shown, not lost. */
    public List<String> missing(Predicate<List<String>> exists) {
        List<String> missing = new ArrayList<>();
        for (String entry : entries) {
            if (!exists.test(Arrays.asList(entry.split("\\.")))) missing.add(entry);
        }
        return missing;
    }

    public void reset() {
        entries.clear();
        entries.addAll(ProtectionRules.DEFAULT_PROTECTED);
    }
}
