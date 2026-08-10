package io.github.endx.vulkanmod.spi;

/** Minimal ABI shared by the mod and class-loader-isolated platform implementations. */
public interface VulkanPlatformDriver extends AutoCloseable {
    String name();
    VulkanProbeResult probe();
    VulkanSurfaceInfo createSurface(VulkanSurfaceRequest request);
    VulkanSurfaceInfo presentClearFrame(int width, int height,
                                        float red, float green, float blue, float alpha);
    @Override default void close() { }
}
