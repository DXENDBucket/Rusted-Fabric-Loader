package io.github.endx.rustedfabricapi.api.ai;

import io.github.endx.rustedfabricapi.api.game.UnitView;
import rustedwarfare.unit.MovementType;

import java.util.Locale;

/** Strategic movement domains backed by the game's native terrain cost maps. */
public enum AiMovementDomain {
    LAND(MovementType.land),
    HOVER(MovementType.hover),
    WATER(MovementType.water),
    OVER_CLIFF(MovementType.overCliff),
    OVER_CLIFF_WATER(MovementType.overCliffWater),
    AIR(MovementType.air);

    private final MovementType nativeType;

    AiMovementDomain(MovementType nativeType) {
        this.nativeType = nativeType;
    }

    public static AiMovementDomain of(UnitView unit) {
        if (unit == null) throw new IllegalArgumentException("unit must not be null");
        if (unit.flying()) return AIR;
        return fromName(unit.movementType());
    }

    public static AiMovementDomain fromName(String movementType) {
        if (movementType == null) return LAND;
        switch (movementType.toLowerCase(Locale.ROOT)) {
            case "water": return WATER;
            case "hover": return HOVER;
            case "overcliff": return OVER_CLIFF;
            case "overcliffwater": return OVER_CLIFF_WATER;
            case "air": return AIR;
            default: return LAND;
        }
    }

    MovementType nativeType() { return nativeType; }
}
