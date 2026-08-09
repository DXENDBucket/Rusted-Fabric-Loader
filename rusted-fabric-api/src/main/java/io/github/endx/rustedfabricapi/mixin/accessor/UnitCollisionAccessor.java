package io.github.endx.rustedfabricapi.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import rustedwarfare.unit.Unit;

/** Internal access to the native unit contact radius. */
@Mixin(Unit.class)
public interface UnitCollisionAccessor {
    @Accessor("collisionRadius")
    float rustedfabricapi$getCollisionRadius();
}
