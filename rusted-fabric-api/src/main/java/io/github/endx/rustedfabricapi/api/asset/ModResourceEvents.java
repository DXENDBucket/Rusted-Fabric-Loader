package io.github.endx.rustedfabricapi.api.asset;

import java.nio.file.Path;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;

/** Observation and cancellation events for API-mediated mod resource access. */
public final class ModResourceEvents {
    public static final RustedFabricEvent<AfterRead> AFTER_READ =
            RustedFabricEvent.create(listeners -> (resource, byteCount) -> {
                for (AfterRead listener : listeners) listener.afterRead(resource, byteCount);
            });
    public static final RustedFabricEvent<BeforeExtract> BEFORE_EXTRACT =
            RustedFabricEvent.create(listeners -> resource -> {
                boolean cancelled = false;
                for (BeforeExtract listener : listeners) cancelled |= listener.beforeExtract(resource);
                return cancelled;
            });
    public static final RustedFabricEvent<AfterExtract> AFTER_EXTRACT =
            RustedFabricEvent.create(listeners -> (resource, path, success) -> {
                for (AfterExtract listener : listeners) listener.afterExtract(resource, path, success);
            });

    private ModResourceEvents() {
    }

    @FunctionalInterface public interface AfterRead {
        void afterRead(ModResource resource, int byteCount);
    }
    @FunctionalInterface public interface BeforeExtract {
        boolean beforeExtract(ModResource resource);
    }
    @FunctionalInterface public interface AfterExtract {
        void afterExtract(ModResource resource, Path extractedPath, boolean success);
    }
}
