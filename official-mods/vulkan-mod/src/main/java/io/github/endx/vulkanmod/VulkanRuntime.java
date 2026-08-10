package io.github.endx.vulkanmod;

import io.github.endx.vulkanmod.spi.VulkanDeviceInfo;
import io.github.endx.vulkanmod.spi.VulkanClipRect;
import io.github.endx.vulkanmod.spi.VulkanFrameCommands;
import io.github.endx.vulkanmod.mixin.LibRocketUiEngineStateAccessor;
import rustedwarfare.client.render.GameImage;
import rustedwarfare.client.render.SlickGraphicsBackend;
import rustedwarfare.ui.LibRocketSlickRenderer;
import rustedwarfare.ui.LibRocketTextureHolder;
import io.github.endx.vulkanmod.spi.VulkanProbeResult;
import io.github.endx.vulkanmod.spi.VulkanSurfaceInfo;
import io.github.endx.vulkanmod.spi.VulkanWindowRequest;

import java.util.Optional;
import java.lang.reflect.Method;

/** Owns Vulkan startup state and the explicitly opt-in takeover experiment. */
public final class VulkanRuntime {
    private static final int MIRROR_STABILITY_FRAMES = 60;
    private static final long MIRROR_STABILITY_NANOS = 5_000_000_000L;
    private static volatile VulkanProbeResult probeResult;
    private static VulkanDriverLoader.LoadedDriver activeDriver;
    private static volatile VulkanSurfaceInfo surfaceInfo;
    private static VulkanMode configuredMode = VulkanMode.OFF;
    private static boolean frameTestAttempted;
    private static int frameTestWaitFrames;
    private static int frameTestFramesPresented;
    private static GameImageVulkanTextureCache gameTextureCache;
    private static VulkanTextTextureCache textTextureCache;
    private static SlickImageVulkanTextureCache slickImageTextureCache;
    private static AsyncVulkanPresenter takeoverPresenter;
    private static final SlickRenderCapture takeoverCapture = new SlickRenderCapture();
    private static VulkanFrameCommands pendingTakeoverFrame;
    private static int pendingTakeoverCommands;
    private static int pendingTakeoverUnsupported;
    private static long takeoverFramesPresented;
    private static boolean takeoverFailureLogged;
    private static int mirrorCompleteFrames;
    private static long mirrorStableSinceNanos;
    private static TakeoverPhase takeoverPhase = TakeoverPhase.INACTIVE;
    private static boolean takeoverFrameOpen;
    private static Method displayIsActive;
    private static Method displayIsVisible;
    private static boolean displayStateUnavailableLogged;
    private static long nativeFramesPresented;
    private static boolean legacyDisplayInvariantChecked;
    private static StartupPhase startupPhase = StartupPhase.MOD_INITIALIZED;

    private enum StartupPhase {
        MOD_INITIALIZED,
        RENDERER_SELECTED,
        NATIVE_WINDOW_READY,
        LEGACY_DISPLAY_CREATED,
        COMPATIBILITY_SURFACE_READY,
        GAME_GRAPHICS_ENGINE_READY
    }

    private enum TakeoverPhase {
        INACTIVE,
        /** Capture Vulkan commands while OpenGL remains authoritative and visible. */
        MIRROR,
        /** The first Vulkan present succeeded and the overlay was shown after that frame. */
        ARMED,
        /** Captured calls may now be suppressed from the legacy OpenGL renderer. */
        TAKEOVER
    }

    private VulkanRuntime() { }

