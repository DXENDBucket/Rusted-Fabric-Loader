package io.github.endx.vulkanmod;

import io.github.endx.vulkanmod.framestream.FrameStreamEncoder;
import io.github.endx.vulkanmod.framestream.FrameStreamArenaPool;
import io.github.endx.vulkanmod.framestream.FrameStreamCapacityException;
import io.github.endx.vulkanmod.framestream.FrameStreamFormat;
import io.github.endx.vulkanmod.framestream.FrameStreamResourceMapper;
import io.github.endx.vulkanmod.spi.VulkanDeviceInfo;
import io.github.endx.vulkanmod.spi.VulkanClipRect;
import io.github.endx.vulkanmod.spi.VulkanFrameCommands;
import io.github.endx.vulkanmod.spi.VulkanFrameSubmission;
import io.github.endx.vulkanmod.spi.VulkanRenderTargetPass;
import io.github.endx.vulkanmod.spi.VulkanColoredQuad;
import io.github.endx.vulkanmod.spi.VulkanTexturedQuad;
import io.github.endx.vulkanmod.spi.VulkanDrawState;
import io.github.endx.vulkanmod.spi.VulkanColoredTriangle;
import io.github.endx.vulkanmod.spi.VulkanTexturedTriangle;
import io.github.endx.vulkanmod.spi.VulkanInputEvent;
import io.github.endx.vulkanmod.spi.VulkanTextureData;
import io.github.endx.vulkanmod.spi.VulkanTextMetrics;
import io.github.endx.vulkanmod.spi.VulkanCustomFragmentShader;
import io.github.endx.vulkanmod.spi.VulkanCustomShaderProgram;
import android.graphics.Paint;
import android.graphics.Paint$Align;
import io.github.endx.vulkanmod.mixin.LibRocketUiEngineStateAccessor;
import rustedwarfare.client.render.GameImage;
import rustedwarfare.client.render.SlickGraphicsBackend;
import rustedwarfare.ui.LibRocketSlickRenderer;
import rustedwarfare.ui.LibRocketTextureHolder;
import io.github.endx.vulkanmod.spi.VulkanProbeResult;
import io.github.endx.vulkanmod.spi.VulkanSurfaceInfo;
import io.github.endx.vulkanmod.spi.VulkanWindowRequest;

import java.util.Optional;
import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import rustedwarfare.core.GameEngine;
import rustedwarfare.game.Team;
import rustedwarfare.render.GraphicsEngine;
import rustedwarfare.unit.BuiltinUnitType;

/** Owns Vulkan startup state and the explicitly opt-in takeover experiment. */
public final class VulkanRuntime {
    private static final int MIRROR_STABILITY_FRAMES = 60;
    private static final long MIRROR_STABILITY_NANOS = 5_000_000_000L;
    private static volatile VulkanProbeResult probeResult;
    private static VulkanDriverLoader.LoadedDriver activeDriver;
    private static volatile VulkanSurfaceInfo surfaceInfo;
    private static VulkanMode configuredMode = VulkanMode.OFF;
    private static volatile boolean nativeRendererSelected;
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
    private static NativeSlickGameBridge nativeGame;
    private static boolean nativeGameSystemsStarted;
    private static VulkanFrameCommands.Builder nativeFrameBuilder;
    private static List<VulkanRenderTargetPass> nativeRenderTargetPasses;
    private static final NativeFrameClock nativeFrameClock = new NativeFrameClock();
    private static GraphicsEngine nativeGraphicsEngine;
    private static StartupPhase startupPhase = StartupPhase.MOD_INITIALIZED;
    private static FrameStreamEncoder frameStreamEncoder;
    private static FrameStreamArenaPool frameStreamArenas;
    private static long nextFrameStreamId;
    private static long textRunsSubmitted;
    private static long textGlyphQuadsSubmitted;
    private static long textBatchCommandsSubmitted;

    private enum StartupPhase {
        MOD_INITIALIZED,
        RENDERER_SELECTED,
        NATIVE_WINDOW_READY,
        NATIVE_GAME_BOUND,
        NATIVE_GAME_SYSTEMS_READY,
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
        nativeRendererSelected = false;
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

    /** Hot draw-path query resolved once before window creation. */
    public static boolean isNativeRendererSelected() { return nativeRendererSelected; }

    public static synchronized void onGraphicsEngineInstalled(GraphicsEngine engine) {
        if (!isNativeRendererSelected()) return;
        nativeGraphicsEngine = engine;
        if (startupPhase.ordinal() < StartupPhase.GAME_GRAPHICS_ENGINE_READY.ordinal()) {
            startupPhase = StartupPhase.GAME_GRAPHICS_ENGINE_READY;
            log("VulkanGraphicsEngine installed as the native game renderer; "
                    + "no Slick/OpenGL delegate was created");
        }
    }

