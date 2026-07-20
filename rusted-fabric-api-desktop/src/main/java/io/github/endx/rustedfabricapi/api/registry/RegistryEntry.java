package io.github.endx.rustedfabricapi.api.registry;

import io.github.endx.rustedfabricapi.api.util.Identifier;

/** Stable entry assigned an insertion-ordered raw ID by a {@link ModRegistry}. */
public final class RegistryEntry<T> {
    private final RegistryKey<T> registryKey;
    private final Identifier id;
    private final int rawId;
    private final T value;

    RegistryEntry(RegistryKey<T> registryKey, Identifier id, int rawId, T value) {
        this.registryKey = registryKey;
        this.id = id;
        this.rawId = rawId;
        this.value = value;
    }

    public RegistryKey<T> registryKey() { return registryKey; }

    public Identifier id() { return id; }

    public int rawId() { return rawId; }

    public T value() { return value; }

    @Override
    public String toString() {
        return "RegistryEntry{" + registryKey.id() + '/' + id + " #" + rawId + '}';
    }
}
