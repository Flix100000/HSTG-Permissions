package net.bananemdnsa.hstg_permissions.editor;

import org.jetbrains.annotations.Nullable;

/**
 * One row of the picker.
 *
 * @param entry   what picking the row stores: {@code *}, {@code mod.*} or {@code mod.word.word}
 * @param label   the last word, or the text of an info row
 * @param aliasOf for a top-level alias, the command it stands for
 * @param count   for a mod row, how many top-level commands it has
 * @param found   for a typed row, whether the server has the path
 * @param context shown only so a match can be placed; cannot be picked
 */
public record PickerRow(Kind kind, String entry, String label, int depth, boolean hasChildren,
                        boolean expanded, @Nullable String aliasOf, int count, boolean found, boolean context) {

    public enum Kind { ALL, MOD, COMMAND, VALUE, TYPED, INFO }

    public static PickerRow info(String text) {
        return new PickerRow(Kind.INFO, "", text, 0, false, false, null, 0, true, false);
    }
}
