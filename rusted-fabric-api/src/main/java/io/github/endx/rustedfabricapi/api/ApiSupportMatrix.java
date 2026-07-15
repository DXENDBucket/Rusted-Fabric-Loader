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
import java.util.Locale;
import java.util.Map;

/** Machine-readable expected backend coverage; runtime capability checks remain authoritative. */
public final class ApiSupportMatrix {
    public enum Backend { WINDOWS, ANDROID_LOCAL_PATCH, ANDROID_XPOSED }
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
        Backend backend = context.androidRuntime()
                ? (context.hasCapability("platform.android.xposed")
                ? Backend.ANDROID_XPOSED : Backend.ANDROID_LOCAL_PATCH)
                : Backend.WINDOWS;
        return expectedSupport(capability, backend) != Level.UNAVAILABLE;
    }

    private static List<Entry> load() {
        InputStream input = ApiSupportMatrix.class.getResourceAsStream(RESOURCE);
        if (input == null) throw new IllegalStateException("Missing API support matrix " + RESOURCE);
        List<Entry> entries = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                input, StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (!"capability,apiClass,windows,androidLocalPatch,androidXposed".equals(header)) {
                throw new IllegalStateException("Unsupported API support matrix schema");
            }
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("#")) continue;
                String[] fields = line.split(",", -1);
                if (fields.length != 5) throw new IllegalStateException("Malformed API support row");
                entries.add(new Entry(fields[0], fields[1], parse(fields[2]),
                        parse(fields[3]), parse(fields[4])));
            }
        } catch (IOException failure) {
            throw new IllegalStateException("Could not read API support matrix", failure);
        }
        return Collections.unmodifiableList(entries);
    }

    private static Level parse(String value) {
        return Level.valueOf(value.toUpperCase(Locale.ROOT));
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
        private final Level windows;
        private final Level androidLocalPatch;
        private final Level androidXposed;

        private Entry(String capability, String apiClass, Level windows,
                Level androidLocalPatch, Level androidXposed) {
            this.capability = capability;
            this.apiClass = apiClass;
            this.windows = windows;
            this.androidLocalPatch = androidLocalPatch;
            this.androidXposed = androidXposed;
        }

        public String capability() { return capability; }
        public String apiClass() { return apiClass; }
        public Level windows() { return windows; }
        public Level androidLocalPatch() { return androidLocalPatch; }
        public Level androidXposed() { return androidXposed; }

        public Level support(Backend backend) {
            if (backend == Backend.WINDOWS) return windows;
            if (backend == Backend.ANDROID_LOCAL_PATCH) return androidLocalPatch;
            return androidXposed;
        }
    }
}