    public static synchronized void initialize(VulkanMode mode) {
        configuredMode = mode;
        if (mode == VulkanMode.OFF || probeResult != null) return;
        VulkanDriverLoader.LoadedDriver loaded = null;
        try {
            loaded = VulkanDriverLoader.loadDesktop();
            VulkanProbeResult result = loaded.probe();
            probeResult = result;
            if (!result.available()) {
                loaded.close();
                loaded = null;
                log("Vulkan unavailable; retaining Slick/OpenGL: " + result.diagnostic());
                if (mode == VulkanMode.REQUIRED) {
                    throw new IllegalStateException(result.diagnostic());
                }
                return;
            }
            activeDriver = loaded;
            log("Vulkan " + VulkanProbeResult.formatVersion(result.instanceVersion())
                    + " available through " + loaded.name() + "; "
                    + result.devices().size() + " physical device(s)");
            for (VulkanDeviceInfo device : result.devices()) {
                log("  " + device.name() + " (vendor=0x"
                        + Integer.toHexString(device.vendorId()) + ", device=0x"
                        + Integer.toHexString(device.deviceId()) + ", api="
                        + VulkanProbeResult.formatVersion(device.apiVersion()) + ")");
            }
            if (usesTakeoverCapture(mode)) {
                log("Experimental Slick-to-Vulkan takeover capture is enabled");
            } else if (mode == VulkanMode.NATIVE) {
                log("Native Vulkan ownership is enabled; overlay capture is bypassed");
            } else {
                log("Renderer takeover is disabled; Slick/OpenGL remains authoritative");
            }
        } catch (RuntimeException failure) {
            if (loaded != null && loaded != activeDriver) {
                try {
                    loaded.close();
                } catch (RuntimeException closeFailure) {
                    failure.addSuppressed(closeFailure);
                }
            }
            probeResult = VulkanProbeResult.unavailable(
                    failure.getClass().getSimpleName() + ": " + failure.getMessage());
            if (mode == VulkanMode.REQUIRED) throw failure;
            log("Vulkan probe failed; retaining Slick/OpenGL: " + probeResult.diagnostic());
        }
    }

    /** Called immediately after Slick presents an OpenGL frame. */
    public static synchronized void afterOpenGlPresent() {
        if (usesTakeoverCapture(configuredMode)) {
            finishTakeoverFrame();
            enqueueTakeoverFrame();
            return;
        }
        if (configuredMode != VulkanMode.FRAME_TEST || frameTestAttempted
                || activeDriver == null || surfaceInfo == null) return;
        try {
            int width = surfaceInfo.width();
            int height = surfaceInfo.height();
            int colorPhase = Math.min(2, frameTestFramesPresented / 100);
            float red = colorPhase == 0 ? 1.0f : 0.0f;
            float green = colorPhase == 1 ? 1.0f : 0.0f;
            float blue = colorPhase == 2 ? 1.0f : 0.0f;
            VulkanFrameCommands frame = VulkanFrameCommands.builder(width, height)
                    .clear(red, green, blue, 1.0f)
                    .build();
            VulkanSurfaceInfo updated = activeDriver.presentFrame(frame);
            if (updated == null) {
                if (++frameTestWaitFrames == 1 || frameTestWaitFrames % 300 == 0) {
                    log("Vulkan frame-test surface was temporarily unavailable (attempt "
                            + frameTestWaitFrames + ")");
                }
                return;
            }
            surfaceInfo = updated;
            frameTestFramesPresented++;
            if (frameTestFramesPresented == 1 || frameTestFramesPresented % 100 == 0) {
                log("Presented solid-color frame-test frame " + frameTestFramesPresented
                        + "/300 at " + updated.width() + "x" + updated.height()
                        + " (RGB " + red + "," + green + "," + blue + ")");
            }
            if (frameTestFramesPresented >= 300) {
                frameTestAttempted = true;
                log("Completed 300-frame red/green/blue Vulkan presentation test");
            }
        } catch (Throwable failure) {
            log("Vulkan frame test failed; retaining Slick/OpenGL: "
                    + failure.getClass().getSimpleName() + ": " + failure.getMessage());
        }
    }

    public static synchronized boolean isTakeoverActive() {
        return takeoverPhase == TakeoverPhase.TAKEOVER;
    }

    public static synchronized boolean isVulkanAvailable() {
        return probeResult != null && probeResult.available() && activeDriver != null;
    }

    public static synchronized boolean isNativeRendererSelected() {
        return configuredMode == VulkanMode.NATIVE
                && "vulkan".equals(System.getProperty(
                        "rusted.fabric.renderer.resolved", "opengl"));
    }

