package io.github.endx.rustedfabricapi.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import rustedwarfare.game.Projectile;

/** Internal access to the native impact latch used by projectile update. */
@Mixin(value = Projectile.class, remap = false)
public interface ProjectileCollisionAccessor {
    @Accessor("impactTriggered")
    boolean rustedfabricapi$isImpactTriggered();

    @Accessor("impactTriggered")
    void rustedfabricapi$setImpactTriggered(boolean value);
}
