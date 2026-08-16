package io.github.endx.strategicai;

import io.github.endx.rustedfabricapi.api.ai.AiMovementDomain;
import io.github.endx.rustedfabricapi.api.ai.AiStrategicMapSnapshot;
import io.github.endx.rustedfabricapi.api.ai.AiTeamPresence;
import io.github.endx.rustedfabricapi.api.ai.AiTeamRelation;
import io.github.endx.rustedfabricapi.api.ai.AiUnitTypeCapabilities;
import io.github.endx.rustedfabricapi.api.game.UnitView;
import rustedwarfare.unit.Unit;
import rustedwarfare.unit.UnitType;
import rustedwarfare.unit.action.UnitAction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Situation-aware unit choice shared by every factory controlled by one team AI. */
final class StrategicProductionDoctrine {
    private static final java.util.Set<String> ANNOUNCED_AIR_TYPES =
            new java.util.HashSet<String>();
    enum AirBalance {
        DISADVANTAGE,
        PARITY,
        SUPERIORITY
    }

    private StrategicProductionDoctrine() {
    }

    static UnitAction choose(List<UnitAction> candidates,
            TeamPositionDoctrine.Role role, AiStrategicMapSnapshot situation,
            StrategicFrontState frontState, List<UnitView> liveOwn,
            Map<String, Integer> queuedByType, long cycle) {
        if (candidates.isEmpty()) return null;
        ArrayList<UnitAction> pool = new ArrayList<UnitAction>(candidates);
        Economy economy = economy(situation);
        boolean airFactory = containsDomain(pool, AiMovementDomain.AIR);
        AirBalance airBalance = assessAirBalance(situation);
        boolean needsAirToAir = false;
        if (airFactory) {
            ArrayList<UnitAction> air = matchingDomain(pool, AiMovementDomain.AIR);
            if (!air.isEmpty()) pool = air;
            announceAirCapabilities(pool);
            AirComposition ownAir = airComposition(liveOwn, candidates, queuedByType);
            int requiredEscortCore = Math.max(3, (ownAir.airToGround + 1) * 3);
            needsAirToAir = airBalance != AirBalance.SUPERIORITY
                    || alliedAirToAirShare(situation) < 0.80F
                    || ownAir.airToAir < requiredEscortCore;
            ArrayList<UnitAction> mission = matchingTarget(pool, needsAirToAir);
            if (!mission.isEmpty()) pool = mission;
            if (needsAirToAir) {
                UnitAction strongest = strongestAirToAir(pool);
                if (strongest != null) {
                    // Once a factory exposes a materially stronger interceptor, save for it.
                    // Falling back to a cheap T1 aircraft prevents the AI from ever banking the
                    // cost of the T2 air-superiority unit and loses the air war by construction.
                    if (unitCost(strongest) > economy.credits) return null;
                    pool.clear();
                    pool.add(strongest);
                }
            }
        }
        ArrayList<UnitAction> affordable = affordable(pool, economy.credits);
        if (!affordable.isEmpty()) pool = affordable;
        pool = withoutObsoleteLowerTier(pool, economy.credits);

        final int combatTotal = mobileCombatCount(liveOwn);
        pool = withoutSaturatedRecon(pool, liveOwn, queuedByType, combatTotal);
        // A command centre whose only combat product is an already-satisfied scout should
        // remain idle. Returning the scout as a fallback is what caused endless recon spam.
        if (pool.isEmpty()) return null;

        boolean preferHeavy = role == TeamPositionDoctrine.Role.ECONOMY_TECH
                && (economy.phase != Phase.EARLY
                || economy.ownShare >= economy.fairShare * 1.12F)
                && economy.credits >= 2400.0D;
        double mostExpensiveAffordable = maximumCost(pool);
        if (economy.credits >= Math.max(2200.0D, mostExpensiveAffordable * 1.65D)) {
            preferHeavy = true;
        }
        if (role == TeamPositionDoctrine.Role.FRONTLINE && economy.phase == Phase.EARLY
                && economy.credits < mostExpensiveAffordable * 1.35D) preferHeavy = false;
        if (airFactory && airBalance == AirBalance.DISADVANTAGE) preferHeavy = false;

        final boolean heavy = preferHeavy;
        pool.sort(Comparator.comparingDouble((UnitAction action) ->
                -score(action, role, heavy, airFactory, airBalance,
                frontState, liveOwn, queuedByType, combatTotal, cycle))
                .thenComparing(action -> safe(action.getActionIdString())));
        return pool.get(0);
    }

