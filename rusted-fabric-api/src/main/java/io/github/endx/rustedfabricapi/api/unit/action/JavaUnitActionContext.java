package io.github.endx.rustedfabricapi.api.unit.action;

import java.util.Objects;
import java.util.Optional;

import io.github.endx.rustedfabricapi.api.world.WorldPoint;
import rustedwarfare.unit.OrderableUnit;
import rustedwarfare.unit.Unit;

/** Immutable per-unit execution context produced by a synchronized action command. */
public final class JavaUnitActionContext {
    private final JavaUnitAction action;
    private final OrderableUnit unit;
    private final boolean queued;
    private final WorldPoint targetPoint;
    private final Unit targetUnit;

    public JavaUnitActionContext(JavaUnitAction action, OrderableUnit unit, boolean queued,
            WorldPoint targetPoint, Unit targetUnit) {
        this.action = Objects.requireNonNull(action, "action");
        this.unit = Objects.requireNonNull(unit, "unit");
        this.queued = queued;
        this.targetPoint = targetPoint;
        this.targetUnit = targetUnit;
    }

    public JavaUnitAction action() { return action; }
    public OrderableUnit unit() { return unit; }
    public boolean queued() { return queued; }
    public Optional<WorldPoint> targetPoint() { return Optional.ofNullable(targetPoint); }
    public Optional<Unit> targetUnit() { return Optional.ofNullable(targetUnit); }

    @Override
    public String toString() {
        return "JavaUnitActionContext{" + action.id() + ", unit=" + unit
                + ", queued=" + queued + '}';
    }
}
