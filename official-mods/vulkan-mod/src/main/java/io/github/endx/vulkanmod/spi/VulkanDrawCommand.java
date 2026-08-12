package io.github.endx.vulkanmod.spi;

/** Renderer-neutral command metadata used by the FrameStream hot-path scanner. */
public interface VulkanDrawCommand {
    VulkanDrawState state();

    int vertexCount();

    default boolean textured() { return false; }

    default long textureHandle() { return 0L; }
}
