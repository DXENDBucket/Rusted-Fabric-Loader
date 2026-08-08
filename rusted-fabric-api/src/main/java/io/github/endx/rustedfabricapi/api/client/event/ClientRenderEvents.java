package io.github.endx.rustedfabricapi.api.client.event;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import rustedwarfare.core.GameEngine;
import rustedwarfare.render.GraphicsEngine;

/** Render-frame boundaries that do not expose Slick or LWJGL implementation types. */
public final class ClientRenderEvents {
    public static final RustedFabricEvent<Render> START_CLIENT_RENDER = event();
    public static final RustedFabricEvent<Render> END_CLIENT_RENDER = event();

    private ClientRenderEvents() {
    }

    private static RustedFabricEvent<Render> event() {
        return RustedFabricEvent.create(listeners -> (engine, graphics) -> {
            for (Render listener : listeners) {
                listener.onRender(engine, graphics);
            }
        });
    }

    @FunctionalInterface
    public interface Render {
        /** Both arguments may be {@code null} during early loading frames. */
        void onRender(GameEngine engine, GraphicsEngine graphics);
    }
}
