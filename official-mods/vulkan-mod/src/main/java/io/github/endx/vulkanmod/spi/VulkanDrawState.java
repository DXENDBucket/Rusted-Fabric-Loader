package io.github.endx.vulkanmod.spi;

/** Per-primitive CPU transform and optional GPU scissor state. */
public final class VulkanDrawState {
    public static final VulkanDrawState DEFAULT =
            new VulkanDrawState(VulkanTransform2D.IDENTITY, null, VulkanBlendMode.NORMAL,
                    VulkanTextureFilter.LINEAR);

    private final VulkanTransform2D transform;
    private final VulkanClipRect clip;
    private final VulkanBlendMode blendMode;
    private final VulkanTextureFilter textureFilter;

    public VulkanDrawState(VulkanTransform2D transform, VulkanClipRect clip) {
        this(transform, clip, VulkanBlendMode.NORMAL);
    }

    public VulkanDrawState(VulkanTransform2D transform, VulkanClipRect clip,
                           VulkanBlendMode blendMode) {
        this(transform, clip, blendMode, VulkanTextureFilter.LINEAR);
    }

    public VulkanDrawState(VulkanTransform2D transform, VulkanClipRect clip,
                           VulkanBlendMode blendMode, VulkanTextureFilter textureFilter) {
        if (transform == null) throw new NullPointerException("transform");
        if (blendMode == null) throw new NullPointerException("blendMode");
        if (textureFilter == null) throw new NullPointerException("textureFilter");
        this.transform = transform;
        this.clip = clip;
        this.blendMode = blendMode;
        this.textureFilter = textureFilter;
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
    public VulkanBlendMode blendMode() { return blendMode; }
    public VulkanTextureFilter textureFilter() { return textureFilter; }
}
