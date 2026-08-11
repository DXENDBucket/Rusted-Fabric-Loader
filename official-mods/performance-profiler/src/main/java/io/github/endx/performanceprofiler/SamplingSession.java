package io.github.endx.performanceprofiler;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** A small async wall-clock sampler; it never instruments or pauses the game thread. */
final class SamplingSession {
    private static final long SAMPLE_INTERVAL_MILLIS = 10L;
    private static final DateTimeFormatter FILE_TIME =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final Thread target;
    private final long startedNanos = System.nanoTime();
    private final Map<String, MethodSamples> methods =
            new LinkedHashMap<String, MethodSamples>();
    private final Map<String, Integer> foldedStacks =
            new LinkedHashMap<String, Integer>();
    private final Thread worker;
    private volatile boolean running = true;
    private volatile long sampleCount;

    SamplingSession(Thread target) {
        this.target = target;
        this.worker = new Thread(this::sampleLoop, "RFL performance sampler");
        this.worker.setDaemon(true);
        this.worker.setPriority(Thread.MIN_PRIORITY);
        this.worker.start();
    }

    long sampleCount() { return sampleCount; }

    double elapsedSeconds() {
        return (System.nanoTime() - startedNanos) / 1_000_000_000.0;
    }

    Path stopAndWrite(Path directory, FrameStatistics.Snapshot frames) throws IOException {
        running = false;
        worker.interrupt();
        try {
            worker.join(500L);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
        Files.createDirectories(directory);
        String base = "profile-" + LocalDateTime.now().format(FILE_TIME);
        Path report = directory.resolve(base + ".txt");
        Path collapsed = directory.resolve(base + ".collapsed");
        writeReport(report, frames);
        writeCollapsed(collapsed);
        return report;
    }

    private void sampleLoop() {
        while (running) {
            StackTraceElement[] stack = target.getStackTrace();
            if (stack.length > 0) record(stack);
            try {
                Thread.sleep(SAMPLE_INTERVAL_MILLIS);
            } catch (InterruptedException ignored) {
                // The running flag distinguishes stop from a spurious interrupt.
            }
        }
    }

    private void record(StackTraceElement[] stack) {
        sampleCount++;
        Set<String> inclusiveOnce = new HashSet<String>();
        String self = methodName(stack[0]);
        for (StackTraceElement frame : stack) {
            String name = methodName(frame);
            MethodSamples samples = methods.computeIfAbsent(name, ignored -> new MethodSamples());
            if (inclusiveOnce.add(name)) samples.inclusive++;
        }
        methods.get(self).self++;

        StringBuilder folded = new StringBuilder();
        for (int index = stack.length - 1; index >= 0; index--) {
            if (folded.length() > 0) folded.append(';');
            folded.append(methodName(stack[index]));
        }
        String key = folded.toString();
        foldedStacks.put(key, foldedStacks.getOrDefault(key, 0) + 1);
    }

    private void writeReport(Path report, FrameStatistics.Snapshot frames) throws IOException {
        List<Map.Entry<String, MethodSamples>> sorted =
                new ArrayList<Map.Entry<String, MethodSamples>>(methods.entrySet());
        Collections.sort(sorted, (first, second) -> {
            int bySelf = Long.compare(second.getValue().self, first.getValue().self);
            if (bySelf != 0) return bySelf;
            int byInclusive = Long.compare(
                    second.getValue().inclusive, first.getValue().inclusive);
            return byInclusive != 0 ? byInclusive : first.getKey().compareTo(second.getKey());
        });
        try (BufferedWriter writer = Files.newBufferedWriter(report, StandardCharsets.UTF_8)) {
            writer.write("Rusted Fabric Performance Profiler\n");
            writer.write("renderer=" + System.getProperty(
                    "rusted.fabric.renderer.resolved", "unknown") + "\n");
            writer.write("jvm=" + System.getProperty("java.vm.name") + " "
                    + System.getProperty("java.runtime.version") + "\n");
            writer.write("process=" + ManagementFactory.getRuntimeMXBean().getName() + "\n");
            writer.write("targetThread=" + target.getName() + "\n");
            writer.write("durationSeconds=" + format(elapsedSeconds()) + "\n");
            writer.write("samples=" + sampleCount + "\n");
            writer.write("frameAverageMs=" + format(frames.frames.average) + "\n");
            writer.write("frameP95Ms=" + format(frames.frames.p95) + "\n");
            writer.write("frameP99Ms=" + format(frames.frames.p99) + "\n");
            writer.write("onePercentLowFps=" + format(frames.frames.onePercentLowFps()) + "\n");
            writer.write("\nSelf%\tTotal%\tSelf\tTotal\tMethod\n");
            int limit = Math.min(100, sorted.size());
            for (int index = 0; index < limit; index++) {
                Map.Entry<String, MethodSamples> entry = sorted.get(index);
                MethodSamples value = entry.getValue();
                writer.write(format(percent(value.self)) + "\t"
                        + format(percent(value.inclusive)) + "\t"
                        + value.self + "\t" + value.inclusive + "\t"
                        + entry.getKey() + "\n");
            }
        }
    }

    private void writeCollapsed(Path output) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            for (Map.Entry<String, Integer> entry : foldedStacks.entrySet()) {
                writer.write(entry.getKey());
                writer.write(' ');
                writer.write(Integer.toString(entry.getValue()));
                writer.newLine();
            }
        }
    }

    private double percent(long count) {
        return sampleCount == 0 ? 0.0 : count * 100.0 / sampleCount;
    }

    private static String methodName(StackTraceElement frame) {
        return frame.getClassName() + "." + frame.getMethodName();
    }

    private static String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private static final class MethodSamples {
        private long self;
        private long inclusive;
    }
}
