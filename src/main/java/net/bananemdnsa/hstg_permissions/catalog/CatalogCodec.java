package net.bananemdnsa.hstg_permissions.catalog;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The catalog as bytes. Plain java.io, so the round trip is testable without the game. */
public final class CatalogCodec {

    private static final int VERSION = 1;

    private CatalogCodec() {}

    public static byte[] encode(CommandCatalog catalog) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(VERSION);
            out.writeInt(catalog.nodes().size());
            for (CommandCatalog.Node node : catalog.nodes()) {
                writeWords(out, node.path());
                out.writeBoolean(node.value());
            }
            writeMap(out, catalog.rootMods());
            writeMap(out, catalog.aliasTargets());
            out.writeInt(catalog.openValues().size());
            for (List<String> path : catalog.openValues()) writeWords(out, path);
            writeWords(out, catalog.protectedPaths());
            writeWords(out, List.copyOf(catalog.neverLockableMods()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    public static CommandCatalog decode(byte[] data) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            int version = in.readInt();
            if (version != VERSION) {
                throw new IllegalArgumentException("Command catalog version " + version + ", expected " + VERSION);
            }
            int count = in.readInt();
            List<CommandCatalog.Node> nodes = new ArrayList<>(count);
            for (int i = 0; i < count; i++) nodes.add(new CommandCatalog.Node(readWords(in), in.readBoolean()));
            Map<String, String> rootMods = readMap(in);
            Map<String, String> aliasTargets = readMap(in);
            int open = in.readInt();
            Set<List<String>> openValues = new HashSet<>();
            for (int i = 0; i < open; i++) openValues.add(readWords(in));
            List<String> protectedPaths = readWords(in);
            Set<String> neverLockable = Set.copyOf(readWords(in));
            return new CommandCatalog(nodes, rootMods, aliasTargets, openValues, protectedPaths, neverLockable);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void writeWords(DataOutputStream out, List<String> words) throws IOException {
        out.writeInt(words.size());
        for (String word : words) out.writeUTF(word);
    }

    private static List<String> readWords(DataInputStream in) throws IOException {
        int size = in.readInt();
        List<String> words = new ArrayList<>(size);
        for (int i = 0; i < size; i++) words.add(in.readUTF());
        return List.copyOf(words);
    }

    private static void writeMap(DataOutputStream out, Map<String, String> map) throws IOException {
        out.writeInt(map.size());
        for (Map.Entry<String, String> entry : map.entrySet()) {
            out.writeUTF(entry.getKey());
            out.writeUTF(entry.getValue());
        }
    }

    private static Map<String, String> readMap(DataInputStream in) throws IOException {
        int size = in.readInt();
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < size; i++) map.put(in.readUTF(), in.readUTF());
        return map;
    }
}