    public static synchronized GraphicsEngine nativeGraphicsEngine() {
        return nativeGraphicsEngine;
    }

    public static synchronized void drawNativePointer(GameImage image, float x, float y) {
        if (image == null || !(nativeGraphicsEngine
                instanceof io.github.endx.vulkanmod.render.VulkanGraphicsEngine)) return;
        ((io.github.endx.vulkanmod.render.VulkanGraphicsEngine) nativeGraphicsEngine)
                .drawScreenImageRaw(image, x, y, null);
    }

    public static synchronized void drawNativeImage(GameImage image, float x, float y) {
        if (image == null || !(nativeGraphicsEngine
                instanceof io.github.endx.vulkanmod.render.VulkanGraphicsEngine)) return;
        ((io.github.endx.vulkanmod.render.VulkanGraphicsEngine) nativeGraphicsEngine)
                .drawScreenImageRaw(image, x, y, null);
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
            nativeRendererSelected = true;
            if (activeDriver == null) {
                nativeRendererSelected = false;
                throw new IllegalStateException(
                        "RFL selected RustedVK but no Vulkan platform driver is active");
            }
            log("RFL selected RustedVK before Display.create at " + width + "x" + height
                    + "; creating the driver-owned top-level window and swapchain");
            try {
                VulkanSurfaceInfo created = activeDriver.createNativeWindowSurface(
                        new VulkanWindowRequest("Rusted Warfare", width, height, true));
                surfaceInfo = created;
                // Rusted Warfare's pointer is drawn as the final screen-space command. Hide only
                // the Win32 client-area cursor; non-client window chrome retains its system cursor.
                activeDriver.setSystemCursorVisible(false);
                initializeNativeTextureCaches();
                startupPhase = StartupPhase.NATIVE_WINDOW_READY;
                log("Native Vulkan window is authoritative before Display.create: "
                        + created.width() + "x" + created.height() + ", device="
                        + created.deviceName() + ", images=" + created.imageCount());
                return true;
            } catch (Throwable failure) {
                nativeRendererSelected = false;
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
        startNativeGameSystems();
        activeDriver.maintainSurfaceWindow();
        if (activeDriver.isSurfaceCloseRequested()) return false;
        if (nativeGame != null) {
            for (VulkanInputEvent event : activeDriver.pollInputEvents()) {
                nativeGame.vulkanmod$handleNativeInput(event);
            }
        }
        VulkanSurfaceInfo current = surfaceInfo;
        if (current == null) return true;
        if (!activeDriver.prepareSurfaceWindow(current.width(), current.height(), true)) {
            // A minimized Win32 surface has no drawable client extent. Keep pumping messages,
            // but do not run an expensive game/render frame against the stale swapchain.
            java.util.concurrent.locks.LockSupport.parkNanos(16_000_000L);
            nativeFrameClock.reset(System.nanoTime());
            return true;
        }
        long now = System.nanoTime();
        int deltaMillis = nativeFrameClock.nextDeltaMillis(now);
        long frameWorkStarted = System.nanoTime();
        if (gameTextureCache != null) gameTextureCache.beginFrame();
        if (textTextureCache != null) textTextureCache.beginFrame();
        nativeFrameBuilder = VulkanFrameCommands.pooledBuilder(current.width(), current.height())
                .clear(0.035f, 0.045f, 0.06f, 1.0f);
        nativeRenderTargetPasses = new ArrayList<VulkanRenderTargetPass>();
        VulkanFrameCommands frame;
        List<VulkanRenderTargetPass> renderTargetPasses;
        boolean frameBuilt = false;
        try {
            if (nativeGameSystemsStarted && nativeGame != null) {
                nativeGame.vulkanmod$runNativeFrame(
                        deltaMillis, current.width(), current.height());
            }
            frame = nativeFrameBuilder.build();
            frameBuilt = true;
            renderTargetPasses = nativeRenderTargetPasses;
        } finally {
            // Build before presentation so GraphicsEngine calls outside a native frame cannot
            // accidentally append to a command buffer already owned by the driver.
            VulkanFrameCommands.Builder unfinishedBuilder = nativeFrameBuilder;
            List<VulkanRenderTargetPass> unfinishedPasses = nativeRenderTargetPasses;
            nativeFrameBuilder = null;
            nativeRenderTargetPasses = null;
            if (!frameBuilt) {
                if (unfinishedBuilder != null) unfinishedBuilder.discard();
                if (unfinishedPasses != null) {
                    for (VulkanRenderTargetPass pass : unfinishedPasses) {
                        pass.frame().releasePooledCommands();
                    }
                }
            }
        }
        long gameWorkFinished = System.nanoTime();
        int frameCommandCount = frame.commandCount();
        VulkanFrameSubmission submission = new VulkanFrameSubmission(renderTargetPasses, frame);
        VulkanSurfaceInfo updated;
        try {
            updated = presentSubmission(submission);
        } finally {
            submission.releasePooledCommands();
        }
        long presentationFinished = System.nanoTime();
        if (Boolean.getBoolean("rusted.fabric.vulkan.profileSlowFrames")) {
            long gameMicros = (gameWorkFinished - frameWorkStarted) / 1_000L;
            long presentMicros = (presentationFinished - gameWorkFinished) / 1_000L;
            if (gameMicros + presentMicros >= 25_000L) {
                log("Slow native frame: game/render=" + (gameMicros / 1000.0)
                        + "ms, present=" + (presentMicros / 1000.0)
                        + "ms, commands=" + frameCommandCount);
            }
        }
        if (updated != null) {
            boolean resolutionChanged = updated.width() != current.width()
                    || updated.height() != current.height();
            surfaceInfo = updated;
            if (resolutionChanged && nativeGame != null) {
                nativeGame.vulkanmod$syncNativeResolution(
                        updated.width(), updated.height());
                log("Native Vulkan resolution changed to " + updated.width()
                        + "x" + updated.height());
            }
            nativeFramesPresented++;
            if (nativeFramesPresented == 1) {
                log("First native-only frame presented without creating LWJGL2 Display/OpenGL");
            }
        }
        return true;
    }

    private static void initializeNativeTextureCaches() {
        if (activeDriver == null || gameTextureCache != null) return;
        java.util.function.LongConsumer destroyer = activeDriver::destroyTexture;
        gameTextureCache = new GameImageVulkanTextureCache(activeDriver, null, destroyer);
        textTextureCache = new VulkanTextTextureCache(activeDriver, null, destroyer);
        slickImageTextureCache = new SlickImageVulkanTextureCache(activeDriver, null, destroyer);
    }

    public static synchronized boolean recordNativeColoredQuad(VulkanColoredQuad quad) {
        if (nativeFrameBuilder == null || quad == null) return false;
        nativeFrameBuilder.coloredQuad(quad);
        return true;
    }

    public static synchronized boolean recordNativeColoredQuad(
            float x, float y, float width, float height,
            float red, float green, float blue, float alpha, VulkanDrawState state) {
        if (nativeFrameBuilder == null) return false;
        nativeFrameBuilder.coloredQuad(
                x, y, width, height, red, green, blue, alpha, state);
        return true;
    }

    public static synchronized boolean recordNativeColoredLine(
            float x1, float y1, float x2, float y2, float thickness,
            float red, float green, float blue, float alpha, VulkanDrawState state) {
        if (nativeFrameBuilder == null) return false;
        nativeFrameBuilder.coloredLine(x1, y1, x2, y2, thickness,
                red, green, blue, alpha, state);
        return true;
    }

    public static synchronized boolean recordNativeColoredCircle(
            float x, float y, float radius, float thickness,
            float red, float green, float blue, float alpha,
            int segments, boolean filled, VulkanDrawState state) {
        if (nativeFrameBuilder == null) return false;
        nativeFrameBuilder.coloredCircle(x, y, radius, thickness,
                red, green, blue, alpha, segments, filled, state);
        return true;
    }

    public static synchronized boolean recordNativeTexturedQuad(VulkanTexturedQuad quad) {
        if (nativeFrameBuilder == null || quad == null) return false;
        nativeFrameBuilder.texturedQuad(quad);
        return true;
    }

    public static synchronized boolean recordNativeTexturedQuad(
            long textureHandle, float x, float y, float width, float height,
            float u0, float v0, float u1, float v1,
            float red, float green, float blue, float alpha, VulkanDrawState state) {
        if (nativeFrameBuilder == null) return false;
        nativeFrameBuilder.texturedQuad(textureHandle, x, y, width, height,
                u0, v0, u1, v1, red, green, blue, alpha, state);
        return true;
    }

    public static synchronized void clearNativeFrame(int argb) {
        if (nativeFrameBuilder == null) return;
        nativeFrameBuilder.clear(((argb >>> 16) & 255) / 255.0f,
                ((argb >>> 8) & 255) / 255.0f,
                (argb & 255) / 255.0f,
                ((argb >>> 24) & 255) / 255.0f);
    }

    /**
     * Presents the commands collected so far and opens a fresh builder inside the synchronous
     * desktop loading pass. Rusted Warfare 1.15's misleadingly named startLoadingThreaded method
     * actually performs the whole load on the game thread; progress callbacks therefore need an
     * explicit mid-frame present to reproduce Slick's loading animation.
     */
    public static synchronized boolean presentNativeLoadingProgressFrame() {
        if (!isNativeRendererSelected() || activeDriver == null || surfaceInfo == null
                || nativeFrameBuilder == null || nativeRenderTargetPasses == null) {
            return false;
        }
        activeDriver.maintainSurfaceWindow();
        VulkanSurfaceInfo current = surfaceInfo;
        VulkanFrameCommands progressFrame = nativeFrameBuilder.build();
        List<VulkanRenderTargetPass> progressPasses = nativeRenderTargetPasses;
        nativeFrameBuilder = VulkanFrameCommands.pooledBuilder(current.width(), current.height())
                .clear(0.0f, 0.0f, 0.0f, 1.0f);
        nativeRenderTargetPasses = new ArrayList<VulkanRenderTargetPass>();
        VulkanFrameSubmission submission = new VulkanFrameSubmission(
                progressPasses, progressFrame);
        VulkanSurfaceInfo updated;
        try {
            updated = presentSubmission(submission);
        } finally {
            submission.releasePooledCommands();
        }
        if (updated == null) return false;
        surfaceInfo = updated;
        nativeFramesPresented++;
        if (nativeFramesPresented == 1) {
            log("First native loading frame presented without creating LWJGL2 Display/OpenGL");
        }
        return true;
    }

    private static VulkanSurfaceInfo presentSubmission(VulkanFrameSubmission submission) {
        if (!activeDriver.supportsFrameStream()
                || Boolean.getBoolean("rusted.fabric.vulkan.objectSubmission")) {
            return activeDriver.presentFrame(submission);
        }
        if (frameStreamEncoder == null) {
            frameStreamEncoder = new FrameStreamEncoder(
                    activeDriver.supportsResourceStream()
                            ? FrameStreamResourceMapper.typedHandles()
                            : FrameStreamResourceMapper.generationOneSlots(),
                    activeDriver::customShaderUsesExpandedVertexInput);
            frameStreamArenas = new FrameStreamArenaPool(configuredFrameArenaBytes());
            log("RustedVK FrameStream desktop submission is active with "
                    + frameStreamArenas.arenaCount() + " x "
                    + (frameStreamArenas.arenaCapacity() / (1024 * 1024))
                    + " MiB direct arenas");
        }
        if (nextFrameStreamId == Long.MAX_VALUE) {
            throw new IllegalStateException("FrameStream frame IDs exhausted");
        }
        long frameId = ++nextFrameStreamId;
        for (;;) {
            try (FrameStreamArenaPool.WriteLease writer = frameStreamArenas.acquireWriter()) {
                frameStreamEncoder.encodeTo(frameId,
                        activeDriver.requiredResourceSequence(), submission, writer.buffer());
                writer.publish();
            } catch (FrameStreamCapacityException capacity) {
                growFrameArenas(capacity.requiredBytes());
                continue;
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted while acquiring a FrameStream arena",
                        interrupted);
            }
            try (FrameStreamArenaPool.DecodeLease decoder =
                         frameStreamArenas.acquireDecoder()) {
                if (decoder.frameId() != frameId) {
                    throw new IllegalStateException("FrameStream arena order changed: expected "
                            + frameId + " but decoded " + decoder.frameId());
                }
                return activeDriver.presentFrameStream(decoder.buffer());
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted while decoding a FrameStream arena",
                        interrupted);
            }
        }
    }

