package io.github.endx.vulkanmod.framestream;

import io.github.endx.vulkanmod.spi.VulkanBlendMode;
import io.github.endx.vulkanmod.spi.VulkanDrawState;
import io.github.endx.vulkanmod.spi.VulkanFrameCommands;
import io.github.endx.vulkanmod.spi.VulkanFrameSubmission;
import io.github.endx.vulkanmod.spi.VulkanRenderTargetPass;
import io.github.endx.vulkanmod.spi.VulkanShaderState;
import io.github.endx.vulkanmod.spi.VulkanTextureFilter;
import io.github.endx.vulkanmod.spi.VulkanTransform2D;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Standalone contract checks for the frozen FrameStream envelope and resource handles. */
public final class FrameStreamAbiVerification {
    private FrameStreamAbiVerification() { }

    public static void main(String[] args) {
        verifyGoldenFrame();
        verifyCrcAndCorruptionRejection();
        verifyResourceHandles();
        verifyWriterBounds();
        verifyArenaOwnershipAndBackpressure();
        verifySharedFrameEncoder();
        System.out.println("RustedVK FrameStream ABI contracts passed");
    }

    private static void verifyGoldenFrame() {
        FrameStreamWriter writer = canonicalWriter(0);
        ByteBuffer target = ByteBuffer.allocateDirect(192).order(ByteOrder.BIG_ENDIAN);
        target.position(7);
        ByteBuffer encoded = writer.writeTo(target);

        require(target.position() == 159, "writer did not advance the target position");
        require(encoded.isReadOnly(), "published frames must be read-only views");
        byte[] actual = bytes(encoded);
        byte[] expected = hex(
                "52564b46010000008000000098000000"
              + "08070605040302010900000000000000"
              + "0000000040010000b400000004000000"
              + "01000000000000000000000000000000"
              + "01000000800000000800000001000000"
              + "02000000880000000000000000000000"
              + "03000000880000000400000001000000"
              + "05000000900000000800000001000000"
              + "0a0b0c0d0e0f10111516171800000000"
              + "1f20212223242526");
        require(Arrays.equals(expected, actual), "canonical bytes changed:\nexpected "
                + toHex(expected) + "\nactual   " + toHex(actual));

        target.position(7);
        target.limit(159);
        FrameStreamReader reader = FrameStreamReader.read(target);
        require(target.position() == 7, "reader changed its caller's position");
        require(reader.frameId() == 0x0102030405060708L, "frame ID changed");
        require(reader.requiredResourceSequence() == 9L, "resource sequence changed");
        require(reader.width() == 320 && reader.height() == 180, "dimensions changed");
        require(reader.passCount() == 1 && reader.batchCount() == 0, "counts changed");
        require(Arrays.equals(bytes(reader.sectionData(FrameStreamFormat.SECTION_PASSES)),
                hex("0a0b0c0d0e0f1011")), "pass payload changed");
        require(reader.section(FrameStreamFormat.SECTION_VERTICES).offset() == 136,
                "vertex section offset changed");
        require(reader.section(FrameStreamFormat.SECTION_MATERIALS).offset() == 144,
                "material alignment changed");
    }

    private static void verifyCrcAndCorruptionRejection() {
        byte[] crcFrame = bytes(canonicalWriter(FrameStreamFormat.FLAG_HAS_PAYLOAD_CRC32)
                .toDirectBuffer());
        FrameStreamReader.read(ByteBuffer.wrap(crcFrame));
        byte[] damagedPayload = crcFrame.clone();
        damagedPayload[damagedPayload.length - 1] ^= 1;
        expectInvalid("payload CRC32", damagedPayload);

        byte[] badMagic = canonicalBytes();
        badMagic[0] = 'X';
        expectInvalid("magic", badMagic);

        byte[] truncated = Arrays.copyOf(canonicalBytes(), canonicalBytes().length - 1);
        expectInvalid("total length", truncated);

        byte[] overlapping = canonicalBytes();
        littleEndian(overlapping).putInt(64 + 3 * 16 + 4, 128);
        expectInvalid("overlap", overlapping);

        byte[] unknownRequired = canonicalBytes();
        littleEndian(unknownRequired).putInt(64 + 3 * 16, 0x80000077);
        expectInvalid("required section", unknownRequired);

        byte[] duplicate = canonicalBytes();
        littleEndian(duplicate).putInt(64 + 3 * 16, FrameStreamFormat.SECTION_VERTICES);
        expectInvalid("duplicate", duplicate);
    }

