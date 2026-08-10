package io.github.endx.vulkanmod.spi;

import java.util.Arrays;

/** One independently coloured and textured screen-space triangle. */
public final class VulkanTexturedTriangle implements VulkanDrawCommand {
    private final long textureHandle;
    private final float[] positions;
    private final float[] uvs;
    private final float[] colors;
    private final VulkanDrawState state;

    public VulkanTexturedTriangle(long textureHandle, float[] positions, float[] uvs,
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
        this.positions = Arrays.copyOf(positions, positions.length);
        this.uvs = Arrays.copyOf(uvs, uvs.length);
        this.colors = clampCopy(colors);
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

    private static int checkedVertex(int vertex) {
        if (vertex < 0 || vertex > 2) throw new IndexOutOfBoundsException("vertex " + vertex);
        return vertex;
    }

    private static void requireFinite(float[] values) {
        for (float value : values) {
            if (!Float.isFinite(value)) throw new IllegalArgumentException("triangle must be finite");
        }
    }

    private static float[] clampCopy(float[] source) {
        float[] result = Arrays.copyOf(source, source.length);
        for (int index = 0; index < result.length; index++) {
            result[index] = Math.max(0.0f, Math.min(1.0f, result[index]));
        }
        return result;
    }
}
