package io.github.endx.rustedfabricapi.api.data;

import io.github.endx.rustedfabricapi.api.util.Identifier;

import java.util.Objects;

/** Registered, namespaced and versioned key for global or per-unit persistent data. */
public final class PersistentDataKey<T> {
    private final Identifier id;
    private final int version;
    private final PersistentDataCodec<T> codec;

    PersistentDataKey(Identifier id, int version, PersistentDataCodec<T> codec) {
        this.id = Objects.requireNonNull(id, "id");
        if (version < 0) throw new IllegalArgumentException("version must not be negative");
        this.version = version;
        this.codec = Objects.requireNonNull(codec, "codec");
    }

    public Identifier id() { return id; }
    public int version() { return version; }
    public PersistentDataCodec<T> codec() { return codec; }

    @Override
    public String toString() {
        return "PersistentDataKey{" + id + "@" + version + '}';
    }
}
