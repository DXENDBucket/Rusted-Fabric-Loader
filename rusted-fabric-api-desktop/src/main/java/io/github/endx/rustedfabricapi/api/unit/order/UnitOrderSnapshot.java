package io.github.endx.rustedfabricapi.api.unit.order;

import rustedwarfare.unit.Unit;
import rustedwarfare.unit.UnitOrder;
import rustedwarfare.unit.UnitOrderType;
import rustedwarfare.unit.UnitType;

import java.util.Objects;

/** Immutable view of a unit waypoint/order at one instant. */
public final class UnitOrderSnapshot {
    private final UnitOrderType type;
    private final float targetX;
    private final float targetY;
    private final Unit targetUnit;
    private final boolean hasUnitTarget;
    private final UnitType buildUnitType;
    private final int buildIndex;
    private final boolean queuedByPlayer;
    private final float maxTime;
    private final float expiresAtTime;
    private final boolean repeatable;
    private final boolean skipAvailabilityChecks;

    private UnitOrderSnapshot(UnitOrder order) {
        Objects.requireNonNull(order, "order");
        this.type = order.getOrderType();
        this.targetX = order.getTargetX();
        this.targetY = order.getTargetY();
        this.targetUnit = order.getTargetUnit();
        this.hasUnitTarget = order.hasUnitTarget();
        this.buildUnitType = order.getBuildUnitType();
        this.buildIndex = order.getBuildIndex();
        this.queuedByPlayer = order.queueByPlayer;
        this.maxTime = order.maxTime;
        this.expiresAtTime = order.expiresAtTime;
        this.repeatable = order.repeatable;
        this.skipAvailabilityChecks = order.skipAvailabilityChecks;
    }

    public static UnitOrderSnapshot capture(UnitOrder order) {
        return new UnitOrderSnapshot(order);
    }

    public UnitOrderType type() { return type; }
    public float targetX() { return targetX; }
    public float targetY() { return targetY; }
    public Unit targetUnit() { return targetUnit; }
    public UnitType buildUnitType() { return buildUnitType; }
    public int buildIndex() { return buildIndex; }
    public boolean queuedByPlayer() { return queuedByPlayer; }
    public float maxTime() { return maxTime; }
    public float expiresAtTime() { return expiresAtTime; }
    public boolean repeatable() { return repeatable; }
    public boolean skipAvailabilityChecks() { return skipAvailabilityChecks; }
    public boolean hasUnitTarget() { return hasUnitTarget; }
}
