package io.github.endx.strategicai;

import io.github.endx.rustedfabricapi.api.ai.AiMovementDomain;
import io.github.endx.rustedfabricapi.api.ai.AiEngagementAssessment;
import io.github.endx.rustedfabricapi.api.ai.AiResourceObjectiveKind;
import io.github.endx.rustedfabricapi.api.ai.AiStrategicMapSnapshot;
import io.github.endx.rustedfabricapi.api.ai.AiStrategicResource;
import io.github.endx.rustedfabricapi.api.ai.AiTerrainCell;
import io.github.endx.rustedfabricapi.api.ai.AiTickContext;
import io.github.endx.rustedfabricapi.api.ai.AiUnitCapabilities;
import io.github.endx.rustedfabricapi.api.client.RustedWarfareClient;
import io.github.endx.rustedfabricapi.api.game.UnitView;
import io.github.endx.rustedfabricapi.api.world.WorldPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Forms combat units into movement-compatible groups, defenses, and reinforcement streams. */
final class StrategicForcePlanner {
    private static final int MINIMUM_ATTACK_GROUP = 3;
    private static final float LOCAL_BUILDING_DISTANCE = 650.0F;
    private static final float RESOURCE_DEFENDER_DISTANCE = 220.0F;
    private static final float HOME_DEFENSE_DISTANCE = 620.0F;
    private static final float RALLY_FORWARD_DISTANCE = 260.0F;
    private static final float RALLY_ARRIVAL_DISTANCE = 65.0F;
    private static final float MINIMUM_STANDOFF_ADVANTAGE = 8.0F;
    private static final float STANDOFF_SAFETY_PADDING = 3.0F;
    private static final float STANDOFF_INNER_PADDING = 2.0F;

    void update(AiTickContext context, AiStrategicMapSnapshot situation) {
        int minimumAttackGroup = RustedWarfareClient.isSandboxMode()
                ? 1 : MINIMUM_ATTACK_GROUP;
        EnumMap<AiMovementDomain, List<UnitView>> idleGroups =
                new EnumMap<AiMovementDomain, List<UnitView>>(AiMovementDomain.class);
        EnumMap<AiMovementDomain, List<UnitView>> activeGroups =
                new EnumMap<AiMovementDomain, List<UnitView>>(AiMovementDomain.class);
        ArrayList<UnitView> homeAnchors = new ArrayList<UnitView>();
        List<UnitView> currentOwn = context.world().own();
        List<UnitView> currentEnemies = context.world().enemies();
        for (UnitView unit : currentOwn) {
            if (!unit.alive()) continue;
            AiUnitCapabilities capabilities = AiUnitCapabilities.capture(unit);
            if (unit.building() || capabilities.builder()) homeAnchors.add(unit);
            if (!capabilities.mobileCombatUnit()) continue;
            EnumMap<AiMovementDomain, List<UnitView>> destination = capabilities.idle()
                    ? idleGroups : activeGroups;
            destination.computeIfAbsent(capabilities.movementDomain(), ignored ->
                    new ArrayList<UnitView>()).add(unit);
        }
        for (Map.Entry<AiMovementDomain, List<UnitView>> entry : idleGroups.entrySet()) {
            AiMovementDomain domain = entry.getKey();
            List<UnitView> units = entry.getValue();
            units.sort(Comparator.comparingLong(UnitView::id));

            UnitView defensiveTarget = selectDefensiveTarget(situation, domain, units,
                    homeAnchors, currentEnemies);
            if (defensiveTarget != null) {
                engageTarget(context, situation, domain, units, defensiveTarget);
                continue;
            }

            Objective objective = selectObjective(situation, domain, units, currentEnemies);
            if (objective == null) continue;
            if (units.size() < minimumAttackGroup) {
                List<UnitView> active = activeGroups.get(domain);
                WorldPoint destination = active == null || active.isEmpty()
                        ? rallyPoint(situation, domain, units, homeAnchors, objective.point)
                        : reachableCentroid(situation, domain, units, active);
                if (destination != null && !arrived(units, destination)) {
                    context.orders().attackMove(units, destination.x(), destination.y());
                }
                continue;
            }
            if (objective.target != null) {
                engageTarget(context, situation, domain, units, objective.target);
            } else {
                context.orders().attackMove(units, objective.point.x(), objective.point.y());
            }
        }
    }

