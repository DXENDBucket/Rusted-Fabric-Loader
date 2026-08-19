package io.github.endx.rustedfabric.android.launcher.ui;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.DisplayCutout;
import android.view.Display;
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
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.endx.rustedfabric.android.launcher.R;
import io.github.endx.rustedfabric.android.jvm.JvmLaunchPlan;
import io.github.endx.rustedfabric.android.jvm.JvmLaunchPlanFactory;
import io.github.endx.rustedfabric.android.jvm.DesktopGameLayout;
import io.github.endx.rustedfabric.android.launcher.jvm.NativeJvmHost;
import io.github.endx.rustedfabric.android.launcher.jvm.NativeRenderBridge;
import io.github.endx.rustedfabric.android.launcher.jvm.NativeVulkanBridge;
import io.github.endx.rustedfabric.android.launcher.jvm.DesktopGameImportService;
import io.github.endx.rustedfabric.android.launcher.jvm.SharedContentWorkspace;

/** Runs the imported game or an isolated GL4ES/Vulkan renderer test on the Android Surface. */
public final class JvmRenderActivity extends Activity implements SurfaceHolder.Callback {
    public static final String EXTRA_GAME_PROBE = "rusted-fabric.game-probe";
    public static final String EXTRA_VULKAN_SMOKE = "rusted-fabric.vulkan-smoke";
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
    private static final long TWO_FINGER_TAP_MILLIS = 500L;
    private static final long SURFACE_SETTLE_DELAY_MILLIS = 400L;
    private static final float TOUCH_SCROLL_PIXELS_PER_STEP = 32.0f;
    // HotSpot permits only one embedded VM in this dedicated Android process. Keep the guard
    // process-wide so Activity recreation (rotation, task restoration, or Surface recovery) does
    // not attempt a second JNI_CreateJavaVM while the game's renderer/audio threads still live.
    private static final AtomicBoolean RUNNING = new AtomicBoolean();
    private final Handler surfaceHandler = new Handler(Looper.getMainLooper());
    private final Runnable startAfterSurfaceSettles = this::startRenderer;
    private Runnable firstFrameWatcher;
    private TextView status;
    private boolean gameProbe;
    private boolean vulkanSmoke;
    private int touchSlop;
    private int primaryPointerId = -1;
    private boolean leftButtonDown;
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
    private int settledSurfaceWidth;
    private int settledSurfaceHeight;
    private int requestedMaximumFps;
    private boolean safeSideBorders;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        gameProbe = getIntent().getBooleanExtra(EXTRA_GAME_PROBE, false);
        vulkanSmoke = getIntent().getBooleanExtra(EXTRA_VULKAN_SMOKE, false);
        android.content.SharedPreferences gamePreferences =
                GameLaunchPreferences.preferences(this);
        requestedMaximumFps = GameLaunchPreferences.maximumFps(gamePreferences);
        safeSideBorders = GameLaunchPreferences.safeSideBorders(gamePreferences);
        touchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
        configureGameWindow();
        if (Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::sendEscape);
        }
        FrameLayout root = new FrameLayout(this);
        SurfaceView surface = new SurfaceView(this);
        surface.getHolder().addCallback(this);
        requestGameFrameRate(surface);
        surface.setOnTouchListener((view, event) -> handleTouch(event));
        surface.setOnGenericMotionListener((view, event) -> handleGenericMotion(event));
        root.addView(surface, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        if (Build.VERSION.SDK_INT >= 28 && safeSideBorders) {
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                DisplayCutout cutout = insets.getDisplayCutout();
                int left = cutout == null ? 0 : cutout.getSafeInsetLeft();
                int right = cutout == null ? 0 : cutout.getSafeInsetRight();
                FrameLayout.LayoutParams params =
                        (FrameLayout.LayoutParams) surface.getLayoutParams();
                if (params.leftMargin != left || params.rightMargin != right) {
                    params.setMargins(left, 0, right, 0);
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
        // The game status bar is informational. Do not turn the same tap that opened this
        // Activity into an accidental game shutdown.
        if (!gameProbe) status.setOnClickListener(ignored -> finish());
        FrameLayout.LayoutParams overlay = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
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
                    NativeRenderBridge.sendMouseClick(GLFW_MOUSE_BUTTON_RIGHT, x, y);
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
                        NativeRenderBridge.sendMouseClick(0, x, y);
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
        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        float preferredRefreshRate = preferredDisplayRefreshRate(requestedMaximumFps);
        if (preferredRefreshRate > 0.0f) {
            attributes.preferredRefreshRate = preferredRefreshRate;
        }
        if (Build.VERSION.SDK_INT >= 28) {
            attributes.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        getWindow().setAttributes(attributes);
    }

    private void requestGameFrameRate(SurfaceView surface) {
        if (Build.VERSION.SDK_INT < 30) return;
        float rate = preferredDisplayRefreshRate(requestedMaximumFps);
        if (rate <= 0.0f) return;
        surface.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override public void surfaceCreated(SurfaceHolder holder) {
                holder.getSurface().setFrameRate(rate,
                        android.view.Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE);
            }

            @Override public void surfaceChanged(SurfaceHolder holder, int format,
                                                  int width, int height) {
                holder.getSurface().setFrameRate(rate,
                        android.view.Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE);
            }

            @Override public void surfaceDestroyed(SurfaceHolder holder) {
                // The Surface is already invalid here; the next one receives the request again.
            }
        });
    }

    @SuppressWarnings("deprecation")
    private float preferredDisplayRefreshRate(int maximumFps) {
        Display display = getWindowManager().getDefaultDisplay();
        Display.Mode[] modes = display.getSupportedModes();
        float best = 0.0f;
        float bestDistance = Float.MAX_VALUE;
        for (Display.Mode mode : modes) {
            float rate = mode.getRefreshRate();
            if (maximumFps <= 0) {
                if (rate > best) best = rate;
                continue;
            }
            float distance = Math.abs(rate - maximumFps);
            if (distance < bestDistance
                    || (Math.abs(distance - bestDistance) < 0.1f && rate > best)) {
                best = rate;
                bestDistance = distance;
            }
        }
        return best > 0.0f ? best : display.getRefreshRate();
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
        if (RUNNING.get()) return;
        // Full-screen and display-cutout insets can resize the Surface shortly after its first
        // callback. Launching the JVM from that transient size leaves libRocket laid out against
        // stale dimensions until the settings screen forces a reflow. Debounce callbacks and
        // launch only after the Android surface has settled.
        settledSurfaceWidth = width;
        settledSurfaceHeight = height;
        surfaceHandler.removeCallbacks(startAfterSurfaceSettles);
        surfaceHandler.postDelayed(startAfterSurfaceSettles, SURFACE_SETTLE_DELAY_MILLIS);
    }

    private void startRenderer() {
        if (!RUNNING.compareAndSet(false, true)) return;
        int width = settledSurfaceWidth;
        int height = settledSurfaceHeight;
        Thread renderer = new Thread(() -> {
            String detail;
            try {
                detail = gameProbe ? runGameProbe(width, height)
                        : vulkanSmoke ? runVulkanTest() : runLwjglTest();
            } catch (Throwable failure) {
                detail = (gameProbe ? "Android Fabric game launch failed: "
                        : vulkanSmoke ? "Android Vulkan test failed: "
                        : "Android LWJGL2 bridge failed: ") + safeMessage(failure);
            } finally {
                if (vulkanSmoke) RUNNING.set(false);
            }
            writeStatus(this, detail);
            String finalDetail = detail;
            runOnUiThread(() -> {
                status.setVisibility(android.view.View.VISIBLE);
                boolean succeeded = finalDetail.startsWith("rusted-fabric-lwjgl2-smoke=ok")
                        || finalDetail.startsWith("rusted-fabric-vulkan-smoke=ok")
                        || finalDetail.startsWith("rusted-fabric-game-probe=ok");
                status.setText(getString(succeeded
                                ? (gameProbe ? R.string.jvm_game_probe_finished
                                : R.string.jvm_renderer_succeeded)
                                : (gameProbe ? R.string.jvm_game_probe_failed
                                : R.string.jvm_renderer_failed),
                        finalDetail));
            });
        }, vulkanSmoke ? "rusted-fabric-vulkan-smoke" : "rusted-fabric-egl-smoke");
        renderer.start();
    }

    private String runVulkanTest() throws IOException, InterruptedException {
        String detail = NativeVulkanBridge.start();
        try {
            for (int frame = 0; frame < 300; frame++) {
                int phase = Math.min(2, frame / 100);
                float red = phase == 0 ? 0.85f : 0.05f;
                float green = phase == 1 ? 0.85f : 0.05f;
                float blue = phase == 2 ? 0.85f : 0.05f;
                if (!NativeVulkanBridge.presentClear(red, green, blue, 1.0f)) {
                    throw new IOException("clear frame " + frame + " was not presented: "
                            + NativeVulkanBridge.lastDiagnostic());
                }
                Thread.sleep(16L);
            }
            return "rusted-fabric-vulkan-smoke=ok\n" + detail
                    + "\n300 clear-only frames presented";
        } finally {
            NativeVulkanBridge.stop();
        }
    }

    private String runGameProbe(int width, int height) throws IOException {
        File desktopRoot = DesktopGameImportService.importedRoot(this);
        // Configure this separate Android process too. It must pass the public roots into HotSpot
        // instead of traversing the obsolete private-to-emulated-storage symlinks.
        SharedContentWorkspace.ensureReady(this);
        // The initial desktop import deliberately omits user saves and mods. Recreate the writable
        // layout on every launch as a repair path; launcher-managed content remains in these dirs.
        DesktopGameLayout.prepareWritableDirectories(desktopRoot.toPath());
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
        plan = GameLaunchPreferences.apply(this, plan);
        runOnUiThread(() -> {
            status.setText(R.string.jvm_game_probe_starting);
            hideLaunchCoverAfterFirstVulkanFrame();
        });
        NativeJvmHost.Result launch = NativeJvmHost.launch(plan);
        if (!launch.succeeded()) {
            throw new IOException("Native JVM host code " + launch.code() + ": "
                    + launch.detail() + ". See logcat tag RustedFabricJvm for the Java stack.");
        }
        return "rusted-fabric-game-probe=ok\nFabric/game main returned normally";
    }

    private void hideLaunchCoverAfterFirstVulkanFrame() {
        if (!gameProbe) return;
        if (firstFrameWatcher != null) surfaceHandler.removeCallbacks(firstFrameWatcher);
        final long baseline = NativeVulkanBridge.presentedFrameCount();
        final long deadline = SystemClock.uptimeMillis() + 8000L;
        firstFrameWatcher = new Runnable() {
            @Override public void run() {
                if (!RUNNING.get() || status.getVisibility() != View.VISIBLE) return;
                if (NativeVulkanBridge.presentedFrameCount() > baseline
                        || SystemClock.uptimeMillis() >= deadline) {
                    status.setVisibility(View.GONE);
                    firstFrameWatcher = null;
                    return;
                }
                surfaceHandler.postDelayed(this, 40L);
            }
        };
        surfaceHandler.post(firstFrameWatcher);
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
        surfaceHandler.removeCallbacks(startAfterSurfaceSettles);
        if (vulkanSmoke) NativeVulkanBridge.stop();
        NativeRenderBridge.detachSurface();
    }

    @Override
    protected void onDestroy() {
        surfaceHandler.removeCallbacks(startAfterSurfaceSettles);
        if (firstFrameWatcher != null) surfaceHandler.removeCallbacks(firstFrameWatcher);
        if (vulkanSmoke) NativeVulkanBridge.stop();
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
