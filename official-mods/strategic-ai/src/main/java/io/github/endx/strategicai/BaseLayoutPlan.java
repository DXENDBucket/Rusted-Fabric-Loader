package io.github.endx.strategicai;

import io.github.endx.rustedfabricapi.api.game.UnitView;
import io.github.endx.rustedfabricapi.api.world.WorldPoint;

import java.util.List;

/** Immutable anchor and orientation for one base; building slots never follow a roaming builder. */
final class BaseLayoutPlan {
    private final long anchorUnitId;
    private final WorldPoint anchor;
    private final WorldPoint front;

    BaseLayoutPlan(long anchorUnitId, WorldPoint anchor, WorldPoint front) {
        this.anchorUnitId = anchorUnitId;
        this.anchor = anchor;
        this.front = front;
    }

    long anchorUnitId() { return anchorUnitId; }
    WorldPoint anchor() { return anchor; }
    WorldPoint front() { return front; }

    List<WorldPoint> slots(BaseLayoutGeometry.District district) {
        return BaseLayoutGeometry.slots(anchor, front, district);
    }

    boolean anchorAlive(List<UnitView> own) {
        for (UnitView unit : own) {
            if (unit.id() == anchorUnitId) return unit.alive();
        }
        return false;
    }
}
