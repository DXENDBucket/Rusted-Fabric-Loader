package net.java.openjdk.cacio.ctc;

/**
 * Compatibility endpoint for the optional Cacio cursor-grab integration.
 *
 * <p>A full Cacio AWT toolkit is not present in the Android JVM backend. Keeping the state here
 * satisfies the adapter ABI without trying to take ownership of Android pointer events.</p>
 */
public final class InfdevGrabHandler {
    private static volatile ExternalMouseReader mouseReader;
    private static volatile boolean grabbed;

    private InfdevGrabHandler() {
    }

    public static void setMouseReader(ExternalMouseReader reader) {
        mouseReader = reader;
    }

    public static void setGrabbed(boolean value) {
        grabbed = value;
    }

    public static ExternalMouseReader getMouseReader() {
        return mouseReader;
    }

    public static boolean isGrabbed() {
        return grabbed;
    }
}
