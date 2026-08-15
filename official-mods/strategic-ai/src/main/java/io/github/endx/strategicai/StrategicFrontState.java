package io.github.endx.strategicai;

import io.github.endx.rustedfabricapi.api.ai.AiUnitCapabilities;
import io.github.endx.rustedfabricapi.api.ai.AiUnitTypeCapabilities;
import io.github.endx.rustedfabricapi.api.game.UnitView;
import io.github.endx.rustedfabricapi.api.world.WorldPoint;
import rustedwarfare.unit.Unit;

import java.util.List;

/** Aggregate tower-line assessment used by both production and force control. */
final class StrategicFrontState {
    enum Mode {
        OPEN,
        ATTRITION,
        MUSTER,
        ASSAULT
    }

    private static final float FRONT_RADIUS = 520.0F;
    private final Mode mode;
    private final WorldPoint point;
    private final UnitView primaryDefense;
    private final float exchangeRatio;
    private final int friendlyUnits;
    private final int alliedUnits;
    private final int enemyDefenses;

    private StrategicFrontState(Mode mode, WorldPoint point, UnitView primaryDefense,
            float exchangeRatio, int friendlyUnits, int alliedUnits, int enemyDefenses) {
        this.mode = mode;
        this.point = point;
        this.primaryDefense = primaryDefense;
        this.exchangeRatio = exchangeRatio;
        this.friendlyUnits = friendlyUnits;
        this.alliedUnits = alliedUnits;
        this.enemyDefenses = enemyDefenses;
    }

    static StrategicFrontState assess(io.github.endx.rustedfabricapi.api.ai.AiStrategicMapSnapshot situation,
            StrategicTeamPlan teamPlan) {
        WorldPoint point = teamPlan.preferredFrontierPoint();
        if (point == null) point = situation.primaryFront().orElse(null);
        if (point == null) return new StrategicFrontState(
                Mode.OPEN, null, null, Float.POSITIVE_INFINITY, 0, 0, 0);
        CombatMass own = combatMass(situation.world().own(), point, false);
        CombatMass allies = combatMass(situation.world().allies(), point, false);
        CombatMass enemyMobile = combatMass(situation.world().enemies(), point, false);
        CombatMass enemyTowers = combatMass(situation.world().enemies(), point, true);
        UnitView primary = primaryDefense(situation.world().enemies(), point);
        if (enemyTowers.count == 0) {
            return new StrategicFrontState(Mode.OPEN, point, null,
                    Float.POSITIVE_INFINITY, own.count, allies.count, 0);
        }
        float friendlyHp = own.durability + allies.durability;
        float friendlyDps = own.dps + allies.dps;
        float enemyHp = enemyTowers.durability + enemyMobile.durability * 0.55F;
        float enemyDps = enemyTowers.dps + enemyMobile.dps * 0.65F;
        float ratio = friendlyHp * Math.max(0.01F, friendlyDps)
                / (Math.max(1.0F, enemyHp) * Math.max(0.01F, enemyDps));
        int friendlyCount = own.count + allies.count;
        Mode mode = FrontEngagementPolicy.select(
                ratio, friendlyCount, enemyTowers.count);
        return new StrategicFrontState(mode, point, primary, ratio,
                own.count, allies.count, enemyTowers.count);
    }

    Mode mode() { return mode; }
    WorldPoint point() { return point; }
    UnitView primaryDefense() { return primaryDefense; }
    float exchangeRatio() { return exchangeRatio; }
    int friendlyUnits() { return friendlyUnits; }
    int alliedUnits() { return alliedUnits; }
    int enemyDefenses() { return enemyDefenses; }

    private static CombatMass combatMass(List<UnitView> units, WorldPoint point,
            boolean buildingsOnly) {
        CombatMass result = new CombatMass();
        float radiusSquared = FRONT_RADIUS * FRONT_RADIUS;
        for (UnitView unit : units) {
            if (!unit.alive() || buildingsOnly != unit.building()) continue;
            float dx = unit.x() - point.x();
            float dy = unit.y() - point.y();
            if (dx * dx + dy * dy > radiusSquared) continue;
            AiUnitCapabilities live = AiUnitCapabilities.capture(unit);
            if (!live.attacker() || !(unit.raw() instanceof Unit)) continue;
            AiUnitTypeCapabilities type = AiUnitTypeCapabilities.capture(
                    ((Unit) unit.raw()).r());
            result.count++;
            result.durability += Math.max(1.0F, unit.health() + unit.shield());
            result.dps += type.estimatedSustainedDps();
        }
        return result;
    }

    private static UnitView primaryDefense(List<UnitView> enemies, WorldPoint point) {
        UnitView best = null;
        float bestScore = Float.NEGATIVE_INFINITY;
        float radiusSquared = FRONT_RADIUS * FRONT_RADIUS;
        for (UnitView enemy : enemies) {
            if (!enemy.alive() || !enemy.building() || !(enemy.raw() instanceof Unit)) continue;
            float dx = enemy.x() - point.x();
            float dy = enemy.y() - point.y();
            if (dx * dx + dy * dy > radiusSquared) continue;
            AiUnitTypeCapabilities type = AiUnitTypeCapabilities.capture(
                    ((Unit) enemy.raw()).r());
            if (!type.attacker()) continue;
            float score = type.maximumAttackRange() * 0.8F
                    + enemy.health() * 0.05F + type.estimatedSustainedDps() * 20.0F;
            if (score > bestScore || score == bestScore
                    && (best == null || enemy.id() < best.id())) {
                best = enemy;
                bestScore = score;
            }
        }
        return best;
    }

    private static final class CombatMass {
        int count;
        float durability;
        float dps;
    }
}
