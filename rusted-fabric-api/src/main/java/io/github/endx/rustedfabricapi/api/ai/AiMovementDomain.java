package io.github.endx.rustedfabricapi.api.ai;

import rustedwarfare.unit.MovementType;

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

    MovementType nativeType() { return nativeType; }
}
