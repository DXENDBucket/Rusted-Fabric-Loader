package io.github.endx.rustedfabricapi.api.networking;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

import io.github.endx.rustedfabricapi.api.util.Identifier;

/**
 * Bounded packet reader/writer for named channels, similar to Fabric's packet byte buffers.
 * A buffer is either a writer or a reader; mixing modes fails immediately.
 */
public final class PacketBuffer {
    public static final int DEFAULT_MAX_STRING_CHARS = 32_767;

    private final ByteArrayOutputStream outputBytes;
    private final DataOutputStream output;
    private final ByteArrayInputStream inputBytes;
    private final DataInputStream input;

    private PacketBuffer(ByteArrayOutputStream outputBytes, DataOutputStream output,
            ByteArrayInputStream inputBytes, DataInputStream input) {
        this.outputBytes = outputBytes;
        this.output = output;
        this.inputBytes = inputBytes;
        this.input = input;
    }

    public static PacketBuffer writer() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        return new PacketBuffer(bytes, new DataOutputStream(bytes), null, null);
    }

    public static PacketBuffer reader(PacketPayload payload) {
        Objects.requireNonNull(payload, "payload");
        ByteArrayInputStream bytes = new ByteArrayInputStream(payload.copyBytes());
        return new PacketBuffer(null, null, bytes, new DataInputStream(bytes));
    }

    public boolean isWriting() {
        return output != null;
    }

    public boolean isReading() {
        return input != null;
    }

    public int size() {
        return isWriting() ? outputBytes.size() : inputBytes.available();
    }

    public int readableBytes() {
        requireReader();
        return inputBytes.available();
    }

    public PacketPayload toPayload() {
        requireWriter();
        checkSize();
        return PacketPayload.of(outputBytes.toByteArray());
    }

    public void requireFullyRead() {
        requireReader();
        if (inputBytes.available() != 0) {
            throw new IllegalArgumentException("Packet has " + inputBytes.available()
                    + " unread trailing bytes");
        }
    }

    public PacketBuffer writeBoolean(boolean value) {
        write(stream -> stream.writeBoolean(value));
        return this;
    }

    public boolean readBoolean() {
        return read(DataInputStream::readBoolean);
    }

    public PacketBuffer writeByte(int value) {
        write(stream -> stream.writeByte(value));
        return this;
    }

    public byte readByte() {
        return read(DataInputStream::readByte);
    }

    public int readUnsignedByte() {
        return read(DataInputStream::readUnsignedByte);
    }

    public PacketBuffer writeShort(int value) {
        write(stream -> stream.writeShort(value));
        return this;
    }

    public short readShort() {
        return read(DataInputStream::readShort);
    }

    public int readUnsignedShort() {
        return read(DataInputStream::readUnsignedShort);
    }

    public PacketBuffer writeInt(int value) {
        write(stream -> stream.writeInt(value));
        return this;
    }

    public int readInt() {
        return read(DataInputStream::readInt);
    }

    public PacketBuffer writeLong(long value) {
        write(stream -> stream.writeLong(value));
        return this;
    }

    public long readLong() {
        return read(DataInputStream::readLong);
    }

    public PacketBuffer writeFloat(float value) {
        write(stream -> stream.writeFloat(value));
        return this;
    }

    public float readFloat() {
        return read(DataInputStream::readFloat);
    }

    public PacketBuffer writeDouble(double value) {
        write(stream -> stream.writeDouble(value));
        return this;
    }

    public double readDouble() {
        return read(DataInputStream::readDouble);
    }

    public PacketBuffer writeVarInt(int value) {
        int current = value;
        while ((current & ~0x7F) != 0) {
            writeByte((current & 0x7F) | 0x80);
            current >>>= 7;
        }
        return writeByte(current);
    }

    public int readVarInt() {
        int result = 0;
        for (int index = 0; index < 5; index++) {
            int current = readUnsignedByte();
            result |= (current & 0x7F) << (index * 7);
            if ((current & 0x80) == 0) return result;
        }
        throw new IllegalArgumentException("VarInt exceeds 5 bytes");
    }

    public PacketBuffer writeVarLong(long value) {
        long current = value;
        while ((current & ~0x7FL) != 0L) {
            writeByte((int) (current & 0x7F) | 0x80);
            current >>>= 7;
        }
        return writeByte((int) current);
    }

    public long readVarLong() {
        long result = 0L;
        for (int index = 0; index < 10; index++) {
            int current = readUnsignedByte();
            result |= (long) (current & 0x7F) << (index * 7);
            if ((current & 0x80) == 0) return result;
        }
        throw new IllegalArgumentException("VarLong exceeds 10 bytes");
    }

    public PacketBuffer writeBytes(byte[] value) {
        Objects.requireNonNull(value, "value");
        if (value.length > PacketPayload.MAX_BYTES) {
            throw new IllegalArgumentException("Byte array exceeds packet payload limit");
        }
        write(stream -> stream.write(value));
        return this;
    }

    public byte[] readBytes(int length) {
        if (length < 0 || length > PacketPayload.MAX_BYTES || length > readableBytes()) {
            throw new IllegalArgumentException("Invalid byte array length: " + length);
        }
        byte[] result = new byte[length];
        read(stream -> {
            stream.readFully(result);
            return null;
        });
        return result;
    }

    public PacketBuffer writeByteArray(byte[] value) {
        Objects.requireNonNull(value, "value");
        if (value.length > PacketPayload.MAX_BYTES) {
            throw new IllegalArgumentException("Byte array exceeds packet payload limit");
        }
        writeVarInt(value.length);
        return writeBytes(value);
    }

    public byte[] readByteArray() {
        return readBytes(readVarInt());
    }

    public PacketBuffer writeString(String value) {
        return writeString(value, DEFAULT_MAX_STRING_CHARS);
    }

    public PacketBuffer writeString(String value, int maxChars) {
        Objects.requireNonNull(value, "value");
        requirePositive(maxChars, "maxChars");
        if (value.length() > maxChars) {
            throw new IllegalArgumentException("String exceeds " + maxChars + " characters");
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > maxEncodedStringBytes(maxChars)) {
            throw new IllegalArgumentException("Encoded string is too large");
        }
        writeVarInt(bytes.length);
        return writeBytes(bytes);
    }

    public String readString() {
        return readString(DEFAULT_MAX_STRING_CHARS);
    }

    public String readString(int maxChars) {
        requirePositive(maxChars, "maxChars");
        int encodedLength = readVarInt();
        if (encodedLength < 0 || encodedLength > maxEncodedStringBytes(maxChars)
                || encodedLength > readableBytes()) {
            throw new IllegalArgumentException("Invalid encoded string length: " + encodedLength);
        }
        try {
            String result = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(readBytes(encodedLength))).toString();
            if (result.length() > maxChars) {
                throw new IllegalArgumentException("Decoded string exceeds " + maxChars + " characters");
            }
            return result;
        } catch (CharacterCodingException failure) {
            throw new IllegalArgumentException("String is not valid UTF-8", failure);
        }
    }

    public PacketBuffer writeChannelId(ChannelId value) {
        return writeString(Objects.requireNonNull(value, "value").toString(),
                ChannelId.MAX_ENCODED_BYTES);
    }

    public ChannelId readChannelId() {
        return ChannelId.parse(readString(ChannelId.MAX_ENCODED_BYTES));
    }

    public PacketBuffer writeIdentifier(Identifier value) {
        return writeString(Objects.requireNonNull(value, "value").toString(),
                Identifier.MAX_ENCODED_BYTES);
    }

    public Identifier readIdentifier() {
        return Identifier.parse(readString(Identifier.MAX_ENCODED_BYTES));
    }

    public PacketBuffer writeUuid(UUID value) {
        Objects.requireNonNull(value, "value");
        return writeLong(value.getMostSignificantBits()).writeLong(value.getLeastSignificantBits());
    }

    public UUID readUuid() {
        return new UUID(readLong(), readLong());
    }

    private int maxEncodedStringBytes(int maxChars) {
        long bytes = (long) maxChars * 3L;
        return (int) Math.min(bytes, PacketPayload.MAX_BYTES);
    }

    private void write(IoWriter writer) {
        requireWriter();
        try {
            writer.write(output);
            checkSize();
        } catch (IOException impossible) {
            throw new IllegalStateException("Could not write in-memory packet", impossible);
        }
    }

    private <T> T read(IoReader<T> reader) {
        requireReader();
        try {
            return reader.read(input);
        } catch (EOFException failure) {
            throw new IllegalArgumentException("Packet ended unexpectedly", failure);
        } catch (IOException failure) {
            throw new IllegalArgumentException("Could not read packet", failure);
        }
    }

    private void checkSize() {
        if (outputBytes.size() > PacketPayload.MAX_BYTES) {
            throw new IllegalArgumentException("Packet exceeds " + PacketPayload.MAX_BYTES + " bytes");
        }
    }

    private void requireWriter() {
        if (!isWriting()) throw new IllegalStateException("PacketBuffer is in read mode");
    }

    private void requireReader() {
        if (!isReading()) throw new IllegalStateException("PacketBuffer is in write mode");
    }

    private static void requirePositive(int value, String name) {
        if (value < 0) throw new IllegalArgumentException(name + " must not be negative");
    }

    @FunctionalInterface
    private interface IoWriter {
        void write(DataOutputStream output) throws IOException;
    }

    @FunctionalInterface
    private interface IoReader<T> {
        T read(DataInputStream input) throws IOException;
    }
}