    private static int configuredFrameArenaBytes() {
        int mib = Integer.getInteger("rusted.fabric.vulkan.frameArenaMiB", 16);
        if (mib < 1 || mib > 256) {
            throw new IllegalArgumentException(
                    "rusted.fabric.vulkan.frameArenaMiB must be in [1,256]");
        }
        return Math.multiplyExact(mib, 1024 * 1024);
    }

    private static void growFrameArenas(int requiredBytes) {
        int capacity = frameStreamArenas.arenaCapacity();
        int grown = capacity;
        while (grown < requiredBytes && grown < FrameStreamFormat.MAX_STREAM_BYTES) {
            long doubled = (long) grown * 2L;
            grown = (int) Math.min(doubled, FrameStreamFormat.MAX_STREAM_BYTES);
        }
        if (grown < requiredBytes || grown == capacity) {
            throw new IllegalStateException("FrameStream exceeds the maximum arena capacity: "
                    + requiredBytes);
        }
        frameStreamArenas = new FrameStreamArenaPool(grown);
        log("Expanded the bounded FrameStream arena set from " + capacity + " to "
                + grown + " bytes per arena");
    }

    public static synchronized boolean recordNativeText(
            String text, float x, float y, Paint paint, VulkanDrawState state) {
        if (nativeFrameBuilder == null || text == null || text.isEmpty() || paint == null) {
            return false;
        }
        return recordNativeText(nativeFrameBuilder, text, x, y, paint, state);
    }

