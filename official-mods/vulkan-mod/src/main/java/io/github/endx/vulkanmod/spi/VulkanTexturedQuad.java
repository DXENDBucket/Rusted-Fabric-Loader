package io.github.endx.vulkanmod.spi;

/** A screen-space textured rectangle with normalized UVs and a straight-alpha colour tint. */
public final class VulkanTexturedQuad implements VulkanDrawCommand {
    private long textureHandle;
    private float x;
    private float y;
    private float width;
    private float height;
    private float u0;
    private float v0;
    private float u1;
    private float v1;
    private float red;
    private float green;
    private float blue;
    private float alpha;
    private VulkanDrawState state;
    private VulkanFrameCommandPool owner;

    VulkanTexturedQuad() { }

    public VulkanTexturedQuad(long textureHandle,
                              float x, float y, float width, float height,
                              float u0, float v0, float u1, float v1,
                              float red, float green, float blue, float alpha) {
        this(textureHandle, x, y, width, height, u0, v0, u1, v1,
                red, green, blue, alpha, VulkanDrawState.DEFAULT);
    }

    public VulkanTexturedQuad(long textureHandle,
                              float x, float y, float width, float height,
                              float u0, float v0, float u1, float v1,
                              float red, float green, float blue, float alpha,
                              VulkanDrawState state) {
        set(textureHandle, x, y, width, height, u0, v0, u1, v1,
                red, green, blue, alpha, state);
    }

    static VulkanTexturedQuad acquire(VulkanFrameCommandPool pool, long textureHandle,
                                      float x, float y, float width, float height,
                                      float u0, float v0, float u1, float v1,
                                      float red, float green, float blue, float alpha,
                                      VulkanDrawState state) {
        VulkanTexturedQuad command = pool.acquireTexturedQuad();
        command.owner = pool;
        try {
            command.set(textureHandle, x, y, width, height, u0, v0, u1, v1,
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
        state = null;
        pool.recycle(this);
    }

    private void set(long textureHandle,
                     float x, float y, float width, float height,
                     float u0, float v0, float u1, float v1,
                     float red, float green, float blue, float alpha,
                     VulkanDrawState state) {
        if (textureHandle <= 0L) throw new IllegalArgumentException("invalid texture handle");
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
        if (state == null) throw new NullPointerException("state");
        this.textureHandle = textureHandle;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.u0 = u0;
        this.v0 = v0;
        this.u1 = u1;
        this.v1 = v1;
        this.red = clamp(red);
        this.green = clamp(green);
        this.blue = clamp(blue);
        this.alpha = clamp(alpha);
        this.state = state;
    }

    public long textureHandle() { return textureHandle; }
    public float x() { return x; }
    public float y() { return y; }
    public float width() { return width; }
    public float height() { return height; }
    public float u0() { return u0; }
    public float v0() { return v0; }
    public float u1() { return u1; }
    public float v1() { return v1; }
    public float red() { return red; }
    public float green() { return green; }
    public float blue() { return blue; }
    public float alpha() { return alpha; }
    public VulkanDrawState state() { return state; }
    @Override public int vertexCount() { return 6; }
    @Override public boolean textured() { return true; }

    private static float clamp(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static void requireFinite(String name, float value) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }
}
