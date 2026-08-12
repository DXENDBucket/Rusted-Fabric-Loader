package io.github.endx.vulkanmod.spi;

/** A compact screen-space circle expanded to a triangle list by the FrameStream encoder. */
public final class VulkanColoredCircle implements VulkanDrawCommand {
    private float x;
    private float y;
    private float radius;
    private float thickness;
    private float red;
    private float green;
    private float blue;
    private float alpha;
    private int segments;
    private boolean filled;
    private VulkanDrawState state;
    private VulkanFrameCommandPool owner;

    VulkanColoredCircle() { }

    public VulkanColoredCircle(float x, float y, float radius, float thickness,
                               float red, float green, float blue, float alpha,
                               int segments, boolean filled, VulkanDrawState state) {
        set(x, y, radius, thickness, red, green, blue, alpha, segments, filled, state);
    }

    static VulkanColoredCircle acquire(VulkanFrameCommandPool pool,
                                       float x, float y, float radius, float thickness,
                                       float red, float green, float blue, float alpha,
                                       int segments, boolean filled, VulkanDrawState state) {
        VulkanColoredCircle command = pool.acquireColoredCircle();
        command.owner = pool;
        try {
            command.set(x, y, radius, thickness, red, green, blue, alpha,
                    segments, filled, state);
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

    private void set(float x, float y, float radius, float thickness,
                     float red, float green, float blue, float alpha,
                     int segments, boolean filled, VulkanDrawState state) {
        requireFinite(x);
        requireFinite(y);
        requireFinite(radius);
        requireFinite(thickness);
        if (radius < 0.0f) throw new IllegalArgumentException("circle radius must be non-negative");
        if (thickness < 0.0f) throw new IllegalArgumentException("circle thickness must be non-negative");
        if (segments < 3 || segments > 256) {
            throw new IllegalArgumentException("circle segments must be between 3 and 256");
        }
        if (state == null) throw new NullPointerException("state");
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.thickness = thickness;
        this.red = clamp(red);
        this.green = clamp(green);
        this.blue = clamp(blue);
        this.alpha = clamp(alpha);
        this.segments = segments;
        this.filled = filled;
        this.state = state;
    }

    public float x() { return x; }
    public float y() { return y; }
    public float radius() { return radius; }
    public float thickness() { return thickness; }
    public float red() { return red; }
    public float green() { return green; }
    public float blue() { return blue; }
    public float alpha() { return alpha; }
    public int segments() { return segments; }
    public boolean filled() { return filled; }
    public VulkanDrawState state() { return state; }

    private static float clamp(float value) {
        requireFinite(value);
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static void requireFinite(float value) {
        if (!Float.isFinite(value)) throw new IllegalArgumentException("circle must be finite");
    }
}
