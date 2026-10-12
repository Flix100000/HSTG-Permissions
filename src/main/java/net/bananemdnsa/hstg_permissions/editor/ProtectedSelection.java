package net.bananemdnsa.hstg_permissions.editor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

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

    public State state(List<String> path) {
        String key = String.join(".", path);
        if (entries.contains(key)) return State.ON;
        for (int size = 1; size < path.size(); size++) {
            if (entries.contains(String.join(".", path.subList(0, size)))) return State.COVERED;
        }
        return State.OFF;
    }

    public void toggle(List<String> path) {
        switch (state(path)) {
            case ON -> entries.remove(String.join(".", path));
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
            String key = String.join(".", root);
            if (all) entries.remove(key);
            else if (!entries.contains(key)) entries.add(key);
        }
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