    public static synchronized boolean recordNativeText(
            VulkanFrameCommands.Builder builder, String text, float x, float y,
            Paint paint, VulkanDrawState state) {
        if (builder == null || text == null || text.isEmpty() || paint == null) return false;
        boolean bold = paint.i() != null && paint.i().a();
        VulkanTextTextureCache.Entry texture = textureForText(
                text, Math.round(paint.k()), bold);
        if (texture == null) return false;
        float left = x;
        if (paint.j() == Paint$Align.b) left -= texture.width * 0.5f;
        else if (paint.j() == Paint$Align.c) left -= texture.width;
        int color = paint.e();
        float red = ((color >>> 16) & 255) / 255.0f;
        float green = ((color >>> 8) & 255) / 255.0f;
        float blue = (color & 255) / 255.0f;
        float alpha = ((color >>> 24) & 255) / 255.0f;
        for (VulkanTextTextureCache.GlyphBatch batch : texture.batches) {
            builder.texturedQuadBatch(batch.textureHandle, left, y, batch.geometry,
                    red, green, blue, alpha, state);
        }
        textRunsSubmitted++;
        textGlyphQuadsSubmitted += texture.glyphs.length;
        textBatchCommandsSubmitted += texture.batches.length;
        return true;
    }

    public static synchronized void registerNativeLibRocketTexture(
            Object holder, int width, int height, byte[] rgba) {
        if (slickImageTextureCache != null) {
            slickImageTextureCache.registerPixels(holder, width, height, rgba);
        }
    }

