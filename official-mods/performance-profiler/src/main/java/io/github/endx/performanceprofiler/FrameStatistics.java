package io.github.endx.performanceprofiler;

import java.util.Arrays;

/** Allocation-free rolling windows; snapshots are only allocated while the HUD is visible. */
final class FrameStatistics {
    private static final int WINDOW = 600;
    private final double[] frameIntervals = new double[WINDOW];
    private final double[] gameLoopTimes = new double[WINDOW];
    private int frameIndex;
    private int frameCount;
    private int loopIndex;
    private int loopCount;
    private long lastFrameStarted;
    private long activeFrameStarted;

    void frameStarted(long now) {
        if (lastFrameStarted != 0L) {
            frameIntervals[frameIndex] = nanosToMillis(now - lastFrameStarted);
            frameIndex = (frameIndex + 1) % WINDOW;
            if (frameCount < WINDOW) frameCount++;
        }
        lastFrameStarted = now;
        activeFrameStarted = now;
    }

    void frameFinished(long now) {
        if (activeFrameStarted == 0L) return;
        gameLoopTimes[loopIndex] = nanosToMillis(now - activeFrameStarted);
        loopIndex = (loopIndex + 1) % WINDOW;
        if (loopCount < WINDOW) loopCount++;
        activeFrameStarted = 0L;
    }

    void reset() {
        frameIndex = 0;
        frameCount = 0;
        loopIndex = 0;
        loopCount = 0;
        lastFrameStarted = 0L;
        activeFrameStarted = 0L;
    }

    Snapshot snapshot() {
        return new Snapshot(summarize(frameIntervals, frameCount),
                summarize(gameLoopTimes, loopCount));
    }

    private static Distribution summarize(double[] values, int count) {
        if (count == 0) return Distribution.EMPTY;
        double[] sorted = Arrays.copyOf(values, count);
        Arrays.sort(sorted);
        double total = 0.0;
        for (double value : sorted) total += value;
        int worstCount = Math.max(1, (int) Math.ceil(count * 0.01));
        double worstTotal = 0.0;
        for (int index = count - worstCount; index < count; index++) {
            worstTotal += sorted[index];
        }
        return new Distribution(total / count,
                percentile(sorted, 0.50), percentile(sorted, 0.95),
                percentile(sorted, 0.99), worstTotal / worstCount, count);
    }

    private static double percentile(double[] sorted, double percentile) {
        int index = (int) Math.ceil(percentile * sorted.length) - 1;
        return sorted[Math.max(0, Math.min(sorted.length - 1, index))];
    }

    private static double nanosToMillis(long nanos) {
        return nanos / 1_000_000.0;
    }

    static final class Snapshot {
        final Distribution frames;
        final Distribution gameLoop;

        Snapshot(Distribution frames, Distribution gameLoop) {
            this.frames = frames;
            this.gameLoop = gameLoop;
        }
    }

    static final class Distribution {
        static final Distribution EMPTY = new Distribution(0, 0, 0, 0, 0, 0);
        final double average;
        final double p50;
        final double p95;
        final double p99;
        final double worstOnePercentAverage;
        final int samples;

        Distribution(double average, double p50, double p95, double p99,
                     double worstOnePercentAverage, int samples) {
            this.average = average;
            this.p50 = p50;
            this.p95 = p95;
            this.p99 = p99;
            this.worstOnePercentAverage = worstOnePercentAverage;
            this.samples = samples;
        }

        double averageFps() { return average > 0.0 ? 1000.0 / average : 0.0; }
        double onePercentLowFps() {
            return worstOnePercentAverage > 0.0 ? 1000.0 / worstOnePercentAverage : 0.0;
        }
    }
}
