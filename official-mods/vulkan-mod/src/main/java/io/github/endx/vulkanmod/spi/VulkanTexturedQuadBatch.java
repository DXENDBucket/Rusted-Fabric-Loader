package io.github.endx.vulkanmod.spi;

/** Homogeneous textured quads recorded as one Java command and one indexed GPU batch. */
public final class VulkanTexturedQuadBatch implements VulkanDrawCommand {
    private long textureHandle;
    private float originX;
    private float originY;
    private VulkanTexturedQuadGeometry geometry;
    private int quadCount;
    private float red;
    private float green;
    private float blue;
    private float alpha;
    private VulkanDrawState state;
    private VulkanFrameCommandPool owner;

    VulkanTexturedQuadBatch() { }

    public VulkanTexturedQuadBatch(long textureHandle, float originX, float originY,
                                   float[] quads, float red, float green,
                                   float blue, float alpha, VulkanDrawState state) {
        this(textureHandle, originX, originY, new VulkanTexturedQuadGeometry(quads),
                red, green, blue, alpha, state);
    }

    public VulkanTexturedQuadBatch(long textureHandle, float originX, float originY,
                                   VulkanTexturedQuadGeometry geometry,
                                   float red, float green,
                                   float blue, float alpha, VulkanDrawState state) {
        set(textureHandle, originX, originY, geometry,
                red, green, blue, alpha, state);
    }

    static VulkanTexturedQuadBatch acquire(VulkanFrameCommandPool pool,
                                            long textureHandle,
                                            float originX, float originY,
                                            VulkanTexturedQuadGeometry geometry,
                                            float red, float green, float blue, float alpha,
                                            VulkanDrawState state) {
        VulkanTexturedQuadBatch command = pool.acquireTexturedQuadBatch();
        command.owner = pool;
        try {
            command.set(textureHandle, originX, originY, geometry,
                    red, green, blue, alpha, state);
            return command;
        } catch (RuntimeException failure) {
            command.release(pool);
            throw failure;
        }
    }

    void release(VulkanFrameCommandPool pool) {
        if (owner != pool) return;
        owner = null;
        geometry = null;
        state = null;
        pool.recycle(this);
    }

    private void set(long textureHandle, float originX, float originY,
                     VulkanTexturedQuadGeometry geometry,
                     float red, float green, float blue, float alpha,
                     VulkanDrawState state) {
        if (textureHandle <= 0L) throw new IllegalArgumentException("invalid texture handle");
        requireFinite("originX", originX);
        requireFinite("originY", originY);
        if (geometry == null) throw new NullPointerException("geometry");
        requireFinite("red", red);
        requireFinite("green", green);
        requireFinite("blue", blue);
        requireFinite("alpha", alpha);
        if (state == null) throw new NullPointerException("state");
        this.textureHandle = textureHandle;
        this.originX = originX;
        this.originY = originY;
        this.geometry = geometry;
        this.quadCount = geometry.quadCount();
        this.red = clamp(red);
        this.green = clamp(green);
        this.blue = clamp(blue);
        this.alpha = clamp(alpha);
        this.state = state;
    }

    public int quadCount() { return quadCount; }
    public float originX() { return originX; }
    public float originY() { return originY; }
    public float x(int quad) { return geometry.x(quad); }
    public float y(int quad) { return geometry.y(quad); }
    public float width(int quad) { return geometry.width(quad); }
    public float height(int quad) { return geometry.height(quad); }
    public float u0(int quad) { return geometry.u0(quad); }
    public float v0(int quad) { return geometry.v0(quad); }
    public float u1(int quad) { return geometry.u1(quad); }
    public float v1(int quad) { return geometry.v1(quad); }
    public float red() { return red; }
    public float green() { return green; }
    public float blue() { return blue; }
    public float alpha() { return alpha; }
    @Override public VulkanDrawState state() { return state; }
    @Override public int vertexCount() { return Math.multiplyExact(quadCount, 4); }
    @Override public boolean textured() { return true; }
    @Override public long textureHandle() { return textureHandle; }

    private static float clamp(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static void requireFinite(String name, float value) {
        if (!Float.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
    }
}
