package io.github.endx.rustedfabric.android.xposed;

import java.lang.reflect.Method;

import io.github.endx.rustedfabric.android.bootstrap.BootstrapDiagnostics;
import io.github.endx.rustedfabric.android.bootstrap.BootstrapPolicy;
import io.github.libxposed.api.XposedModule;

/** Modern Xposed API entrypoint. Phase 1 installs one diagnostic-only Application.attach hook. */
public final class RustedFabricXposedModule extends XposedModule {
    private static final String TAG = "RustedFabric/Bootstrap";
    private static final int LOG_INFO = 4;
    private static final int LOG_ERROR = 6;
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
            Method getClassLoader = contextClass.getMethod("getClassLoader");

            hook(attach)
                    .setId("rusted-fabric:application-attach")
                    .setPriority(PRIORITY_LOWEST)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        try {
                            Object context = chain.getArg(0);
                            ClassLoader targetLoader = (ClassLoader) getClassLoader.invoke(context);
                            Object application = chain.getThisObject();
                            BootstrapDiagnostics.Snapshot snapshot = BootstrapDiagnostics.captureOnce(
                                    packageName, processName,
                                    application == null ? null : application.getClass().getName(),
                                    targetLoader == null ? null : targetLoader.getClass().getName());
                            log(LOG_INFO, TAG, "application-attached package="
                                    + snapshot.getPackageName() + " process=" + snapshot.getProcessName()
                                    + " app=" + snapshot.getApplicationClassName()
                                    + " loader=" + snapshot.getClassLoaderClassName()
                                    + " mapping=" + snapshot.getMappingProfileStatus());
                        } catch (Throwable diagnosticFailure) {
                            // Diagnostics are optional and must never change game startup behavior.
                            log(LOG_ERROR, TAG, "attach diagnostics failed", diagnosticFailure);
                        }
                        return result;
                    });
            log(LOG_INFO, TAG, "hook-installed target=android.app.Application#attach");
        } catch (Throwable hookFailure) {
            // Failure to install the optional bootstrap hook must leave the game untouched.
            log(LOG_ERROR, TAG, "hook installation failed", hookFailure);
        }
    }

    private static String safe(String value) {
        return value == null ? "unknown" : value;
    }
}
