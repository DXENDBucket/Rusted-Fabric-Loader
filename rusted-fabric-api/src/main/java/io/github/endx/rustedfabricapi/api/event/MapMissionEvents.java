package io.github.endx.rustedfabricapi.api.event;

public final class MapMissionEvents {
    public static final RustedFabricEvent<BeforeCurrentMapLoad> BEFORE_CURRENT_MAP_LOAD =
            RustedFabricEvent.create(listeners -> (gameEngine, optionA, optionB, mode) -> {
                boolean cancelled = false;
                for (BeforeCurrentMapLoad listener : listeners) {
                    cancelled |= listener.beforeCurrentMapLoad(gameEngine, optionA, optionB, mode);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<BeforeMapStreamOpen> BEFORE_MAP_STREAM_OPEN =
            RustedFabricEvent.create(listeners -> mapPath -> {
                boolean cancelled = false;
                for (BeforeMapStreamOpen listener : listeners) {
                    cancelled |= listener.beforeMapStreamOpen(mapPath);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterMapObjectGroupsLoaded> AFTER_MAP_OBJECT_GROUPS_LOADED =
            RustedFabricEvent.create(listeners -> mapEngine -> {
                for (AfterMapObjectGroupsLoaded listener : listeners) {
                    listener.afterMapObjectGroupsLoaded(mapEngine);
                }
            });

    public static final RustedFabricEvent<BeforeMissionTriggersParse> BEFORE_MISSION_TRIGGERS_PARSE =
            RustedFabricEvent.create(listeners -> (missionEngine, mapObject) -> {
                boolean cancelled = false;
                for (BeforeMissionTriggersParse listener : listeners) {
                    cancelled |= listener.beforeMissionTriggersParse(missionEngine, mapObject);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterMissionTriggersLinked> AFTER_MISSION_TRIGGERS_LINKED =
            RustedFabricEvent.create(listeners -> (missionEngine, trigger) -> {
                for (AfterMissionTriggersLinked listener : listeners) {
                    listener.afterMissionTriggersLinked(missionEngine, trigger);
                }
            });

    public static final RustedFabricEvent<AfterCurrentMapStarted> AFTER_CURRENT_MAP_STARTED =
            RustedFabricEvent.create(listeners -> (gameEngine, optionA, optionB, mode) -> {
                for (AfterCurrentMapStarted listener : listeners) {
                    listener.afterCurrentMapStarted(gameEngine, optionA, optionB, mode);
                }
            });

    public static final RustedFabricEvent<BeforeTmxDocumentParse> BEFORE_TMX_DOCUMENT_PARSE =
            RustedFabricEvent.create(listeners -> (mapEngine, inputStream, newGame) -> {
                boolean cancelled = false;
                for (BeforeTmxDocumentParse listener : listeners) {
                    cancelled |= listener.beforeTmxDocumentParse(mapEngine, inputStream, newGame);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterMapAttributesRead> AFTER_MAP_ATTRIBUTES_READ =
            RustedFabricEvent.create(listeners -> (mapEngine, inputStream, newGame) -> {
                for (AfterMapAttributesRead listener : listeners) {
                    listener.afterMapAttributesRead(mapEngine, inputStream, newGame);
                }
            });

    public static final RustedFabricEvent<AfterTilesetsLoaded> AFTER_TILESETS_LOADED =
            RustedFabricEvent.create(listeners -> (mapEngine, inputStream, newGame) -> {
                for (AfterTilesetsLoaded listener : listeners) {
                    listener.afterTilesetsLoaded(mapEngine, inputStream, newGame);
                }
            });

    public static final RustedFabricEvent<AfterMapLayersLoaded> AFTER_MAP_LAYERS_LOADED =
            RustedFabricEvent.create(listeners -> (mapEngine, inputStream, newGame) -> {
                for (AfterMapLayersLoaded listener : listeners) {
                    listener.afterMapLayersLoaded(mapEngine, inputStream, newGame);
                }
            });

    public static final RustedFabricEvent<AfterCurrentMapLoadedBeforeStartingUnits> AFTER_CURRENT_MAP_LOADED_BEFORE_STARTING_UNITS =
            RustedFabricEvent.create(listeners -> (gameEngine, mapEngine, optionA, optionB, mode) -> {
                for (AfterCurrentMapLoadedBeforeStartingUnits listener : listeners) {
                    listener.afterCurrentMapLoadedBeforeStartingUnits(gameEngine, mapEngine, optionA, optionB, mode);
                }
            });

    private MapMissionEvents() {
    }

    @FunctionalInterface
    public interface BeforeCurrentMapLoad {
        boolean beforeCurrentMapLoad(Object gameEngine, boolean optionA, boolean optionB, Object mode);
    }

    @FunctionalInterface
    public interface BeforeMapStreamOpen {
        boolean beforeMapStreamOpen(String mapPath);
    }

    @FunctionalInterface
    public interface AfterMapObjectGroupsLoaded {
        void afterMapObjectGroupsLoaded(Object mapEngine);
    }

    @FunctionalInterface
    public interface BeforeMissionTriggersParse {
        boolean beforeMissionTriggersParse(Object missionEngine, Object mapObject);
    }

    @FunctionalInterface
    public interface AfterMissionTriggersLinked {
        void afterMissionTriggersLinked(Object missionEngine, Object trigger);
    }

    @FunctionalInterface
    public interface AfterCurrentMapStarted {
        void afterCurrentMapStarted(Object gameEngine, boolean optionA, boolean optionB, Object mode);
    }

    @FunctionalInterface
    public interface BeforeTmxDocumentParse {
        boolean beforeTmxDocumentParse(Object mapEngine, Object inputStream, boolean newGame);
    }

    @FunctionalInterface
    public interface AfterMapAttributesRead {
        void afterMapAttributesRead(Object mapEngine, Object inputStream, boolean newGame);
    }

    @FunctionalInterface
    public interface AfterTilesetsLoaded {
        void afterTilesetsLoaded(Object mapEngine, Object inputStream, boolean newGame);
    }

    @FunctionalInterface
    public interface AfterMapLayersLoaded {
        void afterMapLayersLoaded(Object mapEngine, Object inputStream, boolean newGame);
    }

    @FunctionalInterface
    public interface AfterCurrentMapLoadedBeforeStartingUnits {
        void afterCurrentMapLoadedBeforeStartingUnits(Object gameEngine, Object mapEngine, boolean optionA, boolean optionB, Object mode);
    }
}
