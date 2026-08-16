package io.github.endx.strategicai;

import io.github.endx.rustedfabricapi.api.ai.AiMovementDomain;
import io.github.endx.rustedfabricapi.api.ai.AiEngagementAssessment;
import io.github.endx.rustedfabricapi.api.ai.AiForceRole;
import io.github.endx.rustedfabricapi.api.ai.AiResourceControl;
import io.github.endx.rustedfabricapi.api.ai.AiResourceObjectiveKind;
import io.github.endx.rustedfabricapi.api.ai.AiStrategicMapSnapshot;
import io.github.endx.rustedfabricapi.api.ai.AiStrategicResource;
import io.github.endx.rustedfabricapi.api.ai.AiTerrainCell;
import io.github.endx.rustedfabricapi.api.ai.AiTickContext;
import io.github.endx.rustedfabricapi.api.ai.AiUnitCapabilities;
import io.github.endx.rustedfabricapi.api.ai.AiUnitTypeCapabilities;
import io.github.endx.rustedfabricapi.api.client.RustedWarfareClient;
import io.github.endx.rustedfabricapi.api.game.UnitView;
import io.github.endx.rustedfabricapi.api.world.WorldPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rustedwarfare.unit.Unit;

/** Forms combat units into movement-compatible groups, defenses, and reinforcement streams. */
final class StrategicForcePlanner {
    private static final int MINIMUM_ATTACK_GROUP = 3;
    private static final float LOCAL_BUILDING_DISTANCE = 650.0F;
    private static final float RESOURCE_DEFENDER_DISTANCE = 220.0F;
    private static final float HOME_DEFENSE_DISTANCE = 620.0F;
    private static final float RALLY_FORWARD_DISTANCE = 260.0F;
    private static final float RALLY_ARRIVAL_DISTANCE = 65.0F;
    private static final long ORDER_LEASE_CYCLES = 8L;
    private static final float MINIMUM_STANDOFF_ADVANTAGE = 8.0F;
    private static final float STANDOFF_SAFETY_PADDING = 3.0F;
    private static final float STANDOFF_INNER_PADDING = 2.0F;
    private static final long RETREAT_LEASE_CYCLES = 12L;
    private static final float RETREAT_STEP = 220.0F;
    private static final long REACTIVE_LEASE_CYCLES = 2L;
    private static final long FRONT_TARGET_LEASE_CYCLES = 12L;
    private static final float FRONT_TARGET_RADIUS = 540.0F;
    private static final float TACTICAL_MINIMUM_SCAN_RADIUS = 420.0F;
    private static final float TACTICAL_MAXIMUM_SCAN_RADIUS = 680.0F;
    private static final float LOCAL_BALANCE_RADIUS = 240.0F;
    private static final long TACTICAL_LEASE_CYCLES = 2L;
    private final Map<Long, Long> orderLeases = new HashMap<Long, Long>();
    private final Map<Long, Long> retreatLeases = new HashMap<Long, Long>();
    private final Map<Long, Long> reactiveLeases = new HashMap<Long, Long>();
    private final Map<Long, AiForceRole> forceRoles = new HashMap<Long, AiForceRole>();
    private final EnumMap<AiMovementDomain, TargetLease> frontTargetLeases =
            new EnumMap<AiMovementDomain, TargetLease>(AiMovementDomain.class);
    private final Map<Long, TacticalLease> tacticalLeases =
            new HashMap<Long, TacticalLease>();
    private StrategicPosture posture;
    private StrategicAirPlan.Mode airMode;
    private StrategicFrontState.Mode frontMode;
    private long latestMicroCycle;

    void onStrategicReplan() {
        orderLeases.clear();
        retreatLeases.clear();
        reactiveLeases.clear();
        forceRoles.clear();
        frontTargetLeases.clear();
        tacticalLeases.clear();
        posture = null;
        airMode = null;
        frontMode = null;
    }

    void updateMicro(AiTickContext context, AiStrategicMapSnapshot situation,
            long cycle, StrategicTeamPlan teamPlan) {
        latestMicroCycle = cycle;
        tacticalLeases.entrySet().removeIf(entry -> entry.getValue().untilCycle <= cycle);
        if (teamPlan == null) return;
        List<UnitView> own = context.world().own();
        List<UnitView> enemies = context.world().enemies();
        if (own.isEmpty() || enemies.isEmpty()) return;
        ArrayList<UnitView> friendlies = new ArrayList<UnitView>(
                own.size() + context.world().allies().size());
        friendlies.addAll(own);
        friendlies.addAll(context.world().allies());
        TacticalIndex index = new TacticalIndex(friendlies, enemies);
        Map<Long, UnitView> enemiesById = indexById(enemies);
        for (UnitView unit : own) {
            if (!unit.alive()) continue;
            AiUnitCapabilities capabilities = AiUnitCapabilities.capture(unit);
            if (!capabilities.mobileCombatUnit()) continue;
            if (UnitMicroPolicy.recovered(unit.healthFraction())) {
                retreatLeases.remove(unit.id());
            }
            TacticalLease previous = tacticalLeases.get(unit.id());
            float scanRadius = clamp(capabilities.maximumAttackRange() + 260.0F,
                    TACTICAL_MINIMUM_SCAN_RADIUS, TACTICAL_MAXIMUM_SCAN_RADIUS);
            UnitView target = selectTacticalTarget(situation, unit, index, enemiesById,
                    previous, scanRadius);
            if (target == null) {
                tacticalLeases.remove(unit.id());
                continue;
            }
            AiEngagementAssessment engagement =
                    AiEngagementAssessment.capture(unit, target);
            AiUnitCapabilities targetCapabilities = AiUnitCapabilities.capture(target);
            float localRatio = localStrengthRatio(unit, target, index);
            UnitMicroPolicy.Decision decision = UnitMicroPolicy.selectLive(
                    unit.healthFraction(), unit.recentDamager(1.5F).isPresent(),
                    engagement.canEngage(), engagement.canReturnFire(),
                    engagement.attackerRange(), engagement.returnFireRange(),
                    engagement.centerDistance(), capabilities.movementSpeed(),
                    targetCapabilities.movementSpeed(), localRatio);
            applyTacticalDecision(context, situation, unit, target,
                    engagement, decision, previous, teamPlan.ownAnchor(), cycle);
        }
    }

