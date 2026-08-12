package io.github.endx.vulkanmod.framestream;

import io.github.endx.vulkanmod.spi.VulkanBlendMode;
import io.github.endx.vulkanmod.spi.VulkanClipRect;
import io.github.endx.vulkanmod.spi.VulkanColoredQuad;
import io.github.endx.vulkanmod.spi.VulkanColoredTriangle;
import io.github.endx.vulkanmod.spi.VulkanDrawCommand;
import io.github.endx.vulkanmod.spi.VulkanDrawState;
import io.github.endx.vulkanmod.spi.VulkanFrameCommands;
import io.github.endx.vulkanmod.spi.VulkanFrameSubmission;
import io.github.endx.vulkanmod.spi.VulkanRenderTargetPass;
import io.github.endx.vulkanmod.spi.VulkanShaderState;
import io.github.endx.vulkanmod.spi.VulkanTextureFilter;
import io.github.endx.vulkanmod.spi.VulkanTexturedQuad;
import io.github.endx.vulkanmod.spi.VulkanTexturedTriangle;
import io.github.endx.vulkanmod.spi.VulkanTransform2D;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Shared Windows/Android batching and vertex-packing implementation for FrameStream version 1. */
public final class FrameStreamEncoder {
    private static final int DIRECT_SECTION_COUNT = 4;

    private final FrameStreamResourceMapper resources;
    private final FrameStreamShaderLayoutResolver shaderLayouts;
    private final ArrayList<PassRecord> directPasses = new ArrayList<PassRecord>();
    private final ArrayList<BatchRecord> directBatches = new ArrayList<BatchRecord>();
    private final ArrayList<MaterialKey> directMaterials = new ArrayList<MaterialKey>();
    private final HashMap<MaterialKey, Integer> directMaterialIndexes =
            new HashMap<MaterialKey, Integer>();
    private final MaterialKey directMaterialProbe = new MaterialKey();
    private int directPassCount;
    private int directBatchCount;
    private int directMaterialCount;
    private int directVertexBytes;
    private int directVertexCount;
    private long directEncodeCount;
    private long directEncodeBytes;
    private long directEncodeNanos;
    private long directCapacityMisses;
    private long directWorkspaceGrowths;

    public FrameStreamEncoder(FrameStreamResourceMapper resources,
                              FrameStreamShaderLayoutResolver shaderLayouts) {
        if (resources == null) throw new NullPointerException("resources");
        if (shaderLayouts == null) throw new NullPointerException("shaderLayouts");
        this.resources = resources;
        this.shaderLayouts = shaderLayouts;
    }

    public ByteBuffer encode(long frameId, long requiredResourceSequence,
                             VulkanFrameSubmission submission) {
        return buildWriter(frameId, requiredResourceSequence, submission).toDirectBuffer();
    }

