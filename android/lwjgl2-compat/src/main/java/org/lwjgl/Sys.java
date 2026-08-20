package org.lwjgl;

import java.lang.reflect.Method;

import org.lwjgl.system.RustedFabricMemory;

/** Minimal LWJGL2 ABI shim layered ahead of the LWJGLX Android implementation. */
public final class Sys {
    private static final long TIMER_RESOLUTION = 1000L;

    private Sys() {
    }

    public static String getVersion() {
        return "2.9.3-rusted-fabric-android";
    }

    public static void initialize() {
        try {
            Class<?> glfw = Class.forName("org.lwjgl.glfw.GLFW");
            Method init = glfw.getMethod("glfwInit");
            Object result = init.invoke(null);
            if (Boolean.FALSE.equals(result)) {
                throw new IllegalStateException("Unable to initialize GLFW: "
                        + RustedFabricMemory.initializationDiagnostic());
            }
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("LWJGLX GLFW initialization is unavailable", failure);
        }
    }

    public static boolean is64Bit() {
        return true;
    }

    public static long getTimerResolution() {
        return TIMER_RESOLUTION;
    }

    public static long getTime() {
        return System.nanoTime() / 1_000_000L;
    }

    public static long getNanoTime() {
        return System.nanoTime();
    }

    public static void alert(String title, String message) {
        System.err.println(String.valueOf(title) + ": " + String.valueOf(message));
    }

    public static boolean openURL(String url) {
        return false;
    }

    public static String getClipboard() {
        return null;
    }
}
