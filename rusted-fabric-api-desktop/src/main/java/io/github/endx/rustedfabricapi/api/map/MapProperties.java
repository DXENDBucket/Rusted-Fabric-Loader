package io.github.endx.rustedfabricapi.api.map;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.TreeMap;

/** Immutable string properties copied from a TMX group or object. */
public final class MapProperties {
    private final Map<String, String> values;

    public MapProperties(Map<String, String> values) {
        TreeMap<String, String> sorted = new TreeMap<String, String>();
        if (values != null) {
            for (Map.Entry<String, String> entry : values.entrySet()) {
                sorted.put(Objects.requireNonNull(entry.getKey(), "property key"),
                        Objects.requireNonNull(entry.getValue(), "property value"));
            }
        }
        this.values = Collections.unmodifiableMap(
                new LinkedHashMap<String, String>(sorted));
    }

    public static MapProperties empty() {
        return new MapProperties(Collections.emptyMap());
    }

    public int size() { return values.size(); }
    public boolean isEmpty() { return values.isEmpty(); }
    public boolean contains(String key) {
        return values.containsKey(Objects.requireNonNull(key, "key"));
    }
    public Optional<String> get(String key) {
        return Optional.ofNullable(values.get(Objects.requireNonNull(key, "key")));
    }
    public String getOrDefault(String key, String fallback) {
        return values.getOrDefault(Objects.requireNonNull(key, "key"),
                Objects.requireNonNull(fallback, "fallback"));
    }
    public Map<String, String> asMap() { return values; }

    public OptionalInt integer(String key) {
        String value = values.get(Objects.requireNonNull(key, "key"));
        if (value == null) return OptionalInt.empty();
        try {
            return OptionalInt.of(Integer.parseInt(value.trim()));
        } catch (NumberFormatException ignored) {
            return OptionalInt.empty();
        }
    }

    public OptionalDouble decimal(String key) {
        String value = values.get(Objects.requireNonNull(key, "key"));
        if (value == null) return OptionalDouble.empty();
        try {
            double parsed = Double.parseDouble(value.trim());
            return Double.isFinite(parsed) ? OptionalDouble.of(parsed) : OptionalDouble.empty();
        } catch (NumberFormatException ignored) {
            return OptionalDouble.empty();
        }
    }

    public Optional<Boolean> flag(String key) {
        String value = values.get(Objects.requireNonNull(key, "key"));
        if (value == null) return Optional.empty();
        switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "true":
            case "yes":
            case "on":
            case "1":
                return Optional.of(Boolean.TRUE);
            case "false":
            case "no":
            case "off":
            case "0":
                return Optional.of(Boolean.FALSE);
            default:
                return Optional.empty();
        }
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof MapProperties
                && values.equals(((MapProperties) other).values);
    }

    @Override
    public int hashCode() { return values.hashCode(); }

    @Override
    public String toString() { return values.toString(); }
}
