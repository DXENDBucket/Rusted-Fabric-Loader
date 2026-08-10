package io.github.endx.vulkanmod.spi;

import java.util.Arrays;

/** One independently coloured screen-space triangle. */
public final class VulkanColoredTriangle implements VulkanDrawCommand {
    private final float[] positions;
    private final float[] colors;
    private final VulkanDrawState state;

    public VulkanColoredTriangle(float[] positions, float[] colors, VulkanDrawState state) {
        if (positions == null || positions.length != 6) {
            throw new IllegalArgumentException("triangle positions must contain 6 floats");
        }
        if (colors == null || colors.length != 12) {
            throw new IllegalArgumentException("triangle colors must contain 12 floats");
        }
        if (state == null) throw new NullPointerException("state");
        requireFinite(positions);
        requireFinite(colors);
        this.positions = Arrays.copyOf(positions, positions.length);
        this.colors = clampCopy(colors);
        this.state = state;
    }

    public float x(int vertex) { return positions[checkedVertex(vertex) * 2]; }
    public float y(int vertex) { return positions[checkedVertex(vertex) * 2 + 1]; }
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
