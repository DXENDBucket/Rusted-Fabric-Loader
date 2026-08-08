package io.github.endx.rustedfabricapi.api.networking;

import io.github.endx.rustedfabricapi.api.util.Identifier;

/** Common immutable codecs for small named-channel payloads. */
public final class PacketCodecs {
    public static final PacketCodec<String> UTF8 = PacketCodec.of(
            PacketBuffer::writeString, PacketBuffer::readString);
    public static final PacketCodec<byte[]> BYTE_ARRAY = PacketCodec.of(
            PacketBuffer::writeByteArray, PacketBuffer::readByteArray);
    public static final PacketCodec<Integer> VAR_INT = PacketCodec.of(
            (buffer, value) -> buffer.writeVarInt(value.intValue()), PacketBuffer::readVarInt);
    public static final PacketCodec<Long> VAR_LONG = PacketCodec.of(
            (buffer, value) -> buffer.writeVarLong(value.longValue()), PacketBuffer::readVarLong);
    public static final PacketCodec<Identifier> IDENTIFIER = PacketCodec.of(
            PacketBuffer::writeIdentifier, PacketBuffer::readIdentifier);

    private PacketCodecs() {
    }
}
