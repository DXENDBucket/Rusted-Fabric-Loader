package io.github.endx.rustedfabric.android.launcher.jvm;

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

    /** Queues a direct, pixel-precise scroll of the Rocket element under the finger. */
    public static boolean scrollUiByTouchDelta(float deltaY) {
        return PACKAGED && nativeScrollUiByTouchDelta(deltaY);
    }

    public static boolean sendMouseButton(int button, int action) {
        return PACKAGED && nativeSendMouseButton(button, action);
    }

    /** Queues a positioned, complete press/release tap for atomic Rocket UI delivery. */
    public static boolean sendMouseClick(int button, float x, float y) {
        return PACKAGED && nativeSendMouseClick(button, x, y);
    }

    public static boolean sendKey(int key, int action) {
        return PACKAGED && nativeSendKey(key, action);
    }

    /** Queues committed Android IME text for the focused libRocket form control. */
    public static boolean sendText(String text) {
        return PACKAGED && text != null && !text.isEmpty() && nativeSendText(text);
    }

    public static boolean sendTextBackspace(int count) {
        return PACKAGED && count > 0 && nativeSendTextKey(0, count);
    }

    public static boolean sendTextEnter() {
        return PACKAGED && nativeSendTextKey(1, 1);
    }

    /** Publishes one Android touch frame to the embedded desktop game core. */
    public static boolean sendTouchFrame(float[] xs, float[] ys, int[] pointerIds,
                                         int count, boolean down, int action) {
        if (!PACKAGED) return false;
        if (xs == null || ys == null || pointerIds == null || count < 0
                || count > 10 || xs.length < count || ys.length < count
                || pointerIds.length < count) {
            throw new IllegalArgumentException("Invalid touch frame");
        }
        return nativeSendTouchFrame(xs, ys, pointerIds, count, down, action);
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

    /** True when the current libRocket focus accepts typed text. */
    public static boolean uiTextInputActive() {
        return PACKAGED && nativeUiTextInputActive();
    }

    public static String smokeTest(int width, int height) {
        if (!PACKAGED) return "Android EGL bridge is not packaged for this ABI";
        return nativeSmokeTest(width, height);
    }

    private static native void nativeAttachSurface(Surface surface);
    private static native void nativeDetachSurface();
    private static native boolean nativeSendPointer(float x, float y, int buttonAction);
    private static native boolean nativeSendScroll(double x, double y);
    private static native boolean nativeScrollUiByTouchDelta(float deltaY);
    private static native boolean nativeSendMouseButton(int button, int action);
    private static native boolean nativeSendMouseClick(int button, float x, float y);
    private static native boolean nativeSendKey(int key, int action);
    private static native boolean nativeSendText(String text);
    private static native boolean nativeSendTextKey(int key, int count);
    private static native boolean nativeSendTouchFrame(float[] xs, float[] ys,
                                                       int[] pointerIds, int count,
                                                       boolean down, int action);
    private static native boolean nativeUiWantsScroll();
    private static native boolean nativeUiPrefersDrag();
    private static native boolean nativeUiIsActive();
    private static native boolean nativeUiTextInputActive();
    private static native String nativeSmokeTest(int width, int height);
}
