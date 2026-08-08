package io.github.endx.rustedfabricapi.api.replay.event;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import rustedwarfare.replay.ReplayEngine;

/** Typed lifecycle events for local replay recording and playback. */
public final class ReplayEvents {
    public static final RustedFabricEvent<BeforeNamedOperation> BEFORE_RECORD = cancellable();
    public static final RustedFabricEvent<AfterNamedOperation> AFTER_RECORD = afterNamed();
    public static final RustedFabricEvent<BeforeNamedOperation> BEFORE_PLAY = cancellable();
    public static final RustedFabricEvent<AfterNamedOperation> AFTER_PLAY = afterNamed();
    public static final RustedFabricEvent<BeforeStop> BEFORE_STOP =
            RustedFabricEvent.create(listeners -> manager -> {
                for (BeforeStop listener : listeners) listener.beforeStop(manager);
            });
    public static final RustedFabricEvent<AfterStop> AFTER_STOP =
            RustedFabricEvent.create(listeners -> manager -> {
                for (AfterStop listener : listeners) listener.afterStop(manager);
            });
    public static final RustedFabricEvent<BeforeNamedOperation> BEFORE_DELETE = cancellable();
    public static final RustedFabricEvent<AfterNamedOperation> AFTER_DELETE = afterNamed();

    private ReplayEvents() {
    }

    private static RustedFabricEvent<BeforeNamedOperation> cancellable() {
        return RustedFabricEvent.create(listeners -> (manager, name) -> {
            boolean cancelled = false;
            for (BeforeNamedOperation listener : listeners) {
                cancelled |= listener.beforeOperation(manager, name);
            }
            return cancelled;
        });
    }

    private static RustedFabricEvent<AfterNamedOperation> afterNamed() {
        return RustedFabricEvent.create(listeners -> (manager, name, success) -> {
            for (AfterNamedOperation listener : listeners) {
                listener.afterOperation(manager, name, success);
            }
        });
    }

    @FunctionalInterface
    public interface BeforeNamedOperation {
        boolean beforeOperation(ReplayEngine manager, String name);
    }

    @FunctionalInterface
    public interface AfterNamedOperation {
        void afterOperation(ReplayEngine manager, String name, boolean success);
    }

    @FunctionalInterface
    public interface BeforeStop {
        void beforeStop(ReplayEngine manager);
    }

    @FunctionalInterface
    public interface AfterStop {
        void afterStop(ReplayEngine manager);
    }
}
