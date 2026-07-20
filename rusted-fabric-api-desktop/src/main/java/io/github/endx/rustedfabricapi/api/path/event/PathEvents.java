package io.github.endx.rustedfabricapi.api.path.event;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import io.github.endx.rustedfabricapi.api.path.PathResult;
import rustedwarfare.path.PathEngine;
import rustedwarfare.path.PathRequest;

/** Native path queue and one-shot completion boundaries. */
public final class PathEvents {
    public static final RustedFabricEvent<Queuing> QUEUING =
            RustedFabricEvent.create(listeners -> (engine, request, refreshCosts) -> {
                for (Queuing listener : listeners) listener.onQueuing(engine, request, refreshCosts);
            });
    public static final RustedFabricEvent<Queued> QUEUED =
            RustedFabricEvent.create(listeners -> (engine, request, refreshCosts) -> {
                for (Queued listener : listeners) listener.onQueued(engine, request, refreshCosts);
            });
    public static final RustedFabricEvent<Solved> SOLVED =
            RustedFabricEvent.create(listeners -> (engine, request, result) -> {
                for (Solved listener : listeners) listener.onSolved(engine, request, result);
            });

    private PathEvents() {
    }

    @FunctionalInterface
    public interface Queuing {
        void onQueuing(PathEngine engine, PathRequest request, boolean refreshCosts);
    }

    @FunctionalInterface
    public interface Queued {
        void onQueued(PathEngine engine, PathRequest request, boolean refreshCosts);
    }

    @FunctionalInterface
    public interface Solved {
        void onSolved(PathEngine engine, PathRequest request, PathResult result);
    }
}
