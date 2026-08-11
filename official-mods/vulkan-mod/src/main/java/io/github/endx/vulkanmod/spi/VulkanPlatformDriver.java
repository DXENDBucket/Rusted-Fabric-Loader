package io.github.endx.vulkanmod.spi;

import java.util.Collections;
import java.util.List;

/** Minimal ABI shared by the mod and class-loader-isolated platform implementations. */
public interface VulkanPlatformDriver extends AutoCloseable {
    String name();
    VulkanProbeResult probe();
    /**
     * Creates the application's top-level native window and its Vulkan surface. This method is
     * called before any legacy display/context creation and transfers window ownership to the
     * Vulkan driver.
     */
    default VulkanSurfaceInfo createNativeWindowSurface(VulkanWindowRequest request) {
        throw new UnsupportedOperationException("native Vulkan windows are not supported");
    }
    VulkanSurfaceInfo createSurface(VulkanSurfaceRequest request);
    long uploadTexture(VulkanTextureData texture);
    /** Creates a sampled color image that can also be used as an offscreen render target. */
    default long createRenderTarget(int width, int height) {
        throw new UnsupportedOperationException("native render targets are not supported");
    }
    /** Replaces the contents of a render-target texture with the supplied draw commands. */
    default void renderToTexture(long textureHandle, VulkanFrameCommands frame) {
        throw new UnsupportedOperationException("native render targets are not supported");
    }
    /** Replaces pixels in an existing same-sized texture without changing its public handle. */
    default void updateTexture(long textureHandle, VulkanTextureData texture) {
        throw new UnsupportedOperationException("in-place texture updates are not supported");
    }
    void destroyTexture(long textureHandle);
    /**
     * Shows or hides the driver-owned presentation surface. Returns {@code true} when the driver
     * owns a surface whose visibility can be controlled independently from the game window.
     */
    default boolean setSurfaceVisible(boolean visible) { return false; }
    /**
     * Updates the native presentation window from its owning window thread. Vulkan WSI calls may
     * run elsewhere, but Win32 visibility and child positioning must remain on this thread.
     */
    default boolean prepareSurfaceWindow(int width, int height, boolean visible) { return true; }
    /** Performs lightweight platform-window maintenance on the window's owning thread. */
    default void maintainSurfaceWindow() { }
    /** Returns whether the driver-owned top-level window has requested application shutdown. */
    default boolean isSurfaceCloseRequested() { return false; }
    /** Drains input collected by the driver-owned application window since the last poll. */
    default List<VulkanInputEvent> pollInputEvents() { return Collections.emptyList(); }
    /** Shows or hides the platform cursor over the driver-owned client area. */
    default void setSystemCursorVisible(boolean visible) { }
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
