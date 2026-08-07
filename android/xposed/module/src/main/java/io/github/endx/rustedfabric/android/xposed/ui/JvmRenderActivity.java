package io.github.endx.rustedfabric.android.xposed.ui;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.endx.rustedfabric.android.xposed.R;
import io.github.endx.rustedfabric.android.jvm.JvmLaunchPlan;
import io.github.endx.rustedfabric.android.jvm.JvmLaunchPlanFactory;
import io.github.endx.rustedfabric.android.xposed.jvm.NativeJvmHost;
import io.github.endx.rustedfabric.android.xposed.jvm.NativeRenderBridge;
import io.github.endx.rustedfabric.android.xposed.jvm.DesktopGameImportService;

/** Runs a real LWJGL2 call path from HotSpot into GL4ES and the Android Surface. */
public final class JvmRenderActivity extends Activity implements SurfaceHolder.Callback {
    public static final String EXTRA_GAME_PROBE = "rusted-fabric.game-probe";
    private static final String STATUS_FILE = "lwjgl2-smoke-status.txt";
    private static final String PAYLOAD_ASSET = "rusted-fabric/jvm-host-smoke.jar";
    private static final String LWJGL_ASSET = "rusted-fabric/lwjgl-glfw-classes.jar";
    private static final String LAUNCHER_ASSET = "rusted-fabric/android-jvm-launcher.jar";
    private static final String LWJGL_COMPAT_ASSET =
            "rusted-fabric/rusted-fabric-lwjgl2-compat.jar";
    private final AtomicBoolean running = new AtomicBoolean();
    private TextView status;
    private boolean gameProbe;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        gameProbe = getIntent().getBooleanExtra(EXTRA_GAME_PROBE, false);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        FrameLayout root = new FrameLayout(this);
        SurfaceView surface = new SurfaceView(this);
        surface.getHolder().addCallback(this);
        surface.setOnTouchListener((view, event) -> handleTouch(event));
        surface.setOnGenericMotionListener((view, event) -> handleGenericMotion(event));
        root.addView(surface, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        status = new TextView(this);
        status.setText(gameProbe ? R.string.jvm_game_probe_waiting : R.string.jvm_renderer_waiting);
        status.setTextColor(Color.WHITE);
        status.setTextSize(14);
        status.setPadding(dp(16), dp(12), dp(16), dp(12));
        status.setBackgroundColor(0xCC000000);
        status.setOnClickListener(ignored -> finish());
        FrameLayout.LayoutParams overlay = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP);
        root.addView(status, overlay);
        setContentView(root);
    }

    private boolean handleTouch(MotionEvent event) {
        int buttonAction = -1;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                buttonAction = 1;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                buttonAction = 0;
                break;
            case MotionEvent.ACTION_MOVE:
                break;
            default:
                return true;
        }
        NativeRenderBridge.sendPointer(event.getX(), event.getY(), buttonAction);
        return true;
    }

    private boolean handleGenericMotion(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_HOVER_MOVE) {
            return NativeRenderBridge.sendPointer(event.getX(), event.getY(), -1);
        }
        if (event.getActionMasked() == MotionEvent.ACTION_SCROLL) {
            NativeRenderBridge.sendPointer(event.getX(), event.getY(), -1);
            return NativeRenderBridge.sendScroll(
                    event.getAxisValue(MotionEvent.AXIS_HSCROLL),
                    event.getAxisValue(MotionEvent.AXIS_VSCROLL));
        }
        return false;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // The sized callback performs the test.
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        try {
            NativeRenderBridge.attachSurface(holder.getSurface());
        } catch (RuntimeException failure) {
            status.setText(getString(R.string.jvm_renderer_failed, safeMessage(failure)));
            return;
        }
        if (!running.compareAndSet(false, true)) return;
        Thread renderer = new Thread(() -> {
            String detail;
            try {
                detail = gameProbe ? runGameProbe(width, height) : runLwjglTest();
            } catch (Throwable failure) {
                detail = (gameProbe ? "Android Fabric game launch failed: "
                        : "Android LWJGL2 bridge failed: ") + safeMessage(failure);
            }
            writeStatus(this, detail);
            String finalDetail = detail;
            runOnUiThread(() -> {
                status.setVisibility(android.view.View.VISIBLE);
                boolean succeeded = finalDetail.startsWith("rusted-fabric-lwjgl2-smoke=ok")
                        || finalDetail.startsWith("rusted-fabric-game-probe=ok");
                status.setText(getString(succeeded
                                ? (gameProbe ? R.string.jvm_game_probe_finished
                                : R.string.jvm_renderer_succeeded)
                                : (gameProbe ? R.string.jvm_game_probe_failed
                                : R.string.jvm_renderer_failed),
                        finalDetail));
            });
        }, "rusted-fabric-egl-smoke");
        renderer.start();
    }

    private String runGameProbe(int width, int height) throws IOException {
        File desktopRoot = DesktopGameImportService.importedRoot(this);
        File launcherDirectory = new File(new File(getFilesDir(), "desktop-jvm"), "launcher");
        if (!launcherDirectory.isDirectory() && !launcherDirectory.mkdirs()) {
            throw new IOException("Cannot create private Fabric launcher directory");
        }
        installAsset(LAUNCHER_ASSET, new File(launcherDirectory, "android-jvm-launcher.jar"));
        installAsset(LWJGL_ASSET, new File(launcherDirectory, "lwjgl-glfw-classes.jar"));
        installAsset(LWJGL_COMPAT_ASSET,
                new File(launcherDirectory, "rusted-fabric-lwjgl2-compat.jar"));
        File runtimeHome = new File(new File(getFilesDir(), "desktop-jvm"), "runtime");
        File nativeDirectory = new File(getApplicationInfo().nativeLibraryDir);
        io.github.endx.rustedfabric.android.jvm.JvmBackendCapabilities capabilities =
                io.github.endx.rustedfabric.android.jvm.JvmRuntimeProbe.inspect(
                        runtimeHome.toPath(), nativeDirectory.toPath(), NativeJvmHost.isPackaged());
        JvmLaunchPlan plan = JvmLaunchPlanFactory.createCompatibilityProbe(
                desktopRoot.toPath(), runtimeHome.toPath(), launcherDirectory.toPath(),
                nativeDirectory.toPath(), capabilities, 1024,
                Math.max(width, 320), Math.max(height, 240));
        runOnUiThread(() -> {
            status.setText(R.string.jvm_game_probe_starting);
            status.postDelayed(() -> {
                if (running.get()) status.setVisibility(android.view.View.GONE);
            }, 5000);
        });
        NativeJvmHost.Result launch = NativeJvmHost.launch(plan);
        if (!launch.succeeded()) {
            throw new IOException("Native JVM host code " + launch.code() + ": "
                    + launch.detail() + ". See logcat tag RustedFabricJvm for the Java stack.");
        }
        return "rusted-fabric-game-probe=ok\nFabric/game main returned normally";
    }

    private String runLwjglTest() throws IOException {
        File work = new File(new File(getFilesDir(), "desktop-jvm"), "lwjgl2-smoke");
        if (!work.isDirectory() && !work.mkdirs()) {
            throw new IOException("Cannot create private LWJGL2 smoke-test directory");
        }
        File payload = installAsset(PAYLOAD_ASSET, new File(work, "jvm-host-smoke.jar"));
        File adapter = installAsset(LWJGL_ASSET, new File(work, "lwjgl-glfw-classes.jar"));
        File resultFile = new File(work, "result.txt");
        Files.deleteIfExists(resultFile.toPath());
        File runtimeHome = new File(new File(getFilesDir(), "desktop-jvm"), "runtime");
        File nativeDirectory = new File(getApplicationInfo().nativeLibraryDir);
        JvmLaunchPlan plan = JvmLaunchPlanFactory.createLwjglSmokeTest(
                runtimeHome.toPath(), payload.toPath(), adapter.toPath(), work.toPath(),
                nativeDirectory.toPath(), resultFile.toPath());
        NativeJvmHost.Result launch = NativeJvmHost.launch(plan);
        if (!launch.succeeded()) {
            String jvmDetail = resultFile.isFile() ? new String(
                    Files.readAllBytes(resultFile.toPath()), StandardCharsets.UTF_8).trim() : "";
            throw new IOException("Native JVM host code " + launch.code() + ": "
                    + launch.detail() + (jvmDetail.isEmpty() ? "" : "\n" + jvmDetail));
        }
        if (!resultFile.isFile()) {
            throw new IOException("HotSpot LWJGL2 smoke test did not create its result");
        }
        String detail = new String(
                Files.readAllBytes(resultFile.toPath()), StandardCharsets.UTF_8).trim();
        if (!detail.startsWith("rusted-fabric-lwjgl2-smoke=ok")) {
            throw new IOException("HotSpot LWJGL2 smoke test returned an invalid result");
        }
        return detail;
    }

    private File installAsset(String assetPath, File target) throws IOException {
        File staging = new File(target.getParentFile(), target.getName() + ".importing");
        Files.deleteIfExists(staging.toPath());
        try (InputStream input = getAssets().open(assetPath);
             FileOutputStream output = new FileOutputStream(staging)) {
            byte[] buffer = new byte[32 * 1024];
            int read;
            while ((read = input.read(buffer)) >= 0) output.write(buffer, 0, read);
        }
        Files.deleteIfExists(target.toPath());
        if (!staging.renameTo(target)) throw new IOException("Cannot install " + assetPath);
        return target;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        NativeRenderBridge.detachSurface();
    }

    @Override
    protected void onDestroy() {
        NativeRenderBridge.detachSurface();
        super.onDestroy();
    }

    public static String lastStatus(android.content.Context context) {
        File file = statusFile(context);
        if (!file.isFile()) return "";
        try {
            return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8).trim();
        } catch (IOException unreadable) {
            return "renderer status unreadable: " + safeMessage(unreadable);
        }
    }

    private static void writeStatus(android.content.Context context, String value) {
        File file = statusFile(context);
        File parent = file.getParentFile();
        try {
            if (!parent.isDirectory() && !parent.mkdirs()) return;
            Files.write(file.toPath(), (value + "\n").getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) {
            // The on-screen result remains available even if persistence fails.
        }
    }

    private static File statusFile(android.content.Context context) {
        return new File(new File(context.getFilesDir(), "desktop-jvm"), STATUS_FILE);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static String safeMessage(Throwable failure) {
        String message = failure.getMessage();
        return message == null || message.trim().isEmpty()
                ? failure.getClass().getSimpleName() : message;
    }
}
