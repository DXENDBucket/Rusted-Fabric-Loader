package io.github.endx.vulkanmod.framestream;

import io.github.endx.vulkanmod.spi.VulkanShaderState;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Fully validated, allocation-light typed view of a FrameStream version-1 frame. */
public final class DecodedFrameStream {
    private final FrameStreamReader envelope;
    private final ByteBuffer passes;
    private final ByteBuffer batches;
    private final ByteBuffer vertices;
    private final ByteBuffer indices;
    private final ByteBuffer materials;

    private DecodedFrameStream(FrameStreamReader envelope, ByteBuffer passes,
                               ByteBuffer batches, ByteBuffer vertices,
                               ByteBuffer indices, ByteBuffer materials) {
        this.envelope = envelope;
        this.passes = passes;
        this.batches = batches;
        this.vertices = vertices;
        this.indices = indices;
        this.materials = materials;
    }

    public static DecodedFrameStream decode(ByteBuffer input) {
        FrameStreamReader envelope = FrameStreamReader.read(input);
        ByteBuffer passes = envelope.sectionData(FrameStreamFormat.SECTION_PASSES);
        ByteBuffer batches = envelope.sectionData(FrameStreamFormat.SECTION_BATCHES);
        ByteBuffer vertices = envelope.sectionData(FrameStreamFormat.SECTION_VERTICES);
        ByteBuffer indices = envelope.sectionData(FrameStreamFormat.SECTION_INDICES);
        ByteBuffer materials = envelope.sectionData(FrameStreamFormat.SECTION_MATERIALS);
        requireExactSize("pass", passes, envelope.passCount(),
                FrameStreamRecordFormat.PASS_BYTES);
        requireExactSize("batch", batches, envelope.batchCount(),
                FrameStreamRecordFormat.BATCH_BYTES);
        int materialCount = envelope.section(FrameStreamFormat.SECTION_MATERIALS).elementCount();
        requireExactSize("material", materials, materialCount,
                FrameStreamRecordFormat.MATERIAL_BYTES);

        DecodedFrameStream result = new DecodedFrameStream(envelope, passes, batches,
                vertices, indices, materials);
        result.validateMaterials(materialCount);
        result.validatePasses();
        result.validateBatches(materialCount);
        return result;
    }

    public long frameId() { return envelope.frameId(); }
    public long requiredResourceSequence() { return envelope.requiredResourceSequence(); }
    public int width() { return envelope.width(); }
    public int height() { return envelope.height(); }
    public int passCount() { return envelope.passCount(); }
    public int batchCount() { return envelope.batchCount(); }
    public int materialCount() {
        return envelope.section(FrameStreamFormat.SECTION_MATERIALS).elementCount();
    }

    public Pass pass(int index) {
        checkedIndex(index, passCount(), "pass");
        return new Pass(passes, index * FrameStreamRecordFormat.PASS_BYTES);
    }

    /** Creates one mutable pass cursor that can be reused with {@link #readPass}. */
    public Pass passCursor() { return new Pass(passes, 0); }

    public Pass readPass(int index, Pass cursor) {
        if (cursor == null) throw new NullPointerException("cursor");
        checkedIndex(index, passCount(), "pass");
        cursor.reset(passes, index * FrameStreamRecordFormat.PASS_BYTES);
        return cursor;
    }

    public Batch batch(int index) {
        checkedIndex(index, batchCount(), "batch");
        return new Batch(batches, index * FrameStreamRecordFormat.BATCH_BYTES);
    }

    /** Creates one mutable batch cursor that can be reused with {@link #readBatch}. */
    public Batch batchCursor() { return new Batch(batches, 0); }

    public Batch readBatch(int index, Batch cursor) {
        if (cursor == null) throw new NullPointerException("cursor");
        checkedIndex(index, batchCount(), "batch");
        cursor.reset(batches, index * FrameStreamRecordFormat.BATCH_BYTES);
        return cursor;
    }

    public Material material(int index) {
        checkedIndex(index, materialCount(), "material");
        return new Material(materials, index * FrameStreamRecordFormat.MATERIAL_BYTES);
    }

