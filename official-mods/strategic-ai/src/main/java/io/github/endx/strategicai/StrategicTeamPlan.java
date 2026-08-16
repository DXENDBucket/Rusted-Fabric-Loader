package io.github.endx.strategicai;

import io.github.endx.rustedfabricapi.api.ai.AiMovementDomain;
import io.github.endx.rustedfabricapi.api.ai.AiInfluenceCell;
import io.github.endx.rustedfabricapi.api.ai.AiResourceControl;
import io.github.endx.rustedfabricapi.api.ai.AiStrategicMapSnapshot;
import io.github.endx.rustedfabricapi.api.ai.AiStrategicResource;
import io.github.endx.rustedfabricapi.api.ai.AiTeamPresence;
import io.github.endx.rustedfabricapi.api.ai.AiTeamRelation;
import io.github.endx.rustedfabricapi.api.ai.AiTerrainCell;
import io.github.endx.rustedfabricapi.api.ai.AiTerrainRouteMap;
import io.github.endx.rustedfabricapi.api.world.WorldPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;

/** Stable allied position assignment and resource ownership plan for one match. */
final class StrategicTeamPlan {
    private static final float MINIMUM_FRONT_APPROACH_COST = 320.0F;
    private final int ownTeamId;
    private final Map<Integer, TeamPositionDoctrine.Role> roles;
    private final Map<Integer, WorldPoint> anchors;
    private final Map<Integer, AiTerrainRouteMap> landRoutes;
    private final Map<Integer, Float> frontAccessCosts;
    private final Set<Integer> operationalTeamIds;
    private final int frontlineTeamId;
    private final long preferredFrontierKey;
    private final WorldPoint preferredFrontierPoint;
    private final boolean forwardOpening;

    private StrategicTeamPlan(int ownTeamId,
            Map<Integer, TeamPositionDoctrine.Role> roles,
            Map<Integer, WorldPoint> anchors,
            Map<Integer, AiTerrainRouteMap> landRoutes,
            Map<Integer, Float> frontAccessCosts, Set<Integer> operationalTeamIds,
            int frontlineTeamId,
            long preferredFrontierKey, WorldPoint preferredFrontierPoint,
            boolean forwardOpening) {
        this.ownTeamId = ownTeamId;
        this.roles = Collections.unmodifiableMap(
                new LinkedHashMap<Integer, TeamPositionDoctrine.Role>(roles));
        this.anchors = Collections.unmodifiableMap(
                new LinkedHashMap<Integer, WorldPoint>(anchors));
        this.landRoutes = Collections.unmodifiableMap(
                new LinkedHashMap<Integer, AiTerrainRouteMap>(landRoutes));
        this.frontAccessCosts = Collections.unmodifiableMap(
                new LinkedHashMap<Integer, Float>(frontAccessCosts));
        this.operationalTeamIds = Collections.unmodifiableSet(
                new HashSet<Integer>(operationalTeamIds));
        this.frontlineTeamId = frontlineTeamId;
        this.preferredFrontierKey = preferredFrontierKey;
        this.preferredFrontierPoint = preferredFrontierPoint;
        this.forwardOpening = forwardOpening;
    }

    static StrategicTeamPlan create(AiStrategicMapSnapshot situation) {
        return replan(situation, null);
    }

    static StrategicTeamPlan replan(AiStrategicMapSnapshot situation,
            StrategicTeamPlan previous) {
        StrategicTeamPlan candidate = compute(situation);
        if (previous == null) return candidate;
        boolean teamSetChanged = !previous.roles.keySet().equals(candidate.roles.keySet());
        Float currentCost = candidate.frontAccessCosts.get(previous.frontlineTeamId);
        Float proposedCost = candidate.frontAccessCosts.get(candidate.frontlineTeamId);
        boolean accept = StrategicReplanPolicy.acceptFrontlineSwitch(
                currentCost != null ? currentCost.floatValue() : Float.POSITIVE_INFINITY,
                proposedCost != null ? proposedCost.floatValue() : Float.POSITIVE_INFINITY,
                candidate.operationalTeamIds.contains(previous.frontlineTeamId),
                teamSetChanged);
        StrategicTeamPlan selected = candidate;
        if (candidate.frontlineTeamId != previous.frontlineTeamId && !accept) {
            selected = new StrategicTeamPlan(candidate.ownTeamId, previous.roles,
                    candidate.anchors, candidate.landRoutes, candidate.frontAccessCosts,
                    candidate.operationalTeamIds, previous.frontlineTeamId,
                    candidate.preferredFrontierKey, candidate.preferredFrontierPoint,
                    candidate.forwardOpening);
        }
        return stabilizeObjective(situation, previous, selected);
    }