    public static synchronized void onGraphicsEngineInstalled() {
        if (!isNativeRendererSelected()) return;
        if (startupPhase.ordinal() < StartupPhase.GAME_GRAPHICS_ENGINE_READY.ordinal()) {
            startupPhase = StartupPhase.GAME_GRAPHICS_ENGINE_READY;
            log("VulkanGraphicsEngine installed as the game's renderer; "
                    + "Slick remains its compatibility delegate during migration");
        }
    }

    /** Called from AppGameContainer.setup before its first Display.create attempt. */
    public static synchronized boolean beforeLegacyDisplayCreation(int width, int height) {
        if (startupPhase.ordinal() >= StartupPhase.NATIVE_WINDOW_READY.ordinal()) {
            return isNativeRendererSelected();
        }
        if (startupPhase.ordinal() >= StartupPhase.RENDERER_SELECTED.ordinal()) {
            return false;
        }
        String selected = System.getProperty("rusted.fabric.renderer.resolved", "opengl");
        if (configuredMode == VulkanMode.NATIVE && !"vulkan".equals(selected)) {
            log("Native Vulkan request was not selected by RFL; retaining OpenGL");
            configuredMode = VulkanMode.PROBE;
            return false;
        }
        startupPhase = StartupPhase.RENDERER_SELECTED;
        if (configuredMode == VulkanMode.NATIVE) {
            if (activeDriver == null) {
                throw new IllegalStateException(
                        "RFL selected RustedVK but no Vulkan platform driver is active");
            }
            log("RFL selected RustedVK before Display.create at " + width + "x" + height
                    + "; creating the driver-owned top-level window and swapchain");
            try {
                VulkanSurfaceInfo created = activeDriver.createNativeWindowSurface(
                        new VulkanWindowRequest("Rusted Warfare", width, height, true));
                surfaceInfo = created;
                startupPhase = StartupPhase.NATIVE_WINDOW_READY;
                log("Native Vulkan window is authoritative before Display.create: "
                        + created.width() + "x" + created.height() + ", device="
                        + created.deviceName() + ", images=" + created.imageCount());
                return true;
            } catch (Throwable failure) {
                throw new IllegalStateException(
                        "Could not create the pre-Display Vulkan window/swapchain", failure);
            }
        }
        return false;
    }

    /**
     * Runs the first native-only frame loop. No LWJGL2 Display or OpenGL context exists here.
     * Game initialization is intentionally moved behind the native Slick compatibility layer.
     */
    public static synchronized boolean runNativeBootstrapFrame() {
        if (!isNativeRendererSelected()
                || startupPhase.ordinal() < StartupPhase.NATIVE_WINDOW_READY.ordinal()) {
            return true;
        }
        assertLegacyDisplayWasNotCreated();
        activeDriver.maintainSurfaceWindow();
        if (activeDriver.isSurfaceCloseRequested()) return false;
        VulkanSurfaceInfo current = surfaceInfo;
        if (current == null) return true;
        VulkanFrameCommands frame = VulkanFrameCommands.builder(
                        current.width(), current.height())
                .clear(0.035f, 0.045f, 0.06f, 1.0f)
                .build();
        VulkanSurfaceInfo updated = activeDriver.presentFrame(frame);
        if (updated != null) {
            surfaceInfo = updated;
            nativeFramesPresented++;
            if (nativeFramesPresented == 1) {
                log("First native-only frame presented without creating LWJGL2 Display/OpenGL");
            }
        }
        return true;
    }

    private static void assertLegacyDisplayWasNotCreated() {
        if (legacyDisplayInvariantChecked) return;
        legacyDisplayInvariantChecked = true;
        try {
            Class<?> display = Class.forName("org.lwjgl.opengl.Display", false,
                    VulkanRuntime.class.getClassLoader());
            boolean created = (Boolean) display.getMethod("isCreated").invoke(null);
            if (created) {
                throw new IllegalStateException(
                        "Native Vulkan invariant failed: LWJGL2 Display/OpenGL was created");
            }
            log("Native invariant verified: Display.isCreated() == false");
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException(
                    "Could not verify the LWJGL2 Display creation invariant", failure);
        }
    }

