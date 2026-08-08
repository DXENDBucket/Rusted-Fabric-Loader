package io.github.endx.rustedfabricapi.api.data;

import io.github.endx.rustedfabricapi.api.networking.PacketBuffer;
import io.github.endx.rustedfabricapi.api.networking.PacketCodec;

import java.util.Objects;

/** Version-aware codec for a persistent data component. */
public interface PersistentDataCodec<T> {
    void encode(PacketBuffer buffer, T value);

    /** Decodes data written with {@code storedVersion}; implementations may migrate older forms. */
    T decode(PacketBuffer buffer, int storedVersion);

    static <T> PersistentDataCodec<T> of(PacketCodec<T> codec) {
        Objects.requireNonNull(codec, "codec");
        return new PersistentDataCodec<T>() {
            @Override
            public void encode(PacketBuffer buffer, T value) {
                codec.encode(buffer, value);
            }

            @Override
            public T decode(PacketBuffer buffer, int storedVersion) {
                return codec.decode(buffer);
            }
        };
    }
}
