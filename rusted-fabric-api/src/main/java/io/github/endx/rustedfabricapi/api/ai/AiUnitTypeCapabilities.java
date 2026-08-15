package io.github.endx.rustedfabricapi.api.ai;

import io.github.endx.rustedfabricapi.api.unit.type.UnitTypes;
import rustedwarfare.unit.OrderableUnit;
import rustedwarfare.unit.Unit;
import rustedwarfare.unit.UnitType;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.custom.TurretTemplate;

import java.util.IdentityHashMap;
import java.util.Map;

/** Base capabilities of a native or custom unit type, sampled from an unregistered prototype. */
public final class AiUnitTypeCapabilities {
    private static final Map<UnitType, AiUnitTypeCapabilities> CACHE =
            new IdentityHashMap<UnitType, AiUnitTypeCapabilities>();

    private final UnitType type;
    private final String typeId;
    private final String displayName;
    private final boolean building;
    private final boolean builder;
    private final boolean harvester;
    private final boolean movable;
    private final boolean attacker;
    private final boolean canAttackAir;
    private final boolean canAttackGround;
    private final AiMovementDomain movementDomain;
    private final float maximumAttackRange;
    private final float movementSpeed;
    private final float maximumHealth;
    private final float maximumShield;
    private final float estimatedSustainedDps;
    private final float estimatedInitialDps;
    private final float estimatedAirDps;
    private final float estimatedGroundDps;
    private final float maximumWarmupTime;
    private final float warmupCooldownRate;
    private final boolean retainsWarmupAfterFiring;
    private final int techLevel;
    private final int creditCost;

    private AiUnitTypeCapabilities(UnitType type, boolean movable, boolean attacker,
            boolean canAttackAir, boolean canAttackGround,
            float maximumAttackRange, float movementSpeed,
            float maximumHealth, float maximumShield,
            float estimatedInitialDps, float estimatedSustainedDps,
            float estimatedAirDps, float estimatedGroundDps,
            float maximumWarmupTime, float warmupCooldownRate,
            boolean retainsWarmupAfterFiring) {
        this.type = type;
        this.typeId = safe(type.getInternalName());
        this.displayName = safe(type.getDisplayName());
        this.building = type.isBuilding();
        this.builder = type.isBuilder() || type.useAsBuilder();
        this.harvester = type.useAsHarvester();
        this.movable = movable;
        this.attacker = attacker;
        this.canAttackAir = canAttackAir;
        this.canAttackGround = canAttackGround;
        this.movementDomain = AiMovementDomain.fromName(
                type.getMovementType() != null ? type.getMovementType().name() : null);
        this.maximumAttackRange = finiteNonNegative(maximumAttackRange);
        this.movementSpeed = finiteNonNegative(movementSpeed);
        this.maximumHealth = finiteNonNegative(maximumHealth);
        this.maximumShield = finiteNonNegative(maximumShield);
        this.estimatedSustainedDps = finiteNonNegative(estimatedSustainedDps);
        this.estimatedInitialDps = finiteNonNegative(estimatedInitialDps);
        this.estimatedAirDps = finiteNonNegative(estimatedAirDps);
        this.estimatedGroundDps = finiteNonNegative(estimatedGroundDps);
        this.maximumWarmupTime = finiteNonNegative(maximumWarmupTime);
        this.warmupCooldownRate = finiteNonNegative(warmupCooldownRate);
        this.retainsWarmupAfterFiring = retainsWarmupAfterFiring;
        this.techLevel = Math.max(0, type.getTechLevel());
        this.creditCost = Math.max(0, type.getBuildCostCredits());
    }

