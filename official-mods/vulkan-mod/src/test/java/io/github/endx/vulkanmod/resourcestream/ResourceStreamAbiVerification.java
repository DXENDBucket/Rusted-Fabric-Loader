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
        verifyTypedTexturePayloads();
        verifyHandleLifetimeAndSequenceClock();
        verifyCorruptionRejection();
        System.out.println("RustedVK ResourceStream ABI contracts passed");
    }

    private static void verifyGoldenStream() {
        ResourceStreamWriter writer = new ResourceStreamWriter(5L, 0, 0L)
                .record(100, 0, 0L, hex("aabbccdd"));
        ByteBuffer target = ByteBuffer.allocateDirect(128).order(ByteOrder.BIG_ENDIAN);
        target.position(7);
        ByteBuffer encoded = writer.writeTo(target);
        require(target.position() == 95, "writer did not advance its target");
        byte[] expected = hex(
                "52564b52010000003000000058000000"
              + "05000000000000000100000000000000"
              + "00000000000000000000000000000000"
              + "64000000200000002800000000000000"
              + "05000000000000000000000000000000"
              + "aabbccdd00000000");
        require(Arrays.equals(expected, bytes(encoded)), "golden ResourceStream changed");

        target.position(7).limit(95);
        ResourceStreamReader reader = ResourceStreamReader.read(target);
        require(target.position() == 7, "reader changed caller position");
        require(reader.firstSequence() == 5L && reader.lastSequence() == 5L,
                "resource sequence changed");
        require(reader.recordCount() == 1
                        && reader.record(0).type() == 100,
                "resource record changed");
        require(Arrays.equals(bytes(reader.payload(0)), hex("aabbccdd00000000")),
                "resource payload/padding changed");
    }

    private static void verifyCrcCompletionAndHandles() {
        long texture = FrameResourceHandle.encode(FrameResourceHandle.TYPE_TEXTURE, 3, 19);
        ResourceStreamWriter writer = new ResourceStreamWriter(9L,
                ResourceStreamFormat.FLAG_HAS_PAYLOAD_CRC32
                        | ResourceStreamFormat.FLAG_REQUIRES_COMPLETION, 77L);
        ResourceStreamRecords.textureReadback(writer, texture, 0, 0, 1, 1,
                ResourceStreamFormat.FORMAT_RGBA8_UNORM);
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

        long texture = FrameResourceHandle.encode(FrameResourceHandle.TYPE_TEXTURE, 1, 7);
        byte[] malformedTransfer = bytes(new ResourceStreamWriter(12L, 0, 0L)
                .record(ResourceStreamFormat.TEXTURE_UPLOAD, 0, texture, new byte[48])
                .toDirectBuffer());
        expectInvalid("dimensions", malformedTransfer);
    }

    private static void verifyTypedTexturePayloads() {
        long texture = FrameResourceHandle.encode(FrameResourceHandle.TYPE_TEXTURE, 4, 23);
        long shader = FrameResourceHandle.encode(FrameResourceHandle.TYPE_SHADER_PROGRAM, 2, 8);
        byte[] pixels = hex("ff0000ff00ff00ff0000ffffffffffff");
        ResourceStreamWriter writer = new ResourceStreamWriter(20L,
                ResourceStreamFormat.FLAG_REQUIRES_COMPLETION, 90L);
        ResourceStreamRecords.textureCreate(writer, texture, 2, 2, 1,
                ResourceStreamFormat.FORMAT_RGBA8_UNORM,
                ResourceStreamFormat.TEXTURE_USAGE_SAMPLED
                        | ResourceStreamFormat.TEXTURE_USAGE_TRANSFER_DESTINATION,
                ResourceStreamFormat.SAMPLER_CLAMP_TO_EDGE);
        ResourceStreamRecords.textureUpload(writer, texture,
                new io.github.endx.vulkanmod.spi.VulkanTextureData(2, 2, pixels));
        ResourceStreamRecords.textureRegionUpdate(writer, texture, 3, 4,
                new io.github.endx.vulkanmod.spi.VulkanTextureData(1, 1,
                        hex("01020304")));
        ResourceStreamRecords.externalTextureTransfer(writer,
                ResourceStreamFormat.TEXTURE_REGION_UPDATE, texture,
                8, 9, 2, 3, 16, ResourceStreamFormat.FORMAT_RGBA8_UNORM,
                48, 6L, 128L);
        ResourceStreamRecords.textureDestroy(writer, texture);
        ResourceStreamRecords.shaderProgramCreate(writer, shader,
                new io.github.endx.vulkanmod.spi.VulkanCustomShaderProgram(
                        "着色器", "void main(){gl_Position=vec4(0.0);}",
                        "void main(){}"));
        ResourceStreamRecords.shaderProgramDestroy(writer, shader);

        ResourceStreamReader reader = ResourceStreamReader.read(writer.toDirectBuffer());
        ResourceStreamRecords.TextureDescriptor descriptor =
                ResourceStreamRecords.decodeTextureDescriptor(reader, 0);
        require(descriptor.width == 2 && descriptor.height == 2 && descriptor.mipLevels == 1,
                "texture descriptor codec changed");
        ResourceStreamRecords.TextureTransfer upload =
                ResourceStreamRecords.decodeTextureTransfer(reader, 1);
        require(!upload.external() && upload.dataBytes == pixels.length
                        && Arrays.equals(bytes(upload.inlinePixels), pixels),
                "inline texture transfer codec changed");
        ResourceStreamRecords.TextureTransfer region =
                ResourceStreamRecords.decodeTextureTransfer(reader, 2);
        require(region.x == 3 && region.y == 4 && region.width == 1 && region.height == 1,
                "texture region codec changed");
        ResourceStreamRecords.TextureTransfer external =
                ResourceStreamRecords.decodeTextureTransfer(reader, 3);
        require(external.external() && external.arenaId == 6L
                        && external.arenaOffset == 128L && external.rowStride == 16,
                "external texture transfer codec changed");
        ResourceStreamRecords.validateEmptyControl(reader, 4);
        ResourceStreamRecords.ShaderProgram decodedShader =
                ResourceStreamRecords.decodeShaderProgram(reader, 5);
        require(decodedShader.hasVertexSource() && "着色器".equals(decodedShader.name)
                        && decodedShader.fragmentSource.contains("main"),
                "shader-program codec changed");
        ResourceStreamRecords.validateEmptyControl(reader, 6);

        ResourceStreamWriter completion = new ResourceStreamWriter(30L,
                ResourceStreamFormat.FLAG_REQUIRES_COMPLETION, 91L);
        ResourceStreamRecords.textureReadback(completion, texture, 1, 2, 3, 4,
                ResourceStreamFormat.FORMAT_RGBA8_UNORM);
        ResourceStreamRecords.LifecycleBarrier barrier = null;
        ResourceStreamReader completionReader = ResourceStreamReader.read(
                completion.toDirectBuffer());
        ResourceStreamRecords.TextureReadback readback =
                ResourceStreamRecords.decodeTextureReadback(completionReader, 0);
        require(readback.rowStride == 12 && readback.width == 3,
                "texture readback codec changed");

        ResourceStreamWriter barrierWriter = new ResourceStreamWriter(40L, 0, 0L);
        ResourceStreamRecords.lifecycleBarrier(barrierWriter,
                ResourceStreamFormat.BARRIER_RESOURCE_TABLE, 0, 39L, false);
        ResourceStreamReader barrierReader = ResourceStreamReader.read(
                barrierWriter.toDirectBuffer());
        barrier = ResourceStreamRecords.decodeLifecycleBarrier(barrierReader, 0);
        require(barrier.scope == ResourceStreamFormat.BARRIER_RESOURCE_TABLE
                        && barrier.waitThroughSequence == 39L,
                "lifecycle barrier codec changed");
    }

    private static void verifyHandleLifetimeAndSequenceClock() {
        ResourceSequenceClock clock = new ResourceSequenceClock();
        ResourceSequenceClock.Reservation create = clock.reserve(2);
        require(create.first == 1L && create.last == 2L
                        && clock.requiredForNextFrame() == 2L,
                "resource sequence reservation changed");
        ResourceHandleTable<String> table = new ResourceHandleTable<String>(
                FrameResourceHandle.TYPE_TEXTURE);
        long first = table.reserve("first", create.first);
        long second = table.reserve("second", create.last);
        require("first".equals(table.requireVisible(first, 1L)),
                "live resource lookup changed");
        expectFailure(() -> table.requireVisible(second, 1L),
                "new resource was visible to an older frame");
        clock.markApplied(create);

        ResourceSequenceClock.Reservation destroy = clock.reserve(1);
        table.retire(first, destroy.first);
        require("first".equals(table.requireVisible(first, 2L)),
                "retirement hid the resource from an older frame");
        expectFailure(() -> table.requireVisible(first, destroy.first),
                "retired resource remained visible to a later frame");
        clock.markApplied(destroy);
        table.releaseRetired(first);

        ResourceSequenceClock.Reservation replacementCreate = clock.reserve(1);
        long replacement = table.reserve("replacement", replacementCreate.first);
        require(FrameResourceHandle.slot(replacement) == FrameResourceHandle.slot(first)
                        && FrameResourceHandle.generation(replacement)
                        == FrameResourceHandle.generation(first) + 1,
                "resource slot did not advance its generation");
        expectFailure(() -> table.requireVisible(first, replacementCreate.first),
                "stale resource generation was accepted");
        require(table.allocatedCount() == 2 && "second".equals(
                        table.requireVisible(second, replacementCreate.first)),
                "resource table allocation count changed");

        ResourceSequenceClock outOfOrder = new ResourceSequenceClock();
        ResourceSequenceClock.Reservation one = outOfOrder.reserve(1);
        ResourceSequenceClock.Reservation two = outOfOrder.reserve(1);
        expectFailure(() -> outOfOrder.markApplied(two),
                "out-of-order resource completion was accepted");
        outOfOrder.markApplied(one);
        outOfOrder.markApplied(two);
    }

    private static void expectFailure(Runnable operation, String message) {
        try {
            operation.run();
            throw new AssertionError(message);
        } catch (IllegalArgumentException | IllegalStateException expected) {
            // Expected.
        }
    }

    private static byte[] canonicalBytes() {
        return bytes(new ResourceStreamWriter(5L, 0, 0L)
                .record(100, 0, 0L, hex("aabbccdd"))
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
