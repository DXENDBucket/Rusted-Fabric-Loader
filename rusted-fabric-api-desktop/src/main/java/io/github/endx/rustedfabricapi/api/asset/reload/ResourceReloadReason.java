package io.github.endx.rustedfabricapi.api.asset.reload;

/** Stable reason supplied to resource-reload lifecycle events and reports. */
public enum ResourceReloadReason {
    INITIAL_ENGINE_READY,
    NATIVE_CUSTOM_UNITS,
    LANGUAGE,
    MANUAL
}
