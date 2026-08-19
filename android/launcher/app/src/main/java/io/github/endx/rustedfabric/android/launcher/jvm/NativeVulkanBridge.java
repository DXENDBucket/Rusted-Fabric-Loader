package io.github.endx.rustedfabric.android.launcher.jvm;

/** ART-side control surface for the isolated clear-only Vulkan renderer test. */
public final class NativeVulkanBridge {
    private static final Throwable LOAD_FAILURE;

    static {
        Throwable failure = null;
        try {
            // The Vulkan backend shares the Activity-owned ANativeWindow registry.
            System.loadLibrary("rustedfabric_renderbridge");
            System.loadLibrary("rustedfabric_vulkan");
        } catch (LinkageError | SecurityException unavailable) {
            failure = unavailable;
        }
        LOAD_FAILURE = failure;
    }

    private NativeVulkanBridge() {
    }

    public static String start() {
        requireAvailable();
        return nativeStart();
    }

    public static boolean presentClear(float red, float green, float blue, float alpha) {
        requireAvailable();
        return nativePresentClear(red, green, blue, alpha);
    }

    public static String lastDiagnostic() {
        return LOAD_FAILURE == null ? nativeLastDiagnostic()
                : "Vulkan native library unavailable: " + LOAD_FAILURE;
    }

    public static void stop() {
        if (LOAD_FAILURE == null) nativeStop();
    }

    private static void requireAvailable() {
        if (LOAD_FAILURE != null) throw new IllegalStateException(
                "Android Vulkan backend is not packaged", LOAD_FAILURE);
    }

    private static native String nativeStart();
    private static native boolean nativePresentClear(float red, float green,
                                                      float blue, float alpha);
    private static native String nativeLastDiagnostic();
    private static native void nativeStop();
}
