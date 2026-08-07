package net.java.openjdk.cacio.ctc;

/**
 * Minimal ABI used by PojavLauncher-derived LWJGL2 adapters.
 *
 * <p>The desktop adapter only needs an object from which a Cacio mouse-grab bridge can read the
 * current absolute cursor position. Android input remains owned by the launcher and GLFW layer.</p>
 */
public interface ExternalMouseReader {
    int getX();

    int getY();
}
