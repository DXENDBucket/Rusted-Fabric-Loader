package io.github.endx.rustedfabricapi.api.unit.action;

import io.github.endx.rustedfabricapi.api.world.WorldPoint;
import rustedwarfare.unit.Unit;

/** Deterministic per-unit validation for a proposed Java action target. */
@FunctionalInterface
public interface JavaUnitActionTargetPredicate {
    boolean canTarget(Unit unit, WorldPoint target);
}
