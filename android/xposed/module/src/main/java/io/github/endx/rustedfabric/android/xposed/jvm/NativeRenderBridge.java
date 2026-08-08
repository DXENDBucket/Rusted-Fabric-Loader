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

    public static boolean sendMouseButton(int button, int action) {
        return PACKAGED && nativeSendMouseButton(button, action);
    }

    public static boolean sendKey(int key, int action) {
        return PACKAGED && nativeSendKey(key, action);
    }

    /** Publishes one Android touch frame to the embedded desktop game core. */
    public static boolean sendTouchFrame(float[] xs, float[] ys, int[] pointerIds,
                                         int count, boolean down) {
        if (!PACKAGED) return false;
        if (xs == null || ys == null || pointerIds == null || count < 0
                || count > 10 || xs.length < count || ys.length < count
                || pointerIds.length < count) {
            throw new IllegalArgumentException("Invalid touch frame");
        }
        return nativeSendTouchFrame(xs, ys, pointerIds, count, down);
    }

    /** True when the Rocket element below the cursor has a vertically scrollable ancestor. */
    public static boolean uiWantsScroll() {
        return PACKAGED && nativeUiWantsScroll();
    }

    /** True when the Rocket element below the cursor is a slider or scrollbar control. */
    public static boolean uiPrefersDrag() {
        return PACKAGED && nativeUiPrefersDrag();
    }

    /** True while libRocket owns a visible menu or popup document. */
    public static boolean uiIsActive() {
        return PACKAGED && nativeUiIsActive();
    }

    public static String smokeTest(int width, int height) {
        if (!PACKAGED) return "Android EGL bridge is not packaged for this ABI";
        return nativeSmokeTest(width, height);
    }

    private static native void nativeAttachSurface(Surface surface);
    private static native void nativeDetachSurface();
    private static native boolean nativeSendPointer(float x, float y, int buttonAction);
    private static native boolean nativeSendScroll(double x, double y);
    private static native boolean nativeSendMouseButton(int button, int action);
    private static native boolean nativeSendKey(int key, int action);
    private static native boolean nativeSendTouchFrame(float[] xs, float[] ys,
                                                       int[] pointerIds, int count,
                                                       boolean down);
    private static native boolean nativeUiWantsScroll();
    private static native boolean nativeUiPrefersDrag();
    private static native boolean nativeUiIsActive();
    private static native String nativeSmokeTest(int width, int height);
}
