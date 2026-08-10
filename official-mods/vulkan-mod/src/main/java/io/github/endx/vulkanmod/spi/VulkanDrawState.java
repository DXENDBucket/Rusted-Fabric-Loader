package io.github.endx.vulkanmod.spi;

/** Per-primitive CPU transform and optional GPU scissor state. */
public final class VulkanDrawState {
    public static final VulkanDrawState DEFAULT =
            new VulkanDrawState(VulkanTransform2D.IDENTITY, null);

    private final VulkanTransform2D transform;
    private final VulkanClipRect clip;

    public VulkanDrawState(VulkanTransform2D transform, VulkanClipRect clip) {
        if (transform == null) throw new NullPointerException("transform");
        this.transform = transform;
        this.clip = clip;
    }

    public static VulkanDrawState transformed(VulkanTransform2D transform) {
        return new VulkanDrawState(transform, null);
    }

    public static VulkanDrawState clipped(VulkanClipRect clip) {
        if (clip == null) throw new NullPointerException("clip");
        return new VulkanDrawState(VulkanTransform2D.IDENTITY, clip);
    }

    public VulkanTransform2D transform() { return transform; }
    public VulkanClipRect clip() { return clip; }
}