    public static synchronized void releaseNativeLibRocketTexture(Object holder) {
        if (slickImageTextureCache != null) slickImageTextureCache.invalidate(holder);
    }

    private static float[] argb(int color) {
        return new float[] {
                ((color >>> 16) & 255) / 255.0f,
                ((color >>> 8) & 255) / 255.0f,
                (color & 255) / 255.0f,
                ((color >>> 24) & 255) / 255.0f
        };
    }

    /**
     * Replaces the non-window portion of AppGameContainer.setup. Slick's original implementation
     * cannot be entered because it treats a live LWJGL2 Display and Graphics as invariants.
     */
    public static synchronized void bindNativeGameContainer(Object container) {
        if (!isNativeRendererSelected()) return;
        if (startupPhase.ordinal() < StartupPhase.NATIVE_WINDOW_READY.ordinal()) {
            throw new IllegalStateException("Native window must exist before binding the game");
        }
        if (nativeGame != null) return;
        try {
            Field gameField = findField(container.getClass(), "game");
            gameField.setAccessible(true);
            Object game = gameField.get(container);
            if (game == null) throw new IllegalStateException("Slick game instance is null");

            invokeContainerOption(container, "setAlwaysRender", boolean.class, true);
            invokeContainerOption(container, "setForceExit", boolean.class, true);
            invokeContainerOption(container, "setShowFPS", boolean.class, false);
            invokeContainerOption(container, "setTargetFrameRate", int.class, 300);
            invokeContainerOption(container, "setUpdateOnlyWhenVisible", boolean.class, false);

            if (!(game instanceof NativeSlickGameBridge)) {
                throw new IllegalStateException("SlickGame native bridge was not applied to "
                        + game.getClass().getName());
            }
            NativeSlickGameBridge bridge = (NativeSlickGameBridge) game;
            bridge.vulkanmod$bindNativeContainer(
                    (org.newdawn.slick.GameContainer) container);
            nativeGame = bridge;
            startupPhase = StartupPhase.NATIVE_GAME_BOUND;
            log("Bound SlickGame to the native container without running SlickGame.init/OpenGL");
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Could not bind the native game container", failure);
        }
    }

    private static void startNativeGameSystems() {
        if (nativeGameSystemsStarted || nativeGame == null) return;
        nativeGameSystemsStarted = true;
        try {
            nativeGame.vulkanmod$startNativeGameSystems();
            VulkanSurfaceInfo current = surfaceInfo;
            if (current != null) {
                nativeGame.vulkanmod$syncNativeResolution(current.width(), current.height());
            }
            startupPhase = StartupPhase.NATIVE_GAME_SYSTEMS_READY;
            log("Native game systems initialized without an LWJGL2 Display");
        } catch (Throwable failure) {
            Throwable cause = failure.getCause() == null ? failure : failure.getCause();
            throw new IllegalStateException(
                    "Native game-system initialization reached an unsupported legacy boundary: "
                            + cause.getClass().getName() + ": " + cause.getMessage(), cause);
        }
    }

