package io.github.endx.rustedfabricapi.api.unit.event;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import rustedwarfare.game.Team;
import rustedwarfare.unit.Unit;
import rustedwarfare.unit.UnitType;

/** Events around units created through {@code UnitSpawns}. */
public final class UnitSpawnEvents {
    public static final RustedFabricEvent<BeforeSpawn> BEFORE_SPAWN =
            RustedFabricEvent.create(listeners -> (type, team, x, y, height, direction) -> {
                boolean cancelled = false;
                for (BeforeSpawn listener : listeners) {
                    cancelled |= listener.beforeSpawn(type, team, x, y, height, direction);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterSpawn> AFTER_SPAWN =
            RustedFabricEvent.create(listeners -> (unit, type, team) -> {
                for (AfterSpawn listener : listeners) listener.afterSpawn(unit, type, team);
            });

    private UnitSpawnEvents() {
    }

    @FunctionalInterface
    public interface BeforeSpawn {
        /** Return true to cancel this API-driven spawn. All listeners are still called. */
        boolean beforeSpawn(UnitType type, Team team, float x, float y,
                float height, float direction);
    }

    @FunctionalInterface
    public interface AfterSpawn {
        void afterSpawn(Unit unit, UnitType type, Team team);
    }
}
