package io.github.endx.vulkanmod;

import io.github.endx.vulkanmod.spi.VulkanDeviceInfo;
import io.github.endx.vulkanmod.spi.VulkanColoredQuad;
import io.github.endx.vulkanmod.spi.VulkanClipRect;
import io.github.endx.vulkanmod.spi.VulkanDrawState;
import io.github.endx.vulkanmod.spi.VulkanFrameCommands;
import io.github.endx.vulkanmod.spi.VulkanTextureData;
import io.github.endx.vulkanmod.spi.VulkanTexturedQuad;
import io.github.endx.vulkanmod.spi.VulkanTransform2D;
import rustedwarfare.client.render.GameImage;
import rustedwarfare.client.render.SlickGraphicsBackend;
import io.github.endx.vulkanmod.spi.VulkanProbeResult;
import io.github.endx.vulkanmod.spi.VulkanSurfaceInfo;

import java.util.Optional;

/** Owns Vulkan startup state and the explicitly opt-in takeover experiment. */
public final class VulkanRuntime {
    private static volatile VulkanProbeResult probeResult;
    private static VulkanDriverLoader.LoadedDriver activeDriver;
    private static volatile VulkanSurfaceInfo surfaceInfo;
    private static VulkanMode configuredMode = VulkanMode.OFF;
    private static boolean frameTestAttempted;
    private static int frameTestWaitFrames;
    private static long frameTestTexture;
    private static int frameTestTextureWidth = 32;
    private static int frameTestTextureHeight = 32;
    private static GameImageVulkanTextureCache gameTextureCache;
    private static final SlickRenderCapture takeoverCapture = new SlickRenderCapture();
    private static VulkanFrameCommands pendingTakeoverFrame;
    private static int pendingTakeoverCommands;
    private static int pendingTakeoverUnsupported;
    private static long takeoverFramesPresented;
    private static boolean takeoverFailureLogged;

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
            if (mode == VulkanMode.TAKEOVER_TEST) {
                log("Experimental Slick-to-Vulkan takeover capture is enabled");
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
        if (configuredMode == VulkanMode.TAKEOVER_TEST) {
            finishTakeoverFrame();
            presentTakeoverFrame();
            return;
        }
        if (configuredMode != VulkanMode.FRAME_TEST || frameTestAttempted
                || activeDriver == null || surfaceInfo == null) return;
        GameImage diagnosticImage = diagnosticGameImage();
        if (diagnosticImage == null && ++frameTestWaitFrames < 600) return;
        frameTestAttempted = true;
        try {
            io.github.endx.vulkanmod.spi.VulkanSurfaceRequest window =
                    Lwjgl2Win32Window.current();
            int width = window.width();
            int height = window.height();
            if (frameTestTexture == 0L) {
                GameImage gameImage = diagnosticImage;
                if (gameImage != null && gameTextureCache != null) {
                    frameTestTexture = gameTextureCache.texture(gameImage);
                    frameTestTextureWidth = gameImage.getWidth();
                    frameTestTextureHeight = gameImage.getHeight();
                    log("Frame test uses cached game image " + gameImage.getName() + " ("
                            + gameImage.getWidth() + "x" + gameImage.getHeight() + ")");
                } else {
                    frameTestTexture = activeDriver.uploadTexture(checkerTexture());
                    log("Frame test uses generated checker texture");
                }
            }
            float textureHeight = height * 0.19f;
            float textureWidth = textureHeight * frameTestTextureWidth
                    / Math.max(1.0f, frameTestTextureHeight);
            textureWidth = Math.min(textureWidth, width * 0.18f);
            float textureX = width * 0.60f;
            float textureY = height * 0.405f;
            VulkanDrawState textureState = new VulkanDrawState(
                    VulkanTransform2D.rotationAround(-9.0f,
                            textureX + textureWidth * 0.5f,
                            textureY + textureHeight * 0.5f),
                    new VulkanClipRect(textureX + textureWidth * 0.08f,
                            textureY + textureHeight * 0.08f,
                            textureWidth * 0.84f, textureHeight * 0.84f));
            VulkanFrameCommands frame = VulkanFrameCommands.builder(width, height)
                    .clear(0.035f, 0.075f, 0.16f, 1.0f)
                    .coloredQuad(new VulkanColoredQuad(width * 0.20f, height * 0.28f,
                            width * 0.60f, height * 0.44f,
                            0.08f, 0.18f, 0.34f, 1.0f))
                    .coloredQuad(new VulkanColoredQuad(width * 0.225f, height * 0.32f,
                            width * 0.55f, height * 0.06f,
                            0.20f, 0.75f, 0.95f, 1.0f))
                    .coloredQuad(new VulkanColoredQuad(width * 0.225f, height * 0.42f,
                            width * 0.34f, height * 0.035f,
                            0.72f, 0.84f, 0.94f, 1.0f))
                    .coloredQuad(new VulkanColoredQuad(width * 0.225f, height * 0.49f,
                            width * 0.45f, height * 0.035f,
                            0.42f, 0.60f, 0.76f, 1.0f))
                    .texturedQuad(new VulkanTexturedQuad(frameTestTexture,
                            textureX, textureY, textureWidth, textureHeight,
                            0.0f, 0.0f, 1.0f, 1.0f,
                            1.0f, 1.0f, 1.0f, 0.92f, textureState))
                    .build();
            VulkanSurfaceInfo updated = activeDriver.presentFrame(frame);
            surfaceInfo = updated;
            log("Presented one Vulkan frame-test batch at "
                    + updated.width() + "x" + updated.height() + " with "
                    + frame.coloredQuads().size() + " colored and "
                    + frame.texturedQuads().size() + " textured quads");
        } catch (Throwable failure) {
            log("Vulkan frame test failed; retaining Slick/OpenGL: "
                    + failure.getClass().getSimpleName() + ": " + failure.getMessage());
        }
    }

