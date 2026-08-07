package io.github.endx.rustedfabric.android.xposed.ui;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.endx.rustedfabric.android.xposed.R;
import io.github.endx.rustedfabric.android.xposed.jvm.NativeRenderBridge;

/** Verifies an Android Surface and EGL context in the future desktop-JVM process. */
public final class JvmRenderActivity extends Activity implements SurfaceHolder.Callback {
    private static final String STATUS_FILE = "egl-smoke-status.txt";
    private final AtomicBoolean running = new AtomicBoolean();
    private TextView status;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        FrameLayout root = new FrameLayout(this);
        SurfaceView surface = new SurfaceView(this);
        surface.getHolder().addCallback(this);
        root.addView(surface, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        status = new TextView(this);
        status.setText(R.string.jvm_renderer_waiting);
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
                detail = NativeRenderBridge.smokeTest(width, height);
            } catch (Throwable failure) {
                detail = "Android EGL bridge failed: " + safeMessage(failure);
            }
            writeStatus(this, detail);
            String finalDetail = detail;
            runOnUiThread(() -> status.setText(getString(
                    finalDetail.startsWith("rusted-fabric-egl-smoke=ok")
                            ? R.string.jvm_renderer_succeeded : R.string.jvm_renderer_failed,
                    finalDetail)));
        }, "rusted-fabric-egl-smoke");
        renderer.start();
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
