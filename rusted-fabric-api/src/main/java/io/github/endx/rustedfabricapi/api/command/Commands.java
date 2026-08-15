package io.github.endx.rustedfabricapi.api.command;

import io.github.endx.rustedfabricapi.api.client.RustedWarfareClient;
import rustedwarfare.command.Command;
import rustedwarfare.command.CommandController;
import rustedwarfare.core.GameEngine;
import rustedwarfare.game.Team;
import rustedwarfare.unit.OrderableUnit;
import rustedwarfare.unit.Unit;

import java.util.Objects;

/** Helpers for constructing commands through the game's normal synchronized command path. */
public final class Commands {
    private Commands() {
    }

    public static Command create(Team team) {
        Objects.requireNonNull(team, "team");
        GameEngine engine = RustedWarfareClient.requireEngine();
        CommandController controller = engine.commandController;
        if (controller == null) {
            throw new IllegalStateException("The command controller is not initialized yet");
        }
        return controller.createCommandForTeam(team);
    }

    public static Command create(Team team, Iterable<? extends OrderableUnit> units) {
        Command command = create(team);
        addUnits(command, units);
        return command;
    }

    public static Command addUnits(Command command, Iterable<? extends OrderableUnit> units) {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(units, "units");
        for (OrderableUnit unit : units) {
            command.addUnit(Objects.requireNonNull(unit, "units contains null"));
        }
        return command;
    }

    public static Command move(Team team, Iterable<? extends OrderableUnit> units,
                               float x, float y) {
        Command command = create(team, units);
        command.setMoveOrder(x, y);
        return issue(command);
    }

    public static Command attackMove(Team team, Iterable<? extends OrderableUnit> units,
                                     float x, float y) {
        Command command = create(team, units);
        command.setAttackMoveOrder(x, y);
        return issue(command);
    }

    public static Command attack(Team team, Iterable<? extends OrderableUnit> units,
                                 Unit target) {
        Objects.requireNonNull(target, "target");
        Command command = create(team, units);
        command.setAttackTargetOrder(target);
        return issue(command);
    }

    public static Command repair(Team team, Iterable<? extends OrderableUnit> units,
                                 Unit target) {
        Command command = create(team, units);
        command.setRepairTargetOrder(Objects.requireNonNull(target, "target"));
        return issue(command);
    }

    public static Command guard(Team team, Iterable<? extends OrderableUnit> units,
                                Unit target) {
        Command command = create(team, units);
        command.setGuardTargetOrder(Objects.requireNonNull(target, "target"));
        return issue(command);
    }

    public static Command patrol(Team team, Iterable<? extends OrderableUnit> units,
                                 float x, float y) {
        Command command = create(team, units);
        command.setPatrolOrder(x, y);
        return issue(command);
    }

    /** Issues the mapped command whose verified runtime meaning is reclaim. */
    public static Command reclaim(Team team, Iterable<? extends OrderableUnit> units,
                                  Unit target) {
        Command command = create(team, units);
        command.setTouchTargetOrder(Objects.requireNonNull(target, "target"));
        return issue(command);
    }

    /** Issues the mapped command whose verified runtime meaning is load into a carrier. */
    public static Command loadInto(Team team, Iterable<? extends OrderableUnit> units,
                                   Unit carrier) {
        Command command = create(team, units);
        command.setFollowTargetOrder(Objects.requireNonNull(carrier, "carrier"));
        return issue(command);
    }

    public static Command loadUp(Team team, Iterable<? extends OrderableUnit> units,
                                 Unit target) {
        Command command = create(team, units);
        command.setLoadUpTargetOrder(Objects.requireNonNull(target, "target"));
        return issue(command);
    }

    /**
     * Finishes configuring a command created through this API.
     *
     * <p>The native {@link CommandController#createCommandForTeam(Team)} method queues the new
     * command immediately. Native callers then populate that queued command and let the command
     * controller execute it at the synchronized command boundary. Calling
     * {@link Command#issueCommand()} here would execute it once immediately and a second time when
     * the controller drains its queue.</p>
     */
    public static Command issue(Command command) {
        return Objects.requireNonNull(command, "command");
    }
}
