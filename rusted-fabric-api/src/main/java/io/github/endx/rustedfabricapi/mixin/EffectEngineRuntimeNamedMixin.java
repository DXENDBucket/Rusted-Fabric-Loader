package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.EffectRuntimeEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "rustedwarfare.render.effect.EffectEngine", remap = false)
public abstract class EffectEngineRuntimeNamedMixin {
    @Inject(
            method = "createLineEffect(FFFFFF)Lrustedwarfare/render/effect/EffectInstance;",
            at = @At("RETURN"),
            require = 1
    )
    private void rustedfabricapi$afterLineEffectCreated(float startX, float startY, float startHeight,
                                                        float targetX, float targetY, float targetHeight,
                                                        CallbackInfoReturnable<Object> cir) {
        EffectRuntimeEvents.AFTER_LINE_EFFECT_CREATED.invoker()
                .afterLineEffectCreated(this, cir.getReturnValue(), startX, startY, startHeight,
                        targetX, targetY, targetHeight);
    }

    @Inject(
            method = "createLightEffect(FFFI)Lrustedwarfare/render/effect/EffectInstance;",
            at = @At("RETURN"),
            require = 1
    )
    private void rustedfabricapi$afterLightEffectCreated(float x, float y, float height, int color,
                                                         CallbackInfoReturnable<Object> cir) {
        EffectRuntimeEvents.AFTER_LIGHT_EFFECT_CREATED.invoker()
                .afterLightEffectCreated(this, cir.getReturnValue(), x, y, height, color);
    }

    @Inject(
            method = "createAttachedLightEffect(Lrustedwarfare/framework/GameObject;I)Lrustedwarfare/render/effect/EffectInstance;",
            at = @At("RETURN"),
            require = 1
    )
    private void rustedfabricapi$afterAttachedLightEffectCreatedDefaultSize(@Coerce Object object, int color,
                                                                            CallbackInfoReturnable<Object> cir) {
        EffectRuntimeEvents.AFTER_ATTACHED_LIGHT_EFFECT_CREATED.invoker()
                .afterAttachedLightEffectCreated(this, cir.getReturnValue(), object, color, 0.5F);
    }

    @Inject(
            method = "createAttachedLightEffect(Lrustedwarfare/framework/GameObject;IF)Lrustedwarfare/render/effect/EffectInstance;",
            at = @At("RETURN"),
            require = 1
    )
    private void rustedfabricapi$afterAttachedLightEffectCreated(@Coerce Object object, int color, float size,
                                                                 CallbackInfoReturnable<Object> cir) {
        EffectRuntimeEvents.AFTER_ATTACHED_LIGHT_EFFECT_CREATED.invoker()
                .afterAttachedLightEffectCreated(this, cir.getReturnValue(), object, color, size);
    }
}
