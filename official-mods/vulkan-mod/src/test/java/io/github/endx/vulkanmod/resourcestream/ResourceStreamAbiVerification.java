package io.github.endx.vulkanmod.resourcestream;

import io.github.endx.vulkanmod.framestream.FrameResourceHandle;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/** Golden and hostile-input contracts for the reliable ResourceStream envelope. */
public final class ResourceStreamAbiVerification {
    private ResourceStreamAbiVerification() { }

    public static void main(String[] arguments) {
        verifyGoldenStream();
        verifyCrcCompletionAndHandles();
        verifyCorruptionRejection();
        System.out.println("RustedVK ResourceStream ABI contracts passed");
    }

    private static void verifyGoldenStream() {
        ResourceStreamWriter writer = new ResourceStreamWriter(5L, 0, 0L)
                .record(ResourceStreamFormat.FLUSH, 0, 0L, hex("aabbccdd"));
        ByteBuffer target = ByteBuffer.allocateDirect(128).order(ByteOrder.BIG_ENDIAN);
        target.position(7);
        ByteBuffer encoded = writer.writeTo(target);
        require(target.position() == 95, "writer did not advance its target");
        byte[] expected = hex(
                "52564b52010000003000000058000000"
              + "05000000000000000100000000000000"
              + "00000000000000000000000000000000"
              + "09000000200000002800000000000000"
              + "05000000000000000000000000000000"
              + "aabbccdd00000000");
        require(Arrays.equals(expected, bytes(encoded)), "golden ResourceStream changed");

        target.position(7).limit(95);
        ResourceStreamReader reader = ResourceStreamReader.read(target);
        require(target.position() == 7, "reader changed caller position");
        require(reader.firstSequence() == 5L && reader.lastSequence() == 5L,
                "resource sequence changed");
        require(reader.recordCount() == 1
                        && reader.record(0).type() == ResourceStreamFormat.FLUSH,
                "resource record changed");
        require(Arrays.equals(bytes(reader.payload(0)), hex("aabbccdd00000000")),
                "resource payload/padding changed");
    }

    private static void verifyCrcCompletionAndHandles() {
        long texture = FrameResourceHandle.encode(FrameResourceHandle.TYPE_TEXTURE, 3, 19);
        ResourceStreamWriter writer = new ResourceStreamWriter(9L,
                ResourceStreamFormat.FLAG_HAS_PAYLOAD_CRC32
                        | ResourceStreamFormat.FLAG_REQUIRES_COMPLETION, 77L)
                .record(ResourceStreamFormat.TEXTURE_CREATE,
                        ResourceStreamFormat.RECORD_EXPECTS_RESULT,
                        texture, new byte[8]);
        byte[] encoded = bytes(writer.toDirectBuffer());
        ResourceStreamReader reader = ResourceStreamReader.read(ByteBuffer.wrap(encoded));
        require(reader.completionId() == 77L && reader.record(0).handle() == texture,
                "completion or typed handle changed");
        encoded[encoded.length - 1] ^= 1;
        expectInvalid("CRC32", encoded);

        try {
            new ResourceStreamWriter(0L, 0, 0L)
                    .record(ResourceStreamFormat.SHADER_PROGRAM_DESTROY, 0,
                            texture, new byte[0]);
            throw new AssertionError("wrong resource handle type was accepted");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    private static void verifyCorruptionRejection() {
        byte[] badSequence = canonicalBytes();
        littleEndian(badSequence).putLong(ResourceStreamFormat.HEADER_BYTES + 16, 8L);
        expectInvalid("sequence", badSequence);

        byte[] badLength = canonicalBytes();
        littleEndian(badLength).putInt(ResourceStreamFormat.HEADER_BYTES + 8, 39);
        expectInvalid("aligned", badLength);

        byte[] requiredUnknown = canonicalBytes();
        littleEndian(requiredUnknown).putShort(ResourceStreamFormat.HEADER_BYTES,
                (short) 0x8001);
        expectInvalid("unknown required", requiredUnknown);

        byte[] trailing = Arrays.copyOf(canonicalBytes(), canonicalBytes().length + 8);
        littleEndian(trailing).putInt(ResourceStreamFormat.OFFSET_TOTAL_BYTES, trailing.length);
        expectInvalid("trailing", trailing);
    }

    private static byte[] canonicalBytes() {
        return bytes(new ResourceStreamWriter(5L, 0, 0L)
                .record(ResourceStreamFormat.FLUSH, 0, 0L, hex("aabbccdd"))
                .toDirectBuffer());
    }

    private static void expectInvalid(String message, byte[] encoded) {
        try {
            ResourceStreamReader.read(ByteBuffer.wrap(encoded));
            throw new AssertionError("invalid ResourceStream was accepted: " + message);
        } catch (ResourceStreamFormatException expected) {
            require(expected.getMessage().contains(message),
                    "wrong rejection: " + expected.getMessage());
        }
    }

    private static byte[] bytes(ByteBuffer source) {
        ByteBuffer view = source.duplicate();
        byte[] result = new byte[view.remaining()];
        view.get(result);
        return result;
    }

    private static ByteBuffer littleEndian(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
    }

    private static byte[] hex(String value) {
        byte[] result = new byte[value.length() / 2];
        for (int index = 0; index < result.length; index++) {
            result[index] = (byte) Integer.parseInt(
                    value.substring(index * 2, index * 2 + 2), 16);
        }
        return result;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
