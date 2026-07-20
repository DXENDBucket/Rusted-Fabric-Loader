package io.github.endx.rustedfabricapi.api.map.event;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import io.github.endx.rustedfabricapi.api.map.MapObjectCatalog;

/** High-level immutable boundary after all TMX object groups have loaded. */
public final class MapObjectEvents {
    public static final RustedFabricEvent<AfterLoad> AFTER_LOAD =
            RustedFabricEvent.create(listeners -> catalog -> {
                for (AfterLoad listener : listeners) listener.afterLoad(catalog);
            });

    private MapObjectEvents() {
    }

    @FunctionalInterface
    public interface AfterLoad {
        void afterLoad(MapObjectCatalog catalog);
    }
}
