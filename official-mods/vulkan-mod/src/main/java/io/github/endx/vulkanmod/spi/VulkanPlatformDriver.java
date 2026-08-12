package io.github.endx.vulkanmod.spi;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
    /** Compiles a renderer-neutral custom fragment shader for textured draw commands. */
    default long compileFragmentShader(VulkanCustomFragmentShader shader) {
        throw new UnsupportedOperationException("custom fragment shaders are not supported");
    }
    /** Releases a custom shader handle previously returned by {@link #compileFragmentShader}. */
    default void destroyFragmentShader(long shaderHandle) { }
    /** Compiles a linked custom vertex/fragment program for textured draw commands. */
    default long compileShaderProgram(VulkanCustomShaderProgram program) {
        throw new UnsupportedOperationException("custom shader programs are not supported");
    }
    /** Releases a custom program handle previously returned by {@link #compileShaderProgram}. */
    default void destroyShaderProgram(long shaderHandle) { }
    /** True when this backend consumes validated RustedVK FrameStream submissions directly. */
    default boolean supportsFrameStream() { return false; }
    /** True when this backend consumes ordered, validated RustedVK ResourceStream records. */
    default boolean supportsResourceStream() { return false; }
    /** Resource metadata needed by the shared encoder's custom vertex layout selection. */
    default boolean customShaderUsesExpandedVertexInput(long shaderHandle) {
        throw new UnsupportedOperationException("custom shader metadata is not available");
    }
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
    /** Replaces one tightly packed RGBA8 rectangle inside an existing texture. */
    default void updateTextureRegion(long textureHandle, int x, int y,
                                     VulkanTextureData texture) {
        throw new UnsupportedOperationException("partial texture updates are not supported");
    }
    /** Returns an RGBA8 snapshot of a readable texture after all earlier GPU writes complete. */
    default VulkanTextureData readTexture(long textureHandle) {
        throw new UnsupportedOperationException("texture readback is not supported");
    }
    /** Returns one tightly packed RGBA8 rectangle after all earlier GPU writes complete. */
    default VulkanTextureData readTextureRegion(long textureHandle, int x, int y,
                                                int width, int height) {
        throw new UnsupportedOperationException("partial texture readback is not supported");
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
     * Executes ordered offscreen passes and presents their final consumer as one logical frame.
     * Platform drivers can override this to collapse the graph into fewer queue submissions.
     */
    default VulkanSurfaceInfo presentFrame(VulkanFrameSubmission submission) {
        if (submission == null) throw new NullPointerException("submission");
        for (VulkanRenderTargetPass pass : submission.renderTargetPasses()) {
            renderToTexture(pass.textureHandle(), pass.frame());
        }
        return presentFrame(submission.presentationFrame());
    }
    /** Presents one complete FrameStream. The backend must validate it before GPU access. */
    default VulkanSurfaceInfo presentFrameStream(ByteBuffer frameStream) {
        throw new UnsupportedOperationException("FrameStream submission is not supported");
    }
    /** Accepts one complete reliable ResourceStream and returns its ordered acknowledgement. */
    default VulkanResourceStreamResult submitResourceStream(ByteBuffer resourceStream) {
        throw new UnsupportedOperationException("ResourceStream submission is not supported");
    }
    /** Polls a previously accepted asynchronous completion, returning null while it is pending. */
    default VulkanResourceStreamResult pollResourceStreamCompletion(long completionId) {
        throw new UnsupportedOperationException("asynchronous resource completions are not supported");
    }
    /** Waits for one asynchronous completion. A negative timeout means an unbounded wait. */
    default VulkanResourceStreamResult awaitResourceStreamCompletion(long completionId,
                                                                      long timeoutNanos) {
        VulkanResourceStreamResult result = pollResourceStreamCompletion(completionId);
        if (result != null) return result;
        throw new UnsupportedOperationException("blocking resource completions are not supported");
    }
    /** Registers stable direct memory referenced by external ResourceStream upload records. */
    default VulkanResourceArenaRegistration registerResourceUploadArena(long arenaId,
                                                                          ByteBuffer memory) {
        throw new UnsupportedOperationException("external resource arenas are not supported");
    }
    /** Unregisters an upload arena only after all submissions referencing it have completed. */
    default void unregisterResourceUploadArena(long arenaId) { }
    /** Allocation-light cumulative counters for diagnostics and the optional profiler HUD. */
    default Map<String, Long> performanceStatistics() { return Collections.emptyMap(); }
    /**
     * Presents a frame, or returns {@code null} when the surface temporarily cannot acquire an
     * image (for example while its Win32 window is occluded or minimized).
     */
    VulkanSurfaceInfo presentFrame(VulkanFrameCommands frame);
    @Override default void close() { }
}
