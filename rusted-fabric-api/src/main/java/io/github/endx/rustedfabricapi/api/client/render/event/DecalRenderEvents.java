package io.github.endx.rustedfabricapi.api.client.render.event;

import java.util.List;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.custom.graphics.DecalLayer;
import rustedwarfare.custom.graphics.DecalTemplate;

/** Per-unit boundaries around one native Decal layer pass. */
public final class DecalRenderEvents {
    public static final RustedFabricEvent<LayerBoundary> BEFORE_LAYER = event();
    public static final RustedFabricEvent<LayerBoundary> AFTER_LAYER = event();

    private DecalRenderEvents() { }

    private static RustedFabricEvent<LayerBoundary> event() {
        return RustedFabricEvent.create(listeners -> (unit, delta, layer, decals) -> {
            for (LayerBoundary listener : listeners) {
                listener.onLayer(unit, delta, layer, decals);
            }
        });
    }

    @FunctionalInterface
    public interface LayerBoundary {
        /** The decal list is an unmodifiable view retaining native draw order. */
        void onLayer(CustomUnit unit, float delta, DecalLayer layer,
                     List<DecalTemplate> decals);
    }
}
