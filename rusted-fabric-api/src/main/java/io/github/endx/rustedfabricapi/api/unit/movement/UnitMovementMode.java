package io.github.endx.rustedfabricapi.api.unit.movement;

import rustedwarfare.unit.MovementType;

import java.util.Locale;

/** Per-unit runtime movement and collision-domain override. */
public enum UnitMovementMode {
    NATIVE(null, null),
    NONE(MovementType.none, Boolean.FALSE),
    LAND(MovementType.land, Boolean.FALSE),
    AIR(MovementType.air, Boolean.FALSE),
    WATER(MovementType.water, Boolean.FALSE),
    HOVER(MovementType.hover, Boolean.FALSE),
    OVER_CLIFF(MovementType.overCliff, Boolean.FALSE),
    OVER_CLIFF_WATER(MovementType.overCliffWater, Boolean.FALSE),
    BUILDING(MovementType.building, Boolean.TRUE);

    private final MovementType movementType;
    private final Boolean building;

    UnitMovementMode(MovementType movementType, Boolean building) {
        this.movementType = movementType;
        this.building = building;
    }

    public MovementType movementType() { return movementType; }
    public boolean overridesNative() { return this != NATIVE; }
    public boolean building(boolean nativeValue) {
        return building != null ? building.booleanValue() : nativeValue;
    }
    public static UnitMovementMode parse(String raw) {
        if (raw == null) throw new IllegalArgumentException("movement mode must not be null");
        String normalized = raw.trim().toLowerCase(Locale.ROOT)
                .replace('-', '_').replace(' ', '_');
        switch (normalized) {
            case "native":
            case "default": return NATIVE;
            case "none": return NONE;
            case "land":
            case "ground": return LAND;
            case "air":
            case "flying": return AIR;
            case "water":
            case "naval": return WATER;
            case "hover": return HOVER;
            case "overcliff":
            case "over_cliff": return OVER_CLIFF;
            case "overcliffwater":
            case "over_cliff_water": return OVER_CLIFF_WATER;
            case "building":
            case "structure": return BUILDING;
            default: throw new IllegalArgumentException("unknown movement mode: " + raw);
        }
    }

}
