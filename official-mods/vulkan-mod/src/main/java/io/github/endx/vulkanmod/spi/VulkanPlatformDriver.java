package io.github.endx.vulkanmod.spi;

/** Minimal ABI shared by the mod and class-loader-isolated platform implementations. */
public interface VulkanPlatformDriver extends AutoCloseable {
    String name();
    VulkanProbeResult probe();
    VulkanSurfaceInfo createSurface(VulkanSurfaceRequest request);
    long uploadTexture(VulkanTextureData texture);
    void destroyTexture(long textureHandle);
    /**
     * Presents a frame, or returns {@code null} when the surface temporarily cannot acquire an
     * image (for example while its Win32 window is occluded or minimized).
     */
    VulkanSurfaceInfo presentFrame(VulkanFrameCommands frame);
    @Override default void close() { }
}
