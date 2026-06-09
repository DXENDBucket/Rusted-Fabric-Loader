package io.github.endx.rustedfabricapi.api.event;

public final class GameLifecycleEvents {
    public static final RustedFabricEvent<AfterMapSetup> AFTER_MAP_SETUP =
            RustedFabricEvent.create(listeners -> (minimap, map, fogEnabled) -> {
                for (AfterMapSetup listener : listeners) {
                    listener.afterMapSetup(minimap, map, fogEnabled);
                }
            });

    public static final RustedFabricEvent<AfterGameInterfaceDraw> AFTER_GAME_INTERFACE_DRAW =
            RustedFabricEvent.create(listeners -> (interfaceEngine, delta) -> {
                for (AfterGameInterfaceDraw listener : listeners) {
                    listener.afterGameInterfaceDraw(interfaceEngine, delta);
                }
            });

    public static final RustedFabricEvent<AfterFrameRender> AFTER_FRAME_RENDER =
            RustedFabricEvent.create(listeners -> renderer -> {
                for (AfterFrameRender listener : listeners) {
                    listener.afterFrameRender(renderer);
                }
            });

    public static final RustedFabricEvent<BeforeFrameUpdate> BEFORE_FRAME_UPDATE =
            RustedFabricEvent.create(listeners -> (renderer, gameContainer, delta) -> {
                for (BeforeFrameUpdate listener : listeners) {
                    listener.beforeFrameUpdate(renderer, gameContainer, delta);
                }
            });

    public static final RustedFabricEvent<AfterFrameUpdate> AFTER_FRAME_UPDATE =
            RustedFabricEvent.create(listeners -> (renderer, gameContainer, delta) -> {
                for (AfterFrameUpdate listener : listeners) {
                    listener.afterFrameUpdate(renderer, gameContainer, delta);
                }
            });

    private GameLifecycleEvents() {
    }

    public interface AfterMapSetup {
        void afterMapSetup(Object minimap, Object map, boolean fogEnabled);
    }

    public interface AfterGameInterfaceDraw {
        void afterGameInterfaceDraw(Object interfaceEngine, float delta);
    }

    public interface AfterFrameRender {
        void afterFrameRender(Object renderer);
    }

    public interface BeforeFrameUpdate {
        void beforeFrameUpdate(Object renderer, Object gameContainer, int delta);
    }

    public interface AfterFrameUpdate {
        void afterFrameUpdate(Object renderer, Object gameContainer, int delta);
    }
}
