package io.github.endx.rustedfabricapi.api.unit.action;

import rustedwarfare.unit.Unit;
import rustedwarfare.unit.UnitType;
import rustedwarfare.unit.action.ActionCommandType;
import rustedwarfare.unit.action.ActionDisplayType;
import rustedwarfare.unit.action.UnitAction;

import java.lang.reflect.Proxy;

public final class BuildingPlacementContractVerification {
    private BuildingPlacementContractVerification() {
    }

    public static void verify() {
        UnitAction placement = new FakeAction(unitType(true),
                ActionCommandType.placeBuilding, false);
        require(!placement.isBuildAction(),
                "native placement must remain distinct from queued production");
        require(UnitActions.isBuildingPlacement(placement),
                "native place-building action was not recognized");
        require(UnitActions.buildingVariant(placement) == 1,
                "generic placement must use the base build variant");

        UnitAction production = new FakeAction(unitType(false),
                ActionCommandType.none, true);
        require(production.isBuildAction(), "queued unit action contract changed");
        require(!UnitActions.isBuildingPlacement(production),
                "queued unit production was mistaken for building placement");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static UnitType unitType(boolean building) {
        return (UnitType) Proxy.newProxyInstance(UnitType.class.getClassLoader(),
                new Class<?>[]{UnitType.class}, (proxy, method, args) -> {
                    if ("isBuilding".equals(method.getName())) return building;
                    Class<?> result = method.getReturnType();
                    if (result == boolean.class) return false;
                    if (result == int.class) return 0;
                    if (result == float.class) return 0.0F;
                    return null;
                });
    }

    private static final class FakeAction extends UnitAction {
        private final UnitType type;
        private final ActionCommandType commandType;
        private final boolean queuedBuild;

        FakeAction(UnitType type, ActionCommandType commandType, boolean queuedBuild) {
            super("placement_contract");
            this.type = type;
            this.commandType = commandType;
            this.queuedBuild = queuedBuild;
        }

        @Override public String getText() { return "test"; }
        @Override public String getDescription() { return "test"; }
        @Override public int getCreditCost() { return 0; }
        @Override public int getDisplayQueueCount(Unit unit, boolean includePending) { return 0; }
        @Override public UnitType getBuildUnitType() { return type; }
        @Override public boolean isBuildAction() { return queuedBuild; }
        @Override public ActionCommandType getActionCommandType() { return commandType; }
        @Override public ActionDisplayType getDisplayType() { return ActionDisplayType.none; }
        @Override public int compareTo(Object other) { return 0; }
    }
}