    /** Creates one mutable material cursor that can be reused with {@link #readMaterial}. */
    public Material materialCursor() { return new Material(materials, 0); }

    public Material readMaterial(int index, Material cursor) {
        if (cursor == null) throw new NullPointerException("cursor");
        checkedIndex(index, materialCount(), "material");
        cursor.reset(materials, index * FrameStreamRecordFormat.MATERIAL_BYTES);
        return cursor;
    }

    public ByteBuffer vertices() { return duplicate(vertices); }
    public ByteBuffer indices() { return indices == null ? null : duplicate(indices); }

    private void validatePasses() {
        int expectedBatch = 0;
        int swapchainCount = 0;
        Pass pass = passCursor();
        for (int index = 0; index < passCount(); index++) {
            readPass(index, pass);
            require(pass.firstBatch() == expectedBatch,
                    "pass batches are not one contiguous ordered range");
            require(pass.batchCount() >= 0
                            && (long) pass.firstBatch() + pass.batchCount() <= batchCount(),
                    "pass batch range is outside the batch section");
            expectedBatch += pass.batchCount();
            require((pass.flags() & ~FrameStreamRecordFormat.PASS_KNOWN_FLAGS) == 0,
                    "unknown pass flags");
            boolean swapchain = pass.isSwapchain();
            if (swapchain) swapchainCount++;
            require(swapchain == (pass.targetHandle() == 0L),
                    "swapchain flag and target handle disagree");
            if (!swapchain) requireHandle(pass.targetHandle(),
                    FrameResourceHandle.TYPE_TEXTURE, "pass target");
            require(pass.viewportX() >= 0 && pass.viewportY() >= 0,
                    "pass viewport origin is negative");
            require(pass.viewportWidth() > 0
                            && pass.viewportWidth() <= FrameStreamFormat.MAX_DIMENSION,
                    "pass viewport width is invalid");
            require(pass.viewportHeight() > 0
                            && pass.viewportHeight() <= FrameStreamFormat.MAX_DIMENSION,
                    "pass viewport height is invalid");
            requireFinite(pass.clearRed(), "pass clear red");
            requireFinite(pass.clearGreen(), "pass clear green");
            requireFinite(pass.clearBlue(), "pass clear blue");
            requireFinite(pass.clearAlpha(), "pass clear alpha");
            require(pass.orientation() == 0, "unsupported pass orientation");
            require(pass.reserved() == 0, "pass reserved field is not zero");
            require(!swapchain || index == passCount() - 1,
                    "swapchain pass must be last");
        }
        require(expectedBatch == batchCount(), "some batches are not owned by a pass");
        require(swapchainCount == 1, "FrameStream must have exactly one swapchain pass");
    }

