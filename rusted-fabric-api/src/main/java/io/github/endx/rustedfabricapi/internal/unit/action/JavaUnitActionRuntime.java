package io.github.endx.rustedfabricapi.internal.unit.action;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.graphics.PointF;
import io.github.endx.rustedfabricapi.api.unit.action.JavaUnitAction;
import io.github.endx.rustedfabricapi.api.unit.action.JavaUnitActionContext;
import io.github.endx.rustedfabricapi.api.unit.action.JavaUnitActionTargeting;
import io.github.endx.rustedfabricapi.api.unit.action.JavaUnitActions;
import io.github.endx.rustedfabricapi.api.unit.action.event.JavaUnitActionEvents;
import io.github.endx.rustedfabricapi.api.resource.Resources;
import io.github.endx.rustedfabricapi.api.unit.status.StatusEffects;
import io.github.endx.rustedfabricapi.api.world.WorldPoint;
import rustedwarfare.unit.OrderableUnit;
import rustedwarfare.unit.Unit;
import rustedwarfare.unit.UnitType;
import rustedwarfare.unit.action.UnitAction;

/** Internal native-list and command execution bridge. */
public final class JavaUnitActionRuntime {
    private JavaUnitActionRuntime() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static ArrayList append(UnitType type, int techLevel, ArrayList original) {
        List<JavaUnitAction> additions = JavaUnitActions.forType(type, techLevel);
        if (additions.isEmpty()) return original;
        ArrayList result = original != null ? new ArrayList(original) : new ArrayList();
        Set<String> ids = new HashSet<String>();
        for (Object raw : result) {
            if (raw instanceof UnitAction) ids.add(((UnitAction) raw).getActionIdString());
        }
        for (JavaUnitAction action : additions) {
            if (ids.add(action.getActionIdString())) result.add(action);
        }
        return result;
    }

    public static boolean execute(Unit unit, UnitAction action, boolean queued,
            PointF targetPoint, Unit targetUnit) {
        if (!(unit instanceof OrderableUnit) || !(action instanceof JavaUnitAction)) return false;
        JavaUnitAction javaAction = (JavaUnitAction) action;
        OrderableUnit orderable = (OrderableUnit) unit;
        WorldPoint point = toWorldPoint(targetPoint);
        if (targetPoint != null && point == null) return true;
        if (javaAction.targeting() == JavaUnitActionTargeting.IMMEDIATE) {
            if (point != null || targetUnit != null) return true;
        } else if (javaAction.targeting() == JavaUnitActionTargeting.WORLD_POINT) {
            if (point == null || targetUnit != null || !javaAction.canTarget(unit, point)) return true;
        }
        JavaUnitActionContext context = new JavaUnitActionContext(javaAction, orderable, queued,
                point, targetUnit);
        if (!javaAction.isVisible(unit) || !javaAction.isAvailable(unit)
                || !javaAction.isActiveAndQueueAllowed(unit, false)) return true;
        if (JavaUnitActionEvents.BEFORE_EXECUTE.invoker().beforeExecute(context)) return true;
        if (!Resources.tryPay(orderable, javaAction.getPrice())) return true;
        if (javaAction.cooldownMillis() > 0) {
            StatusEffects.blockAction(orderable, javaAction.getActionId(),
                    javaAction.cooldownMillis());
        }
        javaAction.execute(context);
        JavaUnitActionEvents.AFTER_EXECUTE.invoker().afterExecute(context);
        return true;
    }

    /** Returns null for native actions, otherwise the native convention: true means allowed. */
    public static Boolean targetedActionAllowed(Unit unit, UnitAction action, float x, float y) {
        if (!(action instanceof JavaUnitAction)) return null;
        JavaUnitAction javaAction = (JavaUnitAction) action;
        if (javaAction.targeting() != JavaUnitActionTargeting.WORLD_POINT
                || unit == null || !Float.isFinite(x) || !Float.isFinite(y)) {
            return Boolean.FALSE;
        }
        return Boolean.valueOf(javaAction.canTarget(unit, new WorldPoint(x, y)));
    }

    private static WorldPoint toWorldPoint(PointF point) {
        if (point == null || !Float.isFinite(point.a) || !Float.isFinite(point.b)) return null;
        return new WorldPoint(point.a, point.b);
    }
}