    private static boolean usesTakeoverCapture(VulkanMode mode) {
        return mode == VulkanMode.TAKEOVER_TEST;
    }

    private static boolean isTakeoverCaptureActive() {
        return usesTakeoverCapture(configuredMode)
                && activeDriver != null && surfaceInfo != null
                && takeoverPhase != TakeoverPhase.INACTIVE;
    }

    public static synchronized void beforeOpenGlFrame() {
        if (usesTakeoverCapture(configuredMode) && activeDriver != null) {
            activeDriver.maintainSurfaceWindow();
        }
        takeoverFrameOpen = false;
        if (usesTakeoverCapture(configuredMode) && !legacyWindowReadyForCapture()) {
            pendingTakeoverFrame = null;
            return;
        }
        if (takeoverPhase == TakeoverPhase.ARMED) {
            VulkanSurfaceInfo current = surfaceInfo;
            if (current != null && activeDriver != null
                    && activeDriver.prepareSurfaceWindow(
                            current.width(), current.height(), true)) {
                takeoverPhase = TakeoverPhase.TAKEOVER;
                log("Vulkan overlay revealed on the Win32 owner thread; takeover is active and "
                        + "captured draw calls may now suppress OpenGL");
            }
        }
        if (!isTakeoverCaptureActive() || surfaceInfo == null) return;
        if (gameTextureCache != null) gameTextureCache.beginFrame();
        if (textTextureCache != null) textTextureCache.beginFrame();
        if (slickImageTextureCache != null) slickImageTextureCache.beginFrame();
        takeoverCapture.begin(surfaceInfo.width(), surfaceInfo.height());
        takeoverFrameOpen = true;
    }

    private static void finishTakeoverFrame() {
        if (!isTakeoverCaptureActive() || !takeoverFrameOpen) {
            pendingTakeoverFrame = null;
            return;
        }
        takeoverFrameOpen = false;
        int commands = takeoverCapture.commandCount();
        int unsupported = takeoverCapture.rejectedCount();
        VulkanFrameCommands frame = takeoverCapture.finish();
        if (frame != null && unsupported > 0 && takeoverPhase == TakeoverPhase.TAKEOVER) {
            takeoverPhase = TakeoverPhase.MIRROR;
            mirrorCompleteFrames = 0;
            mirrorStableSinceNanos = 0L;
            pendingTakeoverFrame = null;
            pendingTakeoverCommands = 0;
            pendingTakeoverUnsupported = 0;
            if (activeDriver != null) {
                activeDriver.prepareSurfaceWindow(frame.width(), frame.height(), false);
            }
            log("Vulkan capture became incomplete (" + unsupported
                    + " draw calls); returning to the full OpenGL frame until coverage recovers");
            return;
        }
        if (unsupported > 0 && takeoverPhase == TakeoverPhase.MIRROR) {
            mirrorCompleteFrames = 0;
            mirrorStableSinceNanos = 0L;
        }
        if (frame == null || (commands == 0 && unsupported == 0)) {
            // The Slick container skips rendering while hidden. Do not keep presenting the last
            // captured frame before LWJGL has processed the focus/restore messages for this loop.
            pendingTakeoverFrame = null;
            pendingTakeoverCommands = 0;
            pendingTakeoverUnsupported = 0;
            return;
        }
        pendingTakeoverFrame = frame;
        pendingTakeoverCommands = commands;
        pendingTakeoverUnsupported = unsupported;
    }

    public static synchronized boolean captureClear(SlickGraphicsBackend backend, int argb) {
        return captureSafely("clear", () -> takeoverCapture.clear(backend, argb));
    }

    public static synchronized boolean captureRect(SlickGraphicsBackend backend,
                                                    android.graphics.Rect rect,
                                                    android.graphics.Paint paint) {
        return captureSafely("rectangle", () -> takeoverCapture.rectangle(backend, rect, paint));
    }