    private static void verifyResourceHandles() {
        long handle = FrameResourceHandle.encode(FrameResourceHandle.TYPE_TEXTURE,
                0x123456, 0xfedcba98L);
        require(FrameResourceHandle.type(handle) == FrameResourceHandle.TYPE_TEXTURE,
                "handle type changed");
        require(FrameResourceHandle.generation(handle) == 0x123456,
                "handle generation changed");
        require(FrameResourceHandle.slot(handle) == 0xfedcba98L, "handle slot changed");
        FrameResourceHandle.requireType(handle, FrameResourceHandle.TYPE_TEXTURE);
        expectIllegal(new Runnable() {
            @Override public void run() {
                FrameResourceHandle.requireType(handle, FrameResourceHandle.TYPE_SHADER_PROGRAM);
            }
        });
        expectIllegal(new Runnable() {
            @Override public void run() {
                FrameResourceHandle.encode(FrameResourceHandle.TYPE_TEXTURE, 0, 1);
            }
        });
    }

    private static void verifyWriterBounds() {
        expectIllegal(new Runnable() {
            @Override public void run() {
                new FrameStreamWriter(1, 0, 320, 180, 0)
                        .section(FrameStreamFormat.SECTION_PASSES, 1, new byte[0])
                        .toDirectBuffer();
            }
        });
        expectIllegal(new Runnable() {
            @Override public void run() {
                canonicalWriter(0).writeTo(ByteBuffer.allocate(8));
            }
        });
    }

    private static void verifyArenaOwnershipAndBackpressure() {
        final FrameStreamArenaPool pool = new FrameStreamArenaPool(1, 512);
        require(pool.registeredArena(0).isDirect(), "registered arena is not direct memory");
        require(pool.registeredArena(0).isReadOnly(), "registered arena view is writable");
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            FrameStreamArenaPool.WriteLease first = pool.acquireWriter();
            require(first.arenaIndex() == 0, "unexpected arena index");
            writerForFrame(10).writeTo(first.buffer());
            first.publish();

            CountDownLatch attempted = new CountDownLatch(1);
            Future<FrameStreamArenaPool.WriteLease> waiting = executor.submit(() -> {
                attempted.countDown();
                return pool.acquireWriter();
            });
            require(attempted.await(1, TimeUnit.SECONDS), "writer did not start");
            try {
                waiting.get(100, TimeUnit.MILLISECONDS);
                throw new AssertionError("bounded arena pool did not apply backpressure");
            } catch (TimeoutException expected) {
                // The only arena remains decoder-owned.
            }

            try (FrameStreamArenaPool.DecodeLease decode = pool.acquireDecoder()) {
                require(decode.frameId() == 10L, "decoder observed the wrong frame ID");
                require(decode.usedBytes() == decode.buffer().remaining(),
                        "decoder length and buffer disagree");
                require(FrameStreamReader.read(decode.buffer()).frameId() == 10L,
                        "decoder buffer is not the submitted frame");
                require(!waiting.isDone(), "arena was returned before decode completion");
            }
            FrameStreamArenaPool.WriteLease second = waiting.get(1, TimeUnit.SECONDS);
            second.close();

            try (FrameStreamArenaPool.WriteLease stale = pool.acquireWriter()) {
                writerForFrame(10).writeTo(stale.buffer());
                expectIllegal(new Runnable() {
                    @Override public void run() { stale.publish(); }
                });
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError("arena verification interrupted", interrupted);
        } catch (java.util.concurrent.ExecutionException failure) {
            throw new AssertionError("arena verification failed", failure.getCause());
        } catch (TimeoutException timeout) {
            throw new AssertionError("arena was not returned after decode completion", timeout);
        } finally {
            executor.shutdownNow();
        }
    }