    void update(AiTickContext context, AiStrategicMapSnapshot situation, long cycle,
            StrategicResourceCampaign resourceCampaign, StrategicTeamPlan teamPlan,
            StrategicFrontState frontState) {
        orderLeases.entrySet().removeIf(entry -> entry.getValue() <= cycle);
        retreatLeases.entrySet().removeIf(entry -> entry.getValue() <= cycle);
        reactiveLeases.entrySet().removeIf(entry -> entry.getValue() <= cycle);
        tacticalLeases.entrySet().removeIf(
                entry -> entry.getValue().untilCycle <= latestMicroCycle);
        int minimumAttackGroup = RustedWarfareClient.isSandboxMode()
                ? 1 : MINIMUM_ATTACK_GROUP;
        EnumMap<AiMovementDomain, List<UnitView>> idleGroups =
                new EnumMap<AiMovementDomain, List<UnitView>>(AiMovementDomain.class);
        EnumMap<AiMovementDomain, List<UnitView>> activeGroups =
                new EnumMap<AiMovementDomain, List<UnitView>>(AiMovementDomain.class);
        ArrayList<UnitView> homeAnchors = new ArrayList<UnitView>();
        ArrayList<UnitView> combatUnits = new ArrayList<UnitView>();
        ArrayList<UnitView> airCombatUnits = new ArrayList<UnitView>();
        ArrayList<ForceRoleAllocator.Candidate> roleCandidates =
                new ArrayList<ForceRoleAllocator.Candidate>();
        List<UnitView> currentOwn = context.world().own();
        List<UnitView> currentEnemies = context.world().enemies();
        Map<Long, UnitView> enemiesById = indexById(currentEnemies);
        for (UnitView unit : currentOwn) {
            if (!unit.alive()) continue;
            AiUnitCapabilities capabilities = AiUnitCapabilities.capture(unit);
            if (unit.building() || capabilities.builder()) homeAnchors.add(unit);
            if (!capabilities.mobileCombatUnit()) continue;
            if (capabilities.movementDomain() == AiMovementDomain.AIR) {
                airCombatUnits.add(unit);
                continue;
            }
            combatUnits.add(unit);
        }
        commandEmergencyRetreat(context, situation, combatUnits,
                currentEnemies, enemiesById, teamPlan.ownAnchor(), cycle);
        commandEmergencyRetreat(context, situation, airCombatUnits,
                currentEnemies, enemiesById, teamPlan.ownAnchor(), cycle);
        combatUnits.removeIf(unit -> retreatLeases.containsKey(unit.id()));
        airCombatUnits.removeIf(unit -> retreatLeases.containsKey(unit.id()));
        Set<Long> reactiveUnits = commandReactiveMicro(context, situation,
                combatUnits, enemiesById, cycle);
        for (UnitView unit : combatUnits) {
            AiUnitCapabilities capabilities = AiUnitCapabilities.capture(unit);
            roleCandidates.add(new ForceRoleAllocator.Candidate(unit.id(),
                    capabilities.movementSpeed(), unit.maxHealth()));
        }
        int homeThreats = countHomeThreats(homeAnchors, currentEnemies);
        if (frontState != null && frontState.mode() != frontMode) {
            frontMode = frontState.mode();
            frontTargetLeases.clear();
            for (Map.Entry<Long, AiForceRole> entry : forceRoles.entrySet()) {
                if (entry.getValue() == AiForceRole.FRONTLINE) {
                    orderLeases.remove(entry.getKey());
                }
            }
        }
        commandAirCampaign(context, situation, airCombatUnits,
                teamPlan, cycle);
        StrategicPosture nextPosture = StrategicPosture.select(combatUnits.size(), homeThreats,
                hasUnclaimedResources(situation));
        Map<Long, AiForceRole> nextRoles = ForceRoleAllocator.allocate(
                roleCandidates, nextPosture, homeThreats);
        if (teamPlan.leadsFrontline()) {
            nextRoles.replaceAll((id, role) -> role == AiForceRole.RAIDER
                    ? AiForceRole.FRONTLINE : role);
        }
        allocateResourceEscorts(nextRoles, combatUnits, resourceCampaign,
                situation, teamPlan);
        applyRolePlan(nextRoles);
        if (posture != nextPosture) {
            posture = nextPosture;
            System.out.println("[Strategic AI] Team " + context.team().id()
                    + " posture=" + posture + ", combat=" + combatUnits.size()
                    + ", homeThreats=" + homeThreats);
        }
        for (UnitView unit : combatUnits) {
            if (tacticallyControlled(unit.id())) continue;
            if (reactiveUnits.contains(unit.id())) continue;
            AiUnitCapabilities capabilities = AiUnitCapabilities.capture(unit);
            AiForceRole role = forceRoles.get(unit.id());
            EnumMap<AiMovementDomain, List<UnitView>> destination =
                    orderLeases.containsKey(unit.id()) ? activeGroups : idleGroups;
            if (destination == activeGroups && role != AiForceRole.FRONTLINE) continue;
            destination.computeIfAbsent(capabilities.movementDomain(), ignored ->
                    new ArrayList<UnitView>()).add(unit);
        }
        for (Map.Entry<AiMovementDomain, List<UnitView>> entry : idleGroups.entrySet()) {
            AiMovementDomain domain = entry.getKey();
            List<UnitView> units = entry.getValue();
            units.sort(Comparator.comparingLong(UnitView::id));

            List<UnitView> defenders = roleUnits(units, AiForceRole.STATIC_DEFENSE);
            List<UnitView> raiders = roleUnits(units, AiForceRole.RAIDER);
            List<UnitView> escorts = roleUnits(units, AiForceRole.RESOURCE_ESCORT);
            units = roleUnits(units, AiForceRole.FRONTLINE);

            if (!defenders.isEmpty()) {
                commandDefense(context, situation, domain, defenders,
                        homeAnchors, currentEnemies, cycle);
            }
            if (!raiders.isEmpty()) {
                commandRaid(context, situation, domain, raiders, currentEnemies, cycle);
            }
            if (!escorts.isEmpty()) {
                commandResourceEscort(context, situation, domain, escorts,
                        currentEnemies, resourceCampaign, cycle);
            }
            if (units.isEmpty()) continue;

            UnitView defensiveTarget = selectDefensiveTarget(situation, domain, units,
                    homeAnchors, currentEnemies);
            if (defensiveTarget != null) {
                engageTarget(context, situation, domain, units, defensiveTarget, cycle);
                continue;
            }

            // Every allied position reinforces the shared land front. Position doctrine changes
            // production and timing; it must not leave newly produced T2 units at their factory.
            List<UnitView> activeFront = activeGroups.get(domain);
            if (activeFront == null) activeFront = Collections.emptyList();
            if (commandStructuredFront(context,
                    situation, domain, units, activeFront, currentEnemies,
                    teamPlan, frontState, minimumAttackGroup, cycle)) continue;

            Objective objective = selectObjective(situation, domain, units, currentEnemies);
            if (objective == null) continue;
            if (units.size() < minimumAttackGroup) {
                List<UnitView> active = activeGroups.get(domain);
                WorldPoint destination = active == null || active.isEmpty()
                        ? rallyPoint(situation, domain, units, homeAnchors, objective.point)
                        : reachableCentroid(situation, domain, units, active);
                if (destination != null && !arrived(units, destination)) {
                    attackMove(context, units, destination, cycle);
                }
                continue;
            }
            if (objective.target != null) {
                engageTarget(context, situation, domain, units, objective.target, cycle);
            } else {
                attackMove(context, units, objective.point, cycle);
            }
        }
    }

