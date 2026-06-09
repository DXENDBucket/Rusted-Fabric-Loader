package io.github.endx.rustedfabricapi.api.event;

import java.util.Properties;

public final class MapSpawnEvents {
    public static final RustedFabricEvent<BeforeMapObjectSpawnUnit> BEFORE_MAP_OBJECT_SPAWN_UNIT =
            RustedFabricEvent.create(listeners -> (mapObject, mapEngine, objectGroup, properties, unitName, customUnitName, teamName) -> {
                boolean cancelled = false;
                for (BeforeMapObjectSpawnUnit listener : listeners) {
                    cancelled |= listener.beforeMapObjectSpawnUnit(mapObject, mapEngine, objectGroup, properties, unitName, customUnitName, teamName);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<MapObjectCustomUnitResolveCallback> MAP_OBJECT_CUSTOM_UNIT_RESOLVE =
            RustedFabricEvent.create(listeners -> (mapObject, mapEngine, objectGroup, customUnitName, currentMetadata) -> {
                Object result = currentMetadata;
                for (MapObjectCustomUnitResolveCallback listener : listeners) {
                    Object replacement = listener.mapObjectCustomUnitResolve(mapObject, mapEngine, objectGroup, customUnitName, result);
                    if (replacement != null) {
                        result = replacement;
                    }
                }
                return result;
            });

    public static final RustedFabricEvent<AfterMapObjectSpawnUnit> AFTER_MAP_OBJECT_SPAWN_UNIT =
            RustedFabricEvent.create(listeners -> (mapObject, mapEngine, objectGroup, unit, properties) -> {
                for (AfterMapObjectSpawnUnit listener : listeners) {
                    listener.afterMapObjectSpawnUnit(mapObject, mapEngine, objectGroup, unit, properties);
                }
            });

    public static final RustedFabricEvent<BeforeTilePropertySpawnUnit> BEFORE_TILE_PROPERTY_SPAWN_UNIT =
            RustedFabricEvent.create(listeners -> (tileset, properties, propertyName, propertyValue) -> {
                boolean cancelled = false;
                for (BeforeTilePropertySpawnUnit listener : listeners) {
                    cancelled |= listener.beforeTilePropertySpawnUnit(tileset, properties, propertyName, propertyValue);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterTilePropertySpawnUnit> AFTER_TILE_PROPERTY_SPAWN_UNIT =
            RustedFabricEvent.create(listeners -> (tileset, properties, propertyName, propertyValue) -> {
                for (AfterTilePropertySpawnUnit listener : listeners) {
                    listener.afterTilePropertySpawnUnit(tileset, properties, propertyName, propertyValue);
                }
            });

    public static final RustedFabricEvent<BeforeStartingUnitSpawn> BEFORE_STARTING_UNIT_SPAWN =
            RustedFabricEvent.create(listeners -> (unitType, x, y, direction, height, team) -> {
                boolean cancelled = false;
                for (BeforeStartingUnitSpawn listener : listeners) {
                    cancelled |= listener.beforeStartingUnitSpawn(unitType, x, y, direction, height, team);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterStartingUnitSpawn> AFTER_STARTING_UNIT_SPAWN =
            RustedFabricEvent.create(listeners -> (unitType, x, y, direction, height, team, result) -> {
                boolean finalResult = result;
                for (AfterStartingUnitSpawn listener : listeners) {
                    finalResult = listener.afterStartingUnitSpawn(unitType, x, y, direction, height, team, finalResult);
                }
                return finalResult;
            });

    private MapSpawnEvents() {
    }

    @FunctionalInterface
    public interface BeforeMapObjectSpawnUnit {
        boolean beforeMapObjectSpawnUnit(Object mapObject, Object mapEngine, Object objectGroup, Properties properties, String unitName, String customUnitName, String teamName);
    }

    @FunctionalInterface
    public interface MapObjectCustomUnitResolveCallback {
        Object mapObjectCustomUnitResolve(Object mapObject, Object mapEngine, Object objectGroup, String customUnitName, Object currentMetadata);
    }

    @FunctionalInterface
    public interface AfterMapObjectSpawnUnit {
        void afterMapObjectSpawnUnit(Object mapObject, Object mapEngine, Object objectGroup, Object unit, Properties properties);
    }

    @FunctionalInterface
    public interface BeforeTilePropertySpawnUnit {
        boolean beforeTilePropertySpawnUnit(Object tileset, Properties properties, String propertyName, String propertyValue);
    }

    @FunctionalInterface
    public interface AfterTilePropertySpawnUnit {
        void afterTilePropertySpawnUnit(Object tileset, Properties properties, String propertyName, String propertyValue);
    }

    @FunctionalInterface
    public interface BeforeStartingUnitSpawn {
        boolean beforeStartingUnitSpawn(Object unitType, float x, float y, float direction, float height, Object team);
    }

    @FunctionalInterface
    public interface AfterStartingUnitSpawn {
        boolean afterStartingUnitSpawn(Object unitType, float x, float y, float direction, float height, Object team, boolean result);
    }
}