    private static void verifySharedFrameEncoder() {
        VulkanFrameCommands offscreen = VulkanFrameCommands.builder(64, 32)
                .clear(0.1f, 0.2f, 0.3f, 1.0f)
                .coloredQuad(0, 0, 8, 8, 1, 0, 0, 1, VulkanDrawState.DEFAULT)
                .coloredQuad(8, 0, 8, 8, 0, 1, 0, 1, VulkanDrawState.DEFAULT)
                .build();
        VulkanDrawState texturedState = new VulkanDrawState(VulkanTransform2D.IDENTITY,
                null, VulkanBlendMode.ADDITIVE, VulkanTextureFilter.NEAREST,
                VulkanShaderState.DEFAULT);
        VulkanFrameCommands presentation = VulkanFrameCommands.builder(320, 180)
                .texturedQuad(11, 0, 0, 16, 16, 0, 0, 1, 1,
                        1, 1, 1, 1, texturedState)
                .texturedQuad(11, 16, 0, 16, 16, 0, 0, 1, 1,
                        1, 1, 1, 1, texturedState)
                .texturedTriangle(12, new float[] { 0, 0, 4, 0, 0, 4 },
                        new float[] { 0, 0, 1, 0, 0, 1 },
                        new float[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 },
                        texturedState)
                .coloredTriangle(new float[] { 1, 1, 2, 1, 1, 2 },
                        new float[] { 1, 0, 0, 1, 0, 1, 0, 1, 0, 0, 1, 1 },
                        VulkanDrawState.DEFAULT)
                .build();
        VulkanFrameSubmission submission = new VulkanFrameSubmission(
                Collections.singletonList(new VulkanRenderTargetPass(7, offscreen)),
                presentation);
        FrameStreamEncoder encoder = new FrameStreamEncoder(
                FrameStreamResourceMapper.generationOneSlots(),
                FrameStreamShaderLayoutResolver.NO_CUSTOM_SHADERS);
        ByteBuffer encoded = encoder.encode(20, 4, submission);
        ByteBuffer reusable = ByteBuffer.allocateDirect(encoded.remaining() + 11);
        reusable.position(11);
        ByteBuffer arenaEncoded = encoder.encodeTo(20, 4, submission, reusable);
        require(reusable.position() == 11 + encoded.remaining(),
                "arena encoding did not advance its target");
        require(Arrays.equals(bytes(encoded), bytes(arenaEncoded)),
                "arena and allocating encoders disagree");
        long warmedWorkspace = encoder.directWorkspaceGrowths();
        require(encoder.directEncodeCount() == 1
                        && encoder.directEncodeBytes() == encoded.remaining()
                        && encoder.directEncodeNanos() > 0L
                        && warmedWorkspace > 0L,
                "direct encoder statistics were not updated");
        try {
            encoder.encodeTo(20, 4, submission,
                    ByteBuffer.allocateDirect(encoded.remaining() - 1));
            throw new AssertionError("undersized FrameStream arena was accepted");
        } catch (FrameStreamCapacityException expected) {
            require(expected.requiredBytes() == encoded.remaining(),
                    "capacity error did not report the required size");
        }
        require(encoder.directCapacityMisses() == 1L
                        && encoder.directWorkspaceGrowths() == warmedWorkspace,
                "warm direct encoder workspace grew or missed statistics on retry");
        DecodedFrameStream decoded = DecodedFrameStream.decode(encoded);
        require(decoded.frameId() == 20 && decoded.requiredResourceSequence() == 4,
                "encoded frame identity changed");
        require(decoded.passCount() == 2 && decoded.batchCount() == 4,
                "shared adjacent batching changed");
        require(decoded.materialCount() == 2, "material de-duplication changed");
        require(decoded.pass(0).firstBatch() == 0 && decoded.pass(0).batchCount() == 1,
                "offscreen pass range changed");
        require(decoded.pass(1).firstBatch() == 1 && decoded.pass(1).batchCount() == 3
                        && decoded.pass(1).isSwapchain(),
                "presentation pass range changed");
        require(FrameResourceHandle.slot(decoded.pass(0).targetHandle()) == 7,
                "render-target mapping changed");
        require(decoded.batch(0).vertexByteOffset() == 0
                        && decoded.batch(0).vertexCount() == 8
                        && decoded.batch(0).indexByteOffset() == 0
                        && decoded.batch(0).indexCount() == 12
                        && decoded.batch(0).indexType()
                        == FrameStreamRecordFormat.INDEX_UINT16,
                "colored quad merging changed");
        require(decoded.batch(1).vertexByteOffset() == 192
                        && decoded.batch(1).vertexCount() == 8
                        && decoded.batch(1).indexByteOffset() == 24
                        && decoded.batch(1).indexCount() == 12,
                "textured quad merging changed");
        require(decoded.batch(2).vertexByteOffset() == 448
                        && decoded.batch(2).vertexCount() == 3,
                "texture boundary no longer splits batches");
        require(decoded.batch(3).vertexByteOffset() == 544
                        && decoded.vertices().remaining() == 616
                        && decoded.indices().remaining() == 48,
                "packed vertex layout changed");

        VulkanShaderState custom = VulkanShaderState.custom(5, 0,
                new float[] { 3.0f, 4.0f });
        VulkanDrawState customState = new VulkanDrawState(VulkanTransform2D.translation(2, 3),
                null, VulkanBlendMode.NORMAL, VulkanTextureFilter.LINEAR, custom);
        VulkanFrameCommands customFrame = VulkanFrameCommands.builder(32, 32)
                .texturedQuad(9, 1, 2, 3, 4, 0, 0, 1, 1,
                        1, 1, 1, 1, customState).build();
        FrameStreamEncoder customEncoder = new FrameStreamEncoder(
                FrameStreamResourceMapper.generationOneSlots(), handle -> handle == 5);
        DecodedFrameStream customDecoded = DecodedFrameStream.decode(customEncoder.encode(
                21, 5, new VulkanFrameSubmission(Collections.emptyList(), customFrame)));
        require(customDecoded.batch(0).vertexLayout()
                        == FrameStreamRecordFormat.VERTEX_CUSTOM_TEXTURED,
                "custom vertex layout metadata was ignored");
        require(FrameResourceHandle.slot(customDecoded.material(0).shaderHandle()) == 5,
                "custom shader mapping changed");
        require(customDecoded.material(0).customValueCount() == 2
                        && customDecoded.material(0).customValue(1) == 4.0f,
                "custom shader values changed");

        VulkanFrameCommands compactPrimitives = VulkanFrameCommands.builder(64, 64)
                .coloredLine(1, 2, 9, 6, 2, 1, 0, 0, 1, VulkanDrawState.DEFAULT)
                .coloredCircle(16, 16, 8, 2, 0, 1, 0, 1,
                        8, false, VulkanDrawState.DEFAULT)
                .coloredCircle(32, 32, 6, 1, 0, 0, 1, 1,
                        8, true, VulkanDrawState.DEFAULT)
                .build();
        DecodedFrameStream compactDecoded = DecodedFrameStream.decode(encoder.encode(
                22, 5, new VulkanFrameSubmission(Collections.emptyList(), compactPrimitives)));
        require(compactPrimitives.commandCount() == 3
                        && compactDecoded.batchCount() == 1
                        && compactDecoded.batch(0).vertexCount() == 78
                        && compactDecoded.vertices().remaining() == 78 * 24,
                "compact line/circle commands did not merge into ordinary colored vertices");
        ByteBuffer primitiveVertices = compactDecoded.vertices().order(ByteOrder.LITTLE_ENDIAN);
        while (primitiveVertices.hasRemaining()) {
            require(Float.isFinite(primitiveVertices.getFloat()),
                    "compact primitive encoder emitted a non-finite vertex");
        }

        VulkanFrameCommands.Builder manyQuads = VulkanFrameCommands.builder(64, 64);
        for (int index = 0; index < 16_385; index++) {
            manyQuads.coloredQuad(0, 0, 1, 1, 1, 1, 1, 1, VulkanDrawState.DEFAULT);
        }
        DecodedFrameStream splitDecoded = DecodedFrameStream.decode(encoder.encode(
                23, 5, new VulkanFrameSubmission(Collections.emptyList(), manyQuads.build())));
        require(splitDecoded.batchCount() == 2
                        && splitDecoded.batch(0).vertexCount() == 65_536
                        && splitDecoded.batch(0).indexCount() == 98_304
                        && splitDecoded.batch(1).vertexCount() == 4
                        && splitDecoded.batch(1).indexCount() == 6,
                "uint16 indexed quad batch did not split at its vertex limit");

        float[] glyphQuads = new float[1_000 * 8];
        for (int glyph = 0; glyph < 1_000; glyph++) {
            int offset = glyph * 8;
            glyphQuads[offset] = glyph % 50;
            glyphQuads[offset + 1] = glyph / 50;
            glyphQuads[offset + 2] = 1;
            glyphQuads[offset + 3] = 1;
            glyphQuads[offset + 4] = 0;
            glyphQuads[offset + 5] = 0;
            glyphQuads[offset + 6] = 1;
            glyphQuads[offset + 7] = 1;
        }
        VulkanFrameCommands compactText = VulkanFrameCommands.pooledBuilder(64, 64)
                .texturedQuadBatch(9, 0, 0, glyphQuads,
                        1, 1, 1, 1, VulkanDrawState.DEFAULT)
                .build();
        glyphQuads[0] = Float.NaN;
        DecodedFrameStream compactTextDecoded = DecodedFrameStream.decode(encoder.encode(
                24, 5, new VulkanFrameSubmission(Collections.emptyList(), compactText)));
        require(compactText.commandCount() == 1
                        && compactText.texturedQuadBatchCount() == 1
                        && compactTextDecoded.batchCount() == 1
                        && compactTextDecoded.batch(0).vertexCount() == 4_000
                        && compactTextDecoded.batch(0).indexCount() == 6_000,
                "textured quad batch did not compact Java commands or indexed geometry");
        compactText.releasePooledCommands();

        byte[] corrupt = bytes(encoded);
        FrameStreamReader envelope = FrameStreamReader.read(ByteBuffer.wrap(corrupt));
        int firstBatch = envelope.section(FrameStreamFormat.SECTION_BATCHES).offset();
        littleEndian(corrupt).putInt(firstBatch, 99);
        try {
            DecodedFrameStream.decode(ByteBuffer.wrap(corrupt));
            throw new AssertionError("invalid batch material was accepted");
        } catch (FrameStreamFormatException expected) {
            require(expected.getMessage().contains("material index"),
                    "wrong record rejection: " + expected.getMessage());
        }

        byte[] corruptIndex = bytes(encoded);
        FrameStreamReader indexedEnvelope = FrameStreamReader.read(ByteBuffer.wrap(corruptIndex));
        int firstIndex = indexedEnvelope.section(FrameStreamFormat.SECTION_INDICES).offset();
        littleEndian(corruptIndex).putShort(firstIndex, (short) 8);
        try {
            DecodedFrameStream.decode(ByteBuffer.wrap(corruptIndex));
            throw new AssertionError("out-of-range batch index was accepted");
        } catch (FrameStreamFormatException expected) {
            require(expected.getMessage().contains("index exceeds"),
                    "wrong index rejection: " + expected.getMessage());
        }
    }

