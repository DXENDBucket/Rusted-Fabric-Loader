package io.github.endx.rustedfabricapi.api.unit.order;

import rustedwarfare.unit.OrderableUnit;
import rustedwarfare.unit.Unit;
import rustedwarfare.unit.UnitAttackMode;
import rustedwarfare.unit.UnitOrder;
import rustedwarfare.util.CommonUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Typed access to a unit's runtime waypoint queue.
 *
 * <p>Mutating methods are direct simulation operations. Call them on the game update thread and
 * only from deterministic logic shared by every multiplayer peer. Player-visible orders should
 * normally use {@code api.command.Commands}, which follows the synchronized command path.</p>
 */
public final class UnitOrders {
    private UnitOrders() {
    }

    public static int size(OrderableUnit unit) {
        return require(unit).getWaypointCount();
    }

    public static boolean isEmpty(OrderableUnit unit) {
        return require(unit).hasNoWaypoints();
    }

    public static UnitOrder active(OrderableUnit unit) {
        return require(unit).getActiveWaypoint();
    }

    /** Returns the native active-order enum name, or {@code "none"}. */
    public static String activeTypeName(OrderableUnit unit) {
        OrderableUnit checked = require(unit);
        UnitOrder order = checked.getActiveWaypoint();
        return order != null && order.getOrderType() != null
                ? order.getOrderType().name() : "none";
    }

    /** Returns the live native target X, or the unit's own X when no order is active. */
    public static float activeTargetX(OrderableUnit unit) {
        OrderableUnit checked = require(unit);
        UnitOrder order = checked.getActiveWaypoint();
        return order != null ? order.getTargetX() : ((Unit) checked).x;
    }

    /** Returns the live native target Y, or the unit's own Y when no order is active. */
    public static float activeTargetY(OrderableUnit unit) {
        OrderableUnit checked = require(unit);
        UnitOrder order = checked.getActiveWaypoint();
        return order != null ? order.getTargetY() : ((Unit) checked).y;
    }

    /** Returns the target on the native lateral/right unit-relative X axis. */
    public static float activeTargetRelativeX(OrderableUnit unit) {
        OrderableUnit checked = require(unit);
        if (checked.getActiveWaypoint() == null) return 0.0F;
        float dx = activeTargetX(checked) - ((Unit) checked).x;
        float dy = activeTargetY(checked) - ((Unit) checked).y;
        float sin = CommonUtils.fastSin(checked.direction);
        float cos = CommonUtils.fastCos(checked.direction);
        return -sin * dx + cos * dy;
    }

    /** Returns the target on the native forward unit-relative Y axis. */
    public static float activeTargetRelativeY(OrderableUnit unit) {
        OrderableUnit checked = require(unit);
        if (checked.getActiveWaypoint() == null) return 0.0F;
        float dx = activeTargetX(checked) - ((Unit) checked).x;
        float dy = activeTargetY(checked) - ((Unit) checked).y;
        float sin = CommonUtils.fastSin(checked.direction);
        float cos = CommonUtils.fastCos(checked.direction);
        return cos * dx + sin * dy;
    }

    public static UnitOrder next(OrderableUnit unit) {
        return require(unit).getNextWaypoint();
    }

    public static UnitOrder last(OrderableUnit unit) {
        return require(unit).getLastWaypoint();
    }

    public static UnitOrder get(OrderableUnit unit, int index) {
        OrderableUnit checked = require(unit);
        int count = checked.getWaypointCount();
        if (index < 0 || index >= count) {
            throw new IndexOutOfBoundsException("waypointIndex=" + index + ", count=" + count);
        }
        return checked.getWaypointAt(index);
    }

    public static List<UnitOrderSnapshot> snapshot(OrderableUnit unit) {
        OrderableUnit checked = require(unit);
        int count = checked.getWaypointCount();
        List<UnitOrderSnapshot> result = new ArrayList<UnitOrderSnapshot>(count);
        for (int i = 0; i < count; i++) {
            UnitOrder order = checked.getWaypointAt(i);
            if (order != null) result.add(UnitOrderSnapshot.capture(order));
        }
        return Collections.unmodifiableList(result);
    }

    public static UnitAttackMode attackMode(OrderableUnit unit) {
        return require(unit).attackMode;
    }

    public static void setAttackMode(OrderableUnit unit, UnitAttackMode mode) {
        require(unit).attackMode = Objects.requireNonNull(mode, "mode");
    }

    public static UnitOrder queueMove(OrderableUnit unit, float x, float y) {
        return require(unit).queueMoveWaypoint(x, y);
    }

    public static UnitOrder queueAttackMove(OrderableUnit unit, float x, float y) {
        return require(unit).queueAttackMoveWaypoint(x, y);
    }

    public static UnitOrder queueAttack(OrderableUnit unit, Unit target) {
        return require(unit).queueAttackWaypoint(Objects.requireNonNull(target, "target"));
    }

    public static UnitOrder queueRepair(OrderableUnit unit, Unit target) {
        UnitOrder order = require(unit).appendWaypoint();
        order.setRepairTarget(Objects.requireNonNull(target, "target"));
        return order;
    }

    public static UnitOrder queueReclaim(OrderableUnit unit, Unit target) {
        UnitOrder order = require(unit).appendWaypoint();
        order.setReclaimTarget(Objects.requireNonNull(target, "target"));
        return order;
    }

    public static UnitOrder queueGuard(OrderableUnit unit, Unit target) {
        UnitOrder order = require(unit).appendWaypoint();
        order.setGuardTarget(Objects.requireNonNull(target, "target"));
        return order;
    }

    public static UnitOrder queuePatrol(OrderableUnit unit, float x, float y) {
        UnitOrder order = require(unit).appendWaypoint();
        order.setPatrol(x, y);
        return order;
    }

    public static UnitOrder queueFollow(OrderableUnit unit, Unit target) {
        UnitOrder order = require(unit).appendWaypoint();
        order.setFollowTarget(Objects.requireNonNull(target, "target"));
        return order;
    }

    public static void completeActive(OrderableUnit unit) {
        require(unit).completeActiveWaypoint();
    }

    public static void complete(OrderableUnit unit, int index) {
        require(unit).completeWaypointAt(index);
    }

    public static void removeLast(OrderableUnit unit) {
        require(unit).removeLastWaypoint();
    }

    public static void clear(OrderableUnit unit) {
        require(unit).clearAllWaypoints();
    }

    public static void clearExceptBuildAndRepair(OrderableUnit unit) {
        require(unit).clearNonBuildAndNonRepairWaypoints();
    }

    private static OrderableUnit require(OrderableUnit unit) {
        return Objects.requireNonNull(unit, "unit");
    }
}
