package io.github.endx.rustedfabricapi.api.client.event;

import io.github.endx.rustedfabricapi.api.client.render.WorldDrawContext;
import io.github.endx.rustedfabricapi.api.client.render.WorldLayerDrawContext;
import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;

/** Fabric-style boundaries around the native world and unit render passes. */
public final class WorldRenderEvents {
    /**
     * Runs after the terrain and path-debug pass, immediately before the first visible game
     * object is drawn. The native world transform and viewport clip are active.
     */
    public static final RustedFabricEvent<WorldLayer> BEFORE_UNITS = worldLayerEvent();

    /**
     * Runs after every visible game-object layer, including draw-layer 10, and before weather,
     * mission overlays, and the HUD. The native world transform and viewport clip are active.
     */
    public static final RustedFabricEvent<WorldLayer> AFTER_UNITS = worldLayerEvent();

    /** Runs after all world content with a screen-space drawing facade, before the HUD. */
    public static final RustedFabricEvent<AfterWorld> AFTER_WORLD =
            RustedFabricEvent.create(listeners -> context -> {
                for (AfterWorld listener : listeners) listener.draw(context);
            });

    private WorldRenderEvents() {
    }

    private static RustedFabricEvent<WorldLayer> worldLayerEvent() {
        return RustedFabricEvent.create(listeners -> context -> {
            for (WorldLayer listener : listeners) listener.draw(context);
        });
    }

    @FunctionalInterface
    public interface WorldLayer {
        void draw(WorldLayerDrawContext context);
    }

    @FunctionalInterface
    public interface AfterWorld {
        void draw(WorldDrawContext context);
    }
}
