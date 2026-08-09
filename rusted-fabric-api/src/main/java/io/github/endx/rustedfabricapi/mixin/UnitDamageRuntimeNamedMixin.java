package io.github.endx.rustedfabricapi.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "rustedwarfare.unit.Unit", remap = false)
public abstract class UnitDamageRuntimeNamedMixin {
    @Unique private Float rustedfabricapi$unclampedLethalHp;
    @Unique private rustedwarfare.unit.Unit rustedfabricapi$damageAttacker;
    @Unique private rustedwarfare.game.Projectile rustedfabricapi$damageProjectile;
    @Unique private float rustedfabricapi$requestedDamage;

    @Inject(method = "applyDamage(Lrustedwarfare/unit/Unit;FLrustedwarfare/game/Projectile;)F", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeUnitApplyDamage(@Coerce Object attacker, float amount, @Coerce Object projectile, CallbackInfoReturnable<Float> cir) {
        rustedfabricapi$captureUnclampedLethalHp(
                (rustedwarfare.unit.Unit) attacker, amount,
                (rustedwarfare.game.Projectile) projectile);
        boolean cancelled = io.github.endx.rustedfabricapi.api.unit.event.UnitDamageEvents.BEFORE_DAMAGE.invoker()
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
        io.github.endx.rustedfabricapi.api.unit.event.UnitDamageEvents.AFTER_DAMAGE.invoker()
                .afterDamage((rustedwarfare.unit.Unit) (Object) this,
                        (rustedwarfare.unit.Unit) attacker, amount,
                        (rustedwarfare.game.Projectile) projectile,
                        result != null ? result.floatValue() : 0.0F);
        rustedfabricapi$clearLethalCapture();
    }

    @Redirect(method = "applyDamage(Lrustedwarfare/unit/Unit;FLrustedwarfare/game/Projectile;)F",
            at = @At(value = "INVOKE", target = "Lrustedwarfare/unit/Unit;setHp(F)V", ordinal = 0),
            require = 1)
    private void rustedfabricapi$modifyLethalHealthClamp(rustedwarfare.unit.Unit unit,
                                                         float nativeValue) {
        Float unclamped = rustedfabricapi$unclampedLethalHp;
        float replacement = nativeValue;
        if (unclamped != null) {
            replacement = io.github.endx.rustedfabricapi.api.unit.event.UnitDamageEvents
                    .MODIFY_LETHAL_HEALTH.invoker().modify(
                            unit, rustedfabricapi$damageAttacker, rustedfabricapi$requestedDamage,
                            rustedfabricapi$damageProjectile, nativeValue,
                            unclamped.floatValue(), nativeValue).floatValue();
        }
        rustedfabricapi$clearLethalCapture();
        unit.setHp(replacement);
    }

    @Unique
    private void rustedfabricapi$captureUnclampedLethalHp(rustedwarfare.unit.Unit attacker,
                                                           float amount,
                                                           rustedwarfare.game.Projectile projectile) {
        if (io.github.endx.rustedfabricapi.api.unit.event.UnitDamageEvents
                .MODIFY_LETHAL_HEALTH.listenerCount() == 0) {
            rustedfabricapi$clearLethalCapture();
            return;
        }
        rustedwarfare.unit.Unit unit = (rustedwarfare.unit.Unit) (Object) this;
        float shieldDamageMultiplier = projectile != null ? projectile.shieldDamageMultiplier : 1.0F;
        float shieldDeflectionMultiplier = projectile != null ? projectile.shieldDeflectionMultiplier : 1.0F;
        float hullDamageMultiplier = projectile != null ? projectile.hullDamageMultiplier : 1.0F;
        rustedfabricapi$unclampedLethalHp = Float.valueOf(
                io.github.endx.rustedfabricapi.impl.combat.NativeDamageMath.projectedHp(
                        unit.hp, unit.cm, unit.cz, unit.shield, amount,
                        shieldDamageMultiplier, shieldDeflectionMultiplier,
                        hullDamageMultiplier));
        rustedfabricapi$damageAttacker = attacker;
        rustedfabricapi$damageProjectile = projectile;
        rustedfabricapi$requestedDamage = amount;
    }

    @Unique
    private void rustedfabricapi$clearLethalCapture() {
        rustedfabricapi$unclampedLethalHp = null;
        rustedfabricapi$damageAttacker = null;
        rustedfabricapi$damageProjectile = null;
        rustedfabricapi$requestedDamage = 0.0F;
    }

    @Inject(method = "isDamageImmune()Z", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$modifyUnitDamageImmunity(CallbackInfoReturnable<Boolean> cir) {
        Boolean result = io.github.endx.rustedfabricapi.api.unit.event.UnitDamageEvents.MODIFY_DAMAGE_IMMUNITY
                .invoker().modify((rustedwarfare.unit.Unit) (Object) this,
                        Boolean.TRUE.equals(cir.getReturnValue()));
        if (result != null) {
            cir.setReturnValue(result);
        }
    }

    @Inject(method = "killAndHandleDeathEffects()V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeUnitDeathSequence(CallbackInfo ci) {
        boolean cancelled = io.github.endx.rustedfabricapi.api.unit.event.UnitDamageEvents.BEFORE_DEATH.invoker()
                .beforeDeath((rustedwarfare.unit.Unit) (Object) this);
        if (cancelled) {
            ci.cancel();
        }
    }

    @Inject(method = "killAndHandleDeathEffects()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterUnitDeathSequence(CallbackInfo ci) {
        io.github.endx.rustedfabricapi.api.unit.event.UnitDamageEvents.AFTER_DEATH.invoker()
                .afterDeath((rustedwarfare.unit.Unit) (Object) this);
    }

    @Inject(method = "handleDeathEffects()Z", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$modifyUnitDeathEffectsResult(CallbackInfoReturnable<Boolean> cir) {
        Boolean result = io.github.endx.rustedfabricapi.api.unit.event.UnitDamageEvents
                .MODIFY_KEEP_OBJECT_AFTER_DEATH.invoker().modify(
                        (rustedwarfare.unit.Unit) (Object) this,
                        Boolean.TRUE.equals(cir.getReturnValue()));
        if (result != null) {
            cir.setReturnValue(result);
        }
    }
}
