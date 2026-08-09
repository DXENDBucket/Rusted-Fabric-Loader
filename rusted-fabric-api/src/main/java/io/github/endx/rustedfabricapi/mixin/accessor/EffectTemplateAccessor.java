package io.github.endx.rustedfabricapi.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import rustedwarfare.custom.EffectList;
import rustedwarfare.custom.EffectTemplate;

/** Internal bridge for faithfully running the native custom-effect loading pass. */
@Mixin(value = EffectTemplate.class, remap = false)
public interface EffectTemplateAccessor {
    @Invoker("<init>")
    static EffectTemplate rustedfabricapi$create(String name) {
        throw new AssertionError("Mixin constructor invoker was not applied");
    }

    @Accessor("alsoEmitEffects")
    EffectList rustedfabricapi$getAlsoEmitEffects();

    @Accessor("alsoEmitEffectsOnDeath")
    EffectList rustedfabricapi$getAlsoEmitEffectsOnDeath();

    @Accessor("ifSpawnFailsEmitEffects")
    EffectList rustedfabricapi$getIfSpawnFailsEmitEffects();

    @Accessor("trailEffect")
    EffectList rustedfabricapi$getTrailEffect();
}
