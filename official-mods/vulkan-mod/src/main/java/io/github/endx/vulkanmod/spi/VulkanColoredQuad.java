package io.github.endx.vulkanmod.spi;

/** A screen-space rectangle using top-left pixel coordinates and straight-alpha colour. */
public final class VulkanColoredQuad implements VulkanDrawCommand {
    private float x;
    private float y;
    private float width;
    private float height;
    private float red;
    private float green;
    private float blue;
    private float alpha;
    private VulkanDrawState state;
    private VulkanFrameCommandPool owner;

    VulkanColoredQuad() { }

    public VulkanColoredQuad(float x, float y, float width, float height,
                             float red, float green, float blue, float alpha) {
        this(x, y, width, height, red, green, blue, alpha, VulkanDrawState.DEFAULT);
    }

    public VulkanColoredQuad(float x, float y, float width, float height,
                             float red, float green, float blue, float alpha,
                             VulkanDrawState state) {
        set(x, y, width, height, red, green, blue, alpha, state);
    }

    static VulkanColoredQuad acquire(VulkanFrameCommandPool pool,
                                     float x, float y, float width, float height,
                                     float red, float green, float blue, float alpha,
                                     VulkanDrawState state) {
        VulkanColoredQuad command = pool.acquireColoredQuad();
        command.owner = pool;
        try {
            command.set(x, y, width, height, red, green, blue, alpha, state);
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

    private void set(float x, float y, float width, float height,
                     float red, float green, float blue, float alpha,
                     VulkanDrawState state) {
        requireFinite("x", x);
        requireFinite("y", y);
        requireFinite("width", width);
        requireFinite("height", height);
        requireFinite("red", red);
        requireFinite("green", green);
        requireFinite("blue", blue);
        requireFinite("alpha", alpha);
        requireFinite("x + width", x + width);
        requireFinite("y + height", y + height);
        if (width < 0.0f || height < 0.0f) {
            throw new IllegalArgumentException("quad width and height must be non-negative");
        }
        if (state == null) throw new NullPointerException("state");
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.red = clamp(red);
        this.green = clamp(green);
        this.blue = clamp(blue);
        this.alpha = clamp(alpha);
        this.state = state;
    }

    public float x() { return x; }
    public float y() { return y; }
    public float width() { return width; }
    public float height() { return height; }
    public float red() { return red; }
    public float green() { return green; }
    public float blue() { return blue; }
    public float alpha() { return alpha; }
    public VulkanDrawState state() { return state; }

    private static float clamp(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static void requireFinite(String name, float value) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }
}