    private void validateBatches(int materialCount) {
        int expectedVertexByte = 0;
        long expectedVertexCount = 0L;
        int expectedIndexByte = 0;
        long expectedIndexCount = 0L;
        int indexBytes = indices == null ? 0 : indices.remaining();
        Batch batch = batchCursor();
        Material material = materialCursor();
        for (int index = 0; index < batchCount(); index++) {
            readBatch(index, batch);
            require(batch.materialIndex() >= 0 && batch.materialIndex() < materialCount,
                    "batch material index is invalid");
            require((batch.flags() & ~FrameStreamRecordFormat.BATCH_KNOWN_FLAGS) == 0,
                    "unknown batch flags");
            require(batch.topology() == FrameStreamRecordFormat.TOPOLOGY_TRIANGLE_LIST,
                    "unsupported primitive topology");
            require(batch.reserved() == 0, "batch reserved field is not zero");
            int stride = FrameStreamRecordFormat.vertexStride(batch.vertexLayout());
            require(batch.vertexCount() > 0, "batch has no vertices");
            require(batch.vertexByteOffset() == expectedVertexByte,
                    "batch vertices are not tightly ordered");
            long vertexEnd = (long) batch.vertexByteOffset()
                    + (long) batch.vertexCount() * stride;
            require(vertexEnd <= vertices.remaining(), "batch vertices exceed their section");
            expectedVertexByte = (int) vertexEnd;
            expectedVertexCount += batch.vertexCount();
            require(expectedVertexCount <= Integer.MAX_VALUE, "vertex count overflows");

            boolean textured = (batch.flags() & FrameStreamRecordFormat.BATCH_TEXTURED) != 0;
            require(textured == (batch.primaryTexture() != 0L),
                    "textured flag and primary handle disagree");
            require(textured == (batch.vertexLayout()
                            != FrameStreamRecordFormat.VERTEX_COLORED),
                    "texture state and vertex layout disagree");
            if (textured) requireHandle(batch.primaryTexture(),
                    FrameResourceHandle.TYPE_TEXTURE, "primary texture");
            if (batch.secondaryTexture() != 0L) requireHandle(batch.secondaryTexture(),
                    FrameResourceHandle.TYPE_TEXTURE, "secondary texture");

            boolean clipped = (batch.flags() & FrameStreamRecordFormat.BATCH_HAS_CLIP) != 0;
            requireFinite(batch.clipX(), "clip x");
            requireFinite(batch.clipY(), "clip y");
            requireFinite(batch.clipWidth(), "clip width");
            requireFinite(batch.clipHeight(), "clip height");
            require(!clipped || batch.clipWidth() >= 0.0f && batch.clipHeight() >= 0.0f,
                    "clip dimensions are negative");
            require(clipped || batch.clipX() == 0.0f && batch.clipY() == 0.0f
                            && batch.clipWidth() == 0.0f && batch.clipHeight() == 0.0f,
                    "disabled clip payload is not zero");

            boolean indexed = (batch.flags() & FrameStreamRecordFormat.BATCH_INDEXED) != 0;
            if (!indexed) {
                require(batch.indexType() == FrameStreamRecordFormat.INDEX_NONE
                                && batch.indexByteOffset() == 0 && batch.indexCount() == 0,
                        "non-indexed batch contains index data");
            } else {
                require(indices != null, "indexed batch has no index section");
                int indexStride = batch.indexType() == FrameStreamRecordFormat.INDEX_UINT16 ? 2
                        : batch.indexType() == FrameStreamRecordFormat.INDEX_UINT32 ? 4 : 0;
                require(indexStride != 0, "invalid index type");
                long indexEnd = (long) batch.indexByteOffset()
                        + (long) batch.indexCount() * indexStride;
                require(batch.indexByteOffset() >= 0 && batch.indexCount() > 0
                                && indexEnd <= indexBytes,
                        "batch indices exceed their section");
                require(batch.indexByteOffset() % indexStride == 0,
                        "batch index offset is misaligned");
                require(batch.indexByteOffset() == expectedIndexByte,
                        "batch indices are not tightly ordered");
                validateIndices(batch, indexStride);
                expectedIndexByte = (int) indexEnd;
                expectedIndexCount += batch.indexCount();
                require(expectedIndexCount <= Integer.MAX_VALUE, "index count overflows");
            }
            readMaterial(batch.materialIndex(), material);
            require(batch.vertexLayout() != FrameStreamRecordFormat.VERTEX_CUSTOM_TEXTURED
                            || material.shaderEffect() == VulkanShaderState.CUSTOM,
                    "expanded vertex layout does not use a custom shader");
            require(batch.vertexLayout() != FrameStreamRecordFormat.VERTEX_COLORED
                            || material.shaderEffect() == VulkanShaderState.PLAIN,
                    "colored vertex layout has a textured shader effect");
            if (material.shaderEffect() == VulkanShaderState.POST_DISPLACEMENT) {
                require(batch.secondaryTexture() != 0L,
                        "displacement material has no secondary texture");
            }
        }
        require(expectedVertexByte == vertices.remaining(),
                "vertex section contains unreferenced bytes");
        require(expectedVertexCount
                        == envelope.section(FrameStreamFormat.SECTION_VERTICES).elementCount(),
                "vertex section element count does not match batches");
        require(expectedIndexByte == indexBytes,
                "index section contains unreferenced bytes");
        require(expectedIndexCount == (indices == null ? 0
                        : envelope.section(FrameStreamFormat.SECTION_INDICES).elementCount()),
                "index section element count does not match batches");
    }

