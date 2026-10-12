package net.bananemdnsa.hstg_permissions.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class ProtectedListValueTest {

    @Test
    void entriesAreSplitTrimmedAndLowerCased() {
        assertEquals(List.of("op", "whitelist.add", "ban"), ProtectedListValue.split(" OP ;whitelist.add;;ban; "));
    }

    @Test
    void emptyAndNullAreNoEntries() {
        assertEquals(List.of(), ProtectedListValue.split(""));
        assertEquals(List.of(), ProtectedListValue.split(null));
    }

    @Test
    void duplicatesAreDroppedKeepingTheFirst() {
        assertEquals(List.of("op", "ban"), ProtectedListValue.split("op;ban;OP"));
    }

    @Test
    void joiningIsTheReverse() {
        assertEquals("op;deop", ProtectedListValue.join(List.of("op", "deop")));
        assertEquals(List.of("op", "deop"), ProtectedListValue.split(ProtectedListValue.join(List.of("op", "deop"))));
    }
}
