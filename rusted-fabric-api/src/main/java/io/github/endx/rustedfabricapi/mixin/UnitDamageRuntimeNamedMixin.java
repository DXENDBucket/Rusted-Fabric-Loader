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
        boolean cancelled = UnitDamageEvents.BEFORE_UNIT_APPLY_DAMAGE.invoker()
                .beforeUnitApplyDamage(this, attacker, amount, projectile);
        cancelled |= io.github.endx.rustedfabricapi.api.unit.event.UnitDamageEvents.BEFORE_DAMAGE.invoker()
                .beforeDamage((rustedwarfare.unit.Unit) (Object) this,
                        (rustedwarfare.unit.Unit) attacker, amount,
                        (rustedwarfare.game.Projectile) projectile);
        if (cancelled) {
            cir.setReturnValue(Float.valueOf(0.0F));
        }
    }

    @Inject(method = "applyDamage(Lrustedwarfare/unit/Unit;FLrustedwarfare/game/Projectile;)F", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterUnitApplyDamage(@Coerce Object attacker, float amount, @Coerce Object projectile, CallbackInfoReturnable<Float> cir) {
        Float result = cir.getReturnValue();
        UnitDamageEvents.AFTER_UNIT_APPLY_DAMAGE.invoker().afterUnitApplyDamage(this, attacker, amount, projectile,
                result != null ? result.floatValue() : 0.0F);
        io.github.endx.rustedfabricapi.api.unit.event.UnitDamageEvents.AFTER_DAMAGE.invoker()
                .afterDamage((rustedwarfare.unit.Unit) (Object) this,
                        (rustedwarfare.unit.Unit) attacker, amount,
                        (rustedwarfare.game.Projectile) projectile,
                        result != null ? result.floatValue() : 0.0F);
    }

    @Inject(method = "isDamageImmune()Z", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$modifyUnitDamageImmunity(CallbackInfoReturnable<Boolean> cir) {
        Boolean result = UnitDamageEvents.MODIFY_UNIT_DAMAGE_IMMUNITY.invoker()
                .modifyUnitDamageImmunity(this, Boolean.TRUE.equals(cir.getReturnValue()));
        result = io.github.endx.rustedfabricapi.api.unit.event.UnitDamageEvents.MODIFY_DAMAGE_IMMUNITY.invoker()
                .modify((rustedwarfare.unit.Unit) (Object) this, Boolean.TRUE.equals(result));
        if (result != null) {
            cir.setReturnValue(result);
        }
    }

    @Inject(method = "killAndHandleDeathEffects()V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeUnitDeathSequence(CallbackInfo ci) {
        boolean cancelled = UnitDamageEvents.BEFORE_UNIT_DEATH_SEQUENCE.invoker().beforeUnitDeathSequence(this);
        cancelled |= io.github.endx.rustedfabricapi.api.unit.event.UnitDamageEvents.BEFORE_DEATH.invoker()
                .beforeDeath((rustedwarfare.unit.Unit) (Object) this);
        if (cancelled) {
            ci.cancel();
        }
    }

    @Inject(method = "killAndHandleDeathEffects()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterUnitDeathSequence(CallbackInfo ci) {
        UnitDamageEvents.AFTER_UNIT_DEATH_SEQUENCE.invoker().afterUnitDeathSequence(this);
        io.github.endx.rustedfabricapi.api.unit.event.UnitDamageEvents.AFTER_DEATH.invoker()
                .afterDeath((rustedwarfare.unit.Unit) (Object) this);
    }

    @Inject(method = "handleDeathEffects()Z", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$modifyUnitDeathEffectsResult(CallbackInfoReturnable<Boolean> cir) {
        Boolean result = UnitDamageEvents.MODIFY_UNIT_DEATH_EFFECTS_RESULT.invoker()
                .modifyUnitDeathEffectsResult(this, Boolean.TRUE.equals(cir.getReturnValue()));
        result = io.github.endx.rustedfabricapi.api.unit.event.UnitDamageEvents.MODIFY_KEEP_OBJECT_AFTER_DEATH.invoker()
                .modify((rustedwarfare.unit.Unit) (Object) this, Boolean.TRUE.equals(result));
        if (result != null) {
            cir.setReturnValue(result);
        }
    }
}