    /** Encodes into a reusable arena at its current position without allocating direct memory. */
    public synchronized ByteBuffer encodeTo(long frameId, long requiredResourceSequence,
                                            VulkanFrameSubmission submission,
                                            ByteBuffer target) {
        if (target == null) throw new NullPointerException("target");
        if (submission == null) throw new NullPointerException("submission");
        validateEnvelope(frameId, requiredResourceSequence,
                submission.presentationFrame().width(), submission.presentationFrame().height());
        long started = System.nanoTime();
        prepareDirectWorkspace(submission);

        int headerBytes = Math.addExact(FrameStreamFormat.FIXED_HEADER_BYTES,
                DIRECT_SECTION_COUNT * FrameStreamFormat.SECTION_ENTRY_BYTES);
        int passBytes = Math.multiplyExact(directPassCount,
                FrameStreamRecordFormat.PASS_BYTES);
        int batchBytes = Math.multiplyExact(directBatchCount,
                FrameStreamRecordFormat.BATCH_BYTES);
        int materialBytes = Math.multiplyExact(directMaterialCount,
                FrameStreamRecordFormat.MATERIAL_BYTES);
        int passOffset = FrameStreamFormat.align(headerBytes);
        int batchOffset = FrameStreamFormat.align(Math.addExact(passOffset, passBytes));
        int vertexOffset = FrameStreamFormat.align(Math.addExact(batchOffset, batchBytes));
        int materialOffset = FrameStreamFormat.align(
                Math.addExact(vertexOffset, directVertexBytes));
        int required = Math.addExact(materialOffset, materialBytes);
        if (required > FrameStreamFormat.MAX_STREAM_BYTES) {
            throw new FrameStreamFormatException("FrameStream exceeds maximum size");
        }
        if (target.remaining() < required) {
            directCapacityMisses++;
            throw new FrameStreamCapacityException(required, target.remaining());
        }

        int targetStart = target.position();
        ByteBuffer writable = target.duplicate().order(ByteOrder.LITTLE_ENDIAN);
        writable.position(targetStart).limit(targetStart + required);
        ByteBuffer frame = writable.slice().order(ByteOrder.LITTLE_ENDIAN);
        writeDirectHeader(frame, frameId, requiredResourceSequence,
                submission.presentationFrame(), headerBytes, required);
        int directory = FrameStreamFormat.FIXED_HEADER_BYTES;
        writeDirectoryEntry(frame, directory, FrameStreamFormat.SECTION_PASSES,
                passOffset, passBytes, directPassCount);
        directory += FrameStreamFormat.SECTION_ENTRY_BYTES;
        writeDirectoryEntry(frame, directory, FrameStreamFormat.SECTION_BATCHES,
                batchOffset, batchBytes, directBatchCount);
        directory += FrameStreamFormat.SECTION_ENTRY_BYTES;
        writeDirectoryEntry(frame, directory, FrameStreamFormat.SECTION_VERTICES,
                vertexOffset, directVertexBytes, directVertexCount);
        directory += FrameStreamFormat.SECTION_ENTRY_BYTES;
        writeDirectoryEntry(frame, directory, FrameStreamFormat.SECTION_MATERIALS,
                materialOffset, materialBytes, directMaterialCount);
        zeroRange(frame, headerBytes, passOffset);
        zeroRange(frame, passOffset + passBytes, batchOffset);
        zeroRange(frame, batchOffset + batchBytes, vertexOffset);
        zeroRange(frame, vertexOffset + directVertexBytes, materialOffset);
        writeDirectPasses(section(frame, passOffset, passBytes));
        writeDirectBatches(section(frame, batchOffset, batchBytes));
        writeDirectVertices(submission, section(frame, vertexOffset, directVertexBytes));
        writeDirectMaterials(section(frame, materialOffset, materialBytes));

        target.position(targetStart + required);
        ByteBuffer result = frame.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN);
        result.position(0).limit(required);
        directEncodeCount++;
        directEncodeBytes += required;
        directEncodeNanos += System.nanoTime() - started;
        return result;
    }

    public synchronized long directEncodeCount() { return directEncodeCount; }
    public synchronized long directEncodeBytes() { return directEncodeBytes; }
    public synchronized long directEncodeNanos() { return directEncodeNanos; }
    public synchronized long directCapacityMisses() { return directCapacityMisses; }
    public synchronized long directWorkspaceGrowths() { return directWorkspaceGrowths; }

    private void prepareDirectWorkspace(VulkanFrameSubmission submission) {
        directPassCount = 0;
        directBatchCount = 0;
        directMaterialCount = 0;
        directVertexBytes = 0;
        directVertexCount = 0;
        directMaterialIndexes.clear();
        for (VulkanRenderTargetPass pass : submission.renderTargetPasses()) {
            scanDirectPass(resources.texture(pass.textureHandle()), false, pass.frame());
        }
        scanDirectPass(0L, true, submission.presentationFrame());
        if (directPassCount > FrameStreamFormat.MAX_PASSES) {
            throw new FrameStreamFormatException("FrameStream contains too many passes");
        }
        if (directBatchCount > FrameStreamFormat.MAX_BATCHES) {
            throw new FrameStreamFormatException("FrameStream contains too many batches");
        }
        if (directVertexCount > FrameStreamFormat.MAX_SECTION_ELEMENTS
                || directMaterialCount > FrameStreamFormat.MAX_SECTION_ELEMENTS) {
            throw new FrameStreamFormatException("FrameStream section element count is too large");
        }
    }

    private void scanDirectPass(long targetHandle, boolean swapchain,
                                VulkanFrameCommands frame) {
        int firstBatch = directBatchCount;
        BatchRecord current = null;
        for (int index = 0; index < frame.commandCount(); index++) {
            VulkanDrawCommand command = frame.command(index);
            VulkanDrawState state = state(command);
            int layout = vertexLayout(command);
            int commandVertices = vertexCount(command);
            int byteOffset = directVertexBytes;
            long nextVertexBytes = (long) directVertexBytes
                    + (long) commandVertices * FrameStreamRecordFormat.vertexStride(layout);
            long nextVertexCount = (long) directVertexCount + commandVertices;
            if (nextVertexBytes > FrameStreamFormat.MAX_STREAM_BYTES
                    || nextVertexCount > FrameStreamFormat.MAX_SECTION_ELEMENTS) {
                throw new FrameStreamFormatException(
                        "packed vertices exceed FrameStream limits");
            }
            directVertexBytes = (int) nextVertexBytes;
            directVertexCount = (int) nextVertexCount;

            long primaryTexture = primaryTexture(command);
            long secondaryTexture = primaryTexture == 0L
                    || state.shaderState().secondaryTextureHandle() == 0L
                    ? 0L : resources.texture(state.shaderState().secondaryTextureHandle());
            VulkanTextureFilter filter = primaryTexture == 0L
                    ? VulkanTextureFilter.LINEAR : state.textureFilter();
            VulkanShaderState shader = primaryTexture == 0L
                    ? VulkanShaderState.DEFAULT : state.shaderState();
            directMaterialProbe.set(state.blendMode(), filter, shader);
            Integer materialIndex = directMaterialIndexes.get(directMaterialProbe);
            if (materialIndex == null) {
                if (directMaterialCount == FrameStreamFormat.MAX_SECTION_ELEMENTS) {
                    throw new FrameStreamFormatException(
                            "FrameStream contains too many materials");
                }
                materialIndex = directMaterialCount;
                MaterialKey stable = directMaterial(directMaterialCount++);
                stable.set(state.blendMode(), filter, shader);
                directMaterialIndexes.put(stable, materialIndex);
            }
            if (current == null || !current.compatible(materialIndex, primaryTexture,
                    secondaryTexture, state.clip(), layout, byteOffset)) {
                if (directBatchCount == FrameStreamFormat.MAX_BATCHES) {
                    throw new FrameStreamFormatException(
                            "FrameStream contains too many batches");
                }
                current = directBatch(directBatchCount++);
                current.set(materialIndex, primaryTexture, secondaryTexture,
                        state.clip(), byteOffset, commandVertices, layout);
            } else {
                current.vertexCount = Math.addExact(current.vertexCount, commandVertices);
            }
        }
        if (directPassCount == FrameStreamFormat.MAX_PASSES) {
            throw new FrameStreamFormatException("FrameStream contains too many passes");
        }
        PassRecord pass = directPass(directPassCount++);
        pass.set(targetHandle, firstBatch, directBatchCount - firstBatch, frame, swapchain);
    }

    private PassRecord directPass(int index) {
        if (index == directPasses.size()) {
            directPasses.add(new PassRecord());
            directWorkspaceGrowths++;
        }
        return directPasses.get(index);
    }

    private BatchRecord directBatch(int index) {
        if (index == directBatches.size()) {
            directBatches.add(new BatchRecord());
            directWorkspaceGrowths++;
        }
        return directBatches.get(index);
    }

    private MaterialKey directMaterial(int index) {
        if (index == directMaterials.size()) {
            directMaterials.add(new MaterialKey());
            directWorkspaceGrowths++;
        }
        return directMaterials.get(index);
    }

    private static void validateEnvelope(long frameId, long requiredResourceSequence,
                                         int width, int height) {
        if (frameId < 0L) throw new IllegalArgumentException("frameId must not be negative");
        if (requiredResourceSequence < 0L) {
            throw new IllegalArgumentException("resource sequence must not be negative");
        }
        if (width <= 0 || width > FrameStreamFormat.MAX_DIMENSION) {
            throw new IllegalArgumentException("width is outside FrameStream limits: " + width);
        }
        if (height <= 0 || height > FrameStreamFormat.MAX_DIMENSION) {
            throw new IllegalArgumentException("height is outside FrameStream limits: " + height);
        }
    }

    private void writeDirectHeader(ByteBuffer frame, long frameId,
                                   long requiredResourceSequence,
                                   VulkanFrameCommands presentation,
                                   int headerBytes, int totalBytes) {
        frame.put(FrameStreamFormat.OFFSET_MAGIC, FrameStreamFormat.MAGIC_R);
        frame.put(FrameStreamFormat.OFFSET_MAGIC + 1, FrameStreamFormat.MAGIC_V);
        frame.put(FrameStreamFormat.OFFSET_MAGIC + 2, FrameStreamFormat.MAGIC_K);
        frame.put(FrameStreamFormat.OFFSET_MAGIC + 3, FrameStreamFormat.MAGIC_F);
        frame.putShort(FrameStreamFormat.OFFSET_MAJOR_VERSION,
                (short) FrameStreamFormat.MAJOR_VERSION);
        frame.putShort(FrameStreamFormat.OFFSET_MINOR_VERSION,
                (short) FrameStreamFormat.MINOR_VERSION);
        frame.putInt(FrameStreamFormat.OFFSET_HEADER_BYTES, headerBytes);
        frame.putInt(FrameStreamFormat.OFFSET_TOTAL_BYTES, totalBytes);
        frame.putLong(FrameStreamFormat.OFFSET_FRAME_ID, frameId);
        frame.putLong(FrameStreamFormat.OFFSET_REQUIRED_RESOURCE_SEQUENCE,
                requiredResourceSequence);
        frame.putInt(FrameStreamFormat.OFFSET_FLAGS, 0);
        frame.putInt(FrameStreamFormat.OFFSET_WIDTH, presentation.width());
        frame.putInt(FrameStreamFormat.OFFSET_HEIGHT, presentation.height());
        frame.putInt(FrameStreamFormat.OFFSET_SECTION_COUNT, DIRECT_SECTION_COUNT);
        frame.putInt(FrameStreamFormat.OFFSET_PASS_COUNT, directPassCount);
        frame.putInt(FrameStreamFormat.OFFSET_BATCH_COUNT, directBatchCount);
        frame.putInt(FrameStreamFormat.OFFSET_PAYLOAD_CRC32, 0);
        frame.putInt(FrameStreamFormat.OFFSET_RESERVED, 0);
    }

    private static void writeDirectoryEntry(ByteBuffer frame, int offset, int type,
                                            int sectionOffset, int byteLength,
                                            int elementCount) {
        frame.putInt(offset, type);
        frame.putInt(offset + 4, sectionOffset);
        frame.putInt(offset + 8, byteLength);
        frame.putInt(offset + 12, elementCount);
    }

    private static void zeroRange(ByteBuffer frame, int start, int end) {
        for (int offset = start; offset < end; offset++) frame.put(offset, (byte) 0);
    }

    private static ByteBuffer section(ByteBuffer frame, int offset, int byteLength) {
        ByteBuffer view = frame.duplicate().order(ByteOrder.LITTLE_ENDIAN);
        view.position(offset).limit(offset + byteLength);
        return view.slice().order(ByteOrder.LITTLE_ENDIAN);
    }

    private void writeDirectPasses(ByteBuffer output) {
        for (int index = 0; index < directPassCount; index++) {
            PassRecord record = directPasses.get(index);
            output.putLong(record.targetHandle).putInt(record.firstBatch)
                    .putInt(record.batchCount).putInt(record.flags)
                    .putInt(0).putInt(0).putInt(record.frame.width()).putInt(record.frame.height())
                    .putFloat(record.frame.clearRed()).putFloat(record.frame.clearGreen())
                    .putFloat(record.frame.clearBlue()).putFloat(record.frame.clearAlpha())
                    .putInt(FrameStreamRecordFormat.NO_DEBUG_LABEL).putInt(0).putInt(0);
        }
        if (output.hasRemaining()) throw new IllegalStateException(
                "FrameStream pass size prediction changed");
    }

    private void writeDirectBatches(ByteBuffer output) {
        for (int index = 0; index < directBatchCount; index++) {
            BatchRecord record = directBatches.get(index);
            int flags = record.clip == null ? 0 : FrameStreamRecordFormat.BATCH_HAS_CLIP;
            if (record.primaryTexture != 0L) flags |= FrameStreamRecordFormat.BATCH_TEXTURED;
            output.putInt(record.materialIndex).putInt(flags)
                    .putLong(record.primaryTexture).putLong(record.secondaryTexture)
                    .putInt(record.vertexByteOffset).putInt(record.vertexCount)
                    .putInt(0).putInt(0);
            if (record.clip == null) {
                output.putFloat(0).putFloat(0).putFloat(0).putFloat(0);
            } else {
                output.putFloat(record.clip.x()).putFloat(record.clip.y())
                        .putFloat(record.clip.width()).putFloat(record.clip.height());
            }
            output.putShort((short) FrameStreamRecordFormat.TOPOLOGY_TRIANGLE_LIST)
                    .putShort((short) FrameStreamRecordFormat.INDEX_NONE)
                    .putShort((short) record.vertexLayout).putShort((short) 0);
        }
        if (output.hasRemaining()) throw new IllegalStateException(
                "FrameStream batch size prediction changed");
    }

    private void writeDirectVertices(VulkanFrameSubmission submission, ByteBuffer output) {
        for (VulkanRenderTargetPass pass : submission.renderTargetPasses()) {
            writeDirectVertices(pass.frame(), output);
        }
        writeDirectVertices(submission.presentationFrame(), output);
        if (output.hasRemaining()) throw new IllegalStateException(
                "FrameStream vertex size prediction changed");
    }

    private void writeDirectVertices(VulkanFrameCommands frame, ByteBuffer output) {
        for (int index = 0; index < frame.commandCount(); index++) {
            VulkanDrawCommand command = frame.command(index);
            writeVertices(output, frame, command, vertexLayout(command));
        }
    }

    private void writeDirectMaterials(ByteBuffer output) {
        for (int index = 0; index < directMaterialCount; index++) {
            writeMaterial(output, directMaterials.get(index));
        }
        if (output.hasRemaining()) throw new IllegalStateException(
                "FrameStream material size prediction changed");
    }

    private void writeMaterial(ByteBuffer output, MaterialKey material) {
        VulkanShaderState shader = material.shader;
        long shaderHandle = shader.effect() == VulkanShaderState.CUSTOM
                ? resources.shaderProgram(shader.customShaderHandle()) : 0L;
        output.putInt(0).putInt(blendValue(material.blendMode))
                .putInt(filterValue(material.textureFilter)).putInt(shader.effect())
                .putLong(shaderHandle)
                .putFloat(shader.red()).putFloat(shader.green()).putFloat(shader.blue())
                .putFloat(shader.alpha()).putFloat(shader.teamColorAmount())
                .putFloat(shader.screenBaseWidth()).putFloat(shader.screenBaseHeight())
                .putFloat(shader.resolutionWidth()).putFloat(shader.resolutionHeight())
                .putFloat(shader.displacementOffset()).putFloat(shader.uiScaling());
        int customCount = shader.customValueCount();
        output.putInt(customCount).putInt(0).putInt(0);
        for (int index = 0; index < VulkanShaderState.MAX_CUSTOM_FLOATS; index++) {
            output.putFloat(index < customCount ? shader.customValue(index) : 0.0f);
        }
    }

    private FrameStreamWriter buildWriter(long frameId, long requiredResourceSequence,
                                          VulkanFrameSubmission submission) {
        if (submission == null) throw new NullPointerException("submission");
        VulkanFrameCommands presentation = submission.presentationFrame();
        int vertexBytes = countVertexBytes(submission);
        ByteBuffer vertices = ByteBuffer.allocate(vertexBytes).order(ByteOrder.LITTLE_ENDIAN);
        ArrayList<PassRecord> passes = new ArrayList<PassRecord>(
                submission.renderTargetPasses().size() + 1);
        ArrayList<BatchRecord> batches = new ArrayList<BatchRecord>();
        LinkedHashMap<MaterialKey, Integer> materialIndexes =
                new LinkedHashMap<MaterialKey, Integer>();
        int[] totalVertices = { 0 };

        for (VulkanRenderTargetPass pass : submission.renderTargetPasses()) {
            encodePass(resources.texture(pass.textureHandle()), false, pass.frame(),
                    vertices, totalVertices, batches, materialIndexes, passes);
        }
        encodePass(0L, true, presentation, vertices, totalVertices,
                batches, materialIndexes, passes);
        if (vertices.position() != vertexBytes) {
            throw new IllegalStateException("FrameStream vertex size prediction changed");
        }

        byte[] passBytes = encodePasses(passes);
        byte[] batchBytes = encodeBatches(batches);
        byte[] vertexPayload = new byte[vertices.position()];
        vertices.flip();
        vertices.get(vertexPayload);
        byte[] materialBytes = encodeMaterials(materialIndexes);
        return new FrameStreamWriter(frameId, requiredResourceSequence,
                presentation.width(), presentation.height(), 0)
                .section(FrameStreamFormat.SECTION_PASSES, passes.size(), passBytes)
                .section(FrameStreamFormat.SECTION_BATCHES, batches.size(), batchBytes)
                .section(FrameStreamFormat.SECTION_VERTICES, totalVertices[0], vertexPayload)
                .section(FrameStreamFormat.SECTION_MATERIALS,
                        materialIndexes.size(), materialBytes);
    }

    private int countVertexBytes(VulkanFrameSubmission submission) {
        long total = 0L;
        for (VulkanRenderTargetPass pass : submission.renderTargetPasses()) {
            total += countVertexBytes(pass.frame());
        }
        total += countVertexBytes(submission.presentationFrame());
        if (total > Integer.MAX_VALUE || total > FrameStreamFormat.MAX_STREAM_BYTES) {
            throw new FrameStreamFormatException("packed vertices exceed FrameStream limits");
        }
        return (int) total;
    }

    private long countVertexBytes(VulkanFrameCommands frame) {
        long total = 0L;
        for (int index = 0; index < frame.commandCount(); index++) {
            VulkanDrawCommand command = frame.command(index);
            total += (long) vertexCount(command) * FrameStreamRecordFormat.vertexStride(
                    vertexLayout(command));
        }
        return total;
    }

    private void encodePass(long targetHandle, boolean swapchain, VulkanFrameCommands frame,
                            ByteBuffer vertices, int[] totalVertices,
                            List<BatchRecord> batches,
                            LinkedHashMap<MaterialKey, Integer> materialIndexes,
                            List<PassRecord> passes) {
        int firstBatch = batches.size();
        BatchRecord current = null;
        for (int index = 0; index < frame.commandCount(); index++) {
            VulkanDrawCommand command = frame.command(index);
            VulkanDrawState state = state(command);
            int layout = vertexLayout(command);
            int commandVertices = vertexCount(command);
            int byteOffset = vertices.position();
            writeVertices(vertices, frame, command, layout);
            totalVertices[0] = Math.addExact(totalVertices[0], commandVertices);

            long primaryTexture = primaryTexture(command);
            long secondaryTexture = primaryTexture == 0L
                    || state.shaderState().secondaryTextureHandle() == 0L
                    ? 0L : resources.texture(state.shaderState().secondaryTextureHandle());
            MaterialKey material = primaryTexture == 0L
                    ? new MaterialKey(state.blendMode(), VulkanTextureFilter.LINEAR,
                            VulkanShaderState.DEFAULT)
                    : new MaterialKey(state.blendMode(),
                            state.textureFilter(), state.shaderState());
            Integer materialIndex = materialIndexes.get(material);
            if (materialIndex == null) {
                materialIndex = materialIndexes.size();
                materialIndexes.put(material, materialIndex);
            }
            if (current == null || !current.compatible(materialIndex, primaryTexture,
                    secondaryTexture, state.clip(), layout, byteOffset)) {
                current = new BatchRecord(materialIndex, primaryTexture, secondaryTexture,
                        state.clip(), byteOffset, commandVertices, layout);
                batches.add(current);
            } else {
                current.vertexCount = Math.addExact(current.vertexCount, commandVertices);
            }
        }
        passes.add(new PassRecord(targetHandle, firstBatch, batches.size() - firstBatch,
                frame, swapchain));
    }

    private int vertexLayout(VulkanDrawCommand command) {
        if (command instanceof VulkanColoredQuad || command instanceof VulkanColoredTriangle) {
            return FrameStreamRecordFormat.VERTEX_COLORED;
        }
        VulkanShaderState shader = state(command).shaderState();
        return shader.effect() == VulkanShaderState.CUSTOM
                && shaderLayouts.usesExpandedVertexInput(shader.customShaderHandle())
                ? FrameStreamRecordFormat.VERTEX_CUSTOM_TEXTURED
                : FrameStreamRecordFormat.VERTEX_TEXTURED;
    }

    private long primaryTexture(VulkanDrawCommand command) {
        if (command instanceof VulkanTexturedQuad) {
            return resources.texture(((VulkanTexturedQuad) command).textureHandle());
        }
        if (command instanceof VulkanTexturedTriangle) {
            return resources.texture(((VulkanTexturedTriangle) command).textureHandle());
        }
        return 0L;
    }

    private static VulkanDrawState state(VulkanDrawCommand command) {
        if (command instanceof VulkanColoredQuad) return ((VulkanColoredQuad) command).state();
        if (command instanceof VulkanColoredTriangle) {
            return ((VulkanColoredTriangle) command).state();
        }
        if (command instanceof VulkanTexturedQuad) return ((VulkanTexturedQuad) command).state();
        if (command instanceof VulkanTexturedTriangle) {
            return ((VulkanTexturedTriangle) command).state();
        }
        throw new IllegalArgumentException("unsupported draw command: "
                + command.getClass().getName());
    }

    private static int vertexCount(VulkanDrawCommand command) {
        if (command instanceof VulkanColoredQuad || command instanceof VulkanTexturedQuad) return 6;
        if (command instanceof VulkanColoredTriangle
                || command instanceof VulkanTexturedTriangle) return 3;
        throw new IllegalArgumentException("unsupported draw command: "
                + command.getClass().getName());
    }

    private static void writeVertices(ByteBuffer output, VulkanFrameCommands frame,
                                      VulkanDrawCommand command, int layout) {
        if (command instanceof VulkanColoredQuad) {
            VulkanColoredQuad quad = (VulkanColoredQuad) command;
            float left = quad.x();
            float right = quad.x() + quad.width();
            float top = quad.y();
            float bottom = quad.y() + quad.height();
            putColored(output, frame, quad.state().transform(), left, top,
                    quad.red(), quad.green(), quad.blue(), quad.alpha());
            putColored(output, frame, quad.state().transform(), left, bottom,
                    quad.red(), quad.green(), quad.blue(), quad.alpha());
            putColored(output, frame, quad.state().transform(), right, bottom,
                    quad.red(), quad.green(), quad.blue(), quad.alpha());
            putColored(output, frame, quad.state().transform(), left, top,
                    quad.red(), quad.green(), quad.blue(), quad.alpha());
            putColored(output, frame, quad.state().transform(), right, bottom,
                    quad.red(), quad.green(), quad.blue(), quad.alpha());
            putColored(output, frame, quad.state().transform(), right, top,
                    quad.red(), quad.green(), quad.blue(), quad.alpha());
            return;
        }
        if (command instanceof VulkanColoredTriangle) {
            VulkanColoredTriangle triangle = (VulkanColoredTriangle) command;
            for (int vertex = 0; vertex < 3; vertex++) {
                putColored(output, frame, triangle.state().transform(),
                        triangle.x(vertex), triangle.y(vertex), triangle.red(vertex),
                        triangle.green(vertex), triangle.blue(vertex), triangle.alpha(vertex));
            }
            return;
        }
        if (command instanceof VulkanTexturedQuad) {
            VulkanTexturedQuad quad = (VulkanTexturedQuad) command;
            float left = quad.x();
            float right = quad.x() + quad.width();
            float top = quad.y();
            float bottom = quad.y() + quad.height();
            putTextured(output, frame, quad.state().transform(), layout, left, top,
                    quad.u0(), quad.v0(), quad.red(), quad.green(), quad.blue(), quad.alpha());
            putTextured(output, frame, quad.state().transform(), layout, left, bottom,
                    quad.u0(), quad.v1(), quad.red(), quad.green(), quad.blue(), quad.alpha());
            putTextured(output, frame, quad.state().transform(), layout, right, bottom,
                    quad.u1(), quad.v1(), quad.red(), quad.green(), quad.blue(), quad.alpha());
            putTextured(output, frame, quad.state().transform(), layout, left, top,
                    quad.u0(), quad.v0(), quad.red(), quad.green(), quad.blue(), quad.alpha());
            putTextured(output, frame, quad.state().transform(), layout, right, bottom,
                    quad.u1(), quad.v1(), quad.red(), quad.green(), quad.blue(), quad.alpha());
            putTextured(output, frame, quad.state().transform(), layout, right, top,
                    quad.u1(), quad.v0(), quad.red(), quad.green(), quad.blue(), quad.alpha());
            return;
        }
        if (command instanceof VulkanTexturedTriangle) {
            VulkanTexturedTriangle triangle = (VulkanTexturedTriangle) command;
            for (int vertex = 0; vertex < 3; vertex++) {
                putTextured(output, frame, triangle.state().transform(), layout,
                        triangle.x(vertex), triangle.y(vertex), triangle.u(vertex),
                        triangle.v(vertex), triangle.red(vertex), triangle.green(vertex),
                        triangle.blue(vertex), triangle.alpha(vertex));
            }
            return;
        }
        throw new IllegalArgumentException("unsupported draw command: "
                + command.getClass().getName());
    }

    private static void putColored(ByteBuffer output, VulkanFrameCommands frame,
                                   VulkanTransform2D transform, float x, float y,
                                   float red, float green, float blue, float alpha) {
        output.putFloat(pixelToNdcX(transform.transformX(x, y), frame.width()));
        output.putFloat(pixelToNdcY(transform.transformY(x, y), frame.height()));
        output.putFloat(red).putFloat(green).putFloat(blue).putFloat(alpha);
    }

    private static void putTextured(ByteBuffer output, VulkanFrameCommands frame,
                                    VulkanTransform2D transform, int layout,
                                    float x, float y, float u, float v,
                                    float red, float green, float blue, float alpha) {
        if (layout == FrameStreamRecordFormat.VERTEX_CUSTOM_TEXTURED) {
            output.putFloat(x).putFloat(y).putFloat(u).putFloat(v)
                    .putFloat(red).putFloat(green).putFloat(blue).putFloat(alpha)
                    .putFloat(transform.m00()).putFloat(transform.m01()).putFloat(transform.m02())
                    .putFloat(transform.m10()).putFloat(transform.m11()).putFloat(transform.m12())
                    .putFloat(frame.width()).putFloat(frame.height());
            return;
        }
        output.putFloat(pixelToNdcX(transform.transformX(x, y), frame.width()));
        output.putFloat(pixelToNdcY(transform.transformY(x, y), frame.height()));
        output.putFloat(u).putFloat(v).putFloat(red).putFloat(green)
                .putFloat(blue).putFloat(alpha);
    }

    private byte[] encodePasses(List<PassRecord> records) {
        ByteBuffer output = allocateRecords(records.size(), FrameStreamRecordFormat.PASS_BYTES);
        for (PassRecord record : records) {
            output.putLong(record.targetHandle).putInt(record.firstBatch)
                    .putInt(record.batchCount).putInt(record.flags)
                    .putInt(0).putInt(0).putInt(record.frame.width()).putInt(record.frame.height())
                    .putFloat(record.frame.clearRed()).putFloat(record.frame.clearGreen())
                    .putFloat(record.frame.clearBlue()).putFloat(record.frame.clearAlpha())
                    .putInt(FrameStreamRecordFormat.NO_DEBUG_LABEL).putInt(0).putInt(0);
        }
        return output.array();
    }

    private byte[] encodeBatches(List<BatchRecord> records) {
        ByteBuffer output = allocateRecords(records.size(), FrameStreamRecordFormat.BATCH_BYTES);
        for (BatchRecord record : records) {
            int flags = record.clip == null ? 0 : FrameStreamRecordFormat.BATCH_HAS_CLIP;
            if (record.primaryTexture != 0L) flags |= FrameStreamRecordFormat.BATCH_TEXTURED;
            output.putInt(record.materialIndex).putInt(flags)
                    .putLong(record.primaryTexture).putLong(record.secondaryTexture)
                    .putInt(record.vertexByteOffset).putInt(record.vertexCount)
                    .putInt(0).putInt(0);
            if (record.clip == null) {
                output.putFloat(0).putFloat(0).putFloat(0).putFloat(0);
            } else {
                output.putFloat(record.clip.x()).putFloat(record.clip.y())
                        .putFloat(record.clip.width()).putFloat(record.clip.height());
            }
            output.putShort((short) FrameStreamRecordFormat.TOPOLOGY_TRIANGLE_LIST)
                    .putShort((short) FrameStreamRecordFormat.INDEX_NONE)
                    .putShort((short) record.vertexLayout).putShort((short) 0);
        }
        return output.array();
    }

    private byte[] encodeMaterials(LinkedHashMap<MaterialKey, Integer> indexes) {
        ByteBuffer output = allocateRecords(indexes.size(), FrameStreamRecordFormat.MATERIAL_BYTES);
        for (Map.Entry<MaterialKey, Integer> entry : indexes.entrySet()) {
            MaterialKey material = entry.getKey();
            VulkanShaderState shader = material.shader;
            long shaderHandle = shader.effect() == VulkanShaderState.CUSTOM
                    ? resources.shaderProgram(shader.customShaderHandle()) : 0L;
            output.putInt(0).putInt(blendValue(material.blendMode))
                    .putInt(filterValue(material.textureFilter)).putInt(shader.effect())
                    .putLong(shaderHandle)
                    .putFloat(shader.red()).putFloat(shader.green()).putFloat(shader.blue())
                    .putFloat(shader.alpha()).putFloat(shader.teamColorAmount())
                    .putFloat(shader.screenBaseWidth()).putFloat(shader.screenBaseHeight())
                    .putFloat(shader.resolutionWidth()).putFloat(shader.resolutionHeight())
                    .putFloat(shader.displacementOffset()).putFloat(shader.uiScaling());
            int customCount = shader.customValueCount();
            output.putInt(customCount).putInt(0).putInt(0);
            for (int index = 0; index < VulkanShaderState.MAX_CUSTOM_FLOATS; index++) {
                output.putFloat(index < customCount ? shader.customValue(index) : 0.0f);
            }
        }
        return output.array();
    }

    private static ByteBuffer allocateRecords(int count, int stride) {
        return ByteBuffer.allocate(Math.multiplyExact(count, stride))
                .order(ByteOrder.LITTLE_ENDIAN);
    }

    private static int blendValue(VulkanBlendMode value) {
        switch (value) {
            case NORMAL: return FrameStreamRecordFormat.MATERIAL_BLEND_NORMAL;
            case ADDITIVE: return FrameStreamRecordFormat.MATERIAL_BLEND_ADDITIVE;
            case COPY: return FrameStreamRecordFormat.MATERIAL_BLEND_COPY;
            case MODULATE: return FrameStreamRecordFormat.MATERIAL_BLEND_MODULATE;
            default: throw new AssertionError(value);
        }
    }

    private static int filterValue(VulkanTextureFilter value) {
        switch (value) {
            case LINEAR: return FrameStreamRecordFormat.MATERIAL_FILTER_LINEAR;
            case NEAREST: return FrameStreamRecordFormat.MATERIAL_FILTER_NEAREST;
            default: throw new AssertionError(value);
        }
    }

    private static float pixelToNdcX(float value, int width) {
        return value * 2.0f / width - 1.0f;
    }

    private static float pixelToNdcY(float value, int height) {
        return value * 2.0f / height - 1.0f;
    }

    private static final class PassRecord {
        private long targetHandle;
        private int firstBatch;
        private int batchCount;
        private int flags;
        private VulkanFrameCommands frame;

        private PassRecord() { }

        private PassRecord(long targetHandle, int firstBatch, int batchCount,
                           VulkanFrameCommands frame, boolean swapchain) {
            set(targetHandle, firstBatch, batchCount, frame, swapchain);
        }

        private void set(long targetHandle, int firstBatch, int batchCount,
                         VulkanFrameCommands frame, boolean swapchain) {
            this.targetHandle = targetHandle;
            this.firstBatch = firstBatch;
            this.batchCount = batchCount;
            this.frame = frame;
            flags = FrameStreamRecordFormat.PASS_STORE
                    | (frame.clearRequested() ? FrameStreamRecordFormat.PASS_CLEAR_COLOR : 0)
                    | (swapchain ? FrameStreamRecordFormat.PASS_SWAPCHAIN : 0);
        }
    }

    private static final class BatchRecord {
        private int materialIndex;
        private long primaryTexture;
        private long secondaryTexture;
        private VulkanClipRect clip;
        private int vertexByteOffset;
        private int vertexCount;
        private int vertexLayout;

        private BatchRecord() { }

        private BatchRecord(int materialIndex, long primaryTexture, long secondaryTexture,
                            VulkanClipRect clip, int vertexByteOffset,
                            int vertexCount, int vertexLayout) {
            set(materialIndex, primaryTexture, secondaryTexture, clip,
                    vertexByteOffset, vertexCount, vertexLayout);
        }

        private void set(int materialIndex, long primaryTexture, long secondaryTexture,
                         VulkanClipRect clip, int vertexByteOffset,
                         int vertexCount, int vertexLayout) {
            this.materialIndex = materialIndex;
            this.primaryTexture = primaryTexture;
            this.secondaryTexture = secondaryTexture;
            this.clip = clip;
            this.vertexByteOffset = vertexByteOffset;
            this.vertexCount = vertexCount;
            this.vertexLayout = vertexLayout;
        }

        private boolean compatible(int nextMaterial, long nextPrimary, long nextSecondary,
                                   VulkanClipRect nextClip, int nextLayout, int nextByteOffset) {
            return materialIndex == nextMaterial && primaryTexture == nextPrimary
                    && secondaryTexture == nextSecondary && vertexLayout == nextLayout
                    && (clip == nextClip || clip != null && clip.equals(nextClip))
                    && vertexByteOffset + vertexCount
                            * FrameStreamRecordFormat.vertexStride(vertexLayout) == nextByteOffset;
        }
    }

    private static final class MaterialKey {
        private VulkanBlendMode blendMode;
        private VulkanTextureFilter textureFilter;
        private VulkanShaderState shader;

        private MaterialKey() { }

        private MaterialKey(VulkanBlendMode blendMode, VulkanTextureFilter textureFilter,
                            VulkanShaderState shader) {
            set(blendMode, textureFilter, shader);
        }

        private void set(VulkanBlendMode blendMode, VulkanTextureFilter textureFilter,
                         VulkanShaderState shader) {
            this.blendMode = blendMode;
            this.textureFilter = textureFilter;
            this.shader = shader;
        }

        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof MaterialKey)) return false;
            MaterialKey that = (MaterialKey) other;
            return blendMode == that.blendMode && textureFilter == that.textureFilter
                    && shader.equals(that.shader);
        }

        @Override public int hashCode() {
            int result = blendMode.hashCode();
            result = 31 * result + textureFilter.hashCode();
            return 31 * result + shader.hashCode();
        }
    }
}
