package io.github.endx.vulkanmod.spi;

/** Minimal ABI shared by the mod and class-loader-isolated platform implementations. */
public interface VulkanPlatformDriver extends AutoCloseable {
    String name();
    VulkanProbeResult probe();
    VulkanSurfaceInfo createSurface(VulkanSurfaceRequest request);
    long uploadTexture(VulkanTextureData texture);
    void destroyTexture(long textureHandle);
    /**
     * Shows or hides the driver-owned presentation surface. Returns {@code true} when the driver
     * owns a surface whose visibility can be controlled independently from the game window.
     */
    default boolean setSurfaceVisible(boolean visible) { return false; }
    /**
     * Presents the first prepared frame while revealing a driver-owned surface immediately before
     * the platform presentation call. Drivers without an independently controlled surface may use
     * the ordinary presentation path.
     */
    default VulkanSurfaceInfo presentFrameAndReveal(VulkanFrameCommands frame) {
        return presentFrame(frame);
    }
    /**
     * Presents a frame, or returns {@code null} when the surface temporarily cannot acquire an
     * image (for example while its Win32 window is occluded or minimized).
     */
    VulkanSurfaceInfo presentFrame(VulkanFrameCommands frame);
    @Override default void close() { }
}
