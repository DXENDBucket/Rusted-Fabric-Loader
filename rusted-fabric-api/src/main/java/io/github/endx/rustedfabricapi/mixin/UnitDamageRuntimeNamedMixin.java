package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.UnitDamageEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "rustedwarfare.unit.Unit", remap = false)
public abstract class UnitDamageRuntimeNamedMixin {
    @Inject(method = "applyDamage(Lrustedwarfare/unit/Unit;FLrustedwarfare/game/Projectile;)F", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeUnitApplyDamage(@Coerce Object attacker, float amount, @Coerce Object projectile, CallbackInfoReturnable<Float> cir) {
        if (UnitDamageEvents.BEFORE_UNIT_APPLY_DAMAGE.invoker().beforeUnitApplyDamage(this, attacker, amount, projectile)) {
            cir.setReturnValue(Float.valueOf(0.0F));
        }
    }

    @Inject(method = "applyDamage(Lrustedwarfare/unit/Unit;FLrustedwarfare/game/Projectile;)F", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterUnitApplyDamage(@Coerce Object attacker, float amount, @Coerce Object projectile, CallbackInfoReturnable<Float> cir) {
        Float result = cir.getReturnValue();
        UnitDamageEvents.AFTER_UNIT_APPLY_DAMAGE.invoker().afterUnitApplyDamage(this, attacker, amount, projectile,
                result != null ? result.floatValue() : 0.0F);
    }

    @Inject(method = "isDamageImmune()Z", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$modifyUnitDamageImmunity(CallbackInfoReturnable<Boolean> cir) {
        Boolean result = UnitDamageEvents.MODIFY_UNIT_DAMAGE_IMMUNITY.invoker()
                .modifyUnitDamageImmunity(this, Boolean.TRUE.equals(cir.getReturnValue()));
        if (result != null) {
            cir.setReturnValue(result);
        }
    }

    @Inject(method = "killAndHandleDeathEffects()V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeUnitDeathSequence(CallbackInfo ci) {
        if (UnitDamageEvents.BEFORE_UNIT_DEATH_SEQUENCE.invoker().beforeUnitDeathSequence(this)) {
            ci.cancel();
        }
    }

    @Inject(method = "killAndHandleDeathEffects()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterUnitDeathSequence(CallbackInfo ci) {
        UnitDamageEvents.AFTER_UNIT_DEATH_SEQUENCE.invoker().afterUnitDeathSequence(this);
    }

    @Inject(method = "handleDeathEffects()Z", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$modifyUnitDeathEffectsResult(CallbackInfoReturnable<Boolean> cir) {
        Boolean result = UnitDamageEvents.MODIFY_UNIT_DEATH_EFFECTS_RESULT.invoker()
                .modifyUnitDeathEffectsResult(this, Boolean.TRUE.equals(cir.getReturnValue()));
        if (result != null) {
            cir.setReturnValue(result);
        }
    }
}
