package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.impl.projectile.TurretProjectilePatternRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import rustedwarfare.custom.CustomProjectileTemplate;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.game.Projectile;
import rustedwarfare.unit.Unit;

/** Preserves the native firing method while replacing only its projectile creation segment. */
@Mixin(targets = "rustedwarfare.custom.CustomUnit", remap = false)
public abstract class CustomUnitTurretProjectilePatternNamedMixin {
    @ModifyVariable(
            method = "fireProjectileAtGround(Lrustedwarfare/unit/Unit;FFI"
                    + "Lrustedwarfare/custom/CustomProjectileTemplate;I)"
                    + "Lrustedwarfare/game/Projectile;",
            at = @At("STORE"), index = 9, require = 1, expect = 1)
    private CustomProjectileTemplate rustedfabricapi$planTurretPattern(
            CustomProjectileTemplate selectedTemplate, Unit targetUnit,
            float targetX, float targetY, int turretIndex,
            CustomProjectileTemplate explicitTemplate, int projectileCount) {
        return TurretProjectilePatternRuntime.selectTemplate(
                (CustomUnit) (Object) this, targetUnit, targetX, targetY,
                turretIndex, selectedTemplate, projectileCount);
    }

    @ModifyArgs(
            method = "fireProjectileAtGround(Lrustedwarfare/unit/Unit;FFI"
                    + "Lrustedwarfare/custom/CustomProjectileTemplate;I)"
                    + "Lrustedwarfare/game/Projectile;",
            at = @At(
                    value = "INVOKE",
                    target = "Lrustedwarfare/game/Projectile;"
                            + "createProjectileWithHeightAndTurret("
                            + "Lrustedwarfare/unit/Unit;FFFI)"
                            + "Lrustedwarfare/game/Projectile;"
            ),
            require = 1
    )
    private void rustedfabricapi$movePrimaryToFirstPatternOrigin(Args args) {
        TurretProjectilePatternRuntime.modifyCreateArguments(args);
    }

    @ModifyArgs(
            method = "fireProjectileAtGround(Lrustedwarfare/unit/Unit;FFI"
                    + "Lrustedwarfare/custom/CustomProjectileTemplate;I)"
                    + "Lrustedwarfare/game/Projectile;",
            at = @At(
                    value = "INVOKE",
                    target = "Lrustedwarfare/custom/CustomUnit;"
                            + "applyProjectileTemplateToProjectile("
                            + "Lrustedwarfare/game/Projectile;Lrustedwarfare/unit/Unit;I"
                            + "Lrustedwarfare/custom/CustomProjectileTemplate;FFFF)V"
            ),
            require = 1
    )
    private void rustedfabricapi$applyFirstPatternEntry(Args args) {
        TurretProjectilePatternRuntime.modifyTemplateArguments(args);
    }

    @Redirect(
            method = "fireProjectileAtGround(Lrustedwarfare/unit/Unit;FFI"
                    + "Lrustedwarfare/custom/CustomProjectileTemplate;I)"
                    + "Lrustedwarfare/game/Projectile;",
            at = @At(
                    value = "INVOKE",
                    target = "Lrustedwarfare/custom/CustomProjectileTemplate;"
                            + "applyOnProjectileCreatedEffects("
                            + "Lrustedwarfare/unit/Unit;Lrustedwarfare/game/Projectile;"
                            + "Lrustedwarfare/unit/Unit;FFF)V"
            ),
            require = 1
    )
    private void rustedfabricapi$initializePrimaryAndEmitRemaining(
            CustomProjectileTemplate template, Unit source, Projectile projectile,
            Unit targetUnit, float targetX, float targetY, float targetLeadRange) {
        TurretProjectilePatternRuntime.applyCreatedEffects(template, source, projectile,
                targetUnit, targetX, targetY, targetLeadRange);
    }

    @Inject(
            method = "fireProjectileAtGround(Lrustedwarfare/unit/Unit;FFI"
                    + "Lrustedwarfare/custom/CustomProjectileTemplate;I)"
                    + "Lrustedwarfare/game/Projectile;",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$clearTurretPatternState(
            Unit targetUnit, float targetX, float targetY, int turretIndex,
            CustomProjectileTemplate explicitTemplate, int projectileCount,
            CallbackInfoReturnable<Projectile> cir) {
        TurretProjectilePatternRuntime.clear();
    }
}
