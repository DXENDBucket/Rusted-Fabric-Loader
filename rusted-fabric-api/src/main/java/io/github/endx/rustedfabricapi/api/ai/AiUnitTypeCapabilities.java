package io.github.endx.rustedfabricapi.api.ai;

import io.github.endx.rustedfabricapi.api.unit.type.UnitTypes;
import rustedwarfare.unit.OrderableUnit;
import rustedwarfare.unit.Unit;
import rustedwarfare.unit.UnitType;

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
    private final AiMovementDomain movementDomain;
    private final float maximumAttackRange;
    private final float movementSpeed;
    private final int techLevel;
    private final int creditCost;

    private AiUnitTypeCapabilities(UnitType type, boolean movable, boolean attacker,
            float maximumAttackRange, float movementSpeed) {
        this.type = type;
        this.typeId = safe(type.getInternalName());
        this.displayName = safe(type.getDisplayName());
        this.building = type.isBuilding();
        this.builder = type.isBuilder() || type.useAsBuilder();
        this.harvester = type.useAsHarvester();
        this.movable = movable;
        this.attacker = attacker;
        this.movementDomain = AiMovementDomain.fromName(
                type.getMovementType() != null ? type.getMovementType().name() : null);
        this.maximumAttackRange = finiteNonNegative(maximumAttackRange);
        this.movementSpeed = finiteNonNegative(movementSpeed);
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
        Unit prototype = UnitTypes.createUnregisteredPrototype(type);
        if (prototype instanceof OrderableUnit) {
            OrderableUnit orderable = (OrderableUnit) prototype;
            movable = orderable.canMove();
            attacker = orderable.canAttack();
            range = orderable.getMaxAttackRange();
            speed = orderable.getMoveSpeed();
        }
        AiUnitTypeCapabilities captured = new AiUnitTypeCapabilities(
                type, movable, attacker, range, speed);
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
    public AiMovementDomain movementDomain() { return movementDomain; }
    public float maximumAttackRange() { return maximumAttackRange; }
    public float movementSpeed() { return movementSpeed; }
    public int techLevel() { return techLevel; }
    public int creditCost() { return creditCost; }
    public boolean mobileCombatUnit() { return movable && attacker && !builder; }

    private static float finiteNonNegative(float value) {
        return Float.isFinite(value) ? Math.max(0.0F, value) : 0.0F;
    }

    private static String safe(String value) {
        return value == null || value.isEmpty() ? "unknown" : value;
    }
}
