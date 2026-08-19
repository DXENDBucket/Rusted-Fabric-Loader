package io.github.endx.vulkanmod.android;

import io.github.endx.vulkanmod.framestream.FrameStreamFormat;
import io.github.endx.vulkanmod.framestream.FrameStreamEncoder;
import io.github.endx.vulkanmod.framestream.FrameStreamResourceMapper;
import io.github.endx.vulkanmod.resourcestream.ResourceStreamFormat;
import io.github.endx.vulkanmod.spi.VulkanDeviceInfo;
import io.github.endx.vulkanmod.spi.VulkanFrameCommands;
import io.github.endx.vulkanmod.spi.VulkanFrameSubmission;
import io.github.endx.vulkanmod.spi.VulkanInputEvent;
import io.github.endx.vulkanmod.spi.VulkanPlatformDriver;
import io.github.endx.vulkanmod.spi.VulkanProbeResult;
import io.github.endx.vulkanmod.spi.VulkanSurfaceInfo;
import io.github.endx.vulkanmod.spi.VulkanSurfaceRequest;
import io.github.endx.vulkanmod.spi.VulkanRenderTargetPass;
import io.github.endx.vulkanmod.spi.VulkanTextureData;
import io.github.endx.vulkanmod.spi.VulkanTextRasterizer;
import io.github.endx.vulkanmod.spi.VulkanWindowRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.nio.ByteBuffer;

/**
 * HotSpot-side adapter for the Vulkan backend packaged by the Android launcher APK.
 *
 * <p>This class deliberately contains no Android framework references. The launcher owns the
 * {@code SurfaceView} and publishes its {@code ANativeWindow} through the existing render bridge;
 * this adapter only performs coarse JNI calls into the APK-owned backend.</p>
 */
public final class AndroidVulkanPlatformDriver implements VulkanPlatformDriver {
    private static final int BACKEND_ABI_MAJOR = 1;
    private static final int BACKEND_ABI_MINOR = 0;
    private static final int MAX_REPORTED_DEVICES = 256;
    private static final String NATIVE_LIBRARY = "rustedfabric_vulkan";
    private static final Throwable LOAD_FAILURE;

    static {
        Throwable failure = null;
        try {
            System.loadLibrary(NATIVE_LIBRARY);
        } catch (LinkageError | SecurityException unavailable) {
            failure = unavailable;
        }
        LOAD_FAILURE = failure;
    }

    private VulkanProbeResult probeResult;
    private boolean nativeInitialized;
    private VulkanSurfaceInfo surfaceInfo;
    private FrameStreamEncoder standaloneEncoder;
    private long standaloneFrameId;
    private int pointerX;
    private int pointerY;

    @Override public String name() {
        return "RustedVK Android NDK";
    }

    @Override public synchronized VulkanProbeResult probe() {
        if (probeResult != null) return probeResult;
        if (LOAD_FAILURE != null) {
            probeResult = VulkanProbeResult.unavailable("Android Vulkan backend library "
                    + "is unavailable: " + LOAD_FAILURE.getClass().getSimpleName() + ": "
                    + messageOf(LOAD_FAILURE));
            return probeResult;
        }
        try {
            int status = nativeInitialize(BACKEND_ABI_MAJOR, BACKEND_ABI_MINOR,
                    FrameStreamFormat.MAJOR_VERSION, FrameStreamFormat.MINOR_VERSION,
                    ResourceStreamFormat.MAJOR_VERSION, ResourceStreamFormat.MINOR_VERSION);
            if (status != 0) {
                probeResult = VulkanProbeResult.unavailable(nativeLastDiagnostic());
                return probeResult;
            }
            nativeInitialized = true;
            int count = nativeDeviceCount();
            if (count < 0 || count > MAX_REPORTED_DEVICES) {
                throw new IllegalStateException("native backend reported invalid device count: "
                        + count);
            }
            List<VulkanDeviceInfo> devices = new ArrayList<VulkanDeviceInfo>(count);
            for (int index = 0; index < count; index++) {
                devices.add(new VulkanDeviceInfo(nativeDeviceName(index),
                        nativeDeviceVendorId(index), nativeDeviceId(index),
                        nativeDeviceType(index), nativeDeviceApiVersion(index),
                        nativeDeviceDriverVersion(index)));
            }
            if (devices.isEmpty()) {
                probeResult = VulkanProbeResult.unavailable("Android Vulkan loader reported no "
                        + "physical device");
            } else {
                probeResult = VulkanProbeResult.available(nativeInstanceVersion(), devices);
                long[] surface = currentSurfaceState();
                System.out.println("[Vulkan Mod/Android] Surface generation=" + surface[0]
                        + ", attached=" + (surface[1] != 0L) + ", size=" + surface[2]
                        + "x" + surface[3]);
            }
        } catch (RuntimeException | LinkageError failure) {
            probeResult = VulkanProbeResult.unavailable("Android Vulkan probe failed: "
                    + failure.getClass().getSimpleName() + ": " + messageOf(failure));
        }
        return probeResult;
    }

