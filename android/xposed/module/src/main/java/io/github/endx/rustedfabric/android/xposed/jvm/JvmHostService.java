package io.github.endx.rustedfabric.android.xposed.jvm;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.endx.rustedfabric.android.jvm.JvmLaunchPlan;
import io.github.endx.rustedfabric.android.jvm.JvmLaunchPlanFactory;

/** Runs the external HotSpot smoke test outside the Loader management process. */
public final class JvmHostService extends Service {
    public static final String ACTION_RESULT =
            "io.github.endx.rustedfabric.android.xposed.JVM_HOST_RESULT";
    public static final String EXTRA_SUCCEEDED = "succeeded";
    public static final String EXTRA_DETAIL = "detail";

    private static final String PAYLOAD_ASSET = "rusted-fabric/jvm-host-smoke.jar";
    private static final AtomicBoolean RUNNING = new AtomicBoolean();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!RUNNING.compareAndSet(false, true)) return START_NOT_STICKY;
        Thread test = new Thread(() -> runTest(startId), "rusted-fabric-jvm-smoke");
        test.start();
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void runTest(int startId) {
        boolean succeeded = false;
        String detail;
        try {
            File work = smokeDirectory(this);
            if (!work.isDirectory() && !work.mkdirs()) {
                throw new IOException("Cannot create private JVM smoke-test directory");
            }
            File status = new File(work, "status.txt");
            write(status, "running\n");
            File payload = installPayload(work);
            File marker = new File(work, "result.txt");
            Files.deleteIfExists(marker.toPath());
            File runtimeHome = new File(new File(getFilesDir(), "desktop-jvm"), "runtime");
            File nativeDirectory = new File(getApplicationInfo().nativeLibraryDir);
            JvmLaunchPlan plan = JvmLaunchPlanFactory.createSmokeTest(runtimeHome.toPath(),
                    payload.toPath(), work.toPath(), nativeDirectory.toPath(), marker.toPath());
            NativeJvmHost.Result result = NativeJvmHost.launch(plan);
            if (!result.succeeded()) {
                throw new IOException("Native JVM host code " + result.code() + ": "
                        + result.detail());
            }
            if (!marker.isFile()) throw new IOException("External JVM did not create its result");
            detail = new String(Files.readAllBytes(marker.toPath()), StandardCharsets.UTF_8).trim();
            if (!detail.startsWith("rusted-fabric-jvm-smoke=ok")) {
                throw new IOException("External JVM returned an invalid result");
            }
            succeeded = true;
            write(status, "succeeded\n" + detail + "\n");
        } catch (Throwable failure) {
            detail = safeMessage(failure);
            try {
                File work = smokeDirectory(this);
                if (!work.isDirectory()) work.mkdirs();
                write(new File(work, "status.txt"), "failed\n" + detail + "\n");
            } catch (IOException ignored) {
                // The broadcast still reports the original failure.
            }
        }
        Intent result = new Intent(ACTION_RESULT).setPackage(getPackageName());
        result.putExtra(EXTRA_SUCCEEDED, succeeded);
        result.putExtra(EXTRA_DETAIL, detail);
        sendBroadcast(result);
        RUNNING.set(false);
        stopSelf(startId);
        int processId = Process.myPid();
        new Handler(Looper.getMainLooper()).postDelayed(
                () -> Process.killProcess(processId), 1_000L);
    }

    private File installPayload(File work) throws IOException {
        File target = new File(work, "jvm-host-smoke.jar");
        File staging = new File(work, "jvm-host-smoke.jar.importing");
        Files.deleteIfExists(staging.toPath());
        try (InputStream input = getAssets().open(PAYLOAD_ASSET);
             FileOutputStream output = new FileOutputStream(staging)) {
            byte[] buffer = new byte[32 * 1024];
            int read;
            while ((read = input.read(buffer)) >= 0) output.write(buffer, 0, read);
        }
        Files.deleteIfExists(target.toPath());
        if (!staging.renameTo(target)) throw new IOException("Cannot activate JVM smoke-test JAR");
        return target;
    }

    public static String lastStatus(Context context) {
        File status = new File(smokeDirectory(context), "status.txt");
        if (!status.isFile()) return "";
        try {
            return new String(Files.readAllBytes(status.toPath()), StandardCharsets.UTF_8).trim();
        } catch (IOException unreadable) {
            return "status unreadable: " + safeMessage(unreadable);
        }
    }

    private static File smokeDirectory(Context context) {
        return new File(new File(context.getFilesDir(), "desktop-jvm"), "smoke");
    }

    private static void write(File file, String value) throws IOException {
        Files.write(file.toPath(), value.getBytes(StandardCharsets.UTF_8));
    }

    private static String safeMessage(Throwable failure) {
        String message = failure.getMessage();
        return message == null || message.trim().isEmpty()
                ? failure.getClass().getSimpleName() : message;
    }
}
