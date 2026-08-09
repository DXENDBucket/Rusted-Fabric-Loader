package io.github.endx.rustedfabricapi.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import rustedwarfare.custom.graphics.DecalBehavior;
import rustedwarfare.util.RwArrayList;

/** Internal access to the native Decal lists after their ordinary parser has run. */
@Mixin(value = DecalBehavior.class, remap = false)
public interface DecalBehaviorAccessor {
    @Accessor("shadowDecals")
    RwArrayList rustedfabricapi$getShadowDecals();

    @Accessor("beforeBodyDecals")
    RwArrayList rustedfabricapi$getBeforeBodyDecals();

    @Accessor("afterBodyDecals")
    RwArrayList rustedfabricapi$getAfterBodyDecals();

    @Accessor("onTopDecals")
    RwArrayList rustedfabricapi$getOnTopDecals();

    @Accessor("beforeUiDecals")
    RwArrayList rustedfabricapi$getBeforeUiDecals();
}
