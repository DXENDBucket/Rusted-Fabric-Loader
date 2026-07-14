package io.github.endx.rustedfabric.android.xposed;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;

import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.endx.rustedfabric.android.bootstrap.AndroidMappingProfile;
import io.github.endx.rustedfabric.android.bootstrap.BootstrapDiagnostics;
import io.github.endx.rustedfabric.android.bootstrap.BootstrapPolicy;
import io.github.endx.rustedfabric.android.bootstrap.Sha256;
import io.github.libxposed.api.XposedModule;

/** Modern Xposed entrypoint for exact profile selection and the first diagnostic-only game hook. */
public final class RustedFabricXposedModule extends XposedModule {
    private static final String TAG = "RustedFabric/Bootstrap";
    private static final int LOG_INFO = 4;
    private static final int LOG_ERROR = 6;
    private final AtomicBoolean gameEngineHookInstalled = new AtomicBoolean();
    private final AtomicBoolean gameEngineInitialized = new AtomicBoolean();
    private volatile String processName = "unknown";

    public RustedFabricXposedModule() {
        // The framework attaches XposedInterface after construction. Do not initialize here.
    }

    @Override
    public void onModuleLoaded(ModuleLoadedParam param) {
        processName = param.getProcessName();
        log(LOG_INFO, TAG, "module-loaded process=" + safe(processName)
                + " api=" + getApiVersion());
    }

    @Override
    public void onPackageLoaded(PackageLoadedParam param) {
        String packageName = param.getPackageName();
        if (!BootstrapPolicy.shouldInstall(packageName, param.isFirstPackage())) {
            return;
        }

        try {
            ClassLoader defaultLoader = param.getDefaultClassLoader();
            Class<?> applicationClass = Class.forName("android.app.Application", false, defaultLoader);
            Class<?> contextClass = Class.forName("android.content.Context", false, defaultLoader);
            Method attach = applicationClass.getDeclaredMethod("attach", contextClass);

            hook(attach)
                    .setId("rusted-fabric:application-attach")
                    .setPriority(PRIORITY_LOWEST)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        try {
                            Context context = (Context) chain.getArg(0);
                            ClassLoader targetLoader = context.getClassLoader();
                            Object application = chain.getThisObject();
                            long verificationStarted = System.nanoTime();
                            AndroidMappingProfile.Selection selection = selectMappingProfile(
                                    context, packageName);
                            BootstrapDiagnostics.Snapshot snapshot = BootstrapDiagnostics.captureOnce(
                                    packageName, processName,
                                    application == null ? null : application.getClass().getName(),
                                    targetLoader == null ? null : targetLoader.getClass().getName(),
                                    selection.diagnosticStatus());
                            log(LOG_INFO, TAG, "application-attached package="
                                    + snapshot.getPackageName() + " process=" + snapshot.getProcessName()
                                    + " app=" + snapshot.getApplicationClassName()
                                    + " loader=" + snapshot.getClassLoaderClassName()
                                    + " mapping=" + snapshot.getMappingProfileStatus()
                                    + " verificationMs=" + elapsedMillis(verificationStarted));
                            if (selection.isVerified()) {
                                installGameEngineInitHook(targetLoader, contextClass);
                            } else {
                                log(LOG_INFO, TAG, "game-hook-skipped mapping="
                                        + selection.diagnosticStatus());
                            }
                        } catch (Throwable diagnosticFailure) {
                            // Verification and diagnostics are optional and must never change startup.
                            log(LOG_ERROR, TAG, "profile verification failed; game hooks disabled",
                                    diagnosticFailure);
                        }
                        return result;
                    });
            log(LOG_INFO, TAG, "hook-installed target=android.app.Application#attach");
        } catch (Throwable hookFailure) {
            // Failure to install the optional bootstrap hook must leave the game untouched.
            log(LOG_ERROR, TAG, "hook installation failed", hookFailure);
        }
    }

    @SuppressWarnings("deprecation")
    private AndroidMappingProfile.Selection selectMappingProfile(Context context, String packageName)
            throws Exception {
        PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
        long versionCode = Build.VERSION.SDK_INT >= 28
                ? packageInfo.getLongVersionCode() : packageInfo.versionCode;
        String sourceDir = context.getApplicationInfo().sourceDir;
        if (sourceDir == null || sourceDir.isEmpty()) {
            return AndroidMappingProfile.select(packageName, packageInfo.versionName, versionCode, null);
        }
        String apkSha256;
        try (FileInputStream input = new FileInputStream(sourceDir)) {
            apkSha256 = Sha256.digest(input);
        }
        return AndroidMappingProfile.select(packageName, packageInfo.versionName, versionCode, apkSha256);
    }

    private void installGameEngineInitHook(ClassLoader targetLoader, Class<?> contextClass) {
        if (!gameEngineHookInstalled.compareAndSet(false, true)) {
            return;
        }
        try {
            Class<?> gameEngineClass = Class.forName(
                    AndroidMappingProfile.gameEngineInitBinaryClassName(), false, targetLoader);
            Method init = gameEngineClass.getDeclaredMethod(
                    AndroidMappingProfile.GAME_ENGINE_INIT_NAME, contextClass);
            hook(init)
                    .setId("rusted-fabric:game-engine-init")
                    .setPriority(PRIORITY_LOWEST)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        if (gameEngineInitialized.compareAndSet(false, true)) {
                            log(LOG_INFO, TAG, "game-engine-initialized profile="
                                    + AndroidMappingProfile.ID + " event=after-"
                                    + AndroidMappingProfile.GAME_ENGINE_INIT_NAMED);
                        }
                        return result;
                    });
            log(LOG_INFO, TAG, "hook-installed target="
                    + AndroidMappingProfile.GAME_ENGINE_INIT_OWNER + '#'
                    + AndroidMappingProfile.GAME_ENGINE_INIT_NAME
                    + AndroidMappingProfile.GAME_ENGINE_INIT_DESCRIPTOR);
        } catch (Throwable hookFailure) {
            gameEngineHookInstalled.set(false);
            log(LOG_ERROR, TAG, "GameEngine init hook installation failed", hookFailure);
        }
    }

    private static long elapsedMillis(long startedNanos) {
        return (System.nanoTime() - startedNanos) / 1_000_000L;
    }

    private static String safe(String value) {
        return value == null ? "unknown" : value;
    }
}
