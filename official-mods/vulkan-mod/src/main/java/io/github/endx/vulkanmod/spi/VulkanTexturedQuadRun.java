package io.github.endx.vulkanmod.spi;

import java.util.Arrays;

/**
 * Recyclable adjacent sprite run. Material/clip state is shared while every quad retains its own
 * tint and affine transform, allowing units with different positions/rotations to share one Java
 * command without changing draw order.
 */
public final class VulkanTexturedQuadRun implements VulkanDrawCommand {
    public static final int FLOATS_PER_QUAD = 18;
    public static final int MAX_QUADS = (1 << 16) / 4;

    private long textureHandle;
    private float[] quads = new float[FLOATS_PER_QUAD * 16];
    private int quadCount;
    private VulkanDrawState state;
    private VulkanFrameCommandPool owner;

    VulkanTexturedQuadRun() { }

    static VulkanTexturedQuadRun acquire(VulkanFrameCommandPool pool,
                                         long textureHandle,
                                         float x, float y, float width, float height,
                                         float u0, float v0, float u1, float v1,
                                         float red, float green, float blue, float alpha,
                                         VulkanDrawState state) {
        VulkanTexturedQuadRun run = pool.acquireTexturedQuadRun();
        run.owner = pool;
        try {
            run.begin(textureHandle, state);
            run.append(x, y, width, height, u0, v0, u1, v1,
                    red, green, blue, alpha, state.transform());
            return run;
        } catch (RuntimeException failure) {
            run.release(pool);
            throw failure;
        }
    }

    boolean canAppend(long candidateTexture, VulkanDrawState candidateState) {
        return quadCount < MAX_QUADS && textureHandle == candidateTexture
                && sameMaterialState(state, candidateState);
    }

    void append(float x, float y, float width, float height,
                float u0, float v0, float u1, float v1,
                float red, float green, float blue, float alpha,
                VulkanTransform2D transform) {
        if (quadCount >= MAX_QUADS) throw new IllegalStateException("sprite run is full");
        requireFinite("x", x);
        requireFinite("y", y);
        requireFinite("width", width);
        requireFinite("height", height);
        requireFinite("x + width", x + width);
        requireFinite("y + height", y + height);
        requireFinite("u0", u0);
        requireFinite("v0", v0);
        requireFinite("u1", u1);
        requireFinite("v1", v1);
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
        quads[offset + 4] = u0;
        quads[offset + 5] = v0;
        quads[offset + 6] = u1;
        quads[offset + 7] = v1;
        quads[offset + 8] = clamp(red);
        quads[offset + 9] = clamp(green);
        quads[offset + 10] = clamp(blue);
        quads[offset + 11] = clamp(alpha);
        quads[offset + 12] = transform.m00();
        quads[offset + 13] = transform.m01();
        quads[offset + 14] = transform.m02();
        quads[offset + 15] = transform.m10();
        quads[offset + 16] = transform.m11();
        quads[offset + 17] = transform.m12();
    }

    private void begin(long textureHandle, VulkanDrawState sourceState) {
        if (textureHandle <= 0L) throw new IllegalArgumentException("invalid texture handle");
        if (sourceState == null) throw new NullPointerException("state");
        this.textureHandle = textureHandle;
        this.quadCount = 0;
        this.state = sourceState.transform() == VulkanTransform2D.IDENTITY
                ? sourceState
                : new VulkanDrawState(VulkanTransform2D.IDENTITY, sourceState.clip(),
                        sourceState.blendMode(), sourceState.textureFilter(),
                        sourceState.shaderState());
    }

    void release(VulkanFrameCommandPool pool) {
        if (owner != pool) return;
        owner = null;
        textureHandle = 0L;
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
    public float u0(int quad) { return value(quad, 4); }
    public float v0(int quad) { return value(quad, 5); }
    public float u1(int quad) { return value(quad, 6); }
    public float v1(int quad) { return value(quad, 7); }
    public float red(int quad) { return value(quad, 8); }
    public float green(int quad) { return value(quad, 9); }
    public float blue(int quad) { return value(quad, 10); }
    public float alpha(int quad) { return value(quad, 11); }
    public float transformM00(int quad) { return value(quad, 12); }
    public float transformM01(int quad) { return value(quad, 13); }
    public float transformM02(int quad) { return value(quad, 14); }
    public float transformM10(int quad) { return value(quad, 15); }
    public float transformM11(int quad) { return value(quad, 16); }
    public float transformM12(int quad) { return value(quad, 17); }

    @Override public VulkanDrawState state() { return state; }
    @Override public int vertexCount() { return Math.multiplyExact(quadCount, 4); }
    @Override public boolean textured() { return true; }
    @Override public long textureHandle() { return textureHandle; }

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
                && first.textureFilter() == second.textureFilter()
                && first.shaderState().equals(second.shaderState())
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
