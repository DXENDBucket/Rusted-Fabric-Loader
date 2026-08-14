package io.github.endx.strategicai;

import io.github.endx.rustedfabricapi.api.ai.AiMovementDomain;
import io.github.endx.rustedfabricapi.api.ai.AiResourceObjectiveKind;
import io.github.endx.rustedfabricapi.api.ai.AiStrategicMapSnapshot;
import io.github.endx.rustedfabricapi.api.ai.AiStrategicResource;
import io.github.endx.rustedfabricapi.api.ai.AiTerrainCell;
import io.github.endx.rustedfabricapi.api.ai.AiTickContext;
import io.github.endx.rustedfabricapi.api.ai.AiUnitCapabilities;
import io.github.endx.rustedfabricapi.api.game.UnitView;
import io.github.endx.rustedfabricapi.api.world.WorldPoint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Forms idle combat units into movement-compatible groups and advances them toward objectives. */
final class StrategicForcePlanner {
    private static final int MINIMUM_ATTACK_GROUP = 3;

    void update(AiTickContext context, AiStrategicMapSnapshot situation) {
        EnumMap<AiMovementDomain, List<UnitView>> groups =
                new EnumMap<AiMovementDomain, List<UnitView>>(AiMovementDomain.class);
        for (UnitView unit : situation.world().own()) {
            AiUnitCapabilities capabilities = AiUnitCapabilities.capture(unit);
            if (!capabilities.mobileCombatUnit() || !capabilities.idle()) continue;
            groups.computeIfAbsent(capabilities.movementDomain(), ignored ->
                    new ArrayList<UnitView>()).add(unit);
        }
        for (Map.Entry<AiMovementDomain, List<UnitView>> entry : groups.entrySet()) {
            List<UnitView> units = entry.getValue();
            units.sort(Comparator.comparingLong(UnitView::id));
            if (units.size() < MINIMUM_ATTACK_GROUP) continue;
            WorldPoint target = selectTarget(situation, entry.getKey(), units);
            if (target != null) context.orders().attackMove(units, target.x(), target.y());
        }
    }

    private static WorldPoint selectTarget(AiStrategicMapSnapshot situation,
            AiMovementDomain domain, List<UnitView> units) {
        WorldPoint center = centroid(units);
        for (AiStrategicResource resource : situation.resources()) {
            if (!resource.reachable(domain)) continue;
            if (resource.objective() == AiResourceObjectiveKind.LOCK_DOWN
                    || resource.objective() == AiResourceObjectiveKind.DENY) {
                return resource.site().center();
            }
        }
        if (situation.primaryFront().isPresent()
                && reachable(situation, domain, center, situation.primaryFront().get())) {
            return situation.primaryFront().get();
        }
        UnitView closestBuilding = closestReachableEnemy(
                situation, domain, center, true);
        if (closestBuilding != null) {
            return new WorldPoint(closestBuilding.x(), closestBuilding.y());
        }
        UnitView closestUnit = closestReachableEnemy(
                situation, domain, center, false);
        return closestUnit != null ? new WorldPoint(closestUnit.x(), closestUnit.y()) : null;
    }

    private static UnitView closestReachableEnemy(AiStrategicMapSnapshot situation,
            AiMovementDomain domain, WorldPoint center, boolean buildingsOnly) {
        UnitView best = null;
        float bestDistance = Float.POSITIVE_INFINITY;
        for (UnitView enemy : situation.world().enemies()) {
            if (buildingsOnly && !enemy.building()) continue;
            WorldPoint point = new WorldPoint(enemy.x(), enemy.y());
            if (!reachable(situation, domain, center, point)) continue;
            float distance = center.distanceSquared(point);
            if (distance < bestDistance || distance == bestDistance
                    && (best == null || enemy.id() < best.id())) {
                best = enemy;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static boolean reachable(AiStrategicMapSnapshot situation,
            AiMovementDomain domain, WorldPoint from, WorldPoint to) {
        AiTerrainCell first = situation.terrain().cellAtWorld(from.x(), from.y());
        AiTerrainCell second = situation.terrain().cellAtWorld(to.x(), to.y());
        return situation.terrain().sameRegion(first, second, domain);
    }

    private static WorldPoint centroid(List<UnitView> units) {
        float x = 0.0F;
        float y = 0.0F;
        for (UnitView unit : units) {
            x += unit.x();
            y += unit.y();
        }
        return new WorldPoint(x / units.size(), y / units.size());
    }
}
