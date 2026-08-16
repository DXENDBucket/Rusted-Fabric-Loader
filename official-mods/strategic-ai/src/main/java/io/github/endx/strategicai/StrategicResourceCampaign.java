package io.github.endx.strategicai;

import io.github.endx.rustedfabricapi.api.ai.AiMovementDomain;
import io.github.endx.rustedfabricapi.api.ai.AiResourceControl;
import io.github.endx.rustedfabricapi.api.ai.AiResourceObjectiveKind;
import io.github.endx.rustedfabricapi.api.ai.AiStrategicMapSnapshot;
import io.github.endx.rustedfabricapi.api.ai.AiStrategicResource;
import io.github.endx.rustedfabricapi.api.ai.AiTerrainRouteMap;
import io.github.endx.rustedfabricapi.api.ai.AiUnitCapabilities;
import io.github.endx.rustedfabricapi.api.client.RustedWarfareClient;
import io.github.endx.rustedfabricapi.api.game.UnitView;
import io.github.endx.rustedfabricapi.api.world.WorldPoint;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Persistent secure-build-hold operation for one valuable resource beyond the main base. */
final class StrategicResourceCampaign {
    enum Phase {
        FORTIFY,
        ASSEMBLE,
        SECURE,
        BUILD,
        HOLD
    }

    private static final float LOCAL_RESOURCE_RADIUS = 700.0F;
    private static final float FORCE_RADIUS = 260.0F;
    private static final long HOLD_CYCLES = 40L;
    private static final int BUILD_ORDER_LEASE_UPDATES = 48;
    private static final int ABORT_RETRY_UPDATES = 60;

    private long targetKey = Long.MIN_VALUE;
    private AiStrategicResource target;
    private Phase phase;
    private long holdUntil;
    private int buildOrderLeaseUpdates;
    private int phaseUpdates;
    private int fortificationOrderLeaseUpdates;
    private boolean fortificationObserved;
    private boolean fortificationOrdered;
    private boolean fortificationLost;
    private boolean forwardOpening;
    private final Map<Long, Integer> rejectedTargets = new HashMap<Long, Integer>();

    void onStrategicReplan() {
        target = null;
        targetKey = Long.MIN_VALUE;
        phase = null;
        buildOrderLeaseUpdates = 0;
        fortificationOrderLeaseUpdates = 0;
        fortificationObserved = false;
        fortificationOrdered = false;
        fortificationLost = false;
    }