    private void commandEmergencyRetreat(AiTickContext context,
            AiStrategicMapSnapshot situation, List<UnitView> units,
            List<UnitView> enemies, Map<Long, UnitView> enemiesById,
            WorldPoint home, long cycle) {
        for (UnitView unit : units) {
            if (tacticallyControlled(unit.id())) continue;
            boolean active = retreatLeases.containsKey(unit.id());
            if (active && UnitMicroPolicy.recovered(unit.healthFraction())) {
                retreatLeases.remove(unit.id());
                orderLeases.remove(unit.id());
                active = false;
            }
            UnitView recent = unit.recentDamager(3.0F).orElse(null);
            recent = recent != null ? enemiesById.get(recent.id()) : null;
            UnitView threat = recent;
            if (threat == null && unit.healthFraction() <= 0.22F) {
                threat = closestImmediateThreat(unit, enemies);
            }
            AiEngagementAssessment self = threat != null
                    ? AiEngagementAssessment.capture(unit, threat) : null;
            AiEngagementAssessment incoming = threat != null
                    ? AiEngagementAssessment.capture(threat, unit) : null;
            boolean underThreat = incoming != null && incoming.canEngage()
                    && incoming.centerDistance() <= incoming.attackerRange() + 80.0F;
            UnitMicroPolicy.Decision decision = UnitMicroPolicy.select(
                    unit.healthFraction(), recent != null, underThreat,
                    self != null && self.canEngage(),
                    self != null && self.hasSafeStandoffWindow(MINIMUM_STANDOFF_ADVANTAGE),
                    self != null && self.attackerWithinRange(),
                    incoming != null && incoming.attackerWithinRange());
            if (!active && decision != UnitMicroPolicy.Decision.RETREAT) continue;
            WorldPoint destination = retreatPoint(situation, unit, home, threat);
            if (destination == null) continue;
            if (!active || !orderLeases.containsKey(unit.id())) {
                context.orders().move(Collections.singletonList(unit),
                        destination.x(), destination.y());
                markAssigned(Collections.singletonList(unit), cycle);
            }
            if (!active) retreatLeases.put(unit.id(), cycle + RETREAT_LEASE_CYCLES);
        }
    }

    private Set<Long> commandReactiveMicro(AiTickContext context,
            AiStrategicMapSnapshot situation, List<UnitView> units,
            Map<Long, UnitView> enemiesById, long cycle) {
        HashSet<Long> controlled = new HashSet<Long>();
        for (UnitView unit : units) {
            if (tacticallyControlled(unit.id())) continue;
            if (reactiveLeases.containsKey(unit.id())) {
                controlled.add(unit.id());
                continue;
            }
            UnitView attacker = unit.recentDamager(2.25F).orElse(null);
            attacker = attacker != null ? enemiesById.get(attacker.id()) : null;
            if (attacker == null) {
                continue;
            }
            AiEngagementAssessment own = AiEngagementAssessment.capture(unit, attacker);
            AiEngagementAssessment incoming = AiEngagementAssessment.capture(attacker, unit);
            if (!own.canEngage() || !own.attackerWithinRange()
                    && !incoming.attackerWithinRange()) {
                continue;
            }
            engageTarget(context, situation,
                    AiUnitCapabilities.capture(unit).movementDomain(),
                    Collections.singletonList(unit), attacker, cycle);
            reactiveLeases.put(unit.id(), cycle + REACTIVE_LEASE_CYCLES);
            controlled.add(unit.id());
        }
        return controlled;
    }

    private static UnitView closestImmediateThreat(UnitView unit,
            List<UnitView> enemies) {
        UnitView best = null;
        float bestDistance = Float.POSITIVE_INFINITY;
        for (UnitView enemy : enemies) {
            if (!enemy.alive()) continue;
            AiEngagementAssessment incoming = AiEngagementAssessment.capture(enemy, unit);
            if (!incoming.canEngage()
                    || incoming.centerDistance() > incoming.attackerRange() + 80.0F) continue;
            if (incoming.centerDistance() < bestDistance || incoming.centerDistance() == bestDistance
                    && (best == null || enemy.id() < best.id())) {
                best = enemy;
                bestDistance = incoming.centerDistance();
            }
        }
        return best;
    }

    private static WorldPoint retreatPoint(AiStrategicMapSnapshot situation,
            UnitView unit, WorldPoint home, UnitView threat) {
        WorldPoint current = new WorldPoint(unit.x(), unit.y());
        AiMovementDomain domain = AiUnitCapabilities.capture(unit).movementDomain();
        if (home != null) {
            List<WorldPoint> path = situation.terrain().routesFrom(current, domain).pathTo(home);
            if (!path.isEmpty()) {
                WorldPoint previous = current;
                float travelled = 0.0F;
                for (WorldPoint point : path) {
                    travelled += (float) Math.sqrt(previous.distanceSquared(point));
                    if (travelled >= RETREAT_STEP) return point;
                    previous = point;
                }
                return path.get(path.size() - 1);
            }
            if (domain == AiMovementDomain.AIR) {
                return ForceCoordinationGeometry.advance(current, home, RETREAT_STEP);
            }
        }
        if (threat == null) return null;
        float dx = unit.x() - threat.x();
        float dy = unit.y() - threat.y();
        float length = (float) Math.hypot(dx, dy);
        if (length < 0.01F) { dx = 1.0F; dy = 0.0F; length = 1.0F; }
        float maxX = situation.terrain().worldWidth() - 1.0F;
        float maxY = situation.terrain().worldHeight() - 1.0F;
        return new WorldPoint(clamp(unit.x() + dx / length * RETREAT_STEP, 1.0F, maxX),
                clamp(unit.y() + dy / length * RETREAT_STEP, 1.0F, maxY));
    }

    private static Map<Long, UnitView> indexById(List<UnitView> units) {
        HashMap<Long, UnitView> result = new HashMap<Long, UnitView>();
        for (UnitView unit : units) result.put(unit.id(), unit);
        return result;
    }

