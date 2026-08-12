package io.github.endx.vulkanmod.spi;

import java.util.Arrays;

/**
 * Recyclable adjacent colored-quad run. Blend/clip state is shared while each quad retains its
 * own color and affine transform. This removes one Java command allocation per filled rectangle
 * without changing draw order.
 */
public final class VulkanColoredQuadRun implements VulkanDrawCommand {
    public static final int FLOATS_PER_QUAD = 14;
    public static final int MAX_QUADS = (1 << 16) / 4;

    private float[] quads = new float[FLOATS_PER_QUAD * 16];
    private int quadCount;
    private VulkanDrawState state;
    private VulkanFrameCommandPool owner;

    VulkanColoredQuadRun() { }

    static VulkanColoredQuadRun acquire(VulkanFrameCommandPool pool,
                                        float x, float y, float width, float height,
                                        float red, float green, float blue, float alpha,
                                        VulkanDrawState state) {
        VulkanColoredQuadRun run = pool.acquireColoredQuadRun();
        run.owner = pool;
        try {
            run.begin(state);
            run.append(x, y, width, height, red, green, blue, alpha, state.transform());
            return run;
        } catch (RuntimeException failure) {
            run.release(pool);
            throw failure;
        }
    }

    boolean canAppend(VulkanDrawState candidateState) {
        return quadCount < MAX_QUADS && sameMaterialState(state, candidateState);
    }

    void append(float x, float y, float width, float height,
                float red, float green, float blue, float alpha,
                VulkanTransform2D transform) {
        if (quadCount >= MAX_QUADS) throw new IllegalStateException("colored quad run is full");
        requireFinite("x", x);
        requireFinite("y", y);
        requireFinite("width", width);
        requireFinite("height", height);
        requireFinite("x + width", x + width);
        requireFinite("y + height", y + height);
        requireFinite("red", red);
        requireFinite("green", green);
        requireFinite("blue", blue);
        requireFinite("alpha", alpha);
        if (width < 0.0f || height < 0.0f) {
            throw new IllegalArgumentException("quad width and height must be non-negative");
        }
        if (transform == null) throw new NullPointerException("transform");
        ensureCapacity(quadCount + 1);
        int offset = quadCount++ * FLOATS_PER_QUAD;
        quads[offset] = x;
        quads[offset + 1] = y;
        quads[offset + 2] = width;
        quads[offset + 3] = height;
        quads[offset + 4] = clamp(red);
        quads[offset + 5] = clamp(green);
        quads[offset + 6] = clamp(blue);
        quads[offset + 7] = clamp(alpha);
        quads[offset + 8] = transform.m00();
        quads[offset + 9] = transform.m01();
        quads[offset + 10] = transform.m02();
        quads[offset + 11] = transform.m10();
        quads[offset + 12] = transform.m11();
        quads[offset + 13] = transform.m12();
    }

    private void begin(VulkanDrawState sourceState) {
        if (sourceState == null) throw new NullPointerException("state");
        quadCount = 0;
        state = sourceState.transform() == VulkanTransform2D.IDENTITY
                ? sourceState
                : new VulkanDrawState(VulkanTransform2D.IDENTITY, sourceState.clip(),
                        sourceState.blendMode(), sourceState.textureFilter(),
                        sourceState.shaderState());
    }

    void release(VulkanFrameCommandPool pool) {
        if (owner != pool) return;
        owner = null;
        quadCount = 0;
        state = null;
        pool.recycle(this);
    }

    private void ensureCapacity(int requiredQuads) {
        int required = Math.multiplyExact(requiredQuads, FLOATS_PER_QUAD);
        if (required <= quads.length) return;
        int capacity = quads.length;
        while (capacity < required) capacity = Math.multiplyExact(capacity, 2);
        quads = Arrays.copyOf(quads, capacity);
    }

    public int quadCount() { return quadCount; }
    public float x(int quad) { return value(quad, 0); }
    public float y(int quad) { return value(quad, 1); }
    public float width(int quad) { return value(quad, 2); }
    public float height(int quad) { return value(quad, 3); }
    public float red(int quad) { return value(quad, 4); }
    public float green(int quad) { return value(quad, 5); }
    public float blue(int quad) { return value(quad, 6); }
    public float alpha(int quad) { return value(quad, 7); }
    public float transformM00(int quad) { return value(quad, 8); }
    public float transformM01(int quad) { return value(quad, 9); }
    public float transformM02(int quad) { return value(quad, 10); }
    public float transformM10(int quad) { return value(quad, 11); }
    public float transformM11(int quad) { return value(quad, 12); }
    public float transformM12(int quad) { return value(quad, 13); }

    @Override public VulkanDrawState state() { return state; }
    @Override public int vertexCount() { return Math.multiplyExact(quadCount, 4); }
    @Override public boolean textured() { return false; }
    @Override public long textureHandle() { return 0L; }

    private float value(int quad, int component) {
        return quads[checkedOffset(quad) + component];
    }

    private int checkedOffset(int quad) {
        if (quad < 0 || quad >= quadCount) throw new IndexOutOfBoundsException(quad);
        return quad * FLOATS_PER_QUAD;
    }

    private static boolean sameMaterialState(VulkanDrawState first, VulkanDrawState second) {
        if (first == null || second == null) return false;
        VulkanClipRect firstClip = first.clip();
        VulkanClipRect secondClip = second.clip();
        return first.blendMode() == second.blendMode()
                && (firstClip == secondClip
                        || firstClip != null && firstClip.equals(secondClip));
    }

    private static float clamp(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static void requireFinite(String name, float value) {
        if (!Float.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
    }
}
