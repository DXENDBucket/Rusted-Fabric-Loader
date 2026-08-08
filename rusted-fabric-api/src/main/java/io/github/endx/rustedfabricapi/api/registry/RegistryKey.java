package io.github.endx.rustedfabricapi.api.registry;

import java.util.Objects;

import io.github.endx.rustedfabricapi.api.util.Identifier;

/** Runtime-checked identity of one mod-defined registry. */
public final class RegistryKey<T> {
    private final Identifier id;
    private final Class<T> valueType;

    private RegistryKey(Identifier id, Class<T> valueType) {
        this.id = id;
        this.valueType = valueType;
    }

    public static <T> RegistryKey<T> of(Identifier id, Class<T> valueType) {
        return new RegistryKey<T>(Objects.requireNonNull(id, "id"),
                Objects.requireNonNull(valueType, "valueType"));
    }

    public static <T> RegistryKey<T> of(String id, Class<T> valueType) {
        return of(Identifier.parse(id), valueType);
    }

    public Identifier id() { return id; }

    public Class<T> valueType() { return valueType; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof RegistryKey<?>)) return false;
        RegistryKey<?> key = (RegistryKey<?>) other;
        return id.equals(key.id) && valueType.equals(key.valueType);
    }

    @Override public int hashCode() { return 31 * id.hashCode() + valueType.hashCode(); }

    @Override public String toString() { return "RegistryKey{" + id + " -> " + valueType.getName() + '}'; }
}