    private static StrategicTeamPlan stabilizeObjective(AiStrategicMapSnapshot situation,
            StrategicTeamPlan previous, StrategicTeamPlan selected) {
        if (selected.frontlineTeamId != previous.frontlineTeamId
                || !selected.roles.equals(previous.roles)
                || previous.preferredFrontierPoint == null
                || selected.preferredFrontierPoint == null
                || previous.preferredFrontierPoint.distanceSquared(
                selected.preferredFrontierPoint) > 360.0F * 360.0F
                || !objectiveStillRelevant(situation, previous.preferredFrontierKey)) {
            return selected;
        }
        return new StrategicTeamPlan(selected.ownTeamId, selected.roles,
                selected.anchors, selected.landRoutes, selected.frontAccessCosts,
                selected.operationalTeamIds, selected.frontlineTeamId,
                previous.preferredFrontierKey, previous.preferredFrontierPoint,
                previous.forwardOpening);
    }

    private static boolean objectiveStillRelevant(AiStrategicMapSnapshot situation, long key) {
        if (key == Long.MIN_VALUE) return true;
        for (AiStrategicResource resource : situation.resources()) {
            if (resourceKey(resource) != key) continue;
            return resource.control() != AiResourceControl.OWN
                    && resource.control() != AiResourceControl.ALLY;
        }
        return false;
    }

    private static StrategicTeamPlan compute(AiStrategicMapSnapshot situation) {
        ArrayList<AiTeamPresence> friendlyAi = new ArrayList<AiTeamPresence>();
        ArrayList<AiTeamPresence> enemies = new ArrayList<AiTeamPresence>();
        LinkedHashMap<Integer, WorldPoint> anchors = new LinkedHashMap<Integer, WorldPoint>();
        LinkedHashMap<Integer, AiTerrainRouteMap> routes =
                new LinkedHashMap<Integer, AiTerrainRouteMap>();
        HashSet<Integer> operationalTeamIds = new HashSet<Integer>();
        for (AiTeamPresence presence : situation.teams()) {
            if (presence.relation() == AiTeamRelation.ENEMY) {
                enemies.add(presence);
            } else if ((presence.relation() == AiTeamRelation.OWN
                    || presence.relation() == AiTeamRelation.ALLY)
                    && presence.team().aiControlled()) {
                friendlyAi.add(presence);
                anchors.put(presence.team().id(), presence.anchor());
                if (presence.buildingCount() > 0) {
                    operationalTeamIds.add(presence.team().id());
                }
                routes.put(presence.team().id(), situation.terrain().routesFrom(
                        presence.anchor(), AiMovementDomain.LAND));
            }
        }
        ContestedSite contested = chooseLandFront(situation, friendlyAi, enemies, routes);
        ArrayList<TeamPositionDoctrine.Candidate> candidates =
                new ArrayList<TeamPositionDoctrine.Candidate>();
        LinkedHashMap<Integer, Float> frontAccessCosts =
                new LinkedHashMap<Integer, Float>();
        for (AiTeamPresence friendly : friendlyAi) {
            AiTerrainRouteMap route = routes.get(friendly.team().id());
            // Non-air positions are assigned by travel cost to the actual land corridor,
            // never by straight-line distance or by a convenient side resource.
            float frontCost = contested != null
                    ? routeCost(route, contested.frontPoint)
                    : nearestEnemyRouteCost(route, enemies);
            frontAccessCosts.put(friendly.team().id(), frontCost);
            candidates.add(new TeamPositionDoctrine.Candidate(friendly.team().id(),
                    frontCost, safeResourcePotential(route, situation.resources()),
                    friendly.buildingCount() > 0));
        }
        Map<Integer, TeamPositionDoctrine.Role> roles =
                TeamPositionDoctrine.allocate(candidates);
        int ownId = situation.perspective().id();
        int frontlineId = ownId;
        for (Map.Entry<Integer, TeamPositionDoctrine.Role> entry : roles.entrySet()) {
            if (entry.getValue() == TeamPositionDoctrine.Role.FRONTLINE) {
                frontlineId = entry.getKey();
                break;
            }
        }
        return new StrategicTeamPlan(ownId, roles, anchors, routes,
                frontAccessCosts, operationalTeamIds, frontlineId,
                contested != null ? contested.key : Long.MIN_VALUE,
                contested != null ? contested.frontPoint : null,
                contested != null && contested.openingSuitable);
    }

