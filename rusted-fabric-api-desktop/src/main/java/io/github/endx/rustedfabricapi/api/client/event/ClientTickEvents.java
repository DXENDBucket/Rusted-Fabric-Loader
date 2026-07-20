package io.github.endx.rustedfabricapi.api.client.event;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import rustedwarfare.core.GameEngine;

/** Fabric-style events fired around each desktop client update. */
public final class ClientTickEvents {
    public static final RustedFabricEvent<StartTick> START_CLIENT_TICK =
            RustedFabricEvent.create(listeners -> engine -> {
                for (StartTick listener : listeners) {
                    listener.onStartTick(engine);
                }
            });

    public static final RustedFabricEvent<EndTick> END_CLIENT_TICK =
            RustedFabricEvent.create(listeners -> engine -> {
                for (EndTick listener : listeners) {
                    listener.onEndTick(engine);
                }
            });

    private ClientTickEvents() {
    }

    @FunctionalInterface
    public interface StartTick {
        void onStartTick(GameEngine engine);
    }

    @FunctionalInterface
    public interface EndTick {
        void onEndTick(GameEngine engine);
    }
}