    private UnitView selectTacticalTarget(AiStrategicMapSnapshot situation,
            UnitView unit, TacticalIndex index, Map<Long, UnitView> enemiesById,
            TacticalLease previous, float scanRadius) {
        ArrayList<UnitView> candidates = index.enemiesNear(unit.x(), unit.y(), scanRadius);
        UnitView recent = unit.recentDamager(1.5F).orElse(null);
        recent = recent != null ? enemiesById.get(recent.id()) : null;
        if (recent != null && !containsUnit(candidates, recent.id())) candidates.add(recent);
        UnitView best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        AiMovementDomain domain = AiUnitCapabilities.capture(unit).movementDomain();
        WorldPoint origin = new WorldPoint(unit.x(), unit.y());
        for (UnitView candidate : candidates) {
            if (!candidate.alive()) continue;
            AiEngagementAssessment own = AiEngagementAssessment.capture(unit, candidate);
            AiEngagementAssessment incoming = AiEngagementAssessment.capture(candidate, unit);
            if (!own.canEngage() && !incoming.canEngage()) continue;
            float interaction = Math.max(own.attackerRange(), incoming.attackerRange()) + 180.0F;
            boolean recentThreat = recent != null && candidate.id() == recent.id();
            if (!recentThreat && own.centerDistance() > interaction) continue;
            WorldPoint point = new WorldPoint(candidate.x(), candidate.y());
            if (!own.attackerWithinRange() && domain != AiMovementDomain.AIR
                    && !reachable(situation, domain, origin, point)) continue;
            AiUnitTypeCapabilities type = typeCapabilities(candidate);
            double score = -own.centerDistance() * 0.55D
                    + (1.0F - candidate.healthFraction()) * 135.0D;
            if (own.attackerWithinRange()) score += 85.0D;
            if (incoming.attackerWithinRange()) score += 175.0D;
            if (recentThreat) score += 210.0D;
            if (previous != null && previous.targetId == candidate.id()
                    && previous.untilCycle > latestMicroCycle) score += 70.0D;
            if (type != null) {
                score += type.estimatedSustainedDps() * 7.0D
                        + type.maximumAttackRange() * 0.16D;
            }
            if (candidate.constructionProgress() < 0.98F) score += 180.0D;
            if (score > bestScore || score == bestScore
                    && (best == null || candidate.id() < best.id())) {
                best = candidate;
                bestScore = score;
            }
        }
        return best;
    }

    private static float localStrengthRatio(UnitView unit, UnitView target,
            TacticalIndex index) {
        float x = (unit.x() + target.x()) * 0.5F;
        float y = (unit.y() + target.y()) * 0.5F;
        float friendly = liveCombatPower(unit);
        float hostile = liveCombatPower(target);
        for (UnitView ally : index.friendliesNear(x, y, LOCAL_BALANCE_RADIUS)) {
            if (!ally.alive() || ally.id() == unit.id()) continue;
            AiUnitCapabilities capabilities = AiUnitCapabilities.capture(ally);
            if (!capabilities.mobileCombatUnit()) continue;
            if (AiEngagementAssessment.capture(ally, target).canEngage()) {
                friendly += liveCombatPower(ally);
            }
        }
        for (UnitView enemy : index.enemiesNear(x, y, LOCAL_BALANCE_RADIUS)) {
            if (!enemy.alive() || enemy.id() == target.id()) continue;
            AiUnitCapabilities capabilities = AiUnitCapabilities.capture(enemy);
            if (!capabilities.attacker()) continue;
            if (AiEngagementAssessment.capture(enemy, unit).canEngage()) {
                hostile += liveCombatPower(enemy);
            }
        }
        if (hostile <= 0.001F) return 20.0F;
        return Math.min(20.0F, friendly / hostile);
    }

    private void applyTacticalDecision(AiTickContext context,
            AiStrategicMapSnapshot situation, UnitView unit, UnitView target,
            AiEngagementAssessment engagement, UnitMicroPolicy.Decision decision,
            TacticalLease previous, WorldPoint home, long cycle) {
        if (decision == UnitMicroPolicy.Decision.ENGAGE
                || decision == UnitMicroPolicy.Decision.SUPPORT
                || decision == UnitMicroPolicy.Decision.STANDOFF) {
            tacticalLeases.remove(unit.id());
            return;
        }
        WorldPoint destination = null;
        if (decision == UnitMicroPolicy.Decision.EDGE_CONTROL) {
            destination = tacticalSpacingPoint(situation, unit, target,
                    UnitMicroPolicy.desiredEdgeDistance(engagement.attackerRange()));
        } else if (decision == UnitMicroPolicy.Decision.RETREAT) {
            destination = retreatPoint(situation, unit, home, target);
        } else if (decision == UnitMicroPolicy.Decision.DISENGAGE) {
            destination = tacticalSpacingPoint(situation, unit, target,
                    engagement.centerDistance() + 150.0F);
            if (destination == null) destination = retreatPoint(situation, unit, home, target);
        }
        boolean changed = previous == null || previous.targetId != target.id()
                || previous.decision != decision || previous.untilCycle <= cycle;
        if (destination != null && previous != null
                && previous.destination != null
                && previous.destination.distanceSquared(destination) > 14.0F * 14.0F) {
            changed = true;
        }
        List<UnitView> singleton = Collections.singletonList(unit);
        if (decision == UnitMicroPolicy.Decision.RUSH) {
            if (changed) context.orders().attack(singleton, target);
        } else if (decision == UnitMicroPolicy.Decision.HOLD_FIRE_WINDOW) {
            // Replaces a stale attack waypoint once, then native automatic fire handles the edge.
            if (changed) context.orders().move(singleton, unit.x(), unit.y());
        } else if (destination != null && changed) {
            context.orders().move(singleton, destination.x(), destination.y());
        }
        tacticalLeases.put(unit.id(), new TacticalLease(target.id(), decision,
                cycle + TACTICAL_LEASE_CYCLES, destination));
        orderLeases.remove(unit.id());
        reactiveLeases.remove(unit.id());
    }

    private static WorldPoint tacticalSpacingPoint(AiStrategicMapSnapshot situation,
            UnitView unit, UnitView target, float desiredDistance) {
        if (!Float.isFinite(desiredDistance) || desiredDistance < 0.0F) return null;
        StandoffGeometry.Position position = StandoffGeometry.position(
                unit.x(), unit.y(), unit.id(), target.x(), target.y(), desiredDistance);
        float maxX = situation.terrain().worldWidth() - 1.0F;
        float maxY = situation.terrain().worldHeight() - 1.0F;
        float x = clamp(position.x, 1.0F, maxX);
        float y = clamp(position.y, 1.0F, maxY);
        AiMovementDomain domain = AiUnitCapabilities.capture(unit).movementDomain();
        AiTerrainCell cell = situation.terrain().cellAtWorld(x, y);
        WorldPoint point = cell != null
                ? cell.representativePoint(domain).orElse(new WorldPoint(x, y))
                : new WorldPoint(x, y);
        WorldPoint current = new WorldPoint(unit.x(), unit.y());
        return reachable(situation, domain, current, point) ? point : null;
    }