    public static synchronized boolean captureRect(SlickGraphicsBackend backend,
                                                    android.graphics.RectF rect,
                                                    android.graphics.Paint paint) {
        return captureSafely("rectangle", () -> takeoverCapture.rectangle(backend, rect, paint));
    }

    public static synchronized boolean captureImageQuad(
            SlickGraphicsBackend backend, GameImage image,
            float left, float top, float right, float bottom,
            float sourceLeft, float sourceTop, float sourceRight, float sourceBottom,
            android.graphics.Paint paint) {
        return captureSafely("image quad", () -> takeoverCapture.imageQuad(
                backend, image, left, top, right, bottom,
                sourceLeft, sourceTop, sourceRight, sourceBottom, paint));
    }

    public static synchronized boolean captureText(SlickGraphicsBackend backend, String text,
                                                    float x, float y,
                                                    android.graphics.Paint paint) {
        return captureSafely("text", () ->
                takeoverCapture.text(backend, text, x, y, paint));
    }

    public static synchronized boolean captureLine(SlickGraphicsBackend backend,
                                                    float x1, float y1, float x2, float y2,
                                                    android.graphics.Paint paint) {
        return captureSafely("line", () ->
                takeoverCapture.line(backend, x1, y1, x2, y2, paint));
    }

    public static synchronized boolean captureLines(SlickGraphicsBackend backend,
                                                     float[] points, int offset, int count,
                                                     android.graphics.Paint paint) {
        return captureSafely("lines", () ->
                takeoverCapture.lines(backend, points, offset, count, paint));
    }

    public static synchronized boolean captureCircle(SlickGraphicsBackend backend,
                                                      float x, float y, float radius,
                                                      android.graphics.Paint paint) {
        return captureSafely("circle", () ->
                takeoverCapture.circle(backend, x, y, radius, paint));
    }

    public static synchronized boolean captureLibRocketGeometry(
            LibRocketSlickRenderer renderer, float[] positions, float[] uvs,
            int[] colors, int[] indices, int textureId,
            float translationX, float translationY) {
        if (!isTakeoverCaptureActive() || !takeoverFrameOpen || renderer == null) return false;
        long textureHandle = 0L;
        float uScale = 1.0f;
        float vScale = 1.0f;
        boolean noColor = false;
        float alpha = 1.0f;
        if (textureId != 0) {
            Object candidate = renderer.findTextureHolder(textureId);
            if (!(candidate instanceof LibRocketTextureHolder)
                    || slickImageTextureCache == null) {
                takeoverCapture.unsupportedExternal();
                return false;
            }
            LibRocketTextureHolder holder = (LibRocketTextureHolder) candidate;
            slickImageTextureCache.observeHolder(holder);
            Object image = SlickImageVulkanTextureCache.imageFromHolder(candidate);
            if (image == null) {
                takeoverCapture.unsupportedExternal();
                return false;
            }
            SlickImageVulkanTextureCache.Entry texture =
                    slickImageTextureCache.texture(holder, image);
            if (texture == null) {
                takeoverCapture.unsupportedExternal();
                return false;
            }
            textureHandle = texture.textureHandle;
            uScale = texture.uScale;
            vScale = texture.vScale;
            noColor = holder.noColor;
            alpha = holder.alpha;
        }
        LibRocketUiEngineStateAccessor state =
                (LibRocketUiEngineStateAccessor) (Object) renderer;
        android.graphics.RectF clipRect = state.vulkanmod$isScissorEnabled()
                ? state.vulkanmod$getScissorRectF() : null;
        VulkanClipRect clip = clipRect == null ? null : new VulkanClipRect(
                clipRect.a, clipRect.b, Math.max(0.0f, clipRect.c - clipRect.a),
                Math.max(0.0f, clipRect.d - clipRect.b));
        final long capturedTexture = textureHandle;
        final float capturedUScale = uScale;
        final float capturedVScale = vScale;
        final boolean capturedNoColor = noColor;
        final float capturedAlpha = alpha;
        return captureSafely("LibRocket geometry", () -> takeoverCapture.libRocketGeometry(
                positions, uvs, colors, indices, translationX, translationY,
                capturedTexture, capturedUScale, capturedVScale,
                capturedNoColor, capturedAlpha, clip));
    }

