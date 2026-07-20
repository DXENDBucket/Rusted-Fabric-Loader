package io.github.endx.rustedfabricapi.api.unit.repair;

import rustedwarfare.custom.CustomTagList;
import rustedwarfare.custom.resource.ResourceAmount;
import rustedwarfare.unit.OrderableUnit;
import rustedwarfare.unit.Unit;

import java.util.Objects;

/** Typed repair, construction and reclaim queries. */
public final class RepairReclaim {
    private RepairReclaim() {
    }

    public static Unit activeTarget(OrderableUnit unit) {
        return require(unit).getActiveRepairOrReclaimTarget();
    }

    public static boolean canRepair(OrderableUnit unit, Unit target) {
        return require(unit).canRepairTarget(Objects.requireNonNull(target, "target"));
    }

    public static boolean canReclaim(OrderableUnit unit, Unit target) {
        return require(unit).canReclaimUnitTarget(Objects.requireNonNull(target, "target"));
    }

    public static float buildProgressSpeed(OrderableUnit unit, Unit target) {
        return require(unit).getBuildProgressSpeedForTarget(Objects.requireNonNull(target, "target"));
    }

    public static float unbuildSpeed(OrderableUnit unit, Unit target) {
        return require(unit).getUnbuildSpeedForTarget(Objects.requireNonNull(target, "target"));
    }

    public static ResourceAmount buildPrice(OrderableUnit unit, Unit target) {
        return require(unit).getBuildPriceForTarget(Objects.requireNonNull(target, "target"));
    }

    public static ResourceAmount baseReclaimPrice(Unit unit) {
        return Objects.requireNonNull(unit, "unit").getBaseReclaimPrice();
    }

    public static ResourceAmount reclaimPriceOverride(Unit unit) {
        return Objects.requireNonNull(unit, "unit").getReclaimPriceOverride();
    }

    public static int maxConcurrentReclaimers(Unit unit) {
        return Objects.requireNonNull(unit, "unit").getResourceMaxConcurrentReclaimingThis();
    }

    public static ResourceAmount activeResourceDelta(OrderableUnit unit) {
        return require(unit).getRepairReclaimResourceDelta();
    }

    public static Unit findNearestResourceTarget(OrderableUnit searcher, float x, float y,
                                                  float range, CustomTagList requiredTags) {
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(range) || range < 0.0F) {
            throw new IllegalArgumentException("coordinates and non-negative range must be finite");
        }
        return OrderableUnit.findNearestReclaimResourceTarget(
                require(searcher), x, y, range, requiredTags);
    }

    /** Direct simulation mutation; call only from deterministic update-thread logic. */
    public static void setConstructionProgress(Unit unit, float progress) {
        if (!Float.isFinite(progress)) throw new IllegalArgumentException("progress must be finite");
        Objects.requireNonNull(unit, "unit").setConstructionProgress(progress);
    }

    private static OrderableUnit require(OrderableUnit unit) {
        return Objects.requireNonNull(unit, "unit");
    }
}
