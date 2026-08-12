package io.github.endx.vulkanmod.spi;

/** One independently coloured and textured screen-space triangle. */
public final class VulkanTexturedTriangle implements VulkanDrawCommand {
    private long textureHandle;
    private final float[] positions = new float[6];
    private final float[] uvs = new float[6];
    private final float[] colors = new float[12];
    private VulkanDrawState state;
    private VulkanFrameCommandPool owner;

    VulkanTexturedTriangle() { }

    public VulkanTexturedTriangle(long textureHandle, float[] positions, float[] uvs,
                                  float[] colors, VulkanDrawState state) {
        set(textureHandle, positions, uvs, colors, state);
    }

    static VulkanTexturedTriangle acquire(VulkanFrameCommandPool pool, long textureHandle,
                                          float[] positions, float[] uvs, float[] colors,
                                          VulkanDrawState state) {
        VulkanTexturedTriangle command = pool.acquireTexturedTriangle();
        command.owner = pool;
        try {
            command.set(textureHandle, positions, uvs, colors, state);
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

    private void set(long textureHandle, float[] positions, float[] uvs,
                     float[] colors, VulkanDrawState state) {
        if (textureHandle <= 0L) throw new IllegalArgumentException("invalid texture handle");
        if (positions == null || positions.length != 6) {
            throw new IllegalArgumentException("triangle positions must contain 6 floats");
        }
        if (uvs == null || uvs.length != 6) {
            throw new IllegalArgumentException("triangle UVs must contain 6 floats");
        }
        if (colors == null || colors.length != 12) {
            throw new IllegalArgumentException("triangle colors must contain 12 floats");
        }
        if (state == null) throw new NullPointerException("state");
        requireFinite(positions);
        requireFinite(uvs);
        requireFinite(colors);
        this.textureHandle = textureHandle;
        System.arraycopy(positions, 0, this.positions, 0, positions.length);
        System.arraycopy(uvs, 0, this.uvs, 0, uvs.length);
        for (int index = 0; index < colors.length; index++) {
            this.colors[index] = Math.max(0.0f, Math.min(1.0f, colors[index]));
        }
        this.state = state;
    }

    public long textureHandle() { return textureHandle; }
    public float x(int vertex) { return positions[checkedVertex(vertex) * 2]; }
    public float y(int vertex) { return positions[checkedVertex(vertex) * 2 + 1]; }
    public float u(int vertex) { return uvs[checkedVertex(vertex) * 2]; }
    public float v(int vertex) { return uvs[checkedVertex(vertex) * 2 + 1]; }
    public float red(int vertex) { return colors[checkedVertex(vertex) * 4]; }
    public float green(int vertex) { return colors[checkedVertex(vertex) * 4 + 1]; }
    public float blue(int vertex) { return colors[checkedVertex(vertex) * 4 + 2]; }
    public float alpha(int vertex) { return colors[checkedVertex(vertex) * 4 + 3]; }
    public VulkanDrawState state() { return state; }
    @Override public int vertexCount() { return 3; }
    @Override public boolean textured() { return true; }

    private static int checkedVertex(int vertex) {
        if (vertex < 0 || vertex > 2) throw new IndexOutOfBoundsException("vertex " + vertex);
        return vertex;
    }

    private static void requireFinite(float[] values) {
        for (float value : values) {
            if (!Float.isFinite(value)) throw new IllegalArgumentException("triangle must be finite");
        }
    }

}
