package io.github.endx.strategicai;

import io.github.endx.rustedfabricapi.api.ai.AiMovementDomain;
import io.github.endx.rustedfabricapi.api.ai.AiStrategicMapSnapshot;
import io.github.endx.rustedfabricapi.api.ai.AiUnitCapabilities;
import io.github.endx.rustedfabricapi.api.ai.AiUnitTypeCapabilities;
import io.github.endx.rustedfabricapi.api.game.UnitView;
import io.github.endx.rustedfabricapi.api.unit.action.UnitActions;
import io.github.endx.rustedfabricapi.api.world.WorldPoint;
import rustedwarfare.unit.Unit;
import rustedwarfare.unit.UnitType;
import rustedwarfare.unit.action.UnitAction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Deterministic alliance-wide rendezvous, patrol, interception, and strike plan. */
final class StrategicAirPlan {
    enum Mode {
        REGROUP,
        PATROL,
        INTERCEPT,
        STRIKE
    }

    private static final float ASSEMBLY_RADIUS = 330.0F;
    private static final float AIR_DEFENSE_RADIUS = 520.0F;
    private final Mode mode;
    private final WorldPoint staging;
    private final WorldPoint patrol;
    private final UnitView airTarget;
    private final UnitView strikeTarget;
    private final UnitView escortTarget;

    private StrategicAirPlan(Mode mode, WorldPoint staging, WorldPoint patrol,
            UnitView airTarget, UnitView strikeTarget, UnitView escortTarget) {
        this.mode = mode;
        this.staging = staging;
        this.patrol = patrol;
        this.airTarget = airTarget;
        this.strikeTarget = strikeTarget;
        this.escortTarget = escortTarget;
    }

    static StrategicAirPlan assess(AiStrategicMapSnapshot situation,
            StrategicTeamPlan teamPlan, long cycle) {
        ArrayList<UnitView> friendly = new ArrayList<UnitView>();
        friendly.addAll(situation.world().own());
        friendly.addAll(situation.world().allies());
        ArrayList<UnitView> friendlyAirToAir = selectAir(friendly, true);
        ArrayList<UnitView> friendlyAirToGround = selectAir(friendly, false);
        ArrayList<UnitView> enemyAirToAir = selectAir(
                situation.world().enemies(), true);
        WorldPoint home = teamPlan.alliedHomeCenter();
        WorldPoint front = teamPlan.preferredFrontierPoint();
        if (front == null) front = situation.primaryFront().orElse(home);
        if (home == null) home = new WorldPoint(
                situation.terrain().worldWidth() * 0.5F,
                situation.terrain().worldHeight() * 0.5F);
        if (front == null) front = home;
        WorldPoint staging = lerp(home, front, 0.58F);
        boolean assembled = assembled(friendlyAirToAir, staging);
        float friendlyStrength = strength(friendlyAirToAir);
        float enemyStrength = strength(enemyAirToAir);
        UnitView airTarget = closestToAny(enemyAirToAir,
                friendlyAirToAir.isEmpty() ? friendly : friendlyAirToAir, staging);
        UnitView strike = assembled(friendlyAirToGround, staging)
                ? selectStrikeTarget(situation, friendlyAirToGround,
                friendlyAirToAir, staging) : null;
        Mode mode = AirCampaignPolicy.select(assembled,
                friendlyStrength, enemyStrength, strike != null);
        UnitView escort = friendlyAirToGround.isEmpty() ? null
                : closest(friendlyAirToGround,
                strike != null ? new WorldPoint(strike.x(), strike.y()) : staging);
        return new StrategicAirPlan(mode, staging,
                patrolPoint(staging, front, cycle), airTarget, strike, escort);
    }

    Mode mode() { return mode; }
    WorldPoint staging() { return staging; }
    WorldPoint patrol() { return patrol; }
    UnitView airTarget() { return airTarget; }
    UnitView strikeTarget() { return strikeTarget; }
    UnitView escortTarget() { return escortTarget; }

    static boolean isAirToAir(UnitView unit) {
        AiUnitTypeCapabilities type = type(unit);
        return type != null && type.movementDomain() == AiMovementDomain.AIR
                && type.mobileCombatUnit() && type.airToAirSpecialist();
    }

    static boolean isAirToGroundOnly(UnitView unit) {
        AiUnitTypeCapabilities type = type(unit);
        return type != null && type.movementDomain() == AiMovementDomain.AIR
                && type.mobileCombatUnit() && type.canAttackGround()
                && !type.airToAirSpecialist();
    }

    private static ArrayList<UnitView> selectAir(List<UnitView> units,
            boolean airToAir) {
        ArrayList<UnitView> result = new ArrayList<UnitView>();
        for (UnitView unit : units) {
            if (airToAir ? isAirToAir(unit) : isAirToGroundOnly(unit)) result.add(unit);
        }
        result.sort(Comparator.comparingLong(UnitView::id));
        return result;
    }

    private static boolean assembled(List<UnitView> units, WorldPoint staging) {
        if (units.isEmpty()) return true;
        int near = 0;
        float radiusSquared = ASSEMBLY_RADIUS * ASSEMBLY_RADIUS;
        for (UnitView unit : units) {
            float dx = unit.x() - staging.x();
            float dy = unit.y() - staging.y();
            if (dx * dx + dy * dy <= radiusSquared) near++;
        }
        int required = Math.max(1, (int) Math.ceil(units.size() * 0.65D));
        return near >= required;
    }

