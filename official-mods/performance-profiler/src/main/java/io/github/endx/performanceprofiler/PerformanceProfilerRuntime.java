package io.github.endx.performanceprofiler;

import io.github.endx.rustedfabricapi.api.client.input.ModKeyBinding;
import io.github.endx.rustedfabricapi.api.client.input.MouseButton;
import io.github.endx.rustedfabricapi.api.client.input.PointerInput;
import io.github.endx.rustedfabricapi.api.client.render.DrawStyle;
import io.github.endx.rustedfabricapi.api.client.render.HudDrawContext;

import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

final class PerformanceProfilerRuntime {
    private static final DrawStyle TITLE = DrawStyle.text(0xff7ee787, 14.0f);
    private static final DrawStyle TEXT = DrawStyle.text(0xfff0f3f6, 13.0f);
    private static final DrawStyle WARN = DrawStyle.text(0xffffc857, 13.0f);
    private static final int BACKGROUND = 0xcc10151c;
    private static final int BUTTON_BACKGROUND = 0xe0061a0b;
    private static final int BUTTON_HOVER = 0xe0143420;
    private static final int BORDER = 0xff44ff66;

    private static final float LEFT = 18.0f;
    private static final float TOP = 72.0f;
    private static final float TOGGLE_WIDTH = 166.0f;
    private static final float BUTTON_HEIGHT = 30.0f;
    private static final long PANEL_REFRESH_NANOS = 250_000_000L;

    private final FrameStatistics statistics = new FrameStatistics();
    private Path profileDirectory;
    private ModKeyBinding hudBinding;
    private ModKeyBinding samplingBinding;
    private boolean installed;
    private boolean hudVisible;
    private boolean hudPressed;
    private boolean samplingPressed;
    private volatile float pointerX = -1.0f;
    private volatile float pointerY = -1.0f;
    private volatile boolean pointerClicked;
    private long panelRefreshedAt;
    private FrameStatistics.Snapshot panelSnapshot;
    private long panelHeapUsed;
    private long panelHeapCommitted;
    private long panelHeapMax;
    private long panelGcCount;
    private long panelGcTime;
    private Thread gameThread;
    private SamplingSession sampling;
    private volatile String status = "F9 starts a sampling report";

    void install(Path gameDirectory, ModKeyBinding hud, ModKeyBinding samplingBinding) {
        if (installed) return;
        this.profileDirectory = gameDirectory.resolve(".rusted-fabric").resolve("profiles");
        this.hudBinding = hud;
        this.samplingBinding = samplingBinding;
        this.installed = true;
    }

    void frameStarted() {
        long now = System.nanoTime();
        gameThread = Thread.currentThread();
        pollBindings();
        statistics.frameStarted(now);
    }

    void frameFinished() {
        statistics.frameFinished(System.nanoTime());
    }

    void pointerMoved(PointerInput input) {
        pointerX = input.screenX();
        pointerY = input.screenY();
    }

    void pointerReleased(PointerInput input) {
        pointerMoved(input);
        if (input.button() == MouseButton.LEFT) pointerClicked = true;
    }

    void drawHud(HudDrawContext context) {
        if (!installed) return;
        boolean click = pointerClicked;
        pointerClicked = false;

        if (button(context, LEFT, TOP, TOGGLE_WIDTH, BUTTON_HEIGHT,
                hudVisible ? "Performance: ON" : "Performance", click)) {
            hudVisible = !hudVisible;
            return;
        }
        if (!hudVisible) return;

        refreshPanelSnapshot();
        FrameStatistics.Snapshot snapshot = panelSnapshot;
        String renderer = System.getProperty("rusted.fabric.renderer.resolved", "unknown");
        String samplingText = sampling == null ? "OFF"
                : "ON " + format(sampling.elapsedSeconds()) + "s / "
                        + sampling.sampleCount() + " samples";

        float x = LEFT;
        float y = TOP + BUTTON_HEIGHT + 8.0f;
        float width = Math.min(470.0f, Math.max(340.0f, context.width() - LEFT * 2.0f));
        float height = 198.0f;
        context.fillRect(x, y, width, height, BACKGROUND);
        context.strokeRect(x, y, width, height, BORDER, 2.0f);
        float line = y + 18.0f;
        context.drawText("RFL Performance Profiler", x + 10, line, TITLE);
        line += 18.0f;
        context.drawText("Renderer " + renderer + " | sampler " + samplingText,
                x + 10, line, sampling == null ? TEXT : WARN);
        line += 17.0f;
        context.drawText("FPS avg " + format(snapshot.frames.averageFps())
                + " | 1% low " + format(snapshot.frames.onePercentLowFps())
                + " | samples " + snapshot.frames.samples, x + 10, line, TEXT);
        line += 17.0f;
        context.drawText("Frame ms avg " + format(snapshot.frames.average)
                + " | p95 " + format(snapshot.frames.p95)
                + " | p99 " + format(snapshot.frames.p99), x + 10, line, TEXT);
        line += 17.0f;
        context.drawText("Game loop ms avg " + format(snapshot.gameLoop.average)
                + " | p95 " + format(snapshot.gameLoop.p95)
                + " | p99 " + format(snapshot.gameLoop.p99), x + 10, line, TEXT);
        line += 17.0f;
        context.drawText("Heap " + mib(panelHeapUsed) + "/" + mib(panelHeapCommitted) + " MiB"
                + " (max " + mib(panelHeapMax) + ") | GC " + panelGcCount + " / " + panelGcTime + "ms",
                x + 10, line, TEXT);
        line += 17.0f;
        context.drawText(status, x + 10, line, WARN);

        float actionsTop = y + height - 39.0f;
        float gap = 8.0f;
        float actionWidth = (width - 20.0f - gap * 2.0f) / 3.0f;
        if (button(context, x + 10.0f, actionsTop, actionWidth, 28.0f,
                sampling == null ? "Start sample [F9]" : "Stop + save [F9]", click)) {
            if (sampling == null) startSampling();
            else stopSampling();
        } else if (button(context, x + 10.0f + actionWidth + gap, actionsTop,
                actionWidth, 28.0f, "Reset window", click)) {
            statistics.reset();
            panelRefreshedAt = 0L;
            status = "Rolling statistics reset";
        } else if (button(context, x + 10.0f + (actionWidth + gap) * 2.0f,
                actionsTop, actionWidth, 28.0f, "Report path", click)) {
            status = "Report folder printed to the game log";
            System.out.println("[Performance Profiler] Reports: "
                    + profileDirectory.toAbsolutePath());
        }
    }

