package io.github.endx.rustedfabricapi.api.asset.reload;

import java.util.List;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import io.github.endx.rustedfabricapi.api.util.Identifier;

/** Aggregate lifecycle around Loader-owned resource reloaders. */
public final class ModResourceReloadEvents {
    public static final RustedFabricEvent<BeforeReload> BEFORE_RELOAD =
            RustedFabricEvent.create(listeners -> (reason, ids) -> {
                for (BeforeReload listener : listeners) listener.beforeReload(reason, ids);
            });
    public static final RustedFabricEvent<AfterReload> AFTER_RELOAD =
            RustedFabricEvent.create(listeners -> report -> {
                for (AfterReload listener : listeners) listener.afterReload(report);
            });

    private ModResourceReloadEvents() {
    }

    @FunctionalInterface
    public interface BeforeReload {
        void beforeReload(ResourceReloadReason reason, List<Identifier> listenerIds);
    }

    @FunctionalInterface
    public interface AfterReload {
        void afterReload(ResourceReloadReport report);
    }
}
