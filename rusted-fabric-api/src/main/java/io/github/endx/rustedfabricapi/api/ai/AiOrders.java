package io.github.endx.rustedfabricapi.api.ai;

import io.github.endx.rustedfabricapi.api.command.Commands;
import io.github.endx.rustedfabricapi.api.game.UnitView;
import rustedwarfare.ai.AiTeam;
import rustedwarfare.command.Command;
import rustedwarfare.unit.OrderableUnit;
import rustedwarfare.unit.Unit;
import rustedwarfare.unit.UnitType;

import java.util.ArrayList;
import java.util.List;

/** Synchronized order helpers scoped to one AI team. */
public final class AiOrders {
    private final AiTeam team;

    AiOrders(AiTeam team) {
        this.team = team;
    }

    public Command move(Iterable<UnitView> units, float x, float y) {
        return Commands.move(team, ownedOrderable(units), x, y);
    }

    public Command attackMove(Iterable<UnitView> units, float x, float y) {
        return Commands.attackMove(team, ownedOrderable(units), x, y);
    }

    public Command attack(Iterable<UnitView> units, UnitView target) {
        return Commands.attack(team, ownedOrderable(units), rawUnit(target, "target"));
    }

    public Command repair(Iterable<UnitView> units, UnitView target) {
        return Commands.repair(team, ownedOrderable(units), rawUnit(target, "target"));
    }

    public Command guard(Iterable<UnitView> units, UnitView target) {
        return Commands.guard(team, ownedOrderable(units), rawUnit(target, "target"));
    }

    public Command patrol(Iterable<UnitView> units, float x, float y) {
        return Commands.patrol(team, ownedOrderable(units), x, y);
    }

    public Command reclaim(Iterable<UnitView> units, UnitView target) {
        return Commands.reclaim(team, ownedOrderable(units), rawUnit(target, "target"));
    }

    public Command build(Iterable<UnitView> builders, float x, float y,
            UnitType type, int variant) {
        if (type == null) throw new IllegalArgumentException("type must not be null");
        Command command = Commands.create(team, ownedOrderable(builders));
        command.setBuildOrder(x, y, type, variant);
        return Commands.issue(command);
    }

    private List<OrderableUnit> ownedOrderable(Iterable<UnitView> units) {
        if (units == null) throw new IllegalArgumentException("units must not be null");
        List<OrderableUnit> result = new ArrayList<OrderableUnit>();
        for (UnitView view : units) {
            Unit raw = rawUnit(view, "units contains null");
            if (!(raw instanceof OrderableUnit)) {
                throw new IllegalArgumentException("Unit " + view.id() + " is not orderable");
            }
            if (raw.team != team) {
                throw new IllegalArgumentException("Unit " + view.id()
                        + " is not owned by AI team " + team.teamId);
            }
            result.add((OrderableUnit) raw);
        }
        if (result.isEmpty()) throw new IllegalArgumentException("units must not be empty");
        return result;
    }

    private static Unit rawUnit(UnitView view, String message) {
        if (view == null) throw new IllegalArgumentException(message);
        Object raw = view.raw();
        if (!(raw instanceof Unit)) {
            throw new IllegalArgumentException("Unit view is not backed by the active game namespace");
        }
        return (Unit) raw;
    }
}