    public static AiUnitTypeCapabilities capture(UnitType type) {
        if (type == null) throw new IllegalArgumentException("type must not be null");
        synchronized (CACHE) {
            AiUnitTypeCapabilities existing = CACHE.get(type);
            if (existing != null) return existing;
        }
        boolean movable = !type.isBuilding();
        boolean attacker = false;
        float range = 0.0F;
        float speed = 0.0F;
        boolean attacksAir = false;
        boolean attacksGround = false;
        Unit prototype = UnitTypes.createUnregisteredPrototype(type);
        float maximumHealth = prototype.maxHp;
        float maximumShield = prototype.maxShield;
        float sustainedDps = 0.0F;
        float initialDps = 0.0F;
        float airDps = 0.0F;
        float groundDps = 0.0F;
        float warmupTime = 0.0F;
        float cooldownRate = 0.0F;
        boolean retainsWarmup = false;
        if (prototype instanceof OrderableUnit) {
            OrderableUnit orderable = (OrderableUnit) prototype;
            movable = orderable.canMove();
            attacker = orderable.canAttack();
            range = orderable.getMaxAttackRange();
            speed = orderable.getMoveSpeed();
            if (attacker) {
                Unit airTarget = representativeTarget(AiMovementDomain.AIR);
                Unit groundTarget = representativeTarget(AiMovementDomain.LAND);
                airDps = estimateDpsAgainst(orderable, airTarget, true);
                groundDps = estimateDpsAgainst(orderable, groundTarget, true);
                attacksAir = airDps > 0.0F;
                attacksGround = groundDps > 0.0F;
                initialDps = estimateSustainedDps(orderable, false);
                sustainedDps = estimateSustainedDps(orderable, true);
                for (int turret = 0; turret < orderable.getTurretCount(); turret++) {
                    warmupTime = Math.max(warmupTime,
                            finiteNonNegative(orderable.getTurretWarmupTime(turret)));
                    if (orderable instanceof CustomUnit) {
                        TurretTemplate template = customTurret((CustomUnit) orderable, turret);
                        if (template != null) {
                            cooldownRate = Math.max(cooldownRate,
                                    finiteNonNegative(template.warmupCallDownRate));
                        }
                    }
                    retainsWarmup |= orderable.isTurretWarmupNoReset(turret);
                }
            }
        }
        AiUnitTypeCapabilities captured = new AiUnitTypeCapabilities(
                type, movable, attacker, attacksAir, attacksGround, range, speed,
                maximumHealth, maximumShield, initialDps, sustainedDps,
                airDps, groundDps, warmupTime, cooldownRate, retainsWarmup);
        synchronized (CACHE) {
            AiUnitTypeCapabilities raced = CACHE.get(type);
            if (raced != null) return raced;
            CACHE.put(type, captured);
        }
        return captured;
    }

    public static void invalidate() {
        synchronized (CACHE) {
            CACHE.clear();
        }
    }

    public UnitType rawType() { return type; }
    public String typeId() { return typeId; }
    public String displayName() { return displayName; }
    public boolean building() { return building; }
    public boolean builder() { return builder; }
    public boolean harvester() { return harvester; }
    public boolean movable() { return movable; }
    public boolean attacker() { return attacker; }
    public boolean canAttackAir() { return canAttackAir; }
    public boolean canAttackGround() { return canAttackGround; }
    public AiMovementDomain movementDomain() { return movementDomain; }
    public float maximumAttackRange() { return maximumAttackRange; }
    public float movementSpeed() { return movementSpeed; }
    public float maximumHealth() { return maximumHealth; }
    public float maximumShield() { return maximumShield; }
    /** Coarse direct-damage-per-native-time estimate; intended for relative AI comparisons. */
    public float estimatedSustainedDps() { return estimatedSustainedDps; }
    /** DPS before a progressive-fire weapon has accumulated any warmup. */
    public float estimatedInitialDps() { return estimatedInitialDps; }
    /** Direct DPS from turrets that can actually acquire a representative flying target. */
    public float estimatedAirDps() { return estimatedAirDps; }
    /** Direct DPS from turrets that can actually acquire a representative ground target. */
    public float estimatedGroundDps() { return estimatedGroundDps; }
    public float maximumWarmupTime() { return maximumWarmupTime; }
    public float warmupCooldownRate() { return warmupCooldownRate; }
    public boolean retainsWarmupAfterFiring() { return retainsWarmupAfterFiring; }
    /** Approximate average DPS across one uninterrupted engagement in native time units. */
    public float estimatedEngagementDps(float continuousFireTime) {
        if (!Float.isFinite(continuousFireTime) || continuousFireTime <= 0.0F) {
            return estimatedInitialDps;
        }
        if (maximumWarmupTime <= 0.0F) return estimatedSustainedDps;
        float ramp = Math.min(continuousFireTime, maximumWarmupTime);
        float rampDamage = (estimatedInitialDps + estimatedSustainedDps)
                * 0.5F * ramp;
        float sustainedDamage = estimatedSustainedDps
                * Math.max(0.0F, continuousFireTime - ramp);
        return (rampDamage + sustainedDamage) / continuousFireTime;
    }
    /** True for aircraft whose meaningful combat role is contesting other aircraft. */
    public boolean airToAirSpecialist() {
        return movementDomain == AiMovementDomain.AIR && estimatedAirDps > 0.0F
                && (estimatedGroundDps <= 0.0F
                || estimatedAirDps >= estimatedGroundDps * 0.65F);
    }
    public int techLevel() { return techLevel; }
    public int creditCost() { return creditCost; }
    public boolean mobileCombatUnit() { return movable && attacker && !builder; }