    private static float liveCombatPower(UnitView unit) {
        AiUnitTypeCapabilities type = typeCapabilities(unit);
        if (type == null || !type.attacker()) return 0.0F;
        float durability = Math.max(1.0F, unit.health() + unit.shield());
        float dps = Math.max(0.04F, type.estimatedEngagementDps(180.0F));
        return (float) Math.sqrt(durability * dps);
    }

    private static AiUnitTypeCapabilities typeCapabilities(UnitView unit) {
        if (!(unit.raw() instanceof Unit)) return null;
        rustedwarfare.unit.UnitType type = ((Unit) unit.raw()).r();
        return type != null ? AiUnitTypeCapabilities.capture(type) : null;
    }

    private boolean tacticallyControlled(long unitId) {
        TacticalLease lease = tacticalLeases.get(unitId);
        return lease != null && lease.untilCycle > latestMicroCycle;
    }

    private void commandAirCampaign(AiTickContext context,
            AiStrategicMapSnapshot situation, List<UnitView> ownAir,
            StrategicTeamPlan teamPlan, long cycle) {
        if (ownAir.isEmpty()) return;
        StrategicAirPlan plan = StrategicAirPlan.assess(situation, teamPlan, cycle);
        if (plan.mode() != airMode) {
            airMode = plan.mode();
            for (UnitView unit : ownAir) orderLeases.remove(unit.id());
            System.out.println("[Strategic AI] Team " + context.team().id()
                    + " air=" + airMode
                    + (plan.strikeTarget() != null
                    ? ", strikeTarget=" + plan.strikeTarget().id() : ""));
        }
        ArrayList<UnitView> airToAir = new ArrayList<UnitView>();
        ArrayList<UnitView> airToGround = new ArrayList<UnitView>();
        for (UnitView unit : ownAir) {
            if (tacticallyControlled(unit.id())) continue;
            if (orderLeases.containsKey(unit.id())) continue;
            if (StrategicAirPlan.isAirToAir(unit)) airToAir.add(unit);
            else if (StrategicAirPlan.isAirToGroundOnly(unit)) airToGround.add(unit);
        }
        if (plan.mode() == StrategicAirPlan.Mode.REGROUP) {
            moveAir(context, airToAir, plan.staging(), cycle);
            moveAir(context, airToGround, plan.staging(), cycle);
            return;
        }
        if (plan.mode() == StrategicAirPlan.Mode.INTERCEPT
                && plan.airTarget() != null) {
            if (!airToAir.isEmpty()) {
                context.orders().attack(airToAir, plan.airTarget());
                markAssigned(airToAir, cycle);
            }
            moveAir(context, airToGround, plan.staging(), cycle);
            return;
        }
        if (plan.mode() == StrategicAirPlan.Mode.STRIKE
                && plan.strikeTarget() != null) {
            if (!airToGround.isEmpty()) {
                context.orders().attack(airToGround, plan.strikeTarget());
                markAssigned(airToGround, cycle);
            }
            if (!airToAir.isEmpty()) {
                if (plan.escortTarget() != null) {
                    context.orders().guard(airToAir, plan.escortTarget());
                } else {
                    context.orders().attackMove(airToAir,
                            plan.strikeTarget().x(), plan.strikeTarget().y());
                }
                markAssigned(airToAir, cycle);
            }
            return;
        }
        if (!airToAir.isEmpty()) {
            context.orders().attackMove(airToAir,
                    plan.patrol().x(), plan.patrol().y());
            markAssigned(airToAir, cycle);
        }
        if (!airToGround.isEmpty()) {
            UnitView escort = !airToAir.isEmpty() ? airToAir.get(0) : null;
            if (escort != null) context.orders().guard(airToGround, escort);
            else context.orders().move(airToGround,
                    plan.staging().x(), plan.staging().y());
            markAssigned(airToGround, cycle);
        }
    }

    private void moveAir(AiTickContext context, List<UnitView> units,
            WorldPoint point, long cycle) {
        if (units.isEmpty() || point == null || arrived(units, point)) return;
        context.orders().move(units, point.x(), point.y());
        markAssigned(units, cycle);
    }

    private boolean commandStructuredFront(AiTickContext context,
            AiStrategicMapSnapshot situation, AiMovementDomain domain,
            List<UnitView> units, List<UnitView> activeUnits,
            List<UnitView> enemies, StrategicTeamPlan teamPlan,
            StrategicFrontState frontState, int minimumAttackGroup, long cycle) {
        if (frontState == null || frontState.point() == null) return false;
        WorldPoint home = teamPlan.ownAnchor();
        if (home == null) return false;
        float setback = frontState.mode() == StrategicFrontState.Mode.OPEN
                ? 125.0F : 210.0F;
        WorldPoint rally = situation.terrain().routesFrom(home, domain)
                .pointBefore(frontState.point(), setback)
                .orElse(ForceCoordinationGeometry.advance(
                frontState.point(), home, setback));
        if (!reachable(situation, domain, centroid(units), rally)) return false;
        ArrayList<UnitView> wholeGroup = new ArrayList<UnitView>(activeUnits.size()
                + units.size());
        wholeGroup.addAll(activeUnits);
        wholeGroup.addAll(units);
        ArrayList<UnitView> ready = new ArrayList<UnitView>();
        ArrayList<UnitView> stragglers = new ArrayList<UnitView>();
        float rallyToFront = distance(rally, frontState.point());
        for (UnitView unit : wholeGroup) {
            boolean prepared = BattleGroupPolicy.readyForFront(
                    distance(unit, rally), distance(unit, frontState.point()), rallyToFront);
            if (prepared) ready.add(unit);
        }
        for (UnitView unit : units) {
            if (!containsUnit(ready, unit.id())) stragglers.add(unit);
        }
        boolean commit = BattleGroupPolicy.shouldCommit(frontState.mode(),
                wholeGroup.size(), ready.size(), minimumAttackGroup);
        if (!commit && frontState.mode() != StrategicFrontState.Mode.ATTRITION) {
            if (!arrived(units, rally)) {
                context.orders().move(units, rally.x(), rally.y());
                markAssigned(units, cycle);
            }
            return true;
        }
        if (!stragglers.isEmpty() && !arrived(stragglers, rally)) {
            context.orders().move(stragglers, rally.x(), rally.y());
            markAssigned(stragglers, cycle);
        }
        ArrayList<UnitView> readyIdle = new ArrayList<UnitView>();
        for (UnitView unit : units) {
            if (containsUnit(ready, unit.id())) readyIdle.add(unit);
        }
        if (readyIdle.isEmpty()) return true;
        if (frontState.mode() == StrategicFrontState.Mode.OPEN) {
            attackMove(context, readyIdle, frontState.point(), cycle);
            return true;
        }
        UnitView defense = selectLeasedFrontTarget(situation, domain,
                wholeGroup, enemies, frontState, cycle);
        if (defense == null) return true;
        if (frontState.mode() == StrategicFrontState.Mode.ASSAULT) {
            engageTarget(context, situation, domain, readyIdle, defense, cycle);
            return true;
        }
        ArrayList<UnitView> safePressure = new ArrayList<UnitView>();
        ArrayList<UnitView> reserve = new ArrayList<UnitView>();
        for (UnitView unit : readyIdle) {
            AiEngagementAssessment engagement =
                    AiEngagementAssessment.capture(unit, defense);
            if (engagement.hasSafeStandoffWindow(MINIMUM_STANDOFF_ADVANTAGE)) {
                safePressure.add(unit);
            } else {
                reserve.add(unit);
            }
        }
        if (!safePressure.isEmpty()) {
            engageTarget(context, situation, domain, safePressure, defense, cycle);
        }
        if (!reserve.isEmpty() && !arrived(reserve, rally)) {
            context.orders().move(reserve, rally.x(), rally.y());
            markAssigned(reserve, cycle);
        }
        return true;
    }