    public static synchronized void releaseLibRocketTexture(
            LibRocketSlickRenderer renderer, int textureId) {
        if (renderer == null || slickImageTextureCache == null) return;
        Object holder = renderer.findTextureHolder(textureId);
        Object image = SlickImageVulkanTextureCache.imageFromHolder(holder);
        if (image != null) slickImageTextureCache.invalidate(image);
    }

    public static synchronized void registerGeneratedLibRocketTexture(
            LibRocketSlickRenderer renderer, int textureId, byte[] rgba) {
        if (renderer == null || slickImageTextureCache == null || rgba == null) return;
        Object holder = renderer.findTextureHolder(textureId);
        if (holder == null) return;
        int width = SlickImageVulkanTextureCache.intField(holder, "width");
        int height = SlickImageVulkanTextureCache.intField(holder, "height");
        slickImageTextureCache.registerPixels(holder, width, height, rgba);
    }

    public static synchronized void noteUnsupportedDraw(SlickGraphicsBackend backend) {
        if (isTakeoverCaptureActive() && takeoverFrameOpen) {
            takeoverCapture.unsupported(backend);
        }
    }

    static synchronized long textureForGameImage(GameImage image) {
        if (gameTextureCache == null) {
            throw new IllegalStateException("Vulkan game-image cache is unavailable");
        }
        return gameTextureCache.texture(image);
    }

    static synchronized boolean isRenderTargetImage(GameImage image) {
        return gameTextureCache != null && gameTextureCache.isRenderTarget(image);
    }

    static synchronized VulkanTextTextureCache.Entry textureForText(
            String text, int size, boolean bold) {
        if (textTextureCache == null) {
            throw new IllegalStateException("Vulkan text cache is unavailable");
        }
        return textTextureCache.texture(text, size, bold);
    }

    private static boolean captureSafely(String operation, CaptureOperation capture) {
        if (!isTakeoverCaptureActive() || !takeoverFrameOpen) return false;
        try {
            boolean captured = capture.run();
            return captured && takeoverPhase == TakeoverPhase.TAKEOVER;
        } catch (Throwable failure) {
            if (!takeoverFailureLogged) {
                takeoverFailureLogged = true;
                log("Could not capture Vulkan " + operation + "; leaving this call to OpenGL: "
                        + failure.getClass().getSimpleName() + ": " + failure.getMessage());
            }
            return false;
        }
    }

    private static void enqueueTakeoverFrame() {
        VulkanFrameCommands frame = pendingTakeoverFrame;
        AsyncVulkanPresenter presenter = takeoverPresenter;
        if (presenter == null || surfaceInfo == null || frame == null) return;
        pendingTakeoverFrame = null;
        boolean revealCandidate = takeoverPhase == TakeoverPhase.MIRROR;
        boolean visible = takeoverPhase == TakeoverPhase.TAKEOVER;
        if (activeDriver == null
                || !activeDriver.prepareSurfaceWindow(frame.width(), frame.height(), visible)) {
            return;
        }
        presenter.offer(frame, revealCandidate,
                pendingTakeoverCommands, pendingTakeoverUnsupported);
    }

