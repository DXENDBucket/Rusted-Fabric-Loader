package io.github.endx.vulkanmod.spi;

/** A compact screen-space line expanded to two triangles by the FrameStream encoder. */
public final class VulkanColoredLine implements VulkanDrawCommand {
    private float x1;
    private float y1;
    private float x2;
    private float y2;
    private float thickness;
    private float red;
    private float green;
    private float blue;
    private float alpha;
    private VulkanDrawState state;
    private VulkanFrameCommandPool owner;

    VulkanColoredLine() { }

    public VulkanColoredLine(float x1, float y1, float x2, float y2, float thickness,
                             float red, float green, float blue, float alpha,
                             VulkanDrawState state) {
        set(x1, y1, x2, y2, thickness, red, green, blue, alpha, state);
    }

    static VulkanColoredLine acquire(VulkanFrameCommandPool pool,
                                     float x1, float y1, float x2, float y2, float thickness,
                                     float red, float green, float blue, float alpha,
                                     VulkanDrawState state) {
        VulkanColoredLine command = pool.acquireColoredLine();
        command.owner = pool;
        try {
            command.set(x1, y1, x2, y2, thickness, red, green, blue, alpha, state);
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

    private void set(float x1, float y1, float x2, float y2, float thickness,
                     float red, float green, float blue, float alpha,
                     VulkanDrawState state) {
        requireFinite(x1);
        requireFinite(y1);
        requireFinite(x2);
        requireFinite(y2);
        requireFinite(thickness);
        if (thickness < 0.0f) throw new IllegalArgumentException("line thickness must be non-negative");
        if (state == null) throw new NullPointerException("state");
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.thickness = thickness;
        this.red = clamp(red);
        this.green = clamp(green);
        this.blue = clamp(blue);
        this.alpha = clamp(alpha);
        this.state = state;
    }

    public float x1() { return x1; }
    public float y1() { return y1; }
    public float x2() { return x2; }
    public float y2() { return y2; }
    public float thickness() { return thickness; }
    public float red() { return red; }
    public float green() { return green; }
    public float blue() { return blue; }
    public float alpha() { return alpha; }
    public VulkanDrawState state() { return state; }

    private static float clamp(float value) {
        requireFinite(value);
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static void requireFinite(float value) {
        if (!Float.isFinite(value)) throw new IllegalArgumentException("line must be finite");
    }
}