    boolean frontlineOperational(AiStrategicMapSnapshot situation) {
        for (AiTeamPresence presence : situation.teams()) {
            if (presence.team().id() == frontlineTeamId
                    && (presence.relation() == AiTeamRelation.OWN
                    || presence.relation() == AiTeamRelation.ALLY)) {
                return presence.buildingCount() > 0;
            }
        }
        return false;
    }

    boolean doctrineChangedFrom(StrategicTeamPlan previous) {
        if (previous == null || frontlineTeamId != previous.frontlineTeamId
                || !roles.equals(previous.roles)
                || preferredFrontierKey != previous.preferredFrontierKey) return true;
        if (preferredFrontierPoint == null || previous.preferredFrontierPoint == null) {
            return preferredFrontierPoint != previous.preferredFrontierPoint;
        }
        return preferredFrontierPoint.distanceSquared(previous.preferredFrontierPoint)
                > 240.0F * 240.0F;
    }

    TeamPositionDoctrine.Role ownRole() {
        TeamPositionDoctrine.Role role = roles.get(ownTeamId);
        return role != null ? role : TeamPositionDoctrine.Role.SOLO;
    }

    boolean leadsFrontline() {
        return ownTeamId == frontlineTeamId
                && ownRole() == TeamPositionDoctrine.Role.FRONTLINE;
    }

    boolean usesForwardOpening() {
        return leadsFrontline() && forwardOpening;
    }

    long preferredFrontierKey() { return preferredFrontierKey; }
    WorldPoint preferredFrontierPoint() { return preferredFrontierPoint; }
    WorldPoint ownAnchor() { return anchors.get(ownTeamId); }

    WorldPoint alliedHomeCenter() {
        if (anchors.isEmpty()) return null;
        float x = 0.0F;
        float y = 0.0F;
        for (WorldPoint anchor : anchors.values()) {
            x += anchor.x();
            y += anchor.y();
        }
        return new WorldPoint(x / anchors.size(), y / anchors.size());
    }

    boolean primaryMobileSupport() {
        int first = Integer.MAX_VALUE;
        for (Map.Entry<Integer, TeamPositionDoctrine.Role> entry : roles.entrySet()) {
            if (entry.getValue() == TeamPositionDoctrine.Role.MOBILE_SUPPORT) {
                first = Math.min(first, entry.getKey());
            }
        }
        return ownTeamId == first;
    }

    String assignmentSummary() {
        StringBuilder result = new StringBuilder();
        for (Map.Entry<Integer, TeamPositionDoctrine.Role> entry : roles.entrySet()) {
            if (result.length() > 0) result.append(';');
            WorldPoint anchor = anchors.get(entry.getKey());
            Float access = frontAccessCosts.get(entry.getKey());
            float cost = access != null ? access.floatValue() : Float.POSITIVE_INFINITY;
            result.append(entry.getKey()).append(':').append(entry.getValue());
            if (anchor != null) result.append('@').append((int) anchor.x())
                    .append(',').append((int) anchor.y());
            result.append("/enemyLand=").append(Float.isFinite(cost) ? (int) cost : -1);
        }
        return result.toString();
    }

    boolean reservedForFrontline(AiStrategicResource resource) {
        return roles.size() > 1 && preferredFrontierKey != Long.MIN_VALUE
                && resourceKey(resource) == preferredFrontierKey;
    }

    boolean ownsLocalResource(AiStrategicResource resource) {
        if (roles.size() <= 1) return true;
        return assignedResourceOwner(resource.site().center()) == ownTeamId;
    }

    private int assignedResourceOwner(WorldPoint point) {
        int bestTeam = ownTeamId;
        float bestScore = Float.POSITIVE_INFINITY;
        for (Map.Entry<Integer, WorldPoint> entry : anchors.entrySet()) {
            TeamPositionDoctrine.Role role = roles.get(entry.getKey());
            float distance = routeCost(landRoutes.get(entry.getKey()), point);
            if (!Float.isFinite(distance)) continue;
            float weight = role == TeamPositionDoctrine.Role.ECONOMY_TECH ? 0.68F
                    : role == TeamPositionDoctrine.Role.FRONTLINE ? 1.18F : 0.95F;
            float score = distance * weight;
            if (score < bestScore || score == bestScore && entry.getKey() < bestTeam) {
                bestScore = score;
                bestTeam = entry.getKey();
            }
        }
        return bestTeam;
    }