    /** Returns {generation, attached, width, height} for diagnostics and lifecycle tests. */
    public long[] currentSurfaceState() {
        requireLoaded();
        long[] state = nativeSurfaceState();
        if (state == null || state.length != 4) {
            throw new IllegalStateException("native backend returned an invalid Surface state");
        }
        return state.clone();
    }

    @Override public VulkanSurfaceInfo createSurface(VulkanSurfaceRequest request) {
        if (request == null) throw new NullPointerException("request");
        return createAttachedSurface();
    }

    @Override public VulkanSurfaceInfo createNativeWindowSurface(VulkanWindowRequest request) {
        if (request == null) throw new NullPointerException("request");
        return createAttachedSurface();
    }

    @Override public long uploadTexture(VulkanTextureData texture) {
        if (texture == null) throw new NullPointerException("texture");
        long handle = nativeUploadTexture(
                texture.width(), texture.height(), texture.copyRgba());
        if (handle == 0L) throw new IllegalStateException(nativeLastDiagnostic());
        return handle;
    }

    @Override public void updateTextureRegion(long textureHandle, int x, int y,
                                               VulkanTextureData texture) {
        if (texture == null) throw new NullPointerException("texture");
        if (!nativeUpdateTextureRegion(textureHandle, x, y,
                texture.width(), texture.height(), texture.copyRgba())) {
            throw new IllegalArgumentException(nativeLastDiagnostic());
        }
    }

    @Override public long createRenderTarget(int width, int height) {
        long handle = nativeCreateRenderTarget(width, height);
        if (handle == 0L) throw new IllegalStateException(nativeLastDiagnostic());
        return handle;
    }

    @Override public synchronized void renderToTexture(long textureHandle,
                                                       VulkanFrameCommands frame) {
        if (frame == null) throw new NullPointerException("frame");
        if (surfaceInfo == null) createAttachedSurface();
        if (standaloneEncoder == null) {
            standaloneEncoder = new FrameStreamEncoder(
                    FrameStreamResourceMapper.generationOneSlots(), ignored -> false);
        }
        if (standaloneFrameId == Long.MAX_VALUE) {
            throw new IllegalStateException("Android standalone FrameStream IDs exhausted");
        }
        VulkanFrameCommands presentation = VulkanFrameCommands.builder(
                surfaceInfo.width(), surfaceInfo.height()).clear(0, 0, 0, 1).build();
        VulkanFrameSubmission submission = new VulkanFrameSubmission(
                Collections.singletonList(new VulkanRenderTargetPass(textureHandle, frame)),
                presentation);
        ByteBuffer encoded = standaloneEncoder.encode(++standaloneFrameId, 0L, submission);
        long[] result = nativePresentFrameStream(encoded);
        if (result == null) throw new IllegalStateException(nativeLastDiagnostic());
        surfaceInfo = surfaceInfo(result);
    }

    @Override public void destroyTexture(long textureHandle) {
        if (!nativeDestroyTexture(textureHandle)) {
            throw new IllegalArgumentException(nativeLastDiagnostic());
        }
    }

    @Override public boolean supportsFrameStream() {
        return true;
    }

    @Override public VulkanTextRasterizer createTextRasterizer() {
        return new AndroidAwtTextRasterizer();
    }

    @Override public List<VulkanInputEvent> pollInputEvents() {
        ArrayList<VulkanInputEvent> events = new ArrayList<VulkanInputEvent>();
        while (events.size() < 256) {
            double[] raw = nativePollInputEvent();
            if (raw == null) break;
            if (raw.length != 6) throw new IllegalStateException(
                    "Android native input record has an invalid length");
            int kind = (int) raw[0];
            switch (kind) {
                case 0: // Cursor
                    pointerX = Math.round((float) raw[1]);
                    pointerY = Math.round((float) raw[2]);
                    events.add(VulkanInputEvent.pointer(VulkanInputEvent.Type.POINTER_MOVE,
                            pointerX, pointerY, -1));
                    break;
                case 1: { // MouseButton
                    int button = (int) raw[3];
                    boolean down = (int) raw[4] != 0;
                    events.add(VulkanInputEvent.pointer(down
                                    ? VulkanInputEvent.Type.BUTTON_DOWN
                                    : VulkanInputEvent.Type.BUTTON_UP,
                            pointerX, pointerY, button));
                    break;
                }
                case 2: { // Atomic MouseClick
                    pointerX = Math.round((float) raw[1]);
                    pointerY = Math.round((float) raw[2]);
                    int button = (int) raw[3];
                    events.add(VulkanInputEvent.pointer(VulkanInputEvent.Type.POINTER_MOVE,
                            pointerX, pointerY, -1));
                    events.add(VulkanInputEvent.pointer(VulkanInputEvent.Type.BUTTON_DOWN,
                            pointerX, pointerY, button));
                    events.add(VulkanInputEvent.pointer(VulkanInputEvent.Type.BUTTON_UP,
                            pointerX, pointerY, button));
                    break;
                }
                case 3: // Scroll
                    events.add(VulkanInputEvent.wheel(pointerX, pointerY,
                            (int) Math.round(raw[2] * 120.0)));
                    break;
                case 4: { // GLFW key record; convert the keys currently emitted by the Activity.
                    int key = (int) raw[3] == 256 ? 0x1b : (int) raw[3];
                    boolean down = (int) raw[4] != 0;
                    events.add(VulkanInputEvent.key(down
                                    ? VulkanInputEvent.Type.KEY_DOWN
                                    : VulkanInputEvent.Type.KEY_UP,
                            key, down ? 1 : 0));
                    break;
                }
                default:
                    throw new IllegalStateException("Unknown Android native input kind " + kind);
            }
        }
        return events;
    }

