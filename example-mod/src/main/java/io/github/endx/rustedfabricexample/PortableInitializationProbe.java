package io.github.endx.rustedfabricexample;

import io.github.endx.rustedfabricapi.api.event.RuntimeLifecycleEvents;
import io.github.endx.rustedfabricapi.api.event.MultiplayerCompatibilityEvents;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Source-portable example: this class has no Fabric, Android, or game implementation dependency. */
public final class PortableInitializationProbe {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();

    private PortableInitializationProbe() {
    }

    public static void register(Consumer<String> logger) {
        Consumer<String> checkedLogger = Objects.requireNonNull(logger, "logger");
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        RuntimeLifecycleEvents.BEFORE_ENGINE_INITIALIZATION.register(context ->
                checkedLogger.accept("portable before-engine-init platform=" + context.platform()
                        + " profile=" + context.mappingProfileId()));
        RuntimeLifecycleEvents.AFTER_ENGINE_INITIALIZATION.register(context ->
                checkedLogger.accept("portable after-engine-init platform=" + context.platform()
                        + " namespace=" + context.runtimeNamespace()));
        RuntimeLifecycleEvents.LOADER_READY.register(context ->
                checkedLogger.accept("portable loader-ready multiplayer="
                        + context.multiplayerManifest().map(manifest ->
                        manifest.fingerprint().substring(0, 12)).orElse("unavailable")));
        RuntimeLifecycleEvents.GAME_READY.register(context ->
                checkedLogger.accept("portable game-ready platform=" + context.platform()));
        MultiplayerCompatibilityEvents.COMPATIBILITY_EVALUATED.register(report ->
                checkedLogger.accept("portable multiplayer-compatible=" + report.compatible()
                        + " issues=" + report.issues().size()));
    }
}
