package io.github.endx.strategicai;

import io.github.endx.rustedfabricapi.api.ai.AiMovementDomain;
import io.github.endx.rustedfabricapi.api.ai.AiEngagementAssessment;
import io.github.endx.rustedfabricapi.api.ai.AiResourceObjectiveKind;
import io.github.endx.rustedfabricapi.api.ai.AiStrategicMapSnapshot;
import io.github.endx.rustedfabricapi.api.ai.AiStrategicResource;
import io.github.endx.rustedfabricapi.api.ai.AiTerrainCell;
import io.github.endx.rustedfabricapi.api.ai.AiTickContext;
import io.github.endx.rustedfabricapi.api.ai.AiUnitCapabilities;
import io.github.endx.rustedfabricapi.api.game.UnitView;
import io.github.endx.rustedfabricapi.api.world.WorldPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Forms idle combat units into movement-compatible groups and advances them toward objectives. */
final class StrategicForcePlanner {
    private static final int MINIMUM_ATTACK_GROUP = 3;
    private static final float LOCAL_BUILDING_DISTANCE = 650.0F;
    private static final float RESOURCE_DEFENDER_DISTANCE = 220.0F;
    private static final float MINIMUM_STANDOFF_ADVANTAGE = 8.0F;
    private static final float STANDOFF_SAFETY_PADDING = 3.0F;
    private static final float STANDOFF_INNER_PADDING = 2.0F;

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
            Objective objective = selectObjective(situation, entry.getKey(), units);
            if (objective == null) continue;
            if (objective.target != null) {
                engageTarget(context, situation, entry.getKey(), units, objective.target);
            } else {
                context.orders().attackMove(units, objective.point.x(), objective.point.y());
            }
        }
    }

    private static Objective selectObjective(AiStrategicMapSnapshot situation,
            AiMovementDomain domain, List<UnitView> units) {
        WorldPoint center = centroid(units);
        for (AiStrategicResource resource : situation.resources()) {
            if (!resource.reachable(domain)) continue;
            if (resource.objective() == AiResourceObjectiveKind.LOCK_DOWN
                    || resource.objective() == AiResourceObjectiveKind.DENY) {
                UnitView defender = closestReachableEnemyToPoint(situation, domain,
                        resource.site().center(), true, RESOURCE_DEFENDER_DISTANCE);
                if (defender != null) return Objective.target(defender);
                return Objective.point(resource.site().center());
            }
        }
        UnitView localBuilding = closestReachableEnemyToPoint(situation, domain,
                center, true, LOCAL_BUILDING_DISTANCE);
        if (localBuilding != null) return Objective.target(localBuilding);
        if (situation.primaryFront().isPresent()
                && reachable(situation, domain, center, situation.primaryFront().get())) {
            return Objective.point(situation.primaryFront().get());
        }
        UnitView closestBuilding = closestReachableEnemy(
                situation, domain, center, true);
        if (closestBuilding != null) return Objective.target(closestBuilding);
        UnitView closestUnit = closestReachableEnemy(
                situation, domain, center, false);
        return closestUnit != null ? Objective.target(closestUnit) : null;
    }

    private static void engageTarget(AiTickContext context, AiStrategicMapSnapshot situation,
            AiMovementDomain domain, List<UnitView> units, UnitView target) {
        ArrayList<UnitView> directAttackers = new ArrayList<UnitView>();
        ArrayList<UnitView> supportingUnits = new ArrayList<UnitView>();
        for (UnitView unit : units) {
            AiEngagementAssessment engagement = AiEngagementAssessment.capture(unit, target);
            if (target.building()
                    && engagement.hasSafeStandoffWindow(MINIMUM_STANDOFF_ADVANTAGE)) {
                if (engagement.attackerWithinRange() && !engagement.defenderWithinRange()) {
                    // No attack waypoint: staying idle preserves the native automatic fire range.
                    continue;
                }
                WorldPoint position = standoffPoint(situation, unit, target, engagement);
                WorldPoint current = new WorldPoint(unit.x(), unit.y());
                if (position != null && reachable(situation, domain, current, position)) {
                    context.orders().move(Collections.singletonList(unit),
                            position.x(), position.y());
                    continue;
                }
            }
            if (engagement.canEngage()) directAttackers.add(unit);
            else supportingUnits.add(unit);
        }
        if (!directAttackers.isEmpty()) context.orders().attack(directAttackers, target);
        if (!supportingUnits.isEmpty()) {
            context.orders().attackMove(supportingUnits, target.x(), target.y());
        }
    }

    private static WorldPoint standoffPoint(AiStrategicMapSnapshot situation, UnitView attacker,
            UnitView target, AiEngagementAssessment engagement) {
        float desiredDistance = StandoffGeometry.desiredDistance(engagement.attackerRange(),
                engagement.returnFireRange(), STANDOFF_SAFETY_PADDING,
                STANDOFF_INNER_PADDING);
        if (!Float.isFinite(desiredDistance)) return null;
        StandoffGeometry.Position position = StandoffGeometry.position(attacker.x(), attacker.y(),
                attacker.id(), target.x(), target.y(), desiredDistance);
        float maxX = situation.terrain().mapWidthTiles()
                * situation.terrain().tileWidth() - 1.0F;
        float maxY = situation.terrain().mapHeightTiles()
                * situation.terrain().tileHeight() - 1.0F;
        float x = clamp(position.x, 1.0F, maxX);
        float y = clamp(position.y, 1.0F, maxY);
        return new WorldPoint(x, y);
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

    private static UnitView closestReachableEnemyToPoint(AiStrategicMapSnapshot situation,
            AiMovementDomain domain, WorldPoint center, boolean buildingsOnly, float maximumDistance) {
        UnitView candidate = closestReachableEnemy(situation, domain, center, buildingsOnly);
        if (candidate == null) return null;
        WorldPoint point = new WorldPoint(candidate.x(), candidate.y());
        return center.distanceSquared(point) <= maximumDistance * maximumDistance ? candidate : null;
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

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static final class Objective {
        final WorldPoint point;
        final UnitView target;

        private Objective(WorldPoint point, UnitView target) {
            this.point = point;
            this.target = target;
        }

        static Objective point(WorldPoint point) {
            return new Objective(point, null);
        }

        static Objective target(UnitView target) {
            return new Objective(new WorldPoint(target.x(), target.y()), target);
        }
    }
}