    static AirBalance assessAirBalance(AiStrategicMapSnapshot situation) {
        // Ground-attack aircraft never contribute to air superiority. Only aircraft that can
        // actually engage air targets enter either side of this comparison.
        float friendly = airToAirStrength(situation.world().own())
                + airToAirStrength(situation.world().allies());
        float enemy = airToAirStrength(situation.world().enemies());
        if (friendly + enemy < 0.01F) return AirBalance.PARITY;
        if (friendly < enemy * 0.88F) return AirBalance.DISADVANTAGE;
        if (friendly <= enemy * 1.80F + 0.01F) return AirBalance.PARITY;
        return AirBalance.SUPERIORITY;
    }

    static UnitAction chooseFocusedFallback(List<UnitAction> candidates,
            UnitType focus, double credits) {
        if (candidates.isEmpty()) return null;
        AiUnitTypeCapabilities wanted = focus != null
                ? AiUnitTypeCapabilities.capture(focus) : null;
        UnitAction best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (UnitAction action : candidates) {
            AiUnitTypeCapabilities capabilities = AiUnitTypeCapabilities.capture(
                    action.getBuildUnitType());
            if (isReconType(capabilities) || unitCost(action) > credits) continue;
            double score = Math.log1p(combatPower(capabilities)) * 1.8D
                    + costEfficiency(capabilities, unitCost(action)) * 32.0D;
            if (wanted != null) {
                if (capabilities.movementDomain() == wanted.movementDomain()) score += 5.0D;
                if (capabilities.canAttackAir() == wanted.canAttackAir()) score += 1.2D;
                if (capabilities.canAttackGround() == wanted.canAttackGround()) score += 1.2D;
                score -= Math.abs(capabilities.maximumAttackRange()
                        - wanted.maximumAttackRange()) / 180.0D;
                if (capabilities.techLevel() <= wanted.techLevel()) {
                    score += capabilities.techLevel() * 0.35D;
                }
            }
            if (score > bestScore || score == bestScore && (best == null
                    || safe(action.getActionIdString()).compareToIgnoreCase(
                    safe(best.getActionIdString())) < 0)) {
                best = action;
                bestScore = score;
            }
        }
        return best;
    }

    static boolean shouldUpgrade(UnitAction upgrade, TeamPositionDoctrine.Role role,
            boolean primaryMobileSupport, AiStrategicMapSnapshot situation,
            StrategicFrontState frontState, long cycle) {
        Economy economy = economy(situation);
        double reserveFactor = role == TeamPositionDoctrine.Role.ECONOMY_TECH ? 1.20D : 1.55D;
        if (economy.credits < Math.max(1, upgrade.getCreditCost()) * reserveFactor) return false;
        if (frontState != null && frontState.mode() == StrategicFrontState.Mode.ATTRITION
                && role == TeamPositionDoctrine.Role.FRONTLINE) return false;
        if (role == TeamPositionDoctrine.Role.ECONOMY_TECH) {
            return economy.ownShare >= economy.fairShare * 0.88F;
        }
        if (role == TeamPositionDoctrine.Role.MOBILE_SUPPORT) {
            AirBalance air = assessAirBalance(situation);
            if (air == AirBalance.SUPERIORITY) return false;
            // Split equivalent air positions between immediate light production and teching.
            return economy.ownShare >= economy.fairShare * 0.95F
                    && primaryMobileSupport;
        }
        return role == TeamPositionDoctrine.Role.SOLO
                && economy.phase != Phase.EARLY
                && economy.ownShare >= economy.fairShare;
    }