    private static synchronized void onTakeoverPresented(
            VulkanSurfaceInfo updated, AsyncVulkanPresenter.Submission submission) {
        if (takeoverPresenter == null || takeoverPhase == TakeoverPhase.INACTIVE) return;
        surfaceInfo = updated;
        takeoverFramesPresented++;
        if (takeoverPhase == TakeoverPhase.MIRROR && submission.reveal()) {
            if (submission.unsupportedCommands() == 0) {
                if (mirrorStableSinceNanos == 0L) mirrorStableSinceNanos = System.nanoTime();
                mirrorCompleteFrames++;
            } else {
                mirrorCompleteFrames = 0;
                mirrorStableSinceNanos = 0L;
            }
            if (mirrorCompleteFrames >= MIRROR_STABILITY_FRAMES
                    && System.nanoTime() - mirrorStableSinceNanos
                    >= MIRROR_STABILITY_NANOS) {
                takeoverPhase = TakeoverPhase.ARMED;
                mirrorCompleteFrames = 0;
                mirrorStableSinceNanos = 0L;
                log("Vulkan capture remained complete for five seconds; takeover armed for a "
                        + "window-thread reveal");
            }
        }
        if (takeoverFramesPresented == 1 || takeoverFramesPresented % 300 == 0) {
            VulkanFrameCommands frame = submission.frame();
            log("Presented takeover frame #" + takeoverFramesPresented + " at "
                    + updated.width() + "x" + updated.height() + " ("
                    + submission.capturedCommands() + " captured, "
                    + submission.unsupportedCommands()
                    + " unsupported draw calls; clear RGBA "
                    + frame.clearRed() + "," + frame.clearGreen() + ","
                    + frame.clearBlue() + "," + frame.clearAlpha() + ")");
        }
    }

    private static synchronized void onTakeoverFailure(Throwable failure) {
        if (!takeoverFailureLogged) {
            takeoverFailureLogged = true;
            log("Vulkan takeover presentation failed; the experiment remains isolated: "
                    + failure.getClass().getSimpleName() + ": " + failure.getMessage());
        }
    }

    private interface CaptureOperation {
        boolean run();
    }

    private static boolean legacyWindowReadyForCapture() {
        try {
            if (displayIsActive == null || displayIsVisible == null) {
                Class<?> display = Class.forName("org.lwjgl.opengl.Display");
                displayIsActive = display.getMethod("isActive");
                displayIsVisible = display.getMethod("isVisible");
            }
            return (Boolean) displayIsActive.invoke(null)
                    && (Boolean) displayIsVisible.invoke(null);
        } catch (ReflectiveOperationException | LinkageError failure) {
            if (!displayStateUnavailableLogged) {
                displayStateUnavailableLogged = true;
                log("Could not query the LWJGL window state; capture will remain enabled: "
                        + failure.getClass().getSimpleName() + ": " + failure.getMessage());
            }
            return true;
        }
    }

    public static synchronized void attachToCurrentWindow() {
        if (activeDriver == null || surfaceInfo != null) return;
        try {
            io.github.endx.vulkanmod.spi.VulkanSurfaceRequest request =
                    Lwjgl2Win32Window.current();
            if (usesTakeoverCapture(configuredMode)) {
                request = request.asChildOverlay();
            }
            VulkanSurfaceInfo created = activeDriver.createSurface(request);
            surfaceInfo = created;
            if (usesTakeoverCapture(configuredMode)) {
                activeDriver.setSurfaceVisible(false);
                takeoverPresenter = new AsyncVulkanPresenter(activeDriver,
                        new AsyncVulkanPresenter.Listener() {
                            @Override public void presented(
                                    VulkanSurfaceInfo updated,
                                    AsyncVulkanPresenter.Submission submission) {
                                onTakeoverPresented(updated, submission);
                            }

                            @Override public void failed(Throwable failure) {
                                onTakeoverFailure(failure);
                            }
                        });
                takeoverPhase = TakeoverPhase.MIRROR;
                log("Takeover mirror phase started on a dedicated presenter thread; overlay "
                        + "hidden and OpenGL remains authoritative");
            }
            java.util.function.LongConsumer textureDestroyer = takeoverPresenter == null
                    ? activeDriver::destroyTexture : takeoverPresenter::destroyTexture;
            gameTextureCache = new GameImageVulkanTextureCache(
                    activeDriver, takeoverPresenter, textureDestroyer);
            textTextureCache = new VulkanTextTextureCache(
                    activeDriver, takeoverPresenter, textureDestroyer);
            slickImageTextureCache = new SlickImageVulkanTextureCache(
                    activeDriver, takeoverPresenter, textureDestroyer);
            startupPhase = StartupPhase.LEGACY_DISPLAY_CREATED;
            log("Win32 surface and swapchain ready on " + created.deviceName() + ": "
                    + created.width() + "x" + created.height() + ", images="
                    + created.imageCount() + ", format=" + created.imageFormat()
                    + ", presentMode=" + created.presentMode() + ", queues="
                    + created.graphicsQueueFamily() + "/" + created.presentQueueFamily());
            startupPhase = StartupPhase.COMPATIBILITY_SURFACE_READY;
        } catch (Throwable failure) {
            log("Vulkan surface validation failed; retaining Slick/OpenGL: "
                    + failure.getClass().getSimpleName() + ": " + failure.getMessage());
            if (configuredMode == VulkanMode.REQUIRED) {
                if (failure instanceof Error) throw (Error) failure;
                throw (RuntimeException) failure;
            }
        }
    }

