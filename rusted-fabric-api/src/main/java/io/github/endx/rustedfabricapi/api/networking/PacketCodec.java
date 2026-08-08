package io.github.endx.rustedfabricapi.api.networking;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

/** Reusable typed payload codec for a named networking channel. */
public interface PacketCodec<T> {
    void encode(PacketBuffer buffer, T value);

    T decode(PacketBuffer buffer);

    default PacketPayload encodePayload(T value) {
        PacketBuffer buffer = PacketBuffer.writer();
        encode(buffer, value);
        return buffer.toPayload();
    }

    default T decodePayload(PacketPayload payload) {
        PacketBuffer buffer = PacketBuffer.reader(payload);
        T result = decode(buffer);
        buffer.requireFullyRead();
        return result;
    }

    static <T> PacketCodec<T> of(BiConsumer<PacketBuffer, T> encoder,
            Function<PacketBuffer, T> decoder) {
        Objects.requireNonNull(encoder, "encoder");
        Objects.requireNonNull(decoder, "decoder");
        return new PacketCodec<T>() {
            @Override
            public void encode(PacketBuffer buffer, T value) {
                encoder.accept(buffer, value);
            }

            @Override
            public T decode(PacketBuffer buffer) {
                return decoder.apply(buffer);
            }
        };
    }
}
