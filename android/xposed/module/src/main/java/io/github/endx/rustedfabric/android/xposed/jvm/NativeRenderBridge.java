package io.github.endx.rustedfabric.android.xposed.jvm;

import android.view.Surface;

/** Small Android Surface/EGL boundary; it is not yet the LWJGL2 compatibility implementation. */
public final class NativeRenderBridge {
    private static final boolean PACKAGED;

    static {
        boolean loaded;
        try {
            System.loadLibrary("rustedfabric_renderbridge");
            loaded = true;
        } catch (LinkageError unavailable) {
            loaded = false;
        }
        PACKAGED = loaded;
    }

    private NativeRenderBridge() {
    }

    public static boolean isPackaged() {
        return PACKAGED;
    }

    public static void attachSurface(Surface surface) {
        if (!PACKAGED) throw new IllegalStateException(
                "Android EGL bridge is not packaged for this ABI");
        if (surface == null || !surface.isValid()) throw new IllegalArgumentException(
                "Android Surface is not valid");
        nativeAttachSurface(surface);
    }

    public static void detachSurface() {
        if (PACKAGED) nativeDetachSurface();
    }

    /** Queues a desktop cursor event; buttonAction is -1 for move, 0 for release, 1 for press. */
    public static boolean sendPointer(float x, float y, int buttonAction) {
        return PACKAGED && nativeSendPointer(x, y, buttonAction);
    }

    public static boolean sendScroll(double x, double y) {
        return PACKAGED && nativeSendScroll(x, y);
    }

    public static String smokeTest(int width, int height) {
        if (!PACKAGED) return "Android EGL bridge is not packaged for this ABI";
        return nativeSmokeTest(width, height);
    }

    private static native void nativeAttachSurface(Surface surface);
    private static native void nativeDetachSurface();
    private static native boolean nativeSendPointer(float x, float y, int buttonAction);
    private static native boolean nativeSendScroll(double x, double y);
    private static native String nativeSmokeTest(int width, int height);
}
