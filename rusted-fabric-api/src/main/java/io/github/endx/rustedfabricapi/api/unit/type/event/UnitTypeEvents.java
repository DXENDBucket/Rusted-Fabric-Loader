package io.github.endx.rustedfabricapi.api.unit.type.event;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import rustedwarfare.custom.CustomUnitMetadata;
import rustedwarfare.game.Team;
import rustedwarfare.unit.Unit;
import rustedwarfare.unit.UnitType;

/** Strongly typed creation and starting-spawn events for unit types. */
public final class UnitTypeEvents {
    public static final RustedFabricEvent<BeforeStartingSpawn> BEFORE_STARTING_SPAWN =
            RustedFabricEvent.create(listeners -> (type, x, y, direction, height, team) -> {
                boolean cancelled = false;
                for (BeforeStartingSpawn listener : listeners) {
                    cancelled |= listener.beforeSpawn(type, x, y, direction, height, team);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterStartingSpawn> AFTER_STARTING_SPAWN =
            RustedFabricEvent.create(listeners -> (type, x, y, direction, height, team, current) -> {
                boolean result = current;
                for (AfterStartingSpawn listener : listeners) {
                    result = listener.afterSpawn(type, x, y, direction, height, team, result);
                }
                return result;
            });

    public static final RustedFabricEvent<BeforeCustomCreate> BEFORE_CUSTOM_CREATE =
            RustedFabricEvent.create(listeners -> metadata -> {
                for (BeforeCustomCreate listener : listeners) listener.beforeCreate(metadata);
            });
    public static final RustedFabricEvent<AfterCustomCreate> AFTER_CUSTOM_CREATE =
            RustedFabricEvent.create(listeners -> (metadata, unit) -> {
                Unit result = unit;
                for (AfterCustomCreate listener : listeners) {
                    Unit replacement = listener.afterCreate(metadata, result);
                    if (replacement != null) result = replacement;
                }
                return result;
            });
    public static final RustedFabricEvent<BeforeCustomCreateWithFlag> BEFORE_CUSTOM_CREATE_WITH_FLAG =
            RustedFabricEvent.create(listeners -> (metadata, flag) -> {
                for (BeforeCustomCreateWithFlag listener : listeners) {
                    listener.beforeCreate(metadata, flag);
                }
            });
    public static final RustedFabricEvent<AfterCustomCreateWithFlag> AFTER_CUSTOM_CREATE_WITH_FLAG =
            RustedFabricEvent.create(listeners -> (metadata, flag, unit) -> {
                Unit result = unit;
                for (AfterCustomCreateWithFlag listener : listeners) {
                    Unit replacement = listener.afterCreate(metadata, flag, result);
                    if (replacement != null) result = replacement;
                }
                return result;
            });

    private UnitTypeEvents() {
    }

    public interface BeforeStartingSpawn {
        boolean beforeSpawn(UnitType type, float x, float y, float direction, float height,
                            Team team);
    }

    public interface AfterStartingSpawn {
        boolean afterSpawn(UnitType type, float x, float y, float direction, float height,
                           Team team, boolean currentResult);
    }

    public interface BeforeCustomCreate {
        void beforeCreate(CustomUnitMetadata metadata);
    }

    public interface AfterCustomCreate {
        /** Return {@code null} to keep the current unit. */
        Unit afterCreate(CustomUnitMetadata metadata, Unit currentUnit);
    }

    public interface BeforeCustomCreateWithFlag {
        void beforeCreate(CustomUnitMetadata metadata, boolean createFlag);
    }

    public interface AfterCustomCreateWithFlag {
        /** Return {@code null} to keep the current unit. */
        Unit afterCreate(CustomUnitMetadata metadata, boolean createFlag, Unit currentUnit);
    }
}
