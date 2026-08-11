package io.github.endx.vulkanmod.spi;

/** One ordered offscreen write in a {@link VulkanFrameSubmission}. */
public final class VulkanRenderTargetPass {
    private final long textureHandle;
    private final VulkanFrameCommands frame;

    public VulkanRenderTargetPass(long textureHandle, VulkanFrameCommands frame) {
        if (textureHandle <= 0L) throw new IllegalArgumentException("textureHandle must be positive");
        if (frame == null) throw new NullPointerException("frame");
        this.textureHandle = textureHandle;
        this.frame = frame;
    }

    public long textureHandle() { return textureHandle; }
    public VulkanFrameCommands frame() { return frame; }
}
