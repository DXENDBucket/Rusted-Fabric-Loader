package io.github.endx.rustedfabricapi.api.ai;

import io.github.endx.rustedfabricapi.api.game.UnitView;
import rustedwarfare.unit.OrderableUnit;
import rustedwarfare.unit.Unit;
import rustedwarfare.unit.UnitType;

/** Stable combat/economy capabilities used to classify native and custom units. */
public final class AiUnitCapabilities {
    private final UnitView unit;
    private final boolean orderable;
    private final boolean movable;
    private final boolean attacker;
    private final boolean builder;
    private final boolean harvester;
    private final boolean idle;
    private final AiMovementDomain movementDomain;
    private final float maximumAttackRange;
    private final float movementSpeed;
    private final int techLevel;
    private final String typeId;
    private final String displayName;

    private AiUnitCapabilities(UnitView unit, boolean orderable, boolean movable,
            boolean attacker, boolean builder, boolean harvester, boolean idle,
            AiMovementDomain movementDomain, float maximumAttackRange,
            float movementSpeed, int techLevel, String typeId, String displayName) {
        this.unit = unit;
        this.orderable = orderable;
        this.movable = movable;
        this.attacker = attacker;
        this.builder = builder;
        this.harvester = harvester;
        this.idle = idle;
        this.movementDomain = movementDomain;
        this.maximumAttackRange = maximumAttackRange;
        this.movementSpeed = movementSpeed;
        this.techLevel = techLevel;
        this.typeId = typeId;
        this.displayName = displayName;
    }

    public static AiUnitCapabilities capture(UnitView view) {
        if (view == null) throw new IllegalArgumentException("unit must not be null");
        Object value = view.raw();
        if (!(value instanceof Unit)) {
            throw new IllegalArgumentException("Unit view is not backed by the active game namespace");
        }
        Unit unit = (Unit) value;
        UnitType type = unit.r();
        boolean builder = type != null && (type.isBuilder() || type.useAsBuilder());
        boolean harvester = type != null && type.useAsHarvester();
        boolean orderable = unit instanceof OrderableUnit;
        boolean movable = false;
        boolean attacker = false;
        boolean idle = false;
        float range = 0.0F;
        float speed = 0.0F;
        if (orderable) {
            OrderableUnit controlled = (OrderableUnit) unit;
            movable = controlled.canMove();
            attacker = controlled.canAttack();
            idle = controlled.hasNoWaypoints();
            range = finiteNonNegative(controlled.getMaxAttackRange());
            speed = finiteNonNegative(controlled.getMoveSpeed());
        }
        return new AiUnitCapabilities(view, orderable, movable, attacker,
                builder, harvester, idle, AiMovementDomain.of(view), range, speed,
                Math.max(0, unit.getTechLevel()),
                type != null ? safe(type.getInternalName()) : "unknown",
                type != null ? safe(type.getDisplayName()) : "unknown");
    }

    public UnitView unit() { return unit; }
    public boolean orderable() { return orderable; }
    public boolean movable() { return movable; }
    public boolean attacker() { return attacker; }
    public boolean builder() { return builder; }
    public boolean harvester() { return harvester; }
    public boolean idle() { return idle; }
    public AiMovementDomain movementDomain() { return movementDomain; }
    public float maximumAttackRange() { return maximumAttackRange; }
    public float movementSpeed() { return movementSpeed; }
    public int techLevel() { return techLevel; }
    public String typeId() { return typeId; }
    public String displayName() { return displayName; }

    public boolean mobileCombatUnit() {
        return orderable && movable && attacker && !builder;
    }

    private static float finiteNonNegative(float value) {
        return Float.isFinite(value) ? Math.max(0.0F, value) : 0.0F;
    }

    private static String safe(String value) {
        return value == null || value.isEmpty() ? "unknown" : value;
    }
}
