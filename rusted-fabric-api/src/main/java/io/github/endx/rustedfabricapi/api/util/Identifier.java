package io.github.endx.rustedfabricapi.api.util;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.regex.Pattern;

/** General Fabric-style {@code namespace:path} identifier. */
public final class Identifier implements Comparable<Identifier> {
    public static final int MAX_ENCODED_BYTES = 128;
    private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern PATH = Pattern.compile("[a-z0-9/._-]+");

    private final String namespace;
    private final String path;
    private final String value;

    private Identifier(String namespace, String path) {
        this.namespace = namespace;
        this.path = path;
        this.value = namespace + ':' + path;
    }

    public static Identifier of(String namespace, String path) {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(path, "path");
        if (!NAMESPACE.matcher(namespace).matches()) {
            throw new IllegalArgumentException("Invalid identifier namespace: " + namespace);
        }
        if (!PATH.matcher(path).matches()) {
            throw new IllegalArgumentException("Invalid identifier path: " + path);
        }
        Identifier result = new Identifier(namespace, path);
        if (result.value.getBytes(StandardCharsets.UTF_8).length > MAX_ENCODED_BYTES) {
            throw new IllegalArgumentException("Identifier exceeds "
                    + MAX_ENCODED_BYTES + " UTF-8 bytes");
        }
        return result;
    }

    public static Identifier parse(String value) {
        Objects.requireNonNull(value, "value");
        int separator = value.indexOf(':');
        if (separator <= 0 || separator == value.length() - 1
                || value.indexOf(':', separator + 1) >= 0) {
            throw new IllegalArgumentException("Identifier must use namespace:path: " + value);
        }
        return of(value.substring(0, separator), value.substring(separator + 1));
    }

    public String namespace() { return namespace; }
    public String path() { return path; }

    @Override
    public int compareTo(Identifier other) {
        return value.compareTo(Objects.requireNonNull(other, "other").value);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof Identifier
                && value.equals(((Identifier) other).value);
    }

    @Override
    public int hashCode() { return value.hashCode(); }

    @Override
    public String toString() { return value; }
}
