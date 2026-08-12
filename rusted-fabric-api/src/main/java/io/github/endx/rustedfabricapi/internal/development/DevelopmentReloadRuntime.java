package io.github.endx.rustedfabricapi.internal.development;

import io.github.endx.rustedfabricapi.api.asset.reload.ModResourceReloaders;
import io.github.endx.rustedfabricapi.api.asset.reload.ResourceReloadReason;
import io.github.endx.rustedfabricapi.api.asset.reload.ResourceReloadReport;
import io.github.endx.rustedfabricapi.api.custom.CustomUnits;

import java.lang.reflect.InvocationTargetException;
import java.util.Locale;

/** Internal coordination for native-unit and Java-resource development reloads. */
public final class DevelopmentReloadRuntime {
    private static final Object LOCK = new Object();
    private static volatile boolean integratedUnitReloadRunning;

    private DevelopmentReloadRuntime() { }

    public static ResourceReloadReport reloadInPlace() {
        synchronized (LOCK) {
            syncNativeContent();
            integratedUnitReloadRunning = true;
            try {
                if (isAndroidRuntime()) {
                    // Shared-storage/document providers do not reliably preserve mtimes. The
                    // game's sandbox force-reload path also migrates active unit instances.
                    CustomUnits.reloadActiveInPlace();
                } else {
                    CustomUnits.reloadChangedInPlace();
                }
            } finally {
                integratedUnitReloadRunning = false;
            }
            // The registry event above was deliberately suppressed, so Java resources apply
            // exactly once after the native unit graph has reached its final state.
            return ModResourceReloaders.reloadAll(ResourceReloadReason.MANUAL);
        }
    }

    public static ResourceReloadReport reloadResources() {
        return ModResourceReloaders.reloadAll(ResourceReloadReason.MANUAL);
    }

    /**
     * Mirrors editable native content before the game's own sandbox reload scans timestamps.
     * Integrated API reloads already synchronize before entering the native loader.
     */
    public static void synchronizeBeforeExternalNativeReload() {
        if (integratedUnitReloadRunning) return;
        synchronized (LOCK) {
            if (!integratedUnitReloadRunning) syncNativeContent();
        }
    }

    private static void syncNativeContent() {
        try {
            Class<?> bridge = Class.forName(
                    "io.github.endx.rustedfabricloader.NativeContentDevelopmentBridge");
            bridge.getMethod("syncAll").invoke(null);
        } catch (ClassNotFoundException unavailable) {
            // A standalone API contract test has no Loader on its runtime classpath.
        } catch (NoSuchMethodException | IllegalAccessException failure) {
            throw new IllegalStateException("Loader native-content bridge is incompatible", failure);
        } catch (InvocationTargetException failure) {
            Throwable cause = failure.getCause() != null ? failure.getCause() : failure;
            throw new IllegalStateException("Could not synchronize editable native content", cause);
        }
    }

    public static boolean isIntegratedUnitReloadRunning() {
        return integratedUnitReloadRunning;
    }

    private static boolean isAndroidRuntime() {
        return System.getProperty("rustedfabric.platform", "")
                .toLowerCase(Locale.ROOT).contains("android");
    }
}
