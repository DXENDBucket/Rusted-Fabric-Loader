package io.github.endx.vulkanmod;

/** Converts a high-resolution presentation clock into Slick-compatible integer milliseconds. */
final class NativeFrameClock {
    private static final long NANOS_PER_MILLI = 1_000_000L;
    private static final int MAX_DELTA_MILLIS = 250;
    private static final long MAX_DELTA_NANOS = MAX_DELTA_MILLIS * NANOS_PER_MILLI;

    private boolean initialized;
    private long previousNanos;
    private long remainderNanos;

    int nextDeltaMillis(long nowNanos) {
        if (!initialized) {
            initialized = true;
            previousNanos = nowNanos;
            remainderNanos = 0L;
            return 16;
        }

        long elapsed = nowNanos - previousNanos;
        previousNanos = nowNanos;
        if (elapsed < 0L) {
            remainderNanos = 0L;
            return 0;
        }
        if (elapsed >= MAX_DELTA_NANOS) {
            remainderNanos = 0L;
            return MAX_DELTA_MILLIS;
        }

        long accumulated = elapsed + remainderNanos;
        if (accumulated >= MAX_DELTA_NANOS) {
            remainderNanos = 0L;
            return MAX_DELTA_MILLIS;
        }
        int millis = (int) (accumulated / NANOS_PER_MILLI);
        remainderNanos = accumulated % NANOS_PER_MILLI;
        return millis;
    }

    void reset(long nowNanos) {
        initialized = true;
        previousNanos = nowNanos;
        remainderNanos = 0L;
    }

    void clear() {
        initialized = false;
        previousNanos = 0L;
        remainderNanos = 0L;
    }
}