    private UnitView selectLeasedFrontTarget(AiStrategicMapSnapshot situation,
            AiMovementDomain domain, List<UnitView> units, List<UnitView> enemies,
            StrategicFrontState frontState, long cycle) {
        TargetLease lease = frontTargetLeases.get(domain);
        if (lease != null && lease.untilCycle > cycle) {
            for (UnitView enemy : enemies) {
                if (enemy.id() == lease.targetId
                        && validFrontTarget(situation, domain, units,
                        enemy, frontState.point())) return enemy;
            }
        }
        UnitView best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        WorldPoint group = centroid(units);
        for (UnitView enemy : enemies) {
            if (!validFrontTarget(situation, domain, units,
                    enemy, frontState.point())) continue;
            io.github.endx.rustedfabricapi.api.ai.AiUnitTypeCapabilities type =
                    io.github.endx.rustedfabricapi.api.ai.AiUnitTypeCapabilities.capture(
                    ((rustedwarfare.unit.Unit) enemy.raw()).r());
            float route = travelCost(situation, domain, group,
                    new WorldPoint(enemy.x(), enemy.y()));
            double score = -route * 0.70D
                    - enemy.health() * 0.025D
                    + type.estimatedSustainedDps() * 12.0D
                    + type.maximumAttackRange() * 0.45D;
            if (enemy.constructionProgress() < 0.98F) score += 260.0D;
            if (score > bestScore || score == bestScore
                    && (best == null || enemy.id() < best.id())) {
                best = enemy;
                bestScore = score;
            }
        }
        if (best != null) {
            frontTargetLeases.put(domain,
                    new TargetLease(best.id(), cycle + FRONT_TARGET_LEASE_CYCLES));
        }
        return best;
    }

    private static boolean validFrontTarget(AiStrategicMapSnapshot situation,
            AiMovementDomain domain, List<UnitView> units,
            UnitView enemy, WorldPoint front) {
        if (enemy == null || !enemy.alive() || !enemy.building()
                || !(enemy.raw() instanceof rustedwarfare.unit.Unit)) return false;
        io.github.endx.rustedfabricapi.api.ai.AiUnitTypeCapabilities type =
                io.github.endx.rustedfabricapi.api.ai.AiUnitTypeCapabilities.capture(
                ((rustedwarfare.unit.Unit) enemy.raw()).r());
        if (!type.attacker() || distance(enemy, front) > FRONT_TARGET_RADIUS) return false;
        if (!reachable(situation, domain, centroid(units),
                new WorldPoint(enemy.x(), enemy.y()))) return false;
        for (UnitView unit : units) {
            if (AiEngagementAssessment.capture(unit, enemy).canEngage()) return true;
        }
        return false;
    }

    private void applyRolePlan(Map<Long, AiForceRole> nextRoles) {
        for (Map.Entry<Long, AiForceRole> entry : nextRoles.entrySet()) {
            if (forceRoles.get(entry.getKey()) != entry.getValue()) {
                orderLeases.remove(entry.getKey());
            }
        }
        forceRoles.clear();
        forceRoles.putAll(nextRoles);
    }

    private static void allocateResourceEscorts(Map<Long, AiForceRole> roles,
            List<UnitView> combatUnits, StrategicResourceCampaign campaign,
            AiStrategicMapSnapshot situation, StrategicTeamPlan teamPlan) {
        // Rear/economy positions do not peel land armies away from the shared front to
        // chase their private mine campaign. The designated frontline alone escorts it.
        if (!teamPlan.leadsFrontline()) return;
        if (!campaign.active() || campaign.point() == null) return;
        int desired = RustedWarfareClient.isSandboxMode() ? 1
                : campaign.phase() == StrategicResourceCampaign.Phase.SECURE ? 3 : 2;
        ArrayList<UnitView> candidates = new ArrayList<UnitView>();
        for (UnitView unit : combatUnits) {
            AiForceRole role = roles.get(unit.id());
            if (role == AiForceRole.STATIC_DEFENSE) continue;
            AiMovementDomain domain = AiUnitCapabilities.capture(unit).movementDomain();
            if (!campaign.target().reachable(domain)) continue;
            candidates.add(unit);
        }
        WorldPoint point = campaign.point();
        candidates.sort(Comparator
                .comparingInt((UnitView unit) -> roles.get(unit.id()) == AiForceRole.FRONTLINE
                        ? 0 : 1)
                .thenComparingDouble(unit -> {
                    float dx = unit.x() - point.x();
                    float dy = unit.y() - point.y();
                    return dx * dx + dy * dy;
                })
                .thenComparingLong(UnitView::id));
        for (int index = 0; index < Math.min(desired, candidates.size()); index++) {
            roles.put(candidates.get(index).id(), AiForceRole.RESOURCE_ESCORT);
        }
    }