    public static Optional<VulkanProbeResult> probeResult() {
        return Optional.ofNullable(probeResult);
    }

    public static Optional<VulkanSurfaceInfo> surfaceInfo() {
        return Optional.ofNullable(surfaceInfo);
    }

    public static synchronized void invalidateCachedImage(Object image) {
        if (gameTextureCache != null) gameTextureCache.invalidate(image);
    }

    static synchronized void markRenderTargetImage(Object image) {
        if (gameTextureCache != null) gameTextureCache.markRenderTarget(image);
    }

    public static void shutdown() {
        AsyncVulkanPresenter presenter;
        synchronized (VulkanRuntime.class) {
            presenter = takeoverPresenter;
            takeoverPresenter = null;
            takeoverPhase = TakeoverPhase.INACTIVE;
        }
        boolean presenterStopped = presenter == null || presenter.stopAndWait(500L);
        synchronized (VulkanRuntime.class) {
            shutdownResources(presenterStopped);
        }
    }

    private static void shutdownResources(boolean presenterStopped) {
        if (!presenterStopped) {
            // Texture destruction uses the same synchronized native driver as presentation.
            // Touching any of it here would merely move the shutdown hang to destroyTexture.
            log("Vulkan presenter is still inside the display driver; deferring Vulkan resource "
                    + "cleanup until process exit");
            slickImageTextureCache = null;
            textTextureCache = null;
            gameTextureCache = null;
            activeDriver = null;
            resetRuntimeState();
            return;
        }
        if (slickImageTextureCache != null) {
            try {
                slickImageTextureCache.close();
            } catch (RuntimeException failure) {
                log("Could not release Vulkan Slick-image cache: " + failure.getMessage());
            }
            slickImageTextureCache = null;
        }
        if (textTextureCache != null) {
            try {
                textTextureCache.close();
            } catch (RuntimeException failure) {
                log("Could not release Vulkan text cache: " + failure.getMessage());
            }
            textTextureCache = null;
        }
        if (gameTextureCache != null) {
            try {
                gameTextureCache.close();
            } catch (RuntimeException failure) {
                log("Could not release Vulkan game-image cache: " + failure.getMessage());
            }
            gameTextureCache = null;
        }
        if (activeDriver != null) {
            try {
                activeDriver.close();
            } catch (RuntimeException failure) {
                log("Could not close Vulkan driver cleanly: " + failure.getMessage());
            }
            activeDriver = null;
        }
        resetRuntimeState();
    }

    private static void resetRuntimeState() {
        surfaceInfo = null;
        pendingTakeoverFrame = null;
        pendingTakeoverCommands = 0;
        pendingTakeoverUnsupported = 0;
        takeoverFramesPresented = 0;
        takeoverFailureLogged = false;
        mirrorCompleteFrames = 0;
        mirrorStableSinceNanos = 0L;
        takeoverFrameOpen = false;
        frameTestWaitFrames = 0;
        frameTestFramesPresented = 0;
    }

    private static void log(String message) {
        System.out.println("[Vulkan Mod] " + message);
    }
}
