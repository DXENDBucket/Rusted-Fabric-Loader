package io.github.endx.rustedfabricapi.api.unit.event;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import rustedwarfare.game.Team;
import rustedwarfare.unit.Unit;

/** Observable unit ownership changes made through the game's bookkeeping-aware change path. */
public final class UnitTeamEvents {
    public static final RustedFabricEvent<BeforeChange> BEFORE_CHANGE =
            RustedFabricEvent.create(listeners -> (unit, oldTeam, newTeam) -> {
                for (BeforeChange listener : listeners) listener.beforeChange(unit, oldTeam, newTeam);
            });
    public static final RustedFabricEvent<AfterChange> AFTER_CHANGE =
            RustedFabricEvent.create(listeners -> (unit, newTeam) -> {
                for (AfterChange listener : listeners) listener.afterChange(unit, newTeam);
            });

    private UnitTeamEvents() {
    }

    @FunctionalInterface
    public interface BeforeChange {
        void beforeChange(Unit unit, Team oldTeam, Team newTeam);
    }

    @FunctionalInterface
    public interface AfterChange {
        void afterChange(Unit unit, Team newTeam);
    }
}