    private List<UnitView> roleUnits(List<UnitView> units, AiForceRole role) {
        ArrayList<UnitView> result = new ArrayList<UnitView>();
        for (UnitView unit : units) {
            if (forceRoles.get(unit.id()) == role) result.add(unit);
        }
        return result;
    }

    private static int countHomeThreats(List<UnitView> homeAnchors, List<UnitView> enemies) {
        if (homeAnchors.isEmpty()) return 0;
        int count = 0;
        float maximumDistanceSquared = HOME_DEFENSE_DISTANCE * HOME_DEFENSE_DISTANCE;
        for (UnitView enemy : enemies) {
            if (!enemy.alive()) continue;
            for (UnitView anchor : homeAnchors) {
                float dx = anchor.x() - enemy.x();
                float dy = anchor.y() - enemy.y();
                if (dx * dx + dy * dy <= maximumDistanceSquared) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }

    private static boolean hasUnclaimedResources(AiStrategicMapSnapshot situation) {
        for (AiStrategicResource resource : situation.resources()) {
            if (resource.control() == AiResourceControl.UNCLAIMED) return true;
        }
        return false;
    }

    private void commandDefense(AiTickContext context, AiStrategicMapSnapshot situation,
            AiMovementDomain domain, List<UnitView> defenders, List<UnitView> homeAnchors,
            List<UnitView> enemies, long cycle) {
        UnitView target = selectDefensiveTarget(
                situation, domain, defenders, homeAnchors, enemies);
        if (target != null) {
            engageTarget(context, situation, domain, defenders, target, cycle);
            return;
        }
        UnitView anchor = primaryHomeAnchor(homeAnchors);
        if (anchor != null) {
            context.orders().guard(defenders, anchor);
            markAssigned(defenders, cycle);
        }
    }

    private void commandRaid(AiTickContext context, AiStrategicMapSnapshot situation,
            AiMovementDomain domain, List<UnitView> raiders, List<UnitView> enemies, long cycle) {
        UnitView target = closestRaidTarget(situation, domain, centroid(raiders), enemies);
        if (target != null) {
            engageTarget(context, situation, domain, raiders, target, cycle);
            return;
        }
        Objective objective = selectObjective(situation, domain, raiders, enemies);
        if (objective == null) return;
        if (objective.target != null) {
            engageTarget(context, situation, domain, raiders, objective.target, cycle);
        } else {
            attackMove(context, raiders, objective.point, cycle);
        }
    }

    private void commandResourceEscort(AiTickContext context,
            AiStrategicMapSnapshot situation, AiMovementDomain domain,
            List<UnitView> escorts, List<UnitView> enemies,
            StrategicResourceCampaign campaign, long cycle) {
        WorldPoint point = campaign.point();
        if (point == null) return;
        UnitView threat = closestReachableEnemyToPoint(
                situation, domain, point, false, 340.0F, enemies);
        if (threat != null) {
            engageTarget(context, situation, domain, escorts, threat, cycle);
            return;
        }
        if (campaign.target().control() == AiResourceControl.ENEMY
                && campaign.target().occupant().isPresent()) {
            engageTarget(context, situation, domain, escorts,
                    campaign.target().occupant().get(), cycle);
            return;
        }
        if (campaign.phase() == StrategicResourceCampaign.Phase.HOLD
                && campaign.target().occupant().isPresent()) {
            context.orders().guard(escorts, campaign.target().occupant().get());
            markAssigned(escorts, cycle);
            return;
        }
        attackMove(context, escorts, point, cycle);
    }

    private static UnitView primaryHomeAnchor(List<UnitView> anchors) {
        UnitView best = null;
        for (UnitView anchor : anchors) {
            if (!anchor.alive() || !anchor.building()) continue;
            if (best == null || anchor.maxHealth() > best.maxHealth()
                    || anchor.maxHealth() == best.maxHealth() && anchor.id() < best.id()) {
                best = anchor;
            }
        }
        return best;
    }

    private static UnitView closestRaidTarget(AiStrategicMapSnapshot situation,
            AiMovementDomain domain, WorldPoint center, List<UnitView> enemies) {
        UnitView fallback = null;
        float fallbackDistance = Float.POSITIVE_INFINITY;
        UnitView economy = null;
        float economyDistance = Float.POSITIVE_INFINITY;
        for (UnitView enemy : enemies) {
            if (!enemy.alive() || !enemy.building()) continue;
            WorldPoint point = new WorldPoint(enemy.x(), enemy.y());
            float distance = travelCost(situation, domain, center, point);
            if (!Float.isFinite(distance)) continue;
            if (distance < fallbackDistance) {
                fallback = enemy;
                fallbackDistance = distance;
            }
            if (AiUnitCapabilities.capture(enemy).harvester() && distance < economyDistance) {
                economy = enemy;
                economyDistance = distance;
            }
        }
        return economy != null ? economy : fallback;
    }

    private static Objective selectObjective(AiStrategicMapSnapshot situation,
            AiMovementDomain domain, List<UnitView> units, List<UnitView> enemies) {
        WorldPoint center = centroid(units);
        AiStrategicResource bestResource = null;
        float bestResourceCost = Float.POSITIVE_INFINITY;
        for (AiStrategicResource resource : situation.resources()) {
            if (!resource.reachable(domain)) continue;
            if (resource.objective() == AiResourceObjectiveKind.LOCK_DOWN
                    || resource.objective() == AiResourceObjectiveKind.DENY) {
                float route = travelCost(situation, domain, center, resource.site().center());
                if (route < bestResourceCost) {
                    bestResource = resource;
                    bestResourceCost = route;
                }
            }
        }
        if (bestResource != null) {
            UnitView defender = closestReachableEnemyToPoint(situation, domain,
                    bestResource.site().center(), true,
                    RESOURCE_DEFENDER_DISTANCE, enemies);
            if (defender != null) return Objective.target(defender);
            return Objective.point(bestResource.site().center());
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
            float routeDistance = travelCost(situation, domain, unitCenter, enemyPoint);
            if (!Float.isFinite(routeDistance)) continue;
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
            float distance = routeDistance;
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

    private void engageTarget(AiTickContext context, AiStrategicMapSnapshot situation,
            AiMovementDomain domain, List<UnitView> units, UnitView target, long cycle) {
        ArrayList<UnitView> directAttackers = new ArrayList<UnitView>();
        ArrayList<UnitView> supportingUnits = new ArrayList<UnitView>();
        for (UnitView unit : units) {
            AiEngagementAssessment engagement = AiEngagementAssessment.capture(unit, target);
            UnitMicroPolicy.Decision decision = UnitMicroPolicy.select(
                    unit.healthFraction(), unit.recentDamager(2.25F).isPresent(),
                    engagement.defenderWithinRange(), engagement.canEngage(),
                    engagement.hasSafeStandoffWindow(MINIMUM_STANDOFF_ADVANTAGE),
                    engagement.attackerWithinRange(), engagement.defenderWithinRange());
            if (decision == UnitMicroPolicy.Decision.RETREAT) {
                markAssigned(Collections.singletonList(unit), cycle);
                continue;
            }
            if (decision == UnitMicroPolicy.Decision.HOLD_FIRE_WINDOW
                    || decision == UnitMicroPolicy.Decision.STANDOFF) {
                if (decision == UnitMicroPolicy.Decision.HOLD_FIRE_WINDOW) {
                    // No attack waypoint: staying idle preserves the native automatic fire range.
                    markAssigned(Collections.singletonList(unit), cycle);
                    continue;
                }
                WorldPoint position = standoffPoint(situation, unit, target, engagement);
                WorldPoint current = new WorldPoint(unit.x(), unit.y());
                if (position != null && reachable(situation, domain, current, position)) {
                    context.orders().move(Collections.singletonList(unit),
                            position.x(), position.y());
                    markAssigned(Collections.singletonList(unit), cycle);
                    continue;
                }
            }
            if (engagement.canEngage()) directAttackers.add(unit);
            else supportingUnits.add(unit);
        }
        if (!directAttackers.isEmpty()) {
            context.orders().attack(directAttackers, target);
            markAssigned(directAttackers, cycle);
        }
        if (!supportingUnits.isEmpty()) {
            context.orders().attackMove(supportingUnits, target.x(), target.y());
            markAssigned(supportingUnits, cycle);
        }
    }

    private void attackMove(AiTickContext context, List<UnitView> units,
            WorldPoint destination, long cycle) {
        context.orders().attackMove(units, destination.x(), destination.y());
        markAssigned(units, cycle);
    }

    private void markAssigned(List<UnitView> units, long cycle) {
        long until = cycle + ORDER_LEASE_CYCLES;
        for (UnitView unit : units) orderLeases.put(unit.id(), until);
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
        AiTerrainCell cell = situation.terrain().cellAtWorld(x, y);
        AiMovementDomain domain = AiUnitCapabilities.capture(attacker).movementDomain();
        return cell != null ? cell.representativePoint(domain)
                .orElse(new WorldPoint(x, y)) : new WorldPoint(x, y);
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
            float distance = travelCost(situation, domain, center, point);
            if (!Float.isFinite(distance)) continue;
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
        return travelCost(situation, domain, center, point) <= maximumDistance
                ? candidate : null;
    }

    private static boolean reachable(AiStrategicMapSnapshot situation,
            AiMovementDomain domain, WorldPoint from, WorldPoint to) {
        return Float.isFinite(travelCost(situation, domain, from, to));
    }

    private static float travelCost(AiStrategicMapSnapshot situation,
            AiMovementDomain domain, WorldPoint from, WorldPoint to) {
        if (domain == AiMovementDomain.AIR) {
            return (float) Math.sqrt(from.distanceSquared(to));
        }
        java.util.OptionalDouble route = situation.terrain().routesFrom(from, domain).costTo(to);
        return route.isPresent() ? (float) route.getAsDouble() : Float.POSITIVE_INFINITY;
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

    private static boolean containsUnit(List<UnitView> units, long id) {
        for (UnitView unit : units) {
            if (unit.id() == id) return true;
        }
        return false;
    }

    private static float distance(UnitView unit, WorldPoint point) {
        return (float) Math.hypot(unit.x() - point.x(), unit.y() - point.y());
    }

    private static float distance(WorldPoint first, WorldPoint second) {
        return (float) Math.sqrt(first.distanceSquared(second));
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static final class TargetLease {
        final long targetId;
        final long untilCycle;

        TargetLease(long targetId, long untilCycle) {
            this.targetId = targetId;
            this.untilCycle = untilCycle;
        }
    }

    private static final class TacticalLease {
        final long targetId;
        final UnitMicroPolicy.Decision decision;
        final long untilCycle;
        final WorldPoint destination;

        TacticalLease(long targetId, UnitMicroPolicy.Decision decision,
                long untilCycle, WorldPoint destination) {
            this.targetId = targetId;
            this.decision = decision;
            this.untilCycle = untilCycle;
            this.destination = destination;
        }
    }

    /** One-pass deterministic spatial buckets; avoids ownUnits x enemyUnits tactical scans. */
    private static final class TacticalIndex {
        private static final float CELL_SIZE = 256.0F;
        private final Map<Long, List<UnitView>> friendlies =
                new HashMap<Long, List<UnitView>>();
        private final Map<Long, List<UnitView>> enemies =
                new HashMap<Long, List<UnitView>>();

        TacticalIndex(List<UnitView> friendlyUnits, List<UnitView> enemyUnits) {
            addAll(friendlies, friendlyUnits);
            addAll(enemies, enemyUnits);
        }

        ArrayList<UnitView> friendliesNear(float x, float y, float radius) {
            return nearby(friendlies, x, y, radius);
        }

        ArrayList<UnitView> enemiesNear(float x, float y, float radius) {
            return nearby(enemies, x, y, radius);
        }

        private static void addAll(Map<Long, List<UnitView>> buckets,
                List<UnitView> units) {
            for (UnitView unit : units) {
                if (!unit.alive()) continue;
                long key = key(cell(unit.x()), cell(unit.y()));
                buckets.computeIfAbsent(key, ignored -> new ArrayList<UnitView>()).add(unit);
            }
        }

        private static ArrayList<UnitView> nearby(Map<Long, List<UnitView>> buckets,
                float x, float y, float radius) {
            ArrayList<UnitView> result = new ArrayList<UnitView>();
            float radiusSquared = radius * radius;
            int minimumX = cell(x - radius);
            int maximumX = cell(x + radius);
            int minimumY = cell(y - radius);
            int maximumY = cell(y + radius);
            for (int cellX = minimumX; cellX <= maximumX; cellX++) {
                for (int cellY = minimumY; cellY <= maximumY; cellY++) {
                    List<UnitView> bucket = buckets.get(key(cellX, cellY));
                    if (bucket == null) continue;
                    for (UnitView unit : bucket) {
                        float dx = unit.x() - x;
                        float dy = unit.y() - y;
                        if (dx * dx + dy * dy <= radiusSquared) result.add(unit);
                    }
                }
            }
            return result;
        }

        private static int cell(float coordinate) {
            return (int) Math.floor(coordinate / CELL_SIZE);
        }

        private static long key(int x, int y) {
            return ((long) x << 32) ^ (y & 0xffffffffL);
        }
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