    private void validateIndices(Batch batch, int indexStride) {
        ByteBuffer data = indices.duplicate().order(ByteOrder.LITTLE_ENDIAN);
        int offset = batch.indexByteOffset();
        for (int index = 0; index < batch.indexCount(); index++) {
            long value = indexStride == Short.BYTES
                    ? Short.toUnsignedLong(data.getShort(offset + index * Short.BYTES))
                    : Integer.toUnsignedLong(data.getInt(offset + index * Integer.BYTES));
            require(value < batch.vertexCount(), "batch index exceeds its vertex range");
        }
    }

    private void validateMaterials(int count) {
        Material material = materialCursor();
        for (int index = 0; index < count; index++) {
            readMaterial(index, material);
            require(material.flags() == 0, "unknown material flags");
            require(material.blendMode() >= FrameStreamRecordFormat.MATERIAL_BLEND_NORMAL
                            && material.blendMode()
                            <= FrameStreamRecordFormat.MATERIAL_BLEND_MODULATE,
                    "invalid material blend mode");
            require(material.textureFilter() == FrameStreamRecordFormat.MATERIAL_FILTER_LINEAR
                            || material.textureFilter()
                            == FrameStreamRecordFormat.MATERIAL_FILTER_NEAREST,
                    "invalid material texture filter");
            require(material.shaderEffect() >= VulkanShaderState.PLAIN
                            && material.shaderEffect() <= VulkanShaderState.CUSTOM,
                    "invalid material shader effect");
            if (material.shaderEffect() == VulkanShaderState.CUSTOM) {
                requireHandle(material.shaderHandle(),
                        FrameResourceHandle.TYPE_SHADER_PROGRAM, "material shader");
            } else {
                require(material.shaderHandle() == 0L,
                        "built-in material has a custom shader handle");
            }
            for (int floatIndex = 0; floatIndex < 11; floatIndex++) {
                requireFinite(material.shaderFloat(floatIndex), "material shader value");
            }
            require(material.shaderFloat(5) > 0.0f
                            && material.shaderFloat(6) > 0.0f
                            && material.shaderFloat(7) > 0.0f
                            && material.shaderFloat(8) > 0.0f
                            && material.shaderFloat(10) > 0.0f,
                    "material size/scaling value is not positive");
            require(material.customValueCount() >= 0
                            && material.customValueCount() <= VulkanShaderState.MAX_CUSTOM_FLOATS,
                    "invalid material custom-value count");
            require(material.reserved0() == 0 && material.reserved1() == 0,
                    "material reserved field is not zero");
            for (int custom = 0; custom < VulkanShaderState.MAX_CUSTOM_FLOATS; custom++) {
                float value = material.customValue(custom);
                requireFinite(value, "custom material value");
                require(custom < material.customValueCount() || value == 0.0f,
                        "unused custom material value is not zero");
            }
        }
    }

    private static void requireExactSize(String name, ByteBuffer section, int count, int stride) {
        int expected;
        try {
            expected = Math.multiplyExact(count, stride);
        } catch (ArithmeticException overflow) {
            throw invalid(name + " section size overflows");
        }
        require(section.remaining() == expected, name + " section has the wrong record size");
    }

    private static void requireHandle(long handle, int type, String name) {
        require(handle != 0L && FrameResourceHandle.type(handle) == type
                        && FrameResourceHandle.generation(handle) != 0,
                name + " has an invalid typed handle");
    }

    private static void checkedIndex(int index, int count, String name) {
        if (index < 0 || index >= count) throw new IndexOutOfBoundsException(name + " " + index);
    }

    private static ByteBuffer duplicate(ByteBuffer value) {
        ByteBuffer result = value.duplicate().order(ByteOrder.LITTLE_ENDIAN);
        result.position(0);
        return result;
    }

    private static void requireFinite(float value, String name) {
        require(Float.isFinite(value), name + " is not finite");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw invalid(message);
    }

    private static FrameStreamFormatException invalid(String message) {
        return new FrameStreamFormatException(message);
    }

    public static final class Pass {
        private ByteBuffer bytes;
        private int offset;

