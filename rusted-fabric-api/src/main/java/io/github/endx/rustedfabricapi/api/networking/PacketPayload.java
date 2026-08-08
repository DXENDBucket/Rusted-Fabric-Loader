package io.github.endx.rustedfabricapi.api.networking;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/** Immutable, binary-safe payload passed to named-channel receivers. */
public final class PacketPayload {
    public static final int MAX_BYTES = 256 * 1024;
    private final byte[] bytes;

    private PacketPayload(byte[] bytes) {
        this.bytes = bytes;
    }

    public static PacketPayload of(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        if (bytes.length > MAX_BYTES) {
            throw new IllegalArgumentException("Payload exceeds " + MAX_BYTES + " bytes");
        }
        return new PacketPayload(Arrays.copyOf(bytes, bytes.length));
    }

    public static PacketPayload utf8(String text) {
        return of(Objects.requireNonNull(text, "text").getBytes(StandardCharsets.UTF_8));
    }

    public int size() {
        return bytes.length;
    }

    public boolean isEmpty() {
        return bytes.length == 0;
    }

    public byte[] copyBytes() {
        return Arrays.copyOf(bytes, bytes.length);
    }

    public ByteBuffer asReadOnlyBuffer() {
        return ByteBuffer.wrap(bytes).asReadOnlyBuffer();
    }

    public String utf8() {
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