    private static double score(UnitAction action, TeamPositionDoctrine.Role role,
            boolean preferHeavy, boolean airFactory, AirBalance airBalance,
            StrategicFrontState frontState, List<UnitView> liveOwn,
            Map<String, Integer> queuedByType, int combatTotal, long cycle) {
        UnitType type = action.getBuildUnitType();
        AiUnitTypeCapabilities capabilities = AiUnitTypeCapabilities.capture(type);
        double cost = Math.max(action.getCreditCost(), capabilities.creditCost());
        double power = combatPower(capabilities);
        double efficiency = costEfficiency(capabilities, cost);
        double score = Math.log1p(power) * 1.65D + efficiency * 42.0D;
        score += capabilities.techLevel() * (preferHeavy ? 0.85D : 0.25D);
        if (!preferHeavy) score -= cost * 0.00022D;

        int existing = countType(liveOwn, capabilities.typeId())
                + queuedByType.getOrDefault(capabilities.typeId().toLowerCase(
                java.util.Locale.ROOT), Integer.valueOf(0)).intValue();
        int broadLimit = Math.max(2, (combatTotal + 2) / 3);
        if (existing >= broadLimit) score -= 1.35D * (existing - broadLimit + 1);
        if (isReconType(capabilities)) {
            int reconLimit = Math.max(1, (combatTotal + 5) / 6);
            if (existing >= reconLimit) {
                // Recon has tactical value, but is never the backbone of a cost-efficient army.
                score -= 7.0D + (existing - reconLimit) * 2.0D;
            }
        }
        if (role == TeamPositionDoctrine.Role.FRONTLINE) {
            score += Math.min(1.2D, capabilities.movementSpeed() * 0.28D);
            if (frontState != null
                    && frontState.mode() == StrategicFrontState.Mode.ATTRITION) {
                score += rangeUtility(capabilities) * 3.2D;
                score += Math.log1p(capabilities.maximumHealth()
                        + capabilities.maximumShield()) * 0.35D;
                score += attritionRangeScore(capabilities, frontState);
            } else if (frontState != null
                    && frontState.mode() == StrategicFrontState.Mode.ASSAULT) {
                score += capabilities.movementSpeed() * 0.75D;
                score += Math.log1p(power) * 0.55D;
            }
        }
        if (airFactory && airBalance != AirBalance.SUPERIORITY
                && capabilities.airToAirSpecialist()) score += 4.0D;
        // A small stable rotation prevents one equally scored stock unit monopolising forever.
        score += Math.floorMod((int) (cycle + safe(type.getInternalName()).hashCode()), 5)
                * 0.015D;
        return score;
    }

    private static double attritionRangeScore(AiUnitTypeCapabilities capabilities,
            StrategicFrontState frontState) {
        UnitView defense = frontState != null ? frontState.primaryDefense() : null;
        if (defense == null || !(defense.raw() instanceof Unit)) {
            return capabilities.maximumAttackRange() / 45.0D;
        }
        AiUnitTypeCapabilities tower = AiUnitTypeCapabilities.capture(
                ((Unit) defense.raw()).r());
        float margin = capabilities.maximumAttackRange() - tower.maximumAttackRange();
        return capabilities.maximumAttackRange() / 42.0D
                + (margin >= 5.0F ? 10.0D : -Math.min(9.0D, -margin / 22.0D));
    }

    private static ArrayList<UnitAction> affordable(List<UnitAction> actions,
            double credits) {
        ArrayList<UnitAction> result = new ArrayList<UnitAction>();
        for (UnitAction action : actions) {
            if (unitCost(action) <= credits) result.add(action);
        }
        return result;
    }

    private static ArrayList<UnitAction> withoutObsoleteLowerTier(
            List<UnitAction> actions, double credits) {
        ArrayList<UnitAction> result = new ArrayList<UnitAction>();
        for (UnitAction lower : actions) {
            AiUnitTypeCapabilities low = AiUnitTypeCapabilities.capture(
                    lower.getBuildUnitType());
            double lowCost = unitCost(lower);
            boolean obsolete = false;
            for (UnitAction higher : actions) {
                if (higher == lower) continue;
                AiUnitTypeCapabilities high = AiUnitTypeCapabilities.capture(
                        higher.getBuildUnitType());
                double highCost = unitCost(higher);
                if (high.techLevel() <= low.techLevel() || highCost > credits * 0.92D
                        || high.movementDomain() != low.movementDomain()) continue;
                if (low.canAttackAir() && !high.canAttackAir()
                        || low.canAttackGround() && !high.canAttackGround()) continue;
                if (combatPower(high) >= combatPower(low) * 1.28D
                        && costEfficiency(high, highCost)
                        >= costEfficiency(low, lowCost) * 0.92D) {
                    obsolete = true;
                    break;
                }
            }
            if (!obsolete) result.add(lower);
        }
        return result.isEmpty() ? new ArrayList<UnitAction>(actions) : result;
    }

