package io.github.endx.vulkanmod.spi;

import java.util.Arrays;

/** Immutable reusable relative geometry for a homogeneous textured-quad run. */
public final class VulkanTexturedQuadGeometry {
    public static final int FLOATS_PER_QUAD = 8;
    public static final int MAX_QUADS = (1 << 16) / 4;

    private final float[] quads;
    private final int quadCount;

    public VulkanTexturedQuadGeometry(float[] quads) {
        if (quads == null) throw new NullPointerException("quads");
        if (quads.length == 0 || quads.length % FLOATS_PER_QUAD != 0) {
            throw new IllegalArgumentException("quad data must contain complete non-empty quads");
        }
        quadCount = quads.length / FLOATS_PER_QUAD;
        if (quadCount > MAX_QUADS) {
            throw new IllegalArgumentException("too many quads: " + quadCount);
        }
        for (int index = 0; index < quads.length; index++) {
            if (!Float.isFinite(quads[index])) {
                throw new IllegalArgumentException("quad[" + index + "] must be finite");
            }
        }
        for (int quad = 0; quad < quadCount; quad++) {
            int offset = quad * FLOATS_PER_QUAD;
            if (quads[offset + 2] < 0.0f || quads[offset + 3] < 0.0f) {
                throw new IllegalArgumentException(
                        "quad width and height must be non-negative at " + quad);
            }
        }
        this.quads = Arrays.copyOf(quads, quads.length);
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

    private float value(int quad, int component) {
        if (quad < 0 || quad >= quadCount) throw new IndexOutOfBoundsException(quad);
        return quads[quad * FLOATS_PER_QUAD + component];
    }
}
