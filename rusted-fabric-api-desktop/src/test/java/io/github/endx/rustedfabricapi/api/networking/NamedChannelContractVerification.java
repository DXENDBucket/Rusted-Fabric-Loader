package io.github.endx.rustedfabricapi.api.networking;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.UUID;

public final class NamedChannelContractVerification {
    private NamedChannelContractVerification() {
    }

    public static void verify() {
        verifyIdentifiersAndPayloadCopies();
        verifyEnvelopeRoundTripAndCorruption();
        verifyPacketBufferAndCodec();
        verifyReceiverRegistration();
    }

    private static void verifyPacketBufferAndCodec() {
        UUID uuid = UUID.fromString("01234567-89ab-cdef-0123-456789abcdef");
        PacketBuffer writer = PacketBuffer.writer()
                .writeBoolean(true)
                .writeInt(-17)
                .writeVarInt(300)
                .writeVarInt(-1)
                .writeVarLong(9_876_543_210L)
                .writeFloat(2.5F)
                .writeString("铁锈 Fabric")
                .writeChannelId(ChannelId.of("example", "buffer"))
                .writeUuid(uuid)
                .writeByteArray(new byte[] {4, 5, 6});
        PacketBuffer reader = PacketBuffer.reader(writer.toPayload());
        require(reader.readBoolean(), "packet boolean did not round-trip");
        require(reader.readInt() == -17, "packet int did not round-trip");
        require(reader.readVarInt() == 300 && reader.readVarInt() == -1,
                "packet VarInt did not round-trip");
        require(reader.readVarLong() == 9_876_543_210L,
                "packet VarLong did not round-trip");
        require(Float.compare(reader.readFloat(), 2.5F) == 0,
                "packet float did not round-trip");
        require(reader.readString().equals("铁锈 Fabric"),
                "packet UTF-8 string did not round-trip");
        require(reader.readChannelId().equals(ChannelId.of("example", "buffer")),
                "packet channel id did not round-trip");
        require(reader.readUuid().equals(uuid), "packet UUID did not round-trip");
        require(Arrays.equals(reader.readByteArray(), new byte[] {4, 5, 6}),
                "packet byte array did not round-trip");
        reader.requireFullyRead();

        PacketCodec<String> codec = PacketCodec.of(
                (buffer, value) -> buffer.writeString(value).writeVarInt(value.length()),
                buffer -> {
                    String value = buffer.readString();
                    require(buffer.readVarInt() == value.length(), "typed codec marker mismatch");
                    return value;
                });
        require(codec.decodePayload(codec.encodePayload("typed")).equals("typed"),
                "typed packet codec did not round-trip");
        expectFailure(() -> PacketCodecs.UTF8.decodePayload(
                        PacketBuffer.writer().writeString("ok").writeByte(1).toPayload()),
                "typed codec accepted trailing bytes");
        expectFailure(() -> PacketBuffer.reader(PacketPayload.of(new byte[] {(byte) 0x80,
                        (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, 0})).readVarInt(),
                "oversized VarInt was accepted");
    }

    private static void verifyIdentifiersAndPayloadCopies() {
        ChannelId id = ChannelId.parse("example:unit/state");
        require(id.namespace().equals("example") && id.path().equals("unit/state"),
                "channel identifier did not round-trip");
        expectFailure(() -> ChannelId.parse("Example:state"),
                "uppercase channel identifier was accepted");

        byte[] source = new byte[] {1, 2, 3};
        PacketPayload payload = PacketPayload.of(source);
        source[0] = 9;
        byte[] copy = payload.copyBytes();
        require(copy[0] == 1, "payload retained caller-owned input bytes");
        copy[1] = 9;
        require(payload.copyBytes()[1] == 2, "payload exposed mutable internal bytes");
    }

    private static void verifyEnvelopeRoundTripAndCorruption() {
        ChannelId id = ChannelId.of("example", "sync");
        byte[] encoded = NamedChannelTransport.encode(id, PacketPayload.utf8("hello"));
        NamedChannelTransport.Decoded decoded = NamedChannelTransport.decode(encoded);
        require(decoded.channel.equals(id) && decoded.payload.utf8().equals("hello"),
                "named payload envelope did not round-trip");

        byte[] corrupt = Arrays.copyOf(encoded, encoded.length);
        corrupt[corrupt.length - 1] ^= 1;
        expectFailure(() -> NamedChannelTransport.decode(corrupt),
                "corrupt named payload passed checksum validation");
    }

    private static void verifyReceiverRegistration() {
        ChannelId id = ChannelId.of("example", "receiver");
        AtomicInteger calls = new AtomicInteger();
        RustedFabricEvent.Registration registration = ClientNetworking.registerGlobalReceiver(
                id, (engine, connection, channel, payload) -> calls.incrementAndGet());
        ClientNetworking.receive(null, null, id, PacketPayload.of(new byte[0]));
        require(calls.get() == 1, "client receiver was not called");
        expectFailure(() -> ClientNetworking.registerGlobalReceiver(id,
                        (engine, connection, channel, payload) -> { }),
                "duplicate client receiver was accepted");
        require(registration.unregister(), "receiver registration was not removed");
        require(!registration.unregister(), "receiver removal was not idempotent");
        ClientNetworking.receive(null, null, id, PacketPayload.of(new byte[0]));
        require(calls.get() == 1, "removed client receiver was called");
    }

    private static void expectFailure(Runnable action, String message) {
        try {
            action.run();
        } catch (IllegalArgumentException | IllegalStateException expected) {
            return;
        }
        throw new AssertionError(message);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