    private static ContestedSite chooseLandFront(AiStrategicMapSnapshot situation,
            List<AiTeamPresence> friendly, List<AiTeamPresence> enemies,
            Map<Integer, AiTerrainRouteMap> friendlyRoutes) {
        if (friendly.isEmpty() || enemies.isEmpty()) return null;
        ArrayList<AiTerrainRouteMap> enemyRoutes = new ArrayList<AiTerrainRouteMap>();
        for (AiTeamPresence enemy : enemies) {
            enemyRoutes.add(situation.terrain().routesFrom(
                    enemy.anchor(), AiMovementDomain.LAND));
        }
        float shortestCorridor = Float.POSITIVE_INFINITY;
        for (AiTerrainCell cell : situation.terrain().cells()) {
            if (!cell.representativePoint(AiMovementDomain.LAND).isPresent()) continue;
            WorldPoint point = cell.representativePoint(AiMovementDomain.LAND).get();
            float friendlyCost = minimumRouteCost(friendlyRoutes.values(), point);
            float enemyCost = minimumRouteCost(enemyRoutes, point);
            if (!validFrontApproach(friendlyCost, enemyCost)) continue;
            shortestCorridor = Math.min(shortestCorridor, friendlyCost + enemyCost);
        }
        if (!Float.isFinite(shortestCorridor)) return null;
        WorldPoint frontPoint = null;
        float bestScore = Float.NEGATIVE_INFINITY;
        float bestBalance = 0.0F;
        float bestChoke = 0.0F;
        int bestCellIndex = Integer.MAX_VALUE;
        for (AiTerrainCell cell : situation.terrain().cells()) {
            if (!cell.representativePoint(AiMovementDomain.LAND).isPresent()) continue;
            WorldPoint point = cell.representativePoint(AiMovementDomain.LAND).get();
            float friendlyCost = minimumRouteCost(friendlyRoutes.values(), point);
            float enemyCost = minimumRouteCost(enemyRoutes, point);
            float activeFront = dynamicLandFrontScore(situation, cell);
            if (!validFrontApproach(friendlyCost, enemyCost)
                    || activeFront <= 0.0F
                    && friendlyCost + enemyCost > shortestCorridor * 1.22F + 80.0F) continue;
            float balance = Math.min(friendlyCost, enemyCost)
                    / Math.max(friendlyCost, enemyCost);
            float choke = nearbyChoke(situation, point);
            float corridorPenalty = (friendlyCost + enemyCost - shortestCorridor)
                    / Math.max(1.0F, shortestCorridor);
            float score = balance * 0.58F + choke * 0.30F + activeFront * 0.82F
                    - corridorPenalty * (activeFront > 0.0F ? 0.34F : 0.78F);
            int cellIndex = cell.row() * situation.terrain().columns() + cell.column();
            if (score > bestScore || score == bestScore && cellIndex < bestCellIndex) {
                frontPoint = point;
                bestScore = score;
                bestBalance = balance;
                bestChoke = choke;
                bestCellIndex = cellIndex;
            }
        }
        if (frontPoint == null) return null;

        AiTerrainRouteMap fromFront = situation.terrain().routesFrom(
                frontPoint, AiMovementDomain.LAND);
        AiStrategicResource bestResource = null;
        float bestResourceScore = Float.NEGATIVE_INFINITY;
        float bestFrontDistance = Float.POSITIVE_INFINITY;
        for (AiStrategicResource resource : situation.resources()) {
            if (resource.control() == AiResourceControl.OWN
                    || resource.control() == AiResourceControl.ALLY) continue;
            WorldPoint point = resource.site().center();
            float friendlyCost = minimumRouteCost(friendlyRoutes.values(), point);
            float enemyCost = minimumRouteCost(enemyRoutes, point);
            float frontDistance = routeCost(fromFront, point);
            if (!Float.isFinite(friendlyCost) || !Float.isFinite(enemyCost)
                    || !Float.isFinite(frontDistance)) continue;
            float balance = Math.min(friendlyCost, enemyCost)
                    / Math.max(1.0F, Math.max(friendlyCost, enemyCost));
            float score = balance * 0.55F - frontDistance / 1100.0F
                    + resource.priority() * 0.08F;
            if (score > bestResourceScore || score == bestResourceScore
                    && resourceKey(resource) < resourceKey(bestResource)) {
                bestResource = resource;
                bestResourceScore = score;
                bestFrontDistance = frontDistance;
            }
        }
        boolean suitable = bestResource != null && bestBalance >= 0.55F
                && bestChoke >= 0.10F && bestFrontDistance <= 900.0F;
        return new ContestedSite(bestResource != null
                ? resourceKey(bestResource) : Long.MIN_VALUE, frontPoint, suitable);
    }