        private Pass(ByteBuffer bytes, int offset) { this.bytes = bytes; this.offset = offset; }
        private void reset(ByteBuffer bytes, int offset) {
            this.bytes = bytes;
            this.offset = offset;
        }
        public long targetHandle() { return bytes.getLong(offset); }
        public int firstBatch() { return bytes.getInt(offset + 8); }
        public int batchCount() { return bytes.getInt(offset + 12); }
        public int flags() { return bytes.getInt(offset + 16); }
        public boolean isSwapchain() {
            return (flags() & FrameStreamRecordFormat.PASS_SWAPCHAIN) != 0;
        }
        public int viewportX() { return bytes.getInt(offset + 20); }
        public int viewportY() { return bytes.getInt(offset + 24); }
        public int viewportWidth() { return bytes.getInt(offset + 28); }
        public int viewportHeight() { return bytes.getInt(offset + 32); }
        public float clearRed() { return bytes.getFloat(offset + 36); }
        public float clearGreen() { return bytes.getFloat(offset + 40); }
        public float clearBlue() { return bytes.getFloat(offset + 44); }
        public float clearAlpha() { return bytes.getFloat(offset + 48); }
        public int debugLabelIndex() { return bytes.getInt(offset + 52); }
        public int orientation() { return bytes.getInt(offset + 56); }
        public int reserved() { return bytes.getInt(offset + 60); }
    }

    public static final class Batch {
        private ByteBuffer bytes;
        private int offset;

        private Batch(ByteBuffer bytes, int offset) { this.bytes = bytes; this.offset = offset; }
        private void reset(ByteBuffer bytes, int offset) {
            this.bytes = bytes;
            this.offset = offset;
        }
        public int materialIndex() { return bytes.getInt(offset); }
        public int flags() { return bytes.getInt(offset + 4); }
        public long primaryTexture() { return bytes.getLong(offset + 8); }
        public long secondaryTexture() { return bytes.getLong(offset + 16); }
        public int vertexByteOffset() { return bytes.getInt(offset + 24); }
        public int vertexCount() { return bytes.getInt(offset + 28); }
        public int indexByteOffset() { return bytes.getInt(offset + 32); }
        public int indexCount() { return bytes.getInt(offset + 36); }
        public float clipX() { return bytes.getFloat(offset + 40); }
        public float clipY() { return bytes.getFloat(offset + 44); }
        public float clipWidth() { return bytes.getFloat(offset + 48); }
        public float clipHeight() { return bytes.getFloat(offset + 52); }
        public int topology() { return Short.toUnsignedInt(bytes.getShort(offset + 56)); }
        public int indexType() { return Short.toUnsignedInt(bytes.getShort(offset + 58)); }
        public int vertexLayout() { return Short.toUnsignedInt(bytes.getShort(offset + 60)); }
        public int reserved() { return Short.toUnsignedInt(bytes.getShort(offset + 62)); }
    }

    public static final class Material {
        private ByteBuffer bytes;
        private int offset;

        private Material(ByteBuffer bytes, int offset) { this.bytes = bytes; this.offset = offset; }
        private void reset(ByteBuffer bytes, int offset) {
            this.bytes = bytes;
            this.offset = offset;
        }
        public int flags() { return bytes.getInt(offset); }
        public int blendMode() { return bytes.getInt(offset + 4); }
        public int textureFilter() { return bytes.getInt(offset + 8); }
        public int shaderEffect() { return bytes.getInt(offset + 12); }
        public long shaderHandle() { return bytes.getLong(offset + 16); }
        public float shaderFloat(int index) {
            checkedIndex(index, 11, "shader float");
            return bytes.getFloat(offset + 24 + index * Float.BYTES);
        }
        public int customValueCount() { return bytes.getInt(offset + 68); }
        public int reserved0() { return bytes.getInt(offset + 72); }
        public int reserved1() { return bytes.getInt(offset + 76); }
        public float customValue(int index) {
            checkedIndex(index, VulkanShaderState.MAX_CUSTOM_FLOATS, "custom value");
            return bytes.getFloat(offset + 80 + index * Float.BYTES);
        }
    }
}