    private static Unit representativeTarget(AiMovementDomain domain) {
        for (UnitType candidate : UnitTypes.all()) {
            if (candidate.isBuilding() || candidate.getMovementType() == null) continue;
            if (AiMovementDomain.fromName(candidate.getMovementType().name()) != domain) continue;
            Unit target = UnitTypes.createUnregisteredPrototype(candidate);
            if (domain == AiMovementDomain.AIR && !target.isFlying()) continue;
            if (domain == AiMovementDomain.LAND
                    && (target.isFlying() || target.isUnderwater())) continue;
            return target;
        }
        return null;
    }

    private static float estimateDpsAgainst(OrderableUnit attacker, Unit target,
            boolean fullyWarmed) {
        if (target == null) return 0.0F;
        float result = 0.0F;
        try {
            if (!attacker.canAttackTargetType(target)) return 0.0F;
            for (int turret = 0; turret < attacker.getTurretCount(); turret++) {
                if (!attacker.canTurretFire(turret)
                        || !attacker.canTurretAttackTarget(turret, target, true, false)) {
                    continue;
                }
                float damage = attacker.getTurretProjectileDirectDamage(turret);
                float delay = effectiveFireDelay(attacker, turret, fullyWarmed);
                if (Float.isFinite(damage) && damage > 0.0F
                        && Float.isFinite(delay) && delay > 0.0F) {
                    result += damage / delay;
                }
            }
        } catch (RuntimeException ignored) {
            return 0.0F;
        }
        return result;
    }

    private static float estimateSustainedDps(OrderableUnit unit, boolean fullyWarmed) {
        float result = 0.0F;
        try {
            for (int turret = 0; turret < unit.getTurretCount(); turret++) {
                float damage = unit.getTurretProjectileDirectDamage(turret);
                float delay = effectiveFireDelay(unit, turret, fullyWarmed);
                if (Float.isFinite(damage) && damage > 0.0F
                        && Float.isFinite(delay) && delay > 0.0F) {
                    result += damage / delay;
                }
            }
        } catch (RuntimeException ignored) {
            return 0.0F;
        }
        return result;
    }

    private static float effectiveFireDelay(OrderableUnit unit, int turret,
            boolean fullyWarmed) {
        float delay = unit.getTurretFireDelay(turret);
        if (fullyWarmed && unit.getTurretWarmupTime(turret) > 0.0F
                && unit instanceof CustomUnit) {
            TurretTemplate template = customTurret((CustomUnit) unit, turret);
            if (template != null) delay -= Math.max(0.0F,
                    template.warmupShootDelayTransfer);
        }
        return Math.max(0.01F, delay);
    }

    private static TurretTemplate customTurret(CustomUnit unit, int index) {
        if (unit == null || unit.unitMetadata == null
                || unit.unitMetadata.turretTemplates == null
                || index < 0 || index >= unit.unitMetadata.turretTemplates.length) return null;
        return unit.unitMetadata.turretTemplates[index];
    }

    private static float finiteNonNegative(float value) {
        return Float.isFinite(value) ? Math.max(0.0F, value) : 0.0F;
    }

    private static String safe(String value) {
        return value == null || value.isEmpty() ? "unknown" : value;
    }
}