    private static UnitAction strongestAirToAir(List<UnitAction> actions) {
        UnitAction best = null;
        double bestValue = Double.NEGATIVE_INFINITY;
        for (UnitAction action : actions) {
            AiUnitTypeCapabilities capabilities = AiUnitTypeCapabilities.capture(
                    action.getBuildUnitType());
            if (!capabilities.airToAirSpecialist()) continue;
            double value = airSuperiorityValue(capabilities);
            if (value > bestValue || value == bestValue && (best == null
                    || safe(action.getActionIdString()).compareToIgnoreCase(
                    safe(best.getActionIdString())) < 0)) {
                best = action;
                bestValue = value;
            }
        }
        return best;
    }

    static double airSuperiorityValue(AiUnitTypeCapabilities capabilities) {
        if (capabilities == null || !capabilities.airToAirSpecialist()) return 0.0D;
        double durability = Math.max(1.0D, capabilities.maximumHealth()
                + capabilities.maximumShield());
        return Math.sqrt(durability
                * Math.max(0.04D, capabilities.estimatedAirDps()))
                * (1.0D + capabilities.maximumAttackRange() / 900.0D);
    }

    private static AirComposition airComposition(List<UnitView> liveOwn,
            List<UnitAction> declaredActions, Map<String, Integer> queuedByType) {
        int airToAir = 0;
        int airToGround = 0;
        for (UnitView unit : liveOwn) {
            AiUnitTypeCapabilities capabilities = typeCapabilities(unit);
            if (capabilities == null || !capabilities.mobileCombatUnit()
                    || capabilities.movementDomain() != AiMovementDomain.AIR) continue;
            if (capabilities.airToAirSpecialist()) airToAir++;
            else if (capabilities.canAttackGround()) airToGround++;
        }
        java.util.HashSet<String> counted = new java.util.HashSet<String>();
        for (UnitAction action : declaredActions) {
            UnitType type = action.getBuildUnitType();
            if (type == null) continue;
            AiUnitTypeCapabilities capabilities = AiUnitTypeCapabilities.capture(type);
            if (!capabilities.mobileCombatUnit()
                    || capabilities.movementDomain() != AiMovementDomain.AIR) continue;
            String id = safe(type.getInternalName()).toLowerCase(java.util.Locale.ROOT);
            if (!counted.add(id)) continue;
            int queued = queuedByType.getOrDefault(id, Integer.valueOf(0)).intValue();
            if (capabilities.airToAirSpecialist()) airToAir += queued;
            else if (capabilities.canAttackGround()) airToGround += queued;
        }
        return new AirComposition(airToAir, airToGround);
    }

    private static double combatPower(AiUnitTypeCapabilities capabilities) {
        double durability = Math.max(1.0D, capabilities.maximumHealth()
                + capabilities.maximumShield());
        double dps = Math.max(0.04D, capabilities.estimatedSustainedDps());
        return Math.sqrt(durability * dps)
                * (1.0D + Math.min(0.45D, rangeUtility(capabilities) * 0.16D));
    }

    private static double costEfficiency(AiUnitTypeCapabilities capabilities,
            double cost) {
        return combatPower(capabilities) * 100.0D / Math.max(1.0D, cost);
    }

    private static double rangeUtility(AiUnitTypeCapabilities capabilities) {
        return capabilities.maximumAttackRange() / 150.0D;
    }

    private static double unitCost(UnitAction action) {
        AiUnitTypeCapabilities capabilities = AiUnitTypeCapabilities.capture(
                action.getBuildUnitType());
        return Math.max(1.0D, Math.max(action.getCreditCost(), capabilities.creditCost()));
    }