    private void refreshPanelSnapshot() {
        long now = System.nanoTime();
        if (panelSnapshot != null && now - panelRefreshedAt < PANEL_REFRESH_NANOS) return;
        panelSnapshot = statistics.snapshot();
        Runtime runtime = Runtime.getRuntime();
        panelHeapUsed = runtime.totalMemory() - runtime.freeMemory();
        panelHeapCommitted = runtime.totalMemory();
        panelHeapMax = runtime.maxMemory();
        long[] gc = gcTotals();
        panelGcCount = gc[0];
        panelGcTime = gc[1];
        panelRefreshedAt = now;
    }

    private boolean button(HudDrawContext context, float x, float y, float width,
            float height, String label, boolean click) {
        boolean hover = pointerX >= x && pointerX <= x + width
                && pointerY >= y && pointerY <= y + height;
        context.fillRect(x, y, width, height, hover ? BUTTON_HOVER : BUTTON_BACKGROUND);
        context.strokeRect(x, y, width, height, BORDER, 2.0f);
        context.drawText(label, x + 8.0f, y + 19.0f, hover ? TITLE : TEXT);
        return click && hover;
    }

    synchronized void close() {
        if (sampling != null) stopSampling();
    }

    private void pollBindings() {
        if (!installed || hudBinding == null || samplingBinding == null) return;
        boolean nextHudPressed = hudBinding.isPressed();
        if (nextHudPressed && !hudPressed) hudVisible = !hudVisible;
        hudPressed = nextHudPressed;

        boolean nextSamplingPressed = samplingBinding.isPressed();
        if (nextSamplingPressed && !samplingPressed) {
            if (sampling == null) startSampling();
            else stopSampling();
        }
        samplingPressed = nextSamplingPressed;
    }

    private synchronized void startSampling() {
        if (sampling != null || gameThread == null) return;
        sampling = new SamplingSession(gameThread);
        status = "Sampling active; press F9 to save the report";
        hudVisible = true;
        System.out.println("[Performance Profiler] Sampling started on " + gameThread.getName());
    }

    private synchronized void stopSampling() {
        SamplingSession finished = sampling;
        if (finished == null) return;
        sampling = null;
        FrameStatistics.Snapshot snapshot = statistics.snapshot();
        try {
            Path report = finished.stopAndWrite(profileDirectory, snapshot);
            status = "Saved " + report.getFileName();
            System.out.println("[Performance Profiler] Report saved: "
                    + report.toAbsolutePath());
        } catch (IOException failure) {
            status = "Could not save report: " + failure.getMessage();
            System.err.println("[Performance Profiler] Could not save report: " + failure);
        }
    }

    private static long[] gcTotals() {
        long count = 0L;
        long time = 0L;
        List<GarbageCollectorMXBean> collectors = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean collector : collectors) {
            if (collector.getCollectionCount() > 0) count += collector.getCollectionCount();
            if (collector.getCollectionTime() > 0) time += collector.getCollectionTime();
        }
        return new long[] { count, time };
    }

    private static long mib(long bytes) { return bytes / (1024L * 1024L); }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
