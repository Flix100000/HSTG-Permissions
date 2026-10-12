package net.bananemdnsa.hstg_permissions.rules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/** The protected list as the config editor carries it: entries joined by ';', as everywhere in History Stages. */
public final class ProtectedListValue {

    private ProtectedListValue() {}

    public static List<String> split(String value) {
        List<String> entries = new ArrayList<>();
        if (value == null) return entries;
        for (String part : value.split(";")) {
            String entry = part.trim().toLowerCase(Locale.ROOT);
            if (!entry.isEmpty() && !entries.contains(entry)) entries.add(entry);
        }
        return entries;
    }

    public static String join(Collection<String> entries) {
        return String.join(";", entries);
    }
}
