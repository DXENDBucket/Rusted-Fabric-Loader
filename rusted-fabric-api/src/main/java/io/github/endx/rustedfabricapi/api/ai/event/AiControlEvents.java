package io.github.endx.rustedfabricapi.api.ai.event;

import io.github.endx.rustedfabricapi.api.ai.AiTickContext;
import io.github.endx.rustedfabricapi.api.ai.AiTickOutcome;
import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;

/** Observation events around native or custom AI-team updates. */
public final class AiControlEvents {
    public static final RustedFabricEvent<BeforeTick> BEFORE_TICK =
            RustedFabricEvent.create(listeners -> context -> {
                for (BeforeTick listener : listeners) listener.beforeTick(context);
            });

    public static final RustedFabricEvent<AfterTick> AFTER_TICK =
            RustedFabricEvent.create(listeners -> (context, outcome) -> {
                for (AfterTick listener : listeners) listener.afterTick(context, outcome);
            });

    private AiControlEvents() {
    }

    @FunctionalInterface
    public interface BeforeTick {
        void beforeTick(AiTickContext context);
    }

    @FunctionalInterface
    public interface AfterTick {
        void afterTick(AiTickContext context, AiTickOutcome outcome);
    }
}
