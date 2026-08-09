package io.github.endx.rustedfabricapi.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.gen.Accessor;
import rustedwarfare.custom.CustomUnitMetadata;
import rustedwarfare.custom.graphics.DecalBehavior;
import rustedwarfare.custom.graphics.DecalTemplate;
import rustedwarfare.util.RwArrayList;

/** Internal native Decal lookup and post-parse layer access. */
@Mixin(value = DecalBehavior.class, remap = false)
public interface DecalBehaviorAccessor {
    @Invoker("findDecalTemplateStrict")
    static DecalTemplate rustedfabricapi$invokeDecalLookup(
            CustomUnitMetadata metadata, String name) {
        throw new AssertionError("Mixin invoker was not applied");
    }

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