    void update(AiStrategicMapSnapshot situation, List<UnitView> own,
            List<UnitView> enemies, long cycle, int teamId,
            StrategicTeamPlan teamPlan) {
        rejectedTargets.replaceAll((key, updates) -> updates - 1);
        rejectedTargets.entrySet().removeIf(entry -> entry.getValue() <= 0);
        WorldPoint home = homeAnchor(own);
        AiStrategicResource refreshed = findByKey(situation, targetKey);
        if (target == null || refreshed == null || refreshed.control() == AiResourceControl.ALLY
                || refreshed.control() == AiResourceControl.OWN && phase == Phase.HOLD
                && cycle >= holdUntil) {
            target = select(situation, home, teamPlan);
            targetKey = target != null ? key(target) : Long.MIN_VALUE;
            forwardOpening = target != null && teamPlan.usesForwardOpening()
                    && targetKey == teamPlan.preferredFrontierKey();
            phase = null;
            holdUntil = 0L;
            buildOrderLeaseUpdates = 0;
            fortificationOrderLeaseUpdates = 0;
            fortificationObserved = false;
            fortificationOrdered = false;
            fortificationLost = false;
            phaseUpdates = 0;
        } else {
            target = refreshed;
        }
        if (target == null) return;

        int escorts = nearbyCombat(own, target.site().center(), FORCE_RADIUS);
        int threats = nearbyEnemies(enemies, target.site().center(), FORCE_RADIUS);
        int required = RustedWarfareClient.isSandboxMode() ? 1 : 2;
        boolean towerPresent = nearbyStaticDefense(
                own, target.site().center(), FORCE_RADIUS, 0.01F);
        boolean fortified = nearbyStaticDefense(
                own, target.site().center(), FORCE_RADIUS, 0.98F);
        if (towerPresent) {
            fortificationObserved = true;
            fortificationOrdered = false;
        } else if (fortificationObserved) {
            // The ordered tower really existed and has since been lost. Cancel the old command
            // lease immediately so a surviving forward builder can rebuild or fall back to lock.
            fortificationObserved = false;
            fortificationOrderLeaseUpdates = 0;
            fortificationLost = true;
        }
        Phase next = FrontierResourcePolicy.phase(target.control(), escorts,
                threats, required, forwardOpening, fortified);
        if (buildOrderLeaseUpdates > 0) buildOrderLeaseUpdates--;
        if (fortificationOrderLeaseUpdates > 0) {
            fortificationOrderLeaseUpdates--;
            if (fortificationOrderLeaseUpdates == 0
                    && fortificationOrdered && !towerPresent) {
                fortificationOrdered = false;
                fortificationLost = true;
            }
        }
        if (next == Phase.BUILD && buildOrderLeaseUpdates > 0) next = Phase.ASSEMBLE;
        if (phase == next) phaseUpdates++;
        else phaseUpdates = 0;
        if (next == Phase.SECURE && phaseUpdates >= 40
                && threats >= Math.max(4, escorts * 3)) {
            System.out.println("[Strategic AI] Team " + teamId
                    + " aborted overmatched frontier resource "
                    + target.site().tileX() + "," + target.site().tileY());
            rejectedTargets.put(targetKey, ABORT_RETRY_UPDATES);
            target = null;
            targetKey = Long.MIN_VALUE;
            phase = null;
            phaseUpdates = 0;
            return;
        }
        if (next == Phase.HOLD && phase != Phase.HOLD) holdUntil = cycle + HOLD_CYCLES;
        if (phase != next) {
            phase = next;
            System.out.println("[Strategic AI] Team " + teamId + " frontier resource "
                    + target.site().tileX() + "," + target.site().tileY()
                    + " phase=" + phase + ", escorts=" + escorts
                    + ", threats=" + threats);
        }
    }

    boolean active() { return target != null; }
    Phase phase() { return phase; }
    AiStrategicResource target() { return target; }
    WorldPoint point() { return target != null ? target.site().center() : null; }
    boolean allowsBuild() { return target != null && phase == Phase.BUILD; }
    boolean allowsFortification() {
        return target != null && phase == Phase.FORTIFY
                && fortificationOrderLeaseUpdates <= 0;
    }
    boolean fortificationLost() { return fortificationLost; }

    void markBuildOrdered() {
        buildOrderLeaseUpdates = BUILD_ORDER_LEASE_UPDATES;
    }

    void markFortificationOrdered() {
        fortificationOrderLeaseUpdates = BUILD_ORDER_LEASE_UPDATES;
        fortificationOrdered = true;
        fortificationLost = false;
    }

    void retryFortification() {
        fortificationOrderLeaseUpdates = 0;
        fortificationOrdered = false;
        fortificationLost = true;
    }

