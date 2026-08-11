package io.github.endx.vulkanmod.framestream;

/** Frozen version-1 record sizes, flags, and enum values inside FrameStream sections. */
public final class FrameStreamRecordFormat {
    public static final int PASS_BYTES = 64;
    public static final int BATCH_BYTES = 64;
    public static final int MATERIAL_BYTES = 160;

    public static final int PASS_CLEAR_COLOR = 1;
    public static final int PASS_STORE = 1 << 1;
    public static final int PASS_SWAPCHAIN = 1 << 2;
    public static final int PASS_KNOWN_FLAGS = PASS_CLEAR_COLOR | PASS_STORE | PASS_SWAPCHAIN;

    public static final int BATCH_HAS_CLIP = 1;
    public static final int BATCH_TEXTURED = 1 << 1;
    public static final int BATCH_INDEXED = 1 << 2;
    public static final int BATCH_KNOWN_FLAGS = BATCH_HAS_CLIP | BATCH_TEXTURED | BATCH_INDEXED;

    public static final int TOPOLOGY_TRIANGLE_LIST = 1;
    public static final int INDEX_NONE = 0;
    public static final int INDEX_UINT16 = 1;
    public static final int INDEX_UINT32 = 2;

    public static final int VERTEX_COLORED = 1;
    public static final int VERTEX_TEXTURED = 2;
    public static final int VERTEX_CUSTOM_TEXTURED = 3;
    public static final int COLORED_VERTEX_BYTES = 6 * Float.BYTES;
    public static final int TEXTURED_VERTEX_BYTES = 8 * Float.BYTES;
    public static final int CUSTOM_TEXTURED_VERTEX_BYTES = 16 * Float.BYTES;

    public static final int MATERIAL_BLEND_NORMAL = 0;
    public static final int MATERIAL_BLEND_ADDITIVE = 1;
    public static final int MATERIAL_BLEND_COPY = 2;
    public static final int MATERIAL_BLEND_MODULATE = 3;
    public static final int MATERIAL_FILTER_LINEAR = 0;
    public static final int MATERIAL_FILTER_NEAREST = 1;

    public static final int NO_DEBUG_LABEL = -1;

    private FrameStreamRecordFormat() { }

    public static int vertexStride(int layout) {
        switch (layout) {
            case VERTEX_COLORED: return COLORED_VERTEX_BYTES;
            case VERTEX_TEXTURED: return TEXTURED_VERTEX_BYTES;
            case VERTEX_CUSTOM_TEXTURED: return CUSTOM_TEXTURED_VERTEX_BYTES;
            default: throw new FrameStreamFormatException("unknown vertex layout " + layout);
        }
    }
}
