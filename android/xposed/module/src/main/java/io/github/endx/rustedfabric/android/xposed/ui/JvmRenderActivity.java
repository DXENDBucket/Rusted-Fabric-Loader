package io.github.endx.rustedfabric.android.xposed.ui;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.DisplayCutout;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.window.OnBackInvokedDispatcher;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.endx.rustedfabric.android.xposed.R;
import io.github.endx.rustedfabric.android.jvm.JvmLaunchPlan;
import io.github.endx.rustedfabric.android.jvm.JvmLaunchPlanFactory;
import io.github.endx.rustedfabric.android.jvm.DesktopGameLayout;
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
    private static final int GLFW_MOUSE_BUTTON_RIGHT = 1;
    private static final int GLFW_KEY_ESCAPE = 256;
    private static final int GLFW_RELEASE = 0;
    private static final int GLFW_PRESS = 1;
    private static final long TAP_RELEASE_DELAY_MILLIS = 120L;
    private static final long TWO_FINGER_TAP_MILLIS = 500L;
    private static final float TOUCH_SCROLL_PIXELS_PER_STEP = 32.0f;
    private final AtomicBoolean running = new AtomicBoolean();
    private final Handler inputHandler = new Handler(Looper.getMainLooper());
    private final Runnable releaseLeftTap = () -> {
        NativeRenderBridge.sendMouseButton(0, GLFW_RELEASE);
        leftTapReleasePending = false;
    };
    private final Runnable releaseRightTap = () -> {
        NativeRenderBridge.sendMouseButton(GLFW_MOUSE_BUTTON_RIGHT, GLFW_RELEASE);
        rightTapReleasePending = false;
    };
    private TextView status;
    private boolean gameProbe;
    private int touchSlop;
    private int primaryPointerId = -1;
    private boolean leftButtonDown;
    private boolean leftTapReleasePending;
    private boolean rightTapReleasePending;
    private boolean singleDragging;
    private boolean singleScrolling;
    private boolean multiTouch;
    private boolean multiGesture;
    private boolean gameTouchInput;
    private long multiTouchStarted;
    private float multiStartX;
    private float multiStartY;
    private float touchDownX;
    private float touchDownY;
    private float previousTouchY;
    private float previousSpan;
    private double pendingScroll;
    private final float[] gameTouchXs = new float[10];
    private final float[] gameTouchYs = new float[10];
    private final int[] gameTouchIds = new int[10];

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        gameProbe = getIntent().getBooleanExtra(EXTRA_GAME_PROBE, false);
        touchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
        configureGameWindow();
        if (Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::sendEscape);
        }
        FrameLayout root = new FrameLayout(this);
        SurfaceView surface = new SurfaceView(this);
        surface.getHolder().addCallback(this);
        surface.setOnTouchListener((view, event) -> handleTouch(event));
        surface.setOnGenericMotionListener((view, event) -> handleGenericMotion(event));
        root.addView(surface, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        if (Build.VERSION.SDK_INT >= 28) {
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                DisplayCutout cutout = insets.getDisplayCutout();
                int left = cutout == null ? 0 : cutout.getSafeInsetLeft();
                int top = cutout == null ? 0 : cutout.getSafeInsetTop();
                int right = cutout == null ? 0 : cutout.getSafeInsetRight();
                int bottom = cutout == null ? 0 : cutout.getSafeInsetBottom();
                FrameLayout.LayoutParams params =
                        (FrameLayout.LayoutParams) surface.getLayoutParams();
                if (params.leftMargin != left || params.topMargin != top
                        || params.rightMargin != right || params.bottomMargin != bottom) {
                    params.setMargins(left, top, right, bottom);
                    surface.setLayoutParams(params);
                }
                return insets;
            });
            root.requestApplyInsets();
        }

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
        applyImmersiveMode();
    }

    private boolean handleTouch(MotionEvent event) {
        if (gameTouchInput && event.getActionMasked() != MotionEvent.ACTION_DOWN) {
            publishGameTouchFrame(event);
            if (event.getActionMasked() == MotionEvent.ACTION_UP
                    || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                gameTouchInput = false;
            }
            return true;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                releasePendingTapButtons();
                resetTouchState();
                gameTouchInput = !NativeRenderBridge.uiIsActive();
                if (gameTouchInput) {
                    publishGameTouchFrame(event);
                    return true;
                }
                primaryPointerId = event.getPointerId(0);
                touchDownX = event.getX(0);
                touchDownY = event.getY(0);
                previousTouchY = touchDownY;
                NativeRenderBridge.sendPointer(touchDownX, touchDownY, -1);
                return true;
            }
            case MotionEvent.ACTION_POINTER_DOWN: {
                if (leftButtonDown) {
                    NativeRenderBridge.sendMouseButton(0, GLFW_RELEASE);
                    leftButtonDown = false;
                    multiGesture = true;
                }
                multiTouch = true;
                multiTouchStarted = event.getEventTime();
                multiStartX = pointerCenterX(event);
                multiStartY = pointerCenterY(event);
                previousSpan = pointerSpan(event);
                pendingScroll = 0.0;
                NativeRenderBridge.sendPointer(multiStartX, multiStartY, -1);
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                if (multiTouch || event.getPointerCount() > 1) {
                    float centerX = pointerCenterX(event);
                    float centerY = pointerCenterY(event);
                    NativeRenderBridge.sendPointer(centerX, centerY, -1);
                    float span = pointerSpan(event);
                    if (previousSpan > 0.0f && span > 0.0f) {
                        pendingScroll += Math.log(span / previousSpan) * 8.0;
                        if (Math.abs(pendingScroll) >= 0.15) {
                            NativeRenderBridge.sendScroll(0.0, pendingScroll);
                            pendingScroll = 0.0;
                            multiGesture = true;
                        }
                    }
                    previousSpan = span;
                    if (distance(centerX, centerY, multiStartX, multiStartY) > touchSlop) {
                        multiGesture = true;
                    }
                    return true;
                }
                int index = event.findPointerIndex(primaryPointerId);
                if (index < 0) return true;
                float x = event.getX(index);
                float y = event.getY(index);
                NativeRenderBridge.sendPointer(x, y, -1);
                if (!singleDragging && !singleScrolling
                        && distance(x, y, touchDownX, touchDownY) > touchSlop) {
                    float deltaX = Math.abs(x - touchDownX);
                    float deltaY = Math.abs(y - touchDownY);
                    if (!NativeRenderBridge.uiPrefersDrag()
                            && deltaY > deltaX && NativeRenderBridge.uiWantsScroll()) {
                        singleScrolling = true;
                    } else {
                        singleDragging = true;
                        NativeRenderBridge.sendPointer(touchDownX, touchDownY, -1);
                        NativeRenderBridge.sendMouseButton(0, GLFW_PRESS);
                        leftButtonDown = true;
                        NativeRenderBridge.sendPointer(x, y, -1);
                    }
                }
                if (singleScrolling) {
                    float deltaY = y - previousTouchY;
                    if (!NativeRenderBridge.scrollUiByTouchDelta(deltaY)) {
                        pendingScroll += deltaY / TOUCH_SCROLL_PIXELS_PER_STEP;
                        if (Math.abs(pendingScroll) >= 0.25) {
                            NativeRenderBridge.sendScroll(0.0, pendingScroll);
                            pendingScroll = 0.0;
                        }
                    }
                }
                previousTouchY = y;
                return true;
            }
            case MotionEvent.ACTION_POINTER_UP: {
                if (multiTouch && event.getPointerCount() == 2 && !multiGesture
                        && event.getEventTime() - multiTouchStarted <= TWO_FINGER_TAP_MILLIS) {
                    float x = pointerCenterX(event);
                    float y = pointerCenterY(event);
                    NativeRenderBridge.sendPointer(x, y, -1);
                    NativeRenderBridge.sendMouseButton(GLFW_MOUSE_BUTTON_RIGHT, GLFW_PRESS);
                    rightTapReleasePending = true;
                    inputHandler.postDelayed(releaseRightTap, TAP_RELEASE_DELAY_MILLIS);
                }
                multiGesture = true;
                return true;
            }
            case MotionEvent.ACTION_UP: {
                if (!multiTouch) {
                    float x = event.getX(0);
                    float y = event.getY(0);
                    NativeRenderBridge.sendPointer(x, y, -1);
                    if (leftButtonDown) {
                        NativeRenderBridge.sendMouseButton(0, GLFW_RELEASE);
                    } else if (!singleScrolling) {
                        NativeRenderBridge.sendMouseButton(0, GLFW_PRESS);
                        leftTapReleasePending = true;
                        inputHandler.postDelayed(releaseLeftTap, TAP_RELEASE_DELAY_MILLIS);
                    }
                }
                resetTouchState();
                return true;
            }
            case MotionEvent.ACTION_CANCEL:
                if (leftButtonDown) NativeRenderBridge.sendMouseButton(0, GLFW_RELEASE);
                resetTouchState();
                return true;
            default:
                return true;
        }
    }

    private void publishGameTouchFrame(MotionEvent event) {
        int action = event.getActionMasked();
        int count = Math.min(event.getPointerCount(), gameTouchXs.length);
        for (int index = 0; index < count; index++) {
            gameTouchXs[index] = event.getX(index);
            gameTouchYs[index] = event.getY(index);
            gameTouchIds[index] = event.getPointerId(index);
        }
        // Match the official Android MultiTouchController: an UP (including POINTER_UP)
        // ends the current gesture but retains every pointer and its final coordinates in
        // that MotionEvent. The game needs both final corners to commit a two-finger box.
        boolean down = action != MotionEvent.ACTION_UP
                && action != MotionEvent.ACTION_POINTER_UP
                && action != MotionEvent.ACTION_CANCEL;
        NativeRenderBridge.sendTouchFrame(
                gameTouchXs, gameTouchYs, gameTouchIds, count, down, action);
    }

    private void resetTouchState() {
        primaryPointerId = -1;
        leftButtonDown = false;
        singleDragging = false;
        singleScrolling = false;
        multiTouch = false;
        multiGesture = false;
        previousSpan = 0.0f;
        pendingScroll = 0.0;
    }

    private void releasePendingTapButtons() {
        if (leftTapReleasePending) {
            inputHandler.removeCallbacks(releaseLeftTap);
            releaseLeftTap.run();
        }
        if (rightTapReleasePending) {
            inputHandler.removeCallbacks(releaseRightTap);
            releaseRightTap.run();
        }
    }

    private static float pointerCenterX(MotionEvent event) {
        float total = 0.0f;
        for (int index = 0; index < event.getPointerCount(); index++) total += event.getX(index);
        return total / event.getPointerCount();
    }

    private static float pointerCenterY(MotionEvent event) {
        float total = 0.0f;
        for (int index = 0; index < event.getPointerCount(); index++) total += event.getY(index);
        return total / event.getPointerCount();
    }

    private static float pointerSpan(MotionEvent event) {
        if (event.getPointerCount() < 2) return 0.0f;
        return distance(event.getX(0), event.getY(0), event.getX(1), event.getY(1));
    }

    private static float distance(float firstX, float firstY, float secondX, float secondY) {
        return (float) Math.hypot(firstX - secondX, firstY - secondY);
    }

    private void configureGameWindow() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= 28) {
            WindowManager.LayoutParams attributes = getWindow().getAttributes();
            attributes.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(attributes);
        }
    }

    private void applyImmersiveMode() {
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    private void sendEscape() {
        NativeRenderBridge.sendKey(GLFW_KEY_ESCAPE, GLFW_PRESS);
        NativeRenderBridge.sendKey(GLFW_KEY_ESCAPE, GLFW_RELEASE);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            NativeRenderBridge.sendKey(GLFW_KEY_ESCAPE, GLFW_PRESS);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            NativeRenderBridge.sendKey(GLFW_KEY_ESCAPE, GLFW_RELEASE);
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        sendEscape();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) applyImmersiveMode();
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
        // Imports deliberately omit user saves and mods. Recreate the empty writable layout on
        // every launch as a repair path for older imports and folders removed by the user.
        DesktopGameLayout.prepareWritableDirectories(desktopRoot.toPath());
        prepareAndroidGameDefaults(desktopRoot.toPath());
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
                Math.max(width, 320), Math.max(height, 240), deviceLocale());
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

    private void prepareAndroidGameDefaults(Path desktopRoot) throws IOException {
        Path stateDirectory = desktopRoot.resolve(".rustedfabricloader");
        Path marker = stateDirectory.resolve("android-ui-defaults-v1");
        if (Files.isRegularFile(marker)) return;
        Files.createDirectories(stateDirectory);
        Path preferences = desktopRoot.resolve("preferences.ini");
        List<String> lines = Files.isRegularFile(preferences)
                ? new ArrayList<>(Files.readAllLines(preferences, StandardCharsets.UTF_8))
                : new ArrayList<>();
        boolean densityFound = false;
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            if (!line.startsWith("renderDensity:")) continue;
            densityFound = true;
            String value = line.substring("renderDensity:".length()).trim();
            if (value.isEmpty() || "1".equals(value) || "1.0".equals(value)) {
                lines.set(index, "renderDensity:2.5");
            }
            break;
        }
        if (!densityFound) lines.add("renderDensity:2.5");
        Files.write(preferences, lines, StandardCharsets.UTF_8);
        Files.write(marker, java.util.Collections.singletonList("renderDensity=2.5"),
                StandardCharsets.UTF_8);
    }

    @SuppressWarnings("deprecation")
    private Locale deviceLocale() {
        if (Build.VERSION.SDK_INT >= 24) {
            return getResources().getConfiguration().getLocales().get(0);
        }
        return getResources().getConfiguration().locale;
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
        releasePendingTapButtons();
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
