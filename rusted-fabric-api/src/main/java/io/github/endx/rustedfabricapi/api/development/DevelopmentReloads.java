package io.github.endx.rustedfabricapi.api.development;

import io.github.endx.rustedfabricapi.api.asset.reload.ResourceReloadReport;
import io.github.endx.rustedfabricapi.internal.development.DevelopmentReloadRuntime;

/** Cross-platform development reload entry points used by tools and mod-management UIs. */
public final class DevelopmentReloads {
    private DevelopmentReloads() { }

    /** Reloads changed native INI units in place, then atomically prepares/applies Java resources. */
    public static ResourceReloadReport reloadInPlace() {
        return DevelopmentReloadRuntime.reloadInPlace();
    }

    /** Reloads only registered Java resources, without touching native INI unit definitions. */
    public static ResourceReloadReport reloadResources() {
        return DevelopmentReloadRuntime.reloadResources();
    }
}