    private AiStrategicResource select(AiStrategicMapSnapshot situation,
            WorldPoint home, StrategicTeamPlan teamPlan) {
        if (home == null) return null;
        AiStrategicResource preferred = findByKey(
                situation, teamPlan.preferredFrontierKey());
        if (teamPlan.leadsFrontline() && preferred != null
                && preferred.control() != AiResourceControl.OWN
                && preferred.control() != AiResourceControl.ALLY
                && groundReachable(preferred)) return preferred;
        float diagonal = Math.max(1.0F, (float) Math.hypot(
                situation.terrain().worldWidth(), situation.terrain().worldHeight()));
        AiTerrainRouteMap landRoutes = situation.terrain().routesFrom(
                home, AiMovementDomain.LAND);
        AiStrategicResource best = null;
        float bestScore = Float.NEGATIVE_INFINITY;
        for (AiStrategicResource resource : situation.resources()) {
            if (rejectedTargets.containsKey(key(resource))) continue;
            if (!teamPlan.leadsFrontline()
                    && teamPlan.reservedForFrontline(resource)) continue;
            if (resource.control() != AiResourceControl.UNCLAIMED
                    && resource.control() != AiResourceControl.ENEMY
                    && resource.control() != AiResourceControl.NEUTRAL) continue;
            if (resource.objective() != AiResourceObjectiveKind.LOCK_DOWN
                    && resource.objective() != AiResourceObjectiveKind.DENY
                    && resource.objective() != AiResourceObjectiveKind.CAPTURE) continue;
            if (!groundReachable(resource)) continue;
            java.util.OptionalDouble route = landRoutes.costTo(resource.site().center());
            if (!route.isPresent()) continue;
            float routeDistance = (float) route.getAsDouble();
            if (routeDistance <= LOCAL_RESOURCE_RADIUS) continue;
            float score = FrontierResourcePolicy.score(resource.priority(),
                    resource.objective(), routeDistance / diagonal,
                    resource.friendlyInfluence(), resource.enemyInfluence());
            if (score > bestScore || score == bestScore
                    && (best == null || key(resource) < key(best))) {
                best = resource;
                bestScore = score;
            }
        }
        return best;
    }

    private static boolean groundReachable(AiStrategicResource resource) {
        return resource.reachable(AiMovementDomain.LAND)
                || resource.reachable(AiMovementDomain.HOVER)
                || resource.reachable(AiMovementDomain.OVER_CLIFF)
                || resource.reachable(AiMovementDomain.OVER_CLIFF_WATER);
    }

    private static AiStrategicResource findByKey(AiStrategicMapSnapshot situation, long key) {
        if (key == Long.MIN_VALUE) return null;
        for (AiStrategicResource resource : situation.resources()) {
            if (key(resource) == key) return resource;
        }
        return null;
    }

    private static int nearbyCombat(List<UnitView> units, WorldPoint point, float radius) {
        int count = 0;
        float squared = radius * radius;
        for (UnitView unit : units) {
            if (!unit.alive() || unit.building()) continue;
            if (!AiUnitCapabilities.capture(unit).mobileCombatUnit()) continue;
            float dx = unit.x() - point.x();
            float dy = unit.y() - point.y();
            if (dx * dx + dy * dy <= squared) count++;
        }
        return count;
    }

    private static int nearbyEnemies(List<UnitView> units, WorldPoint point, float radius) {
        int count = 0;
        float squared = radius * radius;
        for (UnitView unit : units) {
            if (!unit.alive()) continue;
            float dx = unit.x() - point.x();
            float dy = unit.y() - point.y();
            if (dx * dx + dy * dy <= squared) count++;
        }
        return count;
    }

    private static boolean nearbyStaticDefense(List<UnitView> units,
            WorldPoint point, float radius, float minimumProgress) {
        float squared = radius * radius;
        for (UnitView unit : units) {
            if (!unit.alive() || !unit.building()) continue;
            AiUnitCapabilities capabilities = AiUnitCapabilities.capture(unit);
            if (!capabilities.attacker()) continue;
            if (unit.constructionProgress() < minimumProgress) continue;
            float dx = unit.x() - point.x();
            float dy = unit.y() - point.y();
            if (dx * dx + dy * dy <= squared) return true;
        }
        return false;
    }

    private static WorldPoint homeAnchor(List<UnitView> own) {
        UnitView best = null;
        for (UnitView unit : own) {
            if (!unit.alive() || !unit.building()) continue;
            if (best == null || unit.maxHealth() > best.maxHealth()
                    || unit.maxHealth() == best.maxHealth() && unit.id() < best.id()) best = unit;
        }
        return best != null ? new WorldPoint(best.x(), best.y()) : null;
    }

    private static long key(AiStrategicResource resource) {
        return ((long) resource.site().tileX() << 32)
                ^ (resource.site().tileY() & 0xffffffffL);
    }
}
