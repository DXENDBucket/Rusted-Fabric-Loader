package io.github.endx.rustedfabricapi.api.client.event;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import rustedwarfare.core.GameEngine;

/** One-shot lifecycle boundaries around desktop {@link GameEngine} initialization. */
public final class ClientLifecycleEvents {
    public static final RustedFabricEvent<EngineInitialization> BEFORE_ENGINE_INITIALIZATION = event();
    public static final RustedFabricEvent<EngineInitialization> AFTER_ENGINE_INITIALIZATION = event();

    private ClientLifecycleEvents() {
    }

    private static RustedFabricEvent<EngineInitialization> event() {
        return RustedFabricEvent.create(listeners -> engine -> {
            for (EngineInitialization listener : listeners) {
                listener.onEngineInitialization(engine);
            }
        });
    }

    @FunctionalInterface
    public interface EngineInitialization {
        void onEngineInitialization(GameEngine engine);
    }
}