    private static FrameStreamWriter canonicalWriter(int flags) {
        return writerForFrame(0x0102030405060708L, flags);
    }

    private static FrameStreamWriter writerForFrame(long frameId) {
        return writerForFrame(frameId, 0);
    }

    private static FrameStreamWriter writerForFrame(long frameId, int flags) {
        return new FrameStreamWriter(frameId, 9L, 320, 180, flags)
                .section(FrameStreamFormat.SECTION_PASSES, 1,
                        hex("0a0b0c0d0e0f1011"))
                .section(FrameStreamFormat.SECTION_BATCHES, 0, new byte[0])
                .section(FrameStreamFormat.SECTION_VERTICES, 1,
                        hex("15161718"))
                .section(FrameStreamFormat.SECTION_MATERIALS, 1,
                        hex("1f20212223242526"));
    }

    private static byte[] canonicalBytes() {
        return bytes(canonicalWriter(0).toDirectBuffer());
    }

    private static ByteBuffer littleEndian(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
    }

    private static byte[] bytes(ByteBuffer source) {
        ByteBuffer view = source.duplicate();
        byte[] result = new byte[view.remaining()];
        view.get(result);
        return result;
    }

    private static byte[] hex(String value) {
        if ((value.length() & 1) != 0) throw new IllegalArgumentException("odd hex length");
        byte[] result = new byte[value.length() / 2];
        for (int index = 0; index < result.length; index++) {
            result[index] = (byte) Integer.parseInt(value.substring(index * 2, index * 2 + 2), 16);
        }
        return result;
    }

    private static String toHex(byte[] value) {
        StringBuilder result = new StringBuilder(value.length * 2);
        for (byte item : value) result.append(String.format("%02x", item & 0xff));
        return result.toString();
    }

    private static void expectInvalid(String expectedMessage, byte[] bytes) {
        try {
            FrameStreamReader.read(ByteBuffer.wrap(bytes));
            throw new AssertionError("invalid FrameStream was accepted: " + expectedMessage);
        } catch (FrameStreamFormatException expected) {
            require(expected.getMessage().contains(expectedMessage), "wrong rejection: "
                    + expected.getMessage());
        }
    }

    private static void expectIllegal(Runnable operation) {
        try {
            operation.run();
            throw new AssertionError("invalid operation was accepted");
        } catch (IllegalArgumentException | IllegalStateException expected) {
            // Expected.
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