    private static void invokeContainerOption(Object target, String name,
                                              Class<?> parameterType, Object value)
            throws ReflectiveOperationException {
        target.getClass().getMethod(name, parameterType).invoke(target, value);
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
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
        boolean nativeFrame = isNativeRendererSelected() && nativeFrameBuilder != null;
        if ((!nativeFrame && (!isTakeoverCaptureActive() || !takeoverFrameOpen))
                || renderer == null) return false;
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
            SlickImageVulkanTextureCache.Entry texture = nativeFrame
                    ? slickImageTextureCache.textureNative(holder)
                    : slickImageTextureCache.texture(holder, image);
            if (texture == null) {
                if (!nativeFrame) {
                    takeoverCapture.unsupportedExternal();
                    return false;
                }
                // Slick deliberately permits a lazy texture to be absent for a frame. Unit icons
                // are drawn through the game's GraphicsEngine, while ordinary lazy images get a
                // faint untextured placeholder until their upload is available. Native mode has
                // no GL Graphics to fall back to, so reproduce that behavior here.
                if (holder.unitType != null && drawNativeLibRocketUnit(holder, positions, indices,
                        translationX, translationY, renderer)) {
                    return true;
                }
                noColor = holder.noColor;
                alpha = holder.lazy ? 0.1f : holder.alpha;
            } else {
                textureHandle = texture.textureHandle;
                uScale = texture.uScale;
                vScale = texture.vScale;
                noColor = holder.noColor;
                alpha = holder.alpha;
            }
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
        if (nativeFrame) {
            return appendNativeLibRocketGeometry(positions, uvs, colors, indices,
                    translationX, translationY, capturedTexture, capturedUScale,
                    capturedVScale, capturedNoColor, capturedAlpha, clip);
        }
        return captureSafely("LibRocket geometry", () -> takeoverCapture.libRocketGeometry(
                positions, uvs, colors, indices, translationX, translationY,
                capturedTexture, capturedUScale, capturedVScale,
                capturedNoColor, capturedAlpha, clip));
    }

    private static boolean drawNativeLibRocketUnit(
            LibRocketTextureHolder holder, float[] positions, int[] indices,
            float translationX, float translationY, LibRocketSlickRenderer renderer) {
        if (holder == null || holder.unitType == null || positions == null || indices == null
                || positions.length < 2 || positions.length % 2 != 0 || indices.length == 0) {
            return false;
        }
        float left = Float.POSITIVE_INFINITY;
        float top = Float.POSITIVE_INFINITY;
        float right = Float.NEGATIVE_INFINITY;
        float bottom = Float.NEGATIVE_INFINITY;
        for (int index : indices) {
            int offset = index * 2;
            if (index < 0 || offset + 1 >= positions.length) return false;
            float x = positions[offset] + translationX;
            float y = positions[offset + 1] + translationY;
            if (!Float.isFinite(x) || !Float.isFinite(y)) return false;
            left = Math.min(left, x);
            top = Math.min(top, y);
            right = Math.max(right, x);
            bottom = Math.max(bottom, y);
        }
        float height = bottom - top;
        if (!(height > 0.0f)) return false;
        GameEngine game = GameEngine.getInstance();
        if (game == null || game.renderGraphicsEngine == null) return false;
        GraphicsEngine graphics = game.renderGraphicsEngine;
        graphics.save();
        try {
            LibRocketUiEngineStateAccessor state =
                    (LibRocketUiEngineStateAccessor) (Object) renderer;
            if (state.vulkanmod$isScissorEnabled()) {
                graphics.setClipRect(state.vulkanmod$getScissorRectF());
            }
            Team team = Team.getTeamById(0);
            if (team == null) team = Team.i;
            float angle = game.renderTimeMillis / 1000.0f / 10.0f * 360.0f % 360.0f;
            BuiltinUnitType.a(holder.unitType, (left + right) * 0.5f,
                    (top + bottom) * 0.5f, angle, 3.0f, team,
                    height * 0.6f, height, false, false, 1, null);
            graphics.flush();
            return true;
        } finally {
            graphics.restore();
        }
    }