    private static UnitView selectStrikeTarget(AiStrategicMapSnapshot situation,
            List<UnitView> airToGround, List<UnitView> escorts, WorldPoint staging) {
        if (airToGround.isEmpty()) return null;
        float strikeStrength = strength(airToGround) + strength(escorts) * 0.28F;
        UnitView best = null;
        float bestScore = Float.NEGATIVE_INFINITY;
        for (UnitView enemy : situation.world().enemies()) {
            if (!enemy.alive() || !enemy.building() || !(enemy.raw() instanceof Unit)) continue;
            float defense = airDefenseStrength(
                    situation.world().enemies(), new WorldPoint(enemy.x(), enemy.y()));
            if (defense > 0.01F && strikeStrength < defense * 1.32F) continue;
            AiUnitCapabilities live = AiUnitCapabilities.capture(enemy);
            AiUnitTypeCapabilities type = type(enemy);
            float value = type.creditCost() + enemy.maxHealth() * 0.35F;
            if (live.harvester()) value += 2600.0F;
            if (offersMobileProduction((Unit) enemy.raw())) value += 2200.0F;
            float distance = (float) Math.sqrt(staging.distanceSquared(
                    new WorldPoint(enemy.x(), enemy.y())));
            float score = value / (1.0F + defense * 0.12F) - distance * 0.22F;
            if (score > bestScore || score == bestScore
                    && (best == null || enemy.id() < best.id())) {
                best = enemy;
                bestScore = score;
            }
        }
        return best;
    }

    private static float airDefenseStrength(List<UnitView> enemies, WorldPoint point) {
        float result = 0.0F;
        float radiusSquared = AIR_DEFENSE_RADIUS * AIR_DEFENSE_RADIUS;
        for (UnitView enemy : enemies) {
            AiUnitTypeCapabilities type = type(enemy);
            if (type == null || !type.attacker() || !type.canAttackAir()) continue;
            float dx = enemy.x() - point.x();
            float dy = enemy.y() - point.y();
            float distanceSquared = dx * dx + dy * dy;
            if (distanceSquared > radiusSquared) continue;
            float coverage = 1.0F + type.maximumAttackRange() / 350.0F;
            result += (float) Math.sqrt(Math.max(1.0F,
                    enemy.health() + enemy.shield()))
                    * Math.max(0.02F, type.estimatedAirDps()) * coverage;
        }
        return result;
    }

    private static float strength(List<UnitView> units) {
        float result = 0.0F;
        for (UnitView unit : units) {
            AiUnitTypeCapabilities type = type(unit);
            if (type == null) continue;
            float relevantDps = type.airToAirSpecialist()
                    ? type.estimatedAirDps() : type.estimatedGroundDps();
            result += (float) Math.sqrt(Math.max(1.0F,
                    unit.health() + unit.shield()))
                    * Math.max(0.02F, relevantDps)
                    * (1.0F + type.maximumAttackRange() / 500.0F);
        }
        return result;
    }

    private static boolean offersMobileProduction(Unit producer) {
        for (UnitAction action : UnitActions.forUnit(producer)) {
            UnitType product = action.getBuildUnitType();
            if (action.isBuildAction() && product != null && !product.isBuilding()
                    && AiUnitTypeCapabilities.capture(product).mobileCombatUnit()) return true;
        }
        return false;
    }

    private static AiUnitTypeCapabilities type(UnitView unit) {
        return unit.raw() instanceof Unit && ((Unit) unit.raw()).r() != null
                ? AiUnitTypeCapabilities.capture(((Unit) unit.raw()).r()) : null;
    }

    private static UnitView closest(List<UnitView> units, WorldPoint point) {
        UnitView best = null;
        float bestDistance = Float.POSITIVE_INFINITY;
        for (UnitView unit : units) {
            float distance = point.distanceSquared(new WorldPoint(unit.x(), unit.y()));
            if (distance < bestDistance || distance == bestDistance
                    && (best == null || unit.id() < best.id())) {
                best = unit;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static UnitView closestToAny(List<UnitView> targets,
            List<UnitView> hunters, WorldPoint fallback) {
        UnitView best = null;
        float bestDistance = Float.POSITIVE_INFINITY;
        for (UnitView target : targets) {
            float distance = fallback.distanceSquared(
                    new WorldPoint(target.x(), target.y()));
            for (UnitView hunter : hunters) {
                float dx = target.x() - hunter.x();
                float dy = target.y() - hunter.y();
                distance = Math.min(distance, dx * dx + dy * dy);
            }
            if (distance < bestDistance || distance == bestDistance
                    && (best == null || target.id() < best.id())) {
                best = target;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static WorldPoint patrolPoint(WorldPoint staging, WorldPoint front, long cycle) {
        float dx = front.x() - staging.x();
        float dy = front.y() - staging.y();
        float length = (float) Math.hypot(dx, dy);
        if (length < 1.0F) { dx = 1.0F; dy = 0.0F; }
        else { dx /= length; dy /= length; }
        float rightX = -dy;
        float rightY = dx;
        int phase = Math.floorMod((int) (cycle / 6L), 4);
        float forward = phase == 0 ? 130.0F : phase == 2 ? -80.0F : 50.0F;
        float side = phase == 1 ? 260.0F : phase == 3 ? -260.0F : 0.0F;
        return new WorldPoint(staging.x() + dx * forward + rightX * side,
                staging.y() + dy * forward + rightY * side);
    }

    private static WorldPoint lerp(WorldPoint from, WorldPoint to, float amount) {
        return new WorldPoint(from.x() + (to.x() - from.x()) * amount,
                from.y() + (to.y() - from.y()) * amount);
    }
}
