package io.github.endx.vulkanmod.spi;

/** Immutable affine transform applied in screen-pixel space before Vulkan projection. */
public final class VulkanTransform2D {
    public static final VulkanTransform2D IDENTITY =
            new VulkanTransform2D(1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);

    private final float m00;
    private final float m01;
    private final float m02;
    private final float m10;
    private final float m11;
    private final float m12;

    public VulkanTransform2D(float m00, float m01, float m02,
                             float m10, float m11, float m12) {
        requireFinite(m00);
        requireFinite(m01);
        requireFinite(m02);
        requireFinite(m10);
        requireFinite(m11);
        requireFinite(m12);
        this.m00 = m00;
        this.m01 = m01;
        this.m02 = m02;
        this.m10 = m10;
        this.m11 = m11;
        this.m12 = m12;
    }

    public static VulkanTransform2D translation(float x, float y) {
        return new VulkanTransform2D(1.0f, 0.0f, x, 0.0f, 1.0f, y);
    }

    public static VulkanTransform2D scale(float x, float y) {
        return new VulkanTransform2D(x, 0.0f, 0.0f, 0.0f, y, 0.0f);
    }

    public static VulkanTransform2D rotationDegrees(float degrees) {
        requireFinite(degrees);
        double radians = Math.toRadians(degrees);
        float cosine = (float) Math.cos(radians);
        float sine = (float) Math.sin(radians);
        return new VulkanTransform2D(cosine, -sine, 0.0f, sine, cosine, 0.0f);
    }

    public static VulkanTransform2D rotationAround(float degrees, float centerX, float centerY) {
        return translation(-centerX, -centerY)
                .then(rotationDegrees(degrees))
                .then(translation(centerX, centerY));
    }

    /** Returns a transform that applies this transform first and {@code after} second. */
    public VulkanTransform2D then(VulkanTransform2D after) {
        if (after == null) throw new NullPointerException("after");
        return new VulkanTransform2D(
                after.m00 * m00 + after.m01 * m10,
                after.m00 * m01 + after.m01 * m11,
                after.m00 * m02 + after.m01 * m12 + after.m02,
                after.m10 * m00 + after.m11 * m10,
                after.m10 * m01 + after.m11 * m11,
                after.m10 * m02 + after.m11 * m12 + after.m12);
    }

    public float transformX(float x, float y) { return m00 * x + m01 * y + m02; }
    public float transformY(float x, float y) { return m10 * x + m11 * y + m12; }
    public float m00() { return m00; }
    public float m01() { return m01; }
    public float m02() { return m02; }
    public float m10() { return m10; }
    public float m11() { return m11; }
    public float m12() { return m12; }

    private static void requireFinite(float value) {
        if (!Float.isFinite(value)) throw new IllegalArgumentException("transform must be finite");
    }
}