    private static double maximumCost(List<UnitAction> actions) {
        double result = 1.0D;
        for (UnitAction action : actions) result = Math.max(result, unitCost(action));
        return result;
    }

    static boolean isReconType(AiUnitTypeCapabilities capabilities) {
        String id = safe(capabilities.typeId()).toLowerCase(java.util.Locale.ROOT);
        String name = safe(capabilities.displayName()).toLowerCase(java.util.Locale.ROOT);
        return id.contains("scout") || id.contains("recon")
                || name.contains("scout") || name.contains("recon")
                || name.contains("侦察");
    }

    private static ArrayList<UnitAction> withoutSaturatedRecon(
            List<UnitAction> actions, List<UnitView> liveOwn,
            Map<String, Integer> queuedByType, int combatTotal) {
        int reconCount = 0;
        for (UnitView unit : liveOwn) {
            AiUnitTypeCapabilities capabilities = typeCapabilities(unit);
            if (capabilities != null && capabilities.mobileCombatUnit()
                    && isReconType(capabilities)) reconCount++;
        }
        java.util.HashSet<String> counted = new java.util.HashSet<String>();
        for (UnitAction action : actions) {
            UnitType type = action.getBuildUnitType();
            if (type == null) continue;
            AiUnitTypeCapabilities capabilities = AiUnitTypeCapabilities.capture(type);
            String id = safe(capabilities.typeId()).toLowerCase(java.util.Locale.ROOT);
            if (isReconType(capabilities) && counted.add(id)) {
                reconCount += queuedByType.getOrDefault(id, Integer.valueOf(0)).intValue();
            }
        }
        int reconLimit = combatTotal >= 40 ? 2 : 1;
        if (reconCount < reconLimit) return new ArrayList<UnitAction>(actions);
        ArrayList<UnitAction> result = new ArrayList<UnitAction>();
        for (UnitAction action : actions) {
            UnitType type = action.getBuildUnitType();
            if (type == null || !isReconType(AiUnitTypeCapabilities.capture(type))) {
                result.add(action);
            }
        }
        return result;
    }

    private static int mobileCombatCount(List<UnitView> units) {
        int count = 0;
        for (UnitView unit : units) {
            if (!unit.alive() || !(unit.raw() instanceof Unit)) continue;
            UnitType type = ((Unit) unit.raw()).r();
            if (type != null && AiUnitTypeCapabilities.capture(type).mobileCombatUnit()) count++;
        }
        return count;
    }

    private static int countType(List<UnitView> units, String typeId) {
        int count = 0;
        for (UnitView unit : units) {
            if (!unit.alive() || !(unit.raw() instanceof Unit)) continue;
            UnitType type = ((Unit) unit.raw()).r();
            if (type != null && safe(type.getInternalName()).equalsIgnoreCase(typeId)) count++;
        }
        return count;
    }

    private static Economy economy(AiStrategicMapSnapshot situation) {
        double ownIncome = Math.max(0, situation.perspective().incomeRate());
        double totalIncome = 0.0D;
        int friendlyTeams = 0;
        int combatUnits = 0;
        for (AiTeamPresence team : situation.teams()) {
            if (team.relation() != AiTeamRelation.OWN
                    && team.relation() != AiTeamRelation.ALLY) continue;
            totalIncome += Math.max(0, team.team().incomeRate());
            friendlyTeams++;
            combatUnits += team.mobileCount();
        }
        float ownShare = totalIncome > 0.0D ? (float) (ownIncome / totalIncome)
                : friendlyTeams > 0 ? 1.0F / friendlyTeams : 1.0F;
        float fairShare = friendlyTeams > 0 ? 1.0F / friendlyTeams : 1.0F;
        Phase phase = combatUnits < 18 ? Phase.EARLY
                : combatUnits < 55 ? Phase.MID : Phase.LATE;
        return new Economy(ownShare, fairShare,
                situation.perspective().credits(), phase);
    }

