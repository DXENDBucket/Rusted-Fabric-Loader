package io.github.endx.rustedfabricapi.api.client.event;

import io.github.endx.rustedfabricapi.api.client.render.WorldDrawContext;
import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;

/** Fabric-style world overlay boundary, fired after world content and before the HUD. */
public final class WorldRenderEvents {
    public static final RustedFabricEvent<AfterWorld> AFTER_WORLD =
            RustedFabricEvent.create(listeners -> context -> {
                for (AfterWorld listener : listeners) listener.draw(context);
            });

    private WorldRenderEvents() {
    }

    @FunctionalInterface
    public interface AfterWorld {
        void draw(WorldDrawContext context);
    }
}