    private static boolean appendNativeLibRocketGeometry(
            float[] positions, float[] uvs, int[] packedColors, int[] indices,
            float translationX, float translationY, long textureHandle,
            float uScale, float vScale, boolean ignoreVertexColor, float alpha,
            VulkanClipRect clip) {
        if (nativeFrameBuilder == null || positions == null || uvs == null
                || packedColors == null || indices == null
                || positions.length % 2 != 0 || uvs.length < positions.length
                || packedColors.length < positions.length / 2 || indices.length % 3 != 0) {
            return false;
        }
        VulkanDrawState drawState = new VulkanDrawState(
                io.github.endx.vulkanmod.spi.VulkanTransform2D.IDENTITY, clip);
        // Reuse one scratch triangle for the whole LibRocket geometry call. Each pooled command
        // copies into its retained arrays, so these buffers can be overwritten immediately.
        float[] trianglePositions = new float[6];
        float[] triangleUvs = textureHandle == 0L ? null : new float[6];
        float[] triangleColors = new float[12];
        for (int triangleIndex = 0; triangleIndex < indices.length; triangleIndex += 3) {
            for (int vertex = 0; vertex < 3; vertex++) {
                int sourceVertex = indices[triangleIndex + vertex];
                if (sourceVertex < 0 || sourceVertex * 2 + 1 >= positions.length) return false;
                trianglePositions[vertex * 2] = positions[sourceVertex * 2] + translationX;
                trianglePositions[vertex * 2 + 1] = positions[sourceVertex * 2 + 1]
                        + translationY;
                if (triangleUvs != null) {
                    triangleUvs[vertex * 2] = uvs[sourceVertex * 2] * uScale;
                    triangleUvs[vertex * 2 + 1] = uvs[sourceVertex * 2 + 1] * vScale;
                }
                int packed = packedColors[sourceVertex];
                int colorOffset = vertex * 4;
                triangleColors[colorOffset] = ignoreVertexColor ? 1.0f
                        : ((packed >>> 24) & 255) / 255.0f;
                triangleColors[colorOffset + 1] = ignoreVertexColor ? 1.0f
                        : ((packed >>> 16) & 255) / 255.0f;
                triangleColors[colorOffset + 2] = ignoreVertexColor ? 1.0f
                        : ((packed >>> 8) & 255) / 255.0f;
                triangleColors[colorOffset + 3] = (ignoreVertexColor ? 1.0f
                        : (packed & 255) / 255.0f) * alpha;
            }
            if (textureHandle == 0L) {
                nativeFrameBuilder.coloredTriangle(
                        trianglePositions, triangleColors, drawState);
            } else {
                nativeFrameBuilder.texturedTriangle(textureHandle,
                        trianglePositions, triangleUvs, triangleColors, drawState);
            }
        }
        return true;
    }