    private static Objective selectObjective(AiStrategicMapSnapshot situation,
            AiMovementDomain domain, List<UnitView> units, List<UnitView> enemies) {
        WorldPoint center = centroid(units);
        for (AiStrategicResource resource : situation.resources()) {
            if (!resource.reachable(domain)) continue;
            if (resource.objective() == AiResourceObjectiveKind.LOCK_DOWN
                    || resource.objective() == AiResourceObjectiveKind.DENY) {
                UnitView defender = closestReachableEnemyToPoint(situation, domain,
                        resource.site().center(), true, RESOURCE_DEFENDER_DISTANCE, enemies);
                if (defender != null) return Objective.target(defender);
                return Objective.point(resource.site().center());
            }
        }
        UnitView localBuilding = closestReachableEnemyToPoint(situation, domain,
                center, true, LOCAL_BUILDING_DISTANCE, enemies);
        if (localBuilding != null) return Objective.target(localBuilding);
        if (situation.primaryFront().isPresent()
                && reachable(situation, domain, center, situation.primaryFront().get())) {
            return Objective.point(situation.primaryFront().get());
        }
        UnitView closestBuilding = closestReachableEnemy(
                situation, domain, center, true, enemies);
        if (closestBuilding != null) return Objective.target(closestBuilding);
        UnitView closestUnit = closestReachableEnemy(
                situation, domain, center, false, enemies);
        return closestUnit != null ? Objective.target(closestUnit) : null;
    }

    private static UnitView selectDefensiveTarget(AiStrategicMapSnapshot situation,
            AiMovementDomain domain, List<UnitView> units, List<UnitView> homeAnchors,
            List<UnitView> enemies) {
        if (homeAnchors.isEmpty()) return null;
        WorldPoint unitCenter = centroid(units);
        UnitView best = null;
        float bestDistance = Float.POSITIVE_INFINITY;
        float maximumDistanceSquared = HOME_DEFENSE_DISTANCE * HOME_DEFENSE_DISTANCE;
        for (UnitView enemy : enemies) {
            if (!enemy.alive()) continue;
            WorldPoint enemyPoint = new WorldPoint(enemy.x(), enemy.y());
            if (!reachable(situation, domain, unitCenter, enemyPoint)) continue;
            boolean threatensHome = false;
            for (UnitView anchor : homeAnchors) {
                float dx = anchor.x() - enemy.x();
                float dy = anchor.y() - enemy.y();
                if (dx * dx + dy * dy <= maximumDistanceSquared) {
                    threatensHome = true;
                    break;
                }
            }
            if (!threatensHome) continue;
            float distance = unitCenter.distanceSquared(enemyPoint);
            if (distance < bestDistance || distance == bestDistance
                    && (best == null || enemy.id() < best.id())) {
                best = enemy;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static WorldPoint reachableCentroid(AiStrategicMapSnapshot situation,
            AiMovementDomain domain, List<UnitView> units, List<UnitView> active) {
        WorldPoint from = centroid(units);
        WorldPoint destination = centroid(active);
        return reachable(situation, domain, from, destination) ? destination : null;
    }

    private static WorldPoint rallyPoint(AiStrategicMapSnapshot situation,
            AiMovementDomain domain, List<UnitView> units, List<UnitView> homeAnchors,
            WorldPoint objective) {
        WorldPoint from = centroid(units);
        WorldPoint home = homeAnchors.isEmpty() ? from : centroid(homeAnchors);
        WorldPoint rally = ForceCoordinationGeometry.advance(
                home, objective, RALLY_FORWARD_DISTANCE);
        return reachable(situation, domain, from, rally) ? rally : null;
    }

    private static boolean arrived(List<UnitView> units, WorldPoint destination) {
        return ForceCoordinationGeometry.arrived(
                centroid(units), destination, RALLY_ARRIVAL_DISTANCE);
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
            AiMovementDomain domain, WorldPoint center, boolean buildingsOnly,
            List<UnitView> enemies) {
        UnitView best = null;
        float bestDistance = Float.POSITIVE_INFINITY;
        for (UnitView enemy : enemies) {
            if (!enemy.alive()) continue;
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
            AiMovementDomain domain, WorldPoint center, boolean buildingsOnly,
            float maximumDistance, List<UnitView> enemies) {
        UnitView candidate = closestReachableEnemy(
                situation, domain, center, buildingsOnly, enemies);
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
