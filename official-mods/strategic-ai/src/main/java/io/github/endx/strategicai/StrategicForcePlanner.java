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
import io.github.endx.rustedfabricapi.api.client.RustedWarfareClient;
import io.github.endx.rustedfabricapi.api.game.UnitView;
import io.github.endx.rustedfabricapi.api.world.WorldPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
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
    private static final long ORDER_LEASE_CYCLES = 8L;
    private static final float MINIMUM_STANDOFF_ADVANTAGE = 8.0F;
    private static final float STANDOFF_SAFETY_PADDING = 3.0F;
    private static final float STANDOFF_INNER_PADDING = 2.0F;
    private final Map<Long, Long> orderLeases = new HashMap<Long, Long>();
    private final Map<Long, AiForceRole> forceRoles = new HashMap<Long, AiForceRole>();
    private StrategicPosture posture;
    private StrategicAirPlan.Mode airMode;

    void update(AiTickContext context, AiStrategicMapSnapshot situation, long cycle,
            StrategicResourceCampaign resourceCampaign, StrategicTeamPlan teamPlan,
            StrategicFrontState frontState) {
        orderLeases.entrySet().removeIf(entry -> entry.getValue() <= cycle);
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
            roleCandidates.add(new ForceRoleAllocator.Candidate(unit.id(),
                    capabilities.movementSpeed(), unit.maxHealth()));
        }
        int homeThreats = countHomeThreats(homeAnchors, currentEnemies);
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
            if (commandStructuredFront(context,
                    situation, domain, units, teamPlan, frontState, cycle)) continue;

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
            List<UnitView> units, StrategicTeamPlan teamPlan,
            StrategicFrontState frontState, long cycle) {
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
        if (frontState.mode() == StrategicFrontState.Mode.OPEN) {
            if (!arrived(units, rally)) attackMove(context, units, rally, cycle);
            return true;
        }
        if (frontState.mode() == StrategicFrontState.Mode.MUSTER) {
            if (!arrived(units, rally)) {
                context.orders().move(units, rally.x(), rally.y());
                markAssigned(units, cycle);
            }
            return true;
        }
        UnitView defense = frontState.primaryDefense();
        if (defense == null) return false;
        if (frontState.mode() == StrategicFrontState.Mode.ASSAULT) {
            engageTarget(context, situation, domain, units, defense, cycle);
            return true;
        }
        ArrayList<UnitView> safePressure = new ArrayList<UnitView>();
        ArrayList<UnitView> reserve = new ArrayList<UnitView>();
        for (UnitView unit : units) {
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
            if (target.building()
                    && engagement.hasSafeStandoffWindow(MINIMUM_STANDOFF_ADVANTAGE)) {
                if (engagement.attackerWithinRange() && !engagement.defenderWithinRange()) {
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
