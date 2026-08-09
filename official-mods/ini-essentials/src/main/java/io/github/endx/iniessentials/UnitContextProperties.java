package io.github.endx.iniessentials;

import io.github.endx.rustedfabricapi.api.client.Selection;
import io.github.endx.rustedfabricapi.api.logic.LogicUnitProperties;
import io.github.endx.rustedfabricapi.api.logic.LogicUnitPropertyDefinition;
import io.github.endx.rustedfabricapi.api.unit.order.UnitOrders;

import java.util.concurrent.atomic.AtomicBoolean;

/** Live active-waypoint and local-selection values exposed as self.* expressions. */
final class UnitContextProperties {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();

    private UnitContextProperties() { }

    static void register() {
        if (!REGISTERED.compareAndSet(false, true)) return;
        LogicUnitProperties.register(LogicUnitPropertyDefinition.stringProperty(
                "self.activeWaypointType",
                LogicUnitPropertyDefinition.Locality.SYNCHRONIZED,
                UnitOrders::activeTypeName));
        LogicUnitProperties.register(LogicUnitPropertyDefinition.numberProperty(
                "self.activeWaypointX",
                LogicUnitPropertyDefinition.Locality.SYNCHRONIZED,
                UnitOrders::activeTargetX));
        LogicUnitProperties.register(LogicUnitPropertyDefinition.numberProperty(
                "self.activeWaypointY",
                LogicUnitPropertyDefinition.Locality.SYNCHRONIZED,
                UnitOrders::activeTargetY));
        LogicUnitProperties.register(LogicUnitPropertyDefinition.numberProperty(
                "self.activeWaypointRelativeX",
                LogicUnitPropertyDefinition.Locality.SYNCHRONIZED,
                UnitOrders::activeTargetRelativeX));
        LogicUnitProperties.register(LogicUnitPropertyDefinition.numberProperty(
                "self.activeWaypointRelativeY",
                LogicUnitPropertyDefinition.Locality.SYNCHRONIZED,
                UnitOrders::activeTargetRelativeY));
        LogicUnitProperties.register(LogicUnitPropertyDefinition.booleanProperty(
                "self.isSelectedByLocalPlayer",
                LogicUnitPropertyDefinition.Locality.CLIENT_LOCAL,
                Selection::isSelected));
    }
}
