package io.github.endx.rustedfabricapi.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Machine-readable API capability catalog; runtime capability checks remain authoritative. */
public final class ApiSupportMatrix {
    public enum Backend { RUNTIME }
    public enum Level { FULL, PARTIAL, UNAVAILABLE }

    private static final String RESOURCE = "/rustedfabricapi/api-support-matrix.csv";
    private static final List<Entry> ENTRIES = load();
    private static final Map<String, Entry> BY_CAPABILITY = index(ENTRIES);

    private ApiSupportMatrix() {
    }

    public static List<Entry> entries() {
        return ENTRIES;
    }

    public static Entry require(String capability) {
        Entry entry = BY_CAPABILITY.get(capability);
        if (entry == null) throw new IllegalArgumentException("Unknown API capability: " + capability);
        return entry;
    }

    public static Level expectedSupport(String capability, Backend backend) {
        return require(capability).support(backend);
    }

    /** Expected matrix support plus the actual capability advertised by this runtime. */
    public static boolean available(RustedFabricAPIContext context, String capability) {
        if (context == null || !context.hasCapability(capability)) return false;
        return expectedSupport(capability, Backend.RUNTIME) != Level.UNAVAILABLE;
    }

    private static List<Entry> load() {
        InputStream input = ApiSupportMatrix.class.getResourceAsStream(RESOURCE);
        if (input == null) throw new IllegalStateException("Missing API support matrix " + RESOURCE);
        List<Entry> entries = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                input, StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (!"capability,apiClass".equals(header)) {
                throw new IllegalStateException("Unsupported API support matrix schema");
            }
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("#")) continue;
                String[] fields = line.split(",", -1);
                if (fields.length != 2) throw new IllegalStateException("Malformed API capability row");
                entries.add(new Entry(fields[0], fields[1]));
            }
        } catch (IOException failure) {
            throw new IllegalStateException("Could not read API support matrix", failure);
        }
        return Collections.unmodifiableList(entries);
    }

    private static Map<String, Entry> index(List<Entry> entries) {
        Map<String, Entry> result = new LinkedHashMap<>();
        for (Entry entry : entries) {
            if (result.put(entry.capability(), entry) != null) {
                throw new IllegalStateException("Duplicate API capability " + entry.capability());
            }
        }
        return Collections.unmodifiableMap(result);
    }

    public static final class Entry {
        private final String capability;
        private final String apiClass;
        private final Level runtime;

        private Entry(String capability, String apiClass) {
            this.capability = capability;
            this.apiClass = apiClass;
            this.runtime = Level.FULL;
        }

        public String capability() { return capability; }
        public String apiClass() { return apiClass; }
        public Level runtime() { return runtime; }

        public Level support(Backend backend) {
            if (backend == null) throw new IllegalArgumentException("backend must not be null");
            return runtime;
        }
    }
}
