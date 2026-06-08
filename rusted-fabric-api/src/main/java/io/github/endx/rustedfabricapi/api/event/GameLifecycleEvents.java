package io.github.endx.rustedfabricapi.api.event;

public final class GameLifecycleEvents {
    public static final RustedFabricEvent<AfterMapSetup> AFTER_MAP_SETUP =
            RustedFabricEvent.create(listeners -> (minimap, map, fogEnabled) -> {
                for (AfterMapSetup listener : listeners) {
                    listener.afterMapSetup(minimap, map, fogEnabled);
                }
            });

    private GameLifecycleEvents() {
    }

    public interface AfterMapSetup {
        void afterMapSetup(Object minimap, Object map, boolean fogEnabled);
    }
}
