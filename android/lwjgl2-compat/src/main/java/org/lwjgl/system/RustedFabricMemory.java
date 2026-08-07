package org.lwjgl.system;

import java.nio.Buffer;

/** Android HotSpot bridge for obtaining the address of any NIO direct-buffer view. */
public final class RustedFabricMemory {
    static {
        System.loadLibrary("pojavexec");
    }

    private RustedFabricMemory() {
    }

    public static long address(Buffer buffer) {
        if (buffer == null) {
            throw new NullPointerException("buffer");
        }
        if (!buffer.isDirect()) {
            throw new IllegalArgumentException("LWJGL requires a direct NIO buffer");
        }

        long address = getDirectBufferAddress(buffer);
        if (address == 0L) {
            throw new IllegalStateException(
                    "Android HotSpot could not resolve a direct NIO buffer address");
        }
        return address;
    }

    private static native long getDirectBufferAddress(Buffer buffer);
}
