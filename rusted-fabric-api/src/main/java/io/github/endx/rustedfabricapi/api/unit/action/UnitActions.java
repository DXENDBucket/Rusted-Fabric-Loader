package io.github.endx.rustedfabricapi.api.unit.action;

import android.graphics.PointF;
import io.github.endx.rustedfabricapi.api.command.Commands;
import rustedwarfare.command.Command;
import rustedwarfare.game.Team;
import rustedwarfare.unit.OrderableUnit;
import rustedwarfare.unit.Unit;
import rustedwarfare.unit.UnitType;
import rustedwarfare.unit.action.ActionCommandType;
import rustedwarfare.unit.action.PlaceBuildingAction;
import rustedwarfare.unit.action.UnitAction;
import rustedwarfare.unit.action.UnitActionId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Discovery and synchronized execution helpers for unit actions. */
public final class UnitActions {
    private UnitActions() {
    }

    public static List<UnitAction> forType(UnitType type, int techLevel) {
        Objects.requireNonNull(type, "type");
        List<?> values = type.getActionsForTechLevel(techLevel);
        if (values == null || values.isEmpty()) return Collections.emptyList();
        List<UnitAction> result = new ArrayList<UnitAction>(values.size());
        for (Object value : values) {
            if (value instanceof UnitAction) result.add((UnitAction) value);
        }
        return Collections.unmodifiableList(result);
    }

    /** Returns all actions declared for the unit's current type and tech level. */
    public static List<UnitAction> forUnit(Unit unit) {
        Objects.requireNonNull(unit, "unit");
        UnitType type = unit.r();
        return type == null ? Collections.emptyList() : forType(type, unit.getTechLevel());
    }

    public static List<UnitAction> visible(Unit unit) {
        List<UnitAction> result = new ArrayList<UnitAction>();
        for (UnitAction action : forUnit(unit)) {
            if (action.isVisible(unit)) result.add(action);
        }
        return Collections.unmodifiableList(result);
    }

    public static List<UnitAction> available(Unit unit) {
        List<UnitAction> result = new ArrayList<UnitAction>();
        for (UnitAction action : forUnit(unit)) {
            if (canRun(unit, action)) {
                result.add(action);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public static Optional<UnitAction> find(Unit unit, String actionId) {
        Objects.requireNonNull(actionId, "actionId");
        for (UnitAction action : forUnit(unit)) {
            if (actionId.equals(action.getActionIdString())) return Optional.of(action);
        }
        return Optional.empty();
    }

    public static boolean canRun(Unit unit, UnitAction action) {
        Objects.requireNonNull(unit, "unit");
        Objects.requireNonNull(action, "action");
        // Native UI and AI command paths both gate execution with visibility and the
        // resource/lock-aware active check. The mapped isAvailable method reports the
        // action filter's locked-state branch and is false for ordinary stock queue actions.
        return action.isVisible(unit) && action.isActiveAndQueueAllowed(unit, false);
    }

    /**
     * Returns whether the action enters the game's building-placement command path.
     *
     * <p>This is intentionally separate from {@link UnitAction#isBuildAction()}: the latter means
     * a queued production action in Rusted Warfare, and returns {@code false} for the stock
     * {@link PlaceBuildingAction}.</p>
     */
    public static boolean isBuildingPlacement(UnitAction action) {
        Objects.requireNonNull(action, "action");
        return action.getActionCommandType() == ActionCommandType.placeBuilding
                && action.getBuildUnitType() != null
                && action.getBuildUnitType().isBuilding();
    }

    /** Returns the native build variant encoded by a building-placement action. */
    public static int buildingVariant(UnitAction action) {
        if (!isBuildingPlacement(action)) {
            throw new IllegalArgumentException("action is not a building-placement action");
        }
        return action instanceof PlaceBuildingAction
                ? Math.max(1, ((PlaceBuildingAction) action).getTechLevel()) : 1;
    }

    /** Issues a building placement through the native synchronized build-order path. */
    public static Command issueBuilding(Team team, Iterable<? extends OrderableUnit> builders,
            UnitAction action, float x, float y) {
        Objects.requireNonNull(action, "action");
        if (!isBuildingPlacement(action)) {
            throw new IllegalArgumentException("action is not a building-placement action");
        }
        if (!Float.isFinite(x) || !Float.isFinite(y)) {
            throw new IllegalArgumentException("target coordinates must be finite");
        }
        Command command = Commands.create(team, builders);
        command.setBuildOrder(x, y, action.getBuildUnitType(), buildingVariant(action));
        return Commands.issue(command);
    }

    /** Issues a non-targeted action through the synchronized command controller. */
    public static Command issue(Team team, Iterable<? extends OrderableUnit> units,
                                UnitAction action) {
        Objects.requireNonNull(action, "action");
        Command command = Commands.create(team, units);
        command.setActionId(requireActionId(action));
        return Commands.issue(command);
    }

    /** Issues a point/unit-targeted action without exposing the game's Android PointF shim. */
    public static Command issueAt(Team team, Iterable<? extends OrderableUnit> units,
                                  UnitAction action, float x, float y, Unit target) {
        Objects.requireNonNull(action, "action");
        if (!Float.isFinite(x) || !Float.isFinite(y)) {
            throw new IllegalArgumentException("target coordinates must be finite");
        }
        Command command = Commands.create(team, units);
        command.setActionTarget(requireActionId(action), new PointF(x, y), target);
        return Commands.issue(command);
    }

    private static UnitActionId requireActionId(UnitAction action) {
        UnitActionId id = action.getActionIdForSerialization();
        if (id == null) throw new IllegalArgumentException("action has no command id");
        return id;
    }
}