    public static synchronized boolean isTakeoverActive() {
        return configuredMode == VulkanMode.TAKEOVER_TEST
                && activeDriver != null && surfaceInfo != null;
    }

    private static void finishTakeoverFrame() {
        if (!isTakeoverActive()) return;
        int commands = takeoverCapture.commandCount();
        int unsupported = takeoverCapture.rejectedCount();
        VulkanFrameCommands frame = takeoverCapture.finish();
        if (frame != null) {
            pendingTakeoverFrame = frame;
            pendingTakeoverCommands = commands;
            pendingTakeoverUnsupported = unsupported;
        }
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

    public static synchronized boolean captureImage(SlickGraphicsBackend backend,
                                                     GameImage image, android.graphics.Rect src,
                                                     android.graphics.RectF dst,
                                                     android.graphics.Paint paint) {
        return captureSafely("image", () ->
                takeoverCapture.image(backend, image, src, dst, paint));
    }

    public static synchronized boolean captureImageRaw(SlickGraphicsBackend backend,
                                                        GameImage image, float x, float y,
                                                        android.graphics.Paint paint) {
        return captureSafely("image", () ->
                takeoverCapture.imageRaw(backend, image, x, y, paint));
    }

    public static synchronized boolean captureImageCentered(SlickGraphicsBackend backend,
                                                             GameImage image, float x, float y,
                                                             android.graphics.Paint paint) {
        return captureSafely("image", () ->
                takeoverCapture.imageCentered(backend, image, x, y, paint));
    }

    public static synchronized void noteUnsupportedDraw(SlickGraphicsBackend backend) {
        if (isTakeoverActive()) takeoverCapture.unsupported(backend);
    }

    static synchronized long textureForGameImage(GameImage image) {
        if (gameTextureCache == null) {
            throw new IllegalStateException("Vulkan game-image cache is unavailable");
        }
        return gameTextureCache.texture(image);
    }

    private static boolean captureSafely(String operation, CaptureOperation capture) {
        if (!isTakeoverActive()) return false;
        try {
            return capture.run();
        } catch (Throwable failure) {
            if (!takeoverFailureLogged) {
                takeoverFailureLogged = true;
                log("Could not capture Vulkan " + operation + "; leaving this call to OpenGL: "
                        + failure.getClass().getSimpleName() + ": " + failure.getMessage());
            }
            return false;
        }
    }

    private static void presentTakeoverFrame() {
        VulkanFrameCommands frame = pendingTakeoverFrame;
        if (activeDriver == null || surfaceInfo == null || frame == null) return;
        pendingTakeoverFrame = null;
        try {
            surfaceInfo = activeDriver.presentFrame(frame);
            takeoverFramesPresented++;
            if (takeoverFramesPresented == 1 || takeoverFramesPresented % 300 == 0) {
                log("Presented takeover frame #" + takeoverFramesPresented + " at "
                        + surfaceInfo.width() + "x" + surfaceInfo.height() + " ("
                        + pendingTakeoverCommands + " captured, "
                        + pendingTakeoverUnsupported + " unsupported draw calls)");
            }
        } catch (Throwable failure) {
            if (!takeoverFailureLogged) {
                takeoverFailureLogged = true;
                log("Vulkan takeover presentation failed; the experiment remains isolated: "
                        + failure.getClass().getSimpleName() + ": " + failure.getMessage());
            }
        }
    }

    private interface CaptureOperation {
        boolean run();
    }

    private static VulkanTextureData checkerTexture() {
        int size = 32;
        byte[] rgba = new byte[size * size * 4];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                boolean bright = ((x / 4) + (y / 4)) % 2 == 0;
                int offset = (y * size + x) * 4;
                rgba[offset] = (byte) (bright ? 56 : 10);
                rgba[offset + 1] = (byte) (bright ? 210 : 72);
                rgba[offset + 2] = (byte) (bright ? 245 : 138);
                rgba[offset + 3] = (byte) 255;
            }
        }
        return new VulkanTextureData(size, size, rgba);
    }

    public static synchronized void attachToCurrentWindow() {
        if (activeDriver == null || surfaceInfo != null) return;
        try {
            VulkanSurfaceInfo created = activeDriver.createSurface(Lwjgl2Win32Window.current());
            surfaceInfo = created;
            gameTextureCache = new GameImageVulkanTextureCache(activeDriver);
            log("Win32 surface and swapchain ready on " + created.deviceName() + ": "
                    + created.width() + "x" + created.height() + ", images="
                    + created.imageCount() + ", format=" + created.imageFormat()
                    + ", presentMode=" + created.presentMode() + ", queues="
                    + created.graphicsQueueFamily() + "/" + created.presentQueueFamily());
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

    public static synchronized void shutdown() {
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
        surfaceInfo = null;
        pendingTakeoverFrame = null;
        pendingTakeoverCommands = 0;
        pendingTakeoverUnsupported = 0;
        takeoverFramesPresented = 0;
        takeoverFailureLogged = false;
        frameTestTexture = 0L;
        frameTestWaitFrames = 0;
        frameTestTextureWidth = 32;
        frameTestTextureHeight = 32;
    }

    private static GameImage diagnosticGameImage() {
        if (SlickGraphicsBackend.generalErrorImage != null) {
            return SlickGraphicsBackend.generalErrorImage;
        }
        if (SlickGraphicsBackend.outOfMemoryErrorImage != null) {
            return SlickGraphicsBackend.outOfMemoryErrorImage;
        }
        return null;
    }

    private static void log(String message) {
        System.out.println("[Vulkan Mod] " + message);
    }
}
