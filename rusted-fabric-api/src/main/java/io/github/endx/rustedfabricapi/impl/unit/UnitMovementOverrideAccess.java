package io.github.endx.rustedfabricapi.impl.unit;

import io.github.endx.rustedfabricapi.api.unit.movement.UnitMovementMode;

/** Internal state bridge implemented directly by the CustomUnit mixin. */
public interface UnitMovementOverrideAccess {
    UnitMovementMode rustedfabricapi$getMovementMode();
    void rustedfabricapi$setMovementMode(UnitMovementMode mode);
}