    private static boolean validFrontApproach(float friendlyCost, float enemyCost) {
        return Float.isFinite(friendlyCost) && Float.isFinite(enemyCost)
                && friendlyCost >= MINIMUM_FRONT_APPROACH_COST
                && enemyCost >= MINIMUM_FRONT_APPROACH_COST;
    }

    private static float nearestEnemyRouteCost(AiTerrainRouteMap route,
            List<AiTeamPresence> enemies) {
        float best = Float.POSITIVE_INFINITY;
        for (AiTeamPresence enemy : enemies) {
            best = Math.min(best, routeCost(route, enemy.anchor()));
        }
        return best;
    }

    private static float safeResourcePotential(AiTerrainRouteMap routes,
            List<AiStrategicResource> resources) {
        float score = 0.0F;
        for (AiStrategicResource resource : resources) {
            float distance = routeCost(routes, resource.site().center());
            if (!Float.isFinite(distance) || distance > 1500.0F) continue;
            float safety = resource.enemyInfluence() <= resource.friendlyInfluence() + 1.0F
                    ? 1.0F : 0.25F;
            score += safety / (1.0F + distance / 700.0F);
        }
        return score;
    }

    private static float minimumRouteCost(Iterable<AiTerrainRouteMap> routes,
            WorldPoint point) {
        float best = Float.POSITIVE_INFINITY;
        for (AiTerrainRouteMap route : routes) {
            best = Math.min(best, routeCost(route, point));
        }
        return best;
    }

    private static float routeCost(AiTerrainRouteMap routes, WorldPoint point) {
        if (routes == null) return Float.POSITIVE_INFINITY;
        OptionalDouble cost = routes.costTo(point);
        return cost.isPresent() ? (float) cost.getAsDouble() : Float.POSITIVE_INFINITY;
    }

    private static float nearbyChoke(AiStrategicMapSnapshot situation, WorldPoint point) {
        AiTerrainCell center = situation.terrain().cellAtWorld(point.x(), point.y());
        if (center == null) return 0.0F;
        float best = center.landChokeScore();
        for (int y = center.row() - 1; y <= center.row() + 1; y++) {
            for (int x = center.column() - 1; x <= center.column() + 1; x++) {
                AiTerrainCell cell = situation.terrain().cell(x, y);
                if (cell != null) best = Math.max(best, cell.landChokeScore());
            }
        }
        return best;
    }

    private static float dynamicLandFrontScore(AiStrategicMapSnapshot situation,
            AiTerrainCell terrain) {
        AiInfluenceCell influence = situation.cell(terrain.column(), terrain.row());
        if (influence == null || !influence.frontline()) return 0.0F;
        AiMovementDomain domain = influence.frontlineDomain().orElse(AiMovementDomain.LAND);
        if (domain == AiMovementDomain.AIR || domain == AiMovementDomain.WATER) return 0.0F;
        int units = influence.ownUnitCount() + influence.alliedUnitCount()
                + influence.enemyUnitCount();
        float presence = Math.min(1.0F, units / 8.0F);
        float strength = Math.min(1.0F,
                (influence.friendlyInfluence() + influence.enemyInfluence()) / 160.0F);
        if (presence < 0.25F && strength < 0.30F) return 0.0F;
        return influence.frontlineScore() * 0.55F
                + presence * 0.25F + strength * 0.20F;
    }

    private static long resourceKey(AiStrategicResource resource) {
        return ((long) resource.site().tileX() << 32)
                ^ (resource.site().tileY() & 0xffffffffL);
    }

    private static final class ContestedSite {
        final long key;
        final WorldPoint frontPoint;
        final boolean openingSuitable;

        ContestedSite(long key, WorldPoint frontPoint, boolean openingSuitable) {
            this.key = key;
            this.frontPoint = frontPoint;
            this.openingSuitable = openingSuitable;
        }
    }
}
