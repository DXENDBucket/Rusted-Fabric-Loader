package io.github.endx.vulkanmod.framestream;

/** Typed 8/24/32 resource handle shared by Java and native FrameStream decoders. */
public final class FrameResourceHandle {
    public static final int TYPE_TEXTURE = 1;
    public static final int TYPE_SHADER_PROGRAM = 2;
    public static final int TYPE_PIPELINE_FAMILY = 3;
    public static final int TYPE_GLYPH_ATLAS = 4;
    public static final int MAX_TYPE = 0xff;
    public static final int MAX_GENERATION = 0x00ffffff;

    private FrameResourceHandle() { }

    public static long encode(int type, int generation, long slot) {
        if (type <= 0 || type > MAX_TYPE) {
            throw new IllegalArgumentException("resource type must be in [1,255]");
        }
        if (generation <= 0 || generation > MAX_GENERATION) {
            throw new IllegalArgumentException(
                    "resource generation must be in [1,16777215]");
        }
        if (slot < 0L || slot > 0xffffffffL) {
            throw new IllegalArgumentException("resource slot must fit unsigned 32 bits");
        }
        return ((long) type << 56) | ((long) generation << 32) | slot;
    }

    public static int type(long handle) {
        return (int) (handle >>> 56);
    }

    public static int generation(long handle) {
        return (int) ((handle >>> 32) & MAX_GENERATION);
    }

    public static long slot(long handle) {
        return handle & 0xffffffffL;
    }

    public static boolean isNull(long handle) {
        return handle == 0L;
    }

    public static void requireType(long handle, int expectedType) {
        if (handle == 0L) throw new IllegalArgumentException("resource handle is null");
        if (type(handle) != expectedType) {
            throw new IllegalArgumentException("resource handle type " + type(handle)
                    + " does not match expected type " + expectedType);
        }
        if (generation(handle) == 0) {
            throw new IllegalArgumentException("resource handle has generation zero");
        }
    }
}
