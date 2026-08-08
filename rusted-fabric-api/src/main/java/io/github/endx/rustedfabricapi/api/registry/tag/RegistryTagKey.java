package io.github.endx.rustedfabricapi.api.registry.tag;

import java.util.Objects;

import io.github.endx.rustedfabricapi.api.registry.RegistryKey;
import io.github.endx.rustedfabricapi.api.util.Identifier;

/** Type-safe identity of one tag inside a specific mod registry. */
public final class RegistryTagKey<T> {
    private final RegistryKey<T> registry;
    private final Identifier id;

    private RegistryTagKey(RegistryKey<T> registry, Identifier id) {
        this.registry = registry;
        this.id = id;
    }

    public static <T> RegistryTagKey<T> of(RegistryKey<T> registry, Identifier id) {
        return new RegistryTagKey<T>(Objects.requireNonNull(registry, "registry"),
                Objects.requireNonNull(id, "id"));
    }

    public static <T> RegistryTagKey<T> of(RegistryKey<T> registry, String id) {
        return of(registry, Identifier.parse(id));
    }

    public RegistryKey<T> registry() { return registry; }

    public Identifier id() { return id; }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof RegistryTagKey<?>)) return false;
        RegistryTagKey<?> key = (RegistryTagKey<?>) other;
        return registry.equals(key.registry) && id.equals(key.id);
    }

    @Override public int hashCode() { return 31 * registry.hashCode() + id.hashCode(); }

    @Override public String toString() { return "RegistryTagKey{" + registry.id() + " #" + id + '}'; }
}