    private static float alliedAirToAirShare(AiStrategicMapSnapshot situation) {
        int allAir = 0;
        int antiAir = 0;
        ArrayList<UnitView> units = new ArrayList<UnitView>();
        units.addAll(situation.world().own());
        units.addAll(situation.world().allies());
        for (UnitView unit : units) {
            AiUnitTypeCapabilities capabilities = typeCapabilities(unit);
            if (capabilities == null || capabilities.movementDomain() != AiMovementDomain.AIR
                    || !capabilities.mobileCombatUnit()) continue;
            allAir++;
            if (capabilities.airToAirSpecialist()) antiAir++;
        }
        return allAir > 0 ? antiAir / (float) allAir : 0.0F;
    }

    private static float airToAirStrength(List<UnitView> units) {
        float result = 0.0F;
        for (UnitView unit : units) {
            AiUnitTypeCapabilities capabilities = typeCapabilities(unit);
            if (capabilities == null || capabilities.movementDomain() != AiMovementDomain.AIR
                    || !capabilities.mobileCombatUnit()
                    || !capabilities.airToAirSpecialist()) continue;
            float healthFraction = unit.maxHealth() > 0.0F
                    ? Math.max(0.15F, unit.health() / unit.maxHealth()) : 1.0F;
            double durability = Math.max(1.0D, capabilities.maximumHealth()
                    + capabilities.maximumShield());
            double airPower = Math.sqrt(durability
                    * Math.max(0.04D, capabilities.estimatedAirDps()));
            result += (float) airPower * healthFraction;
        }
        return result;
    }

    private static AiUnitTypeCapabilities typeCapabilities(UnitView unit) {
        return unit.raw() instanceof Unit
                ? AiUnitTypeCapabilities.capture(((Unit) unit.raw()).r()) : null;
    }

    private static boolean containsDomain(List<UnitAction> actions,
            AiMovementDomain domain) {
        return !matchingDomain(actions, domain).isEmpty();
    }

    private static ArrayList<UnitAction> matchingDomain(List<UnitAction> actions,
            AiMovementDomain domain) {
        ArrayList<UnitAction> result = new ArrayList<UnitAction>();
        for (UnitAction action : actions) {
            UnitType type = action.getBuildUnitType();
            if (type != null && AiUnitTypeCapabilities.capture(type).movementDomain() == domain) {
                result.add(action);
            }
        }
        return result;
    }

    private static ArrayList<UnitAction> matchingTarget(List<UnitAction> actions,
            boolean airToAir) {
        ArrayList<UnitAction> result = new ArrayList<UnitAction>();
        for (UnitAction action : actions) {
            AiUnitTypeCapabilities capabilities =
                    AiUnitTypeCapabilities.capture(action.getBuildUnitType());
            if (airToAir ? capabilities.airToAirSpecialist()
                    : capabilities.canAttackGround()) result.add(action);
        }
        return result;
    }

    private static void announceAirCapabilities(List<UnitAction> actions) {
        for (UnitAction action : actions) {
            UnitType type = action.getBuildUnitType();
            if (type == null) continue;
            AiUnitTypeCapabilities capabilities = AiUnitTypeCapabilities.capture(type);
            String id = safe(capabilities.typeId()).toLowerCase(java.util.Locale.ROOT);
            if (!ANNOUNCED_AIR_TYPES.add(id)) continue;
            System.out.println("[Strategic AI] Air capability " + capabilities.typeId()
                    + " airDps=" + capabilities.estimatedAirDps()
                    + " groundDps=" + capabilities.estimatedGroundDps()
                    + " specialist=" + capabilities.airToAirSpecialist());
        }
    }

    private static String safe(String value) { return value != null ? value : ""; }

    private enum Phase { EARLY, MID, LATE }

    private static final class Economy {
        final float ownShare;
        final float fairShare;
        final double credits;
        final Phase phase;

        Economy(float ownShare, float fairShare, double credits, Phase phase) {
            this.ownShare = ownShare;
            this.fairShare = fairShare;
            this.credits = credits;
            this.phase = phase;
        }
    }

    private static final class AirComposition {
        final int airToAir;
        final int airToGround;

        AirComposition(int airToAir, int airToGround) {
            this.airToAir = airToAir;
            this.airToGround = airToGround;
        }
    }
}