    public static synchronized void releaseLibRocketTexture(
            LibRocketSlickRenderer renderer, int textureId) {
        if (renderer == null || slickImageTextureCache == null) return;
        Object holder = renderer.findTextureHolder(textureId);
        Object image = SlickImageVulkanTextureCache.imageFromHolder(holder);
        if (image != null) slickImageTextureCache.invalidate(image);
        if (holder != null) slickImageTextureCache.invalidate(holder);
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

    public static long textureForGameImage(GameImage image) {
        GameImageVulkanTextureCache cache = gameTextureCache;
        if (cache == null) {
            throw new IllegalStateException("Vulkan game-image cache is unavailable");
        }
        return cache.texture(image);
    }

    public static synchronized long createNativeRenderTarget(int width, int height) {
        if (!isNativeRendererSelected() || activeDriver == null) {
            throw new IllegalStateException("native Vulkan render targets are unavailable");
        }
        return activeDriver.createRenderTarget(width, height);
    }

    public static synchronized long compileNativeFragmentShader(
            VulkanCustomFragmentShader shader) {
        if (!isNativeRendererSelected() || activeDriver == null) {
            throw new IllegalStateException("native Vulkan shader compiler is unavailable");
        }
        return activeDriver.compileFragmentShader(shader);
    }

    public static synchronized void destroyNativeFragmentShader(long shaderHandle) {
        if (shaderHandle != 0L && activeDriver != null) {
            activeDriver.destroyFragmentShader(shaderHandle);
        }
    }

    public static synchronized long compileNativeShaderProgram(
            VulkanCustomShaderProgram program) {
        if (!isNativeRendererSelected() || activeDriver == null) {
            throw new IllegalStateException("native Vulkan shader compiler is unavailable");
        }
        return activeDriver.compileShaderProgram(program);
    }

    public static synchronized void destroyNativeShaderProgram(long shaderHandle) {
        if (shaderHandle != 0L && activeDriver != null) {
            activeDriver.destroyShaderProgram(shaderHandle);
        }
    }

    public static synchronized void renderNativeTarget(
            long textureHandle, VulkanFrameCommands frame) {
        if (!isNativeRendererSelected() || activeDriver == null) {
            throw new IllegalStateException("native Vulkan render targets are unavailable");
        }
        if (nativeRenderTargetPasses != null) {
            nativeRenderTargetPasses.add(new VulkanRenderTargetPass(textureHandle, frame));
        } else {
            try {
                activeDriver.renderToTexture(textureHandle, frame);
            } finally {
                frame.releasePooledCommands();
            }
        }
    }

    private static void flushNativeRenderTargetPasses() {
        if (nativeRenderTargetPasses == null || nativeRenderTargetPasses.isEmpty()) return;
        List<VulkanRenderTargetPass> pending =
                new ArrayList<VulkanRenderTargetPass>(nativeRenderTargetPasses);
        nativeRenderTargetPasses.clear();
        try {
            for (VulkanRenderTargetPass pass : pending) {
                try {
                    activeDriver.renderToTexture(pass.textureHandle(), pass.frame());
                } finally {
                    pass.frame().releasePooledCommands();
                }
            }
        } finally {
            for (VulkanRenderTargetPass pass : pending) {
                pass.frame().releasePooledCommands();
            }
        }
    }

    public static synchronized VulkanTextureData readNativeTexture(long textureHandle) {
        if (!isNativeRendererSelected() || activeDriver == null) {
            throw new IllegalStateException("native Vulkan texture readback is unavailable");
        }
        flushNativeRenderTargetPasses();
        return activeDriver.readTexture(textureHandle);
    }

    public static synchronized VulkanTextureData readNativeTextureRegion(
            long textureHandle, int x, int y, int width, int height) {
        if (!isNativeRendererSelected() || activeDriver == null) {
            throw new IllegalStateException("native Vulkan texture readback is unavailable");
        }
        flushNativeRenderTargetPasses();
        return activeDriver.readTextureRegion(textureHandle, x, y, width, height);
    }

    public static synchronized void updateNativeTexture(
            long textureHandle, VulkanTextureData texture) {
        if (!isNativeRendererSelected() || activeDriver == null) {
            throw new IllegalStateException("native Vulkan texture updates are unavailable");
        }
        flushNativeRenderTargetPasses();
        activeDriver.updateTexture(textureHandle, texture);
    }

    public static synchronized void updateNativeTextureRegion(
            long textureHandle, int x, int y, VulkanTextureData texture) {
        if (!isNativeRendererSelected() || activeDriver == null) {
            throw new IllegalStateException("native Vulkan texture updates are unavailable");
        }
        flushNativeRenderTargetPasses();
        activeDriver.updateTextureRegion(textureHandle, x, y, texture);
    }

    public static synchronized void destroyNativeRenderTarget(long textureHandle) {
        if (textureHandle != 0L && activeDriver != null) {
            flushNativeRenderTargetPasses();
            activeDriver.destroyTexture(textureHandle);
        }
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

    public static synchronized VulkanTextMetrics measureNativeText(
            String text, int size, boolean bold) {
        if (textTextureCache == null) {
            throw new IllegalStateException("native Vulkan text cache is unavailable");
        }
        return textTextureCache.measure(text, size, bold);
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

    /** Cumulative renderer counters intended for low-frequency profiler sampling. */
    public static synchronized java.util.Map<String, Long> performanceStatistics() {
        if (activeDriver == null && frameStreamEncoder == null) {
            return java.util.Collections.emptyMap();
        }
        java.util.LinkedHashMap<String, Long> statistics =
                new java.util.LinkedHashMap<String, Long>();
        if (activeDriver != null) statistics.putAll(activeDriver.performanceStatistics());
        if (frameStreamEncoder != null) {
            statistics.put("frame.encodeCount", frameStreamEncoder.directEncodeCount());
            statistics.put("frame.encodeBytes", frameStreamEncoder.directEncodeBytes());
            statistics.put("frame.encodeNanos", frameStreamEncoder.directEncodeNanos());
            statistics.put("frame.capacityMisses", frameStreamEncoder.directCapacityMisses());
            statistics.put("frame.workspaceGrowths",
                    frameStreamEncoder.directWorkspaceGrowths());
        }
        statistics.put("text.runs", textRunsSubmitted);
        statistics.put("text.glyphQuads", textGlyphQuadsSubmitted);
        statistics.put("text.batchCommands", textBatchCommandsSubmitted);
        return java.util.Collections.unmodifiableMap(statistics);
    }

    public static synchronized void invalidateCachedImage(Object image) {
        if (gameTextureCache != null) gameTextureCache.invalidate(image);
    }

    public static synchronized void markRenderTargetImage(Object image) {
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
        nativeRendererSelected = false;
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
        frameStreamEncoder = null;
        frameStreamArenas = null;
        nextFrameStreamId = 0L;
        textRunsSubmitted = 0L;
        textGlyphQuadsSubmitted = 0L;
        textBatchCommandsSubmitted = 0L;
        nativeFrameClock.clear();
    }

    private static void log(String message) {
        System.out.println("[Vulkan Mod] " + message);
    }
}