    @Override public VulkanSurfaceInfo presentFrameStream(ByteBuffer frameStream) {
        if (frameStream == null) throw new NullPointerException("frameStream");
        if (!frameStream.isDirect()) {
            throw new IllegalArgumentException("Android FrameStream must use direct memory");
        }
        ByteBuffer submitted = frameStream.slice();
        long[] result = nativePresentFrameStream(submitted);
        if (result == null) throw new IllegalStateException(nativeLastDiagnostic());
        surfaceInfo = surfaceInfo(result);
        return surfaceInfo;
    }

    @Override public VulkanSurfaceInfo presentFrame(VulkanFrameCommands frame) {
        if (frame == null) throw new NullPointerException("frame");
        if (frame.commandCount() != 0) {
            throw new UnsupportedOperationException(
                    "Android Vulkan currently supports clear-only frames");
        }
        long[] result = nativePresentClear(frame.clearRequested() ? frame.clearRed() : 0.0f,
                frame.clearRequested() ? frame.clearGreen() : 0.0f,
                frame.clearRequested() ? frame.clearBlue() : 0.0f,
                frame.clearRequested() ? frame.clearAlpha() : 1.0f);
        return result == null ? null : surfaceInfo(result);
    }

    @Override public synchronized void close() {
        if (nativeInitialized) {
            nativeShutdown();
            nativeInitialized = false;
        }
    }

    private VulkanSurfaceInfo createAttachedSurface() {
        requireLoaded();
        long[] result = nativeCreateSurface();
        if (result == null) throw new IllegalStateException(nativeLastDiagnostic());
        surfaceInfo = surfaceInfo(result);
        return surfaceInfo;
    }

    private VulkanSurfaceInfo surfaceInfo(long[] values) {
        if (values.length != 9) throw new IllegalStateException(
                "native backend returned invalid Vulkan surface information");
        String deviceName = nativeDeviceCount() == 0
                ? "Android Vulkan device" : nativeDeviceName(0);
        return new VulkanSurfaceInfo(deviceName, Math.toIntExact(values[0]),
                Math.toIntExact(values[1]), Math.toIntExact(values[2]),
                Math.toIntExact(values[3]), Math.toIntExact(values[4]),
                Math.toIntExact(values[5]), Math.toIntExact(values[6]),
                Math.toIntExact(values[7]));
    }

    private static UnsupportedOperationException presentationNotImplemented() {
        return new UnsupportedOperationException(
                "Android Vulkan presentation is not enabled in the inert backend stage");
    }

    private static void requireLoaded() {
        if (LOAD_FAILURE != null) throw new IllegalStateException(
                "Android Vulkan backend library is unavailable", LOAD_FAILURE);
    }

    private static String messageOf(Throwable failure) {
        String message = failure.getMessage();
        return message == null || message.trim().isEmpty() ? "no detail" : message.trim();
    }

    private static native int nativeInitialize(int backendMajor, int backendMinor,
                                                int frameMajor, int frameMinor,
                                                int resourceMajor, int resourceMinor);
    private static native String nativeLastDiagnostic();
    private static native int nativeInstanceVersion();
    private static native int nativeDeviceCount();
    private static native String nativeDeviceName(int index);
    private static native int nativeDeviceVendorId(int index);
    private static native int nativeDeviceId(int index);
    private static native int nativeDeviceType(int index);
    private static native int nativeDeviceApiVersion(int index);
    private static native int nativeDeviceDriverVersion(int index);
    private static native long[] nativeSurfaceState();
    private static native long[] nativeCreateSurface();
    private static native long nativeUploadTexture(int width, int height, byte[] rgba);
    private static native boolean nativeUpdateTextureRegion(long textureHandle, int x, int y,
                                                             int width, int height, byte[] rgba);
    private static native long nativeCreateRenderTarget(int width, int height);
    private static native boolean nativeDestroyTexture(long textureHandle);
    private static native long[] nativePresentFrameStream(ByteBuffer frameStream);
    private static native double[] nativePollInputEvent();
    private static native long[] nativePresentClear(float red, float green,
                                                     float blue, float alpha);
    private static native void nativeDestroySurface();
    private static native void nativeShutdown();
}
