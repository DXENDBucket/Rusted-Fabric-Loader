package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.CustomUnitRuntimeEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.corrodinggames.rts.game.units.custom.j", remap = false)
public abstract class CustomUnitRuntimeOfficialMixin {
    @Inject(method = "a(Lcom/corrodinggames/rts/game/units/a/s;Landroid/graphics/PointF;Lcom/corrodinggames/rts/game/units/am;I)Z", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeCustomActionExecute(@Coerce Object action, @Coerce Object targetPoint, @Coerce Object targetUnit, int recursionDepth, CallbackInfoReturnable<Boolean> cir) {
        if (CustomUnitRuntimeEvents.BEFORE_CUSTOM_ACTION_EXECUTE.invoker().beforeCustomActionExecute(this, action, targetPoint, targetUnit, recursionDepth)) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }

    @Inject(method = "a(Lcom/corrodinggames/rts/game/units/a/s;Landroid/graphics/PointF;Lcom/corrodinggames/rts/game/units/am;I)Z", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterCustomActionExecute(@Coerce Object action, @Coerce Object targetPoint, @Coerce Object targetUnit, int recursionDepth, CallbackInfoReturnable<Boolean> cir) {
        CustomUnitRuntimeEvents.AFTER_CUSTOM_ACTION_EXECUTE.invoker().afterCustomActionExecute(this, action, targetPoint, targetUnit, recursionDepth, Boolean.TRUE.equals(cir.getReturnValue()));
    }

    @Inject(
            method = "a(Lcom/corrodinggames/rts/game/units/a/s;Landroid/graphics/PointF;Lcom/corrodinggames/rts/game/units/am;I)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/corrodinggames/rts/game/units/custom/j;a(Lcom/corrodinggames/rts/game/units/custom/l;ZZ[Lcom/corrodinggames/rts/game/units/custom/at;)V"
            ),
            cancellable = true,
            require = 1
    )
    private void rustedfabricapi$beforeCustomUnitConvert(@Coerce Object action, @Coerce Object targetPoint, @Coerce Object targetUnit, int recursionDepth, CallbackInfoReturnable<Boolean> cir) {
        if (CustomUnitRuntimeEvents.BEFORE_CUSTOM_UNIT_CONVERT.invoker().beforeCustomUnitConvert(this, action, targetPoint, targetUnit, recursionDepth)) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }

    @Inject(
            method = "a(Lcom/corrodinggames/rts/game/units/a/s;Landroid/graphics/PointF;Lcom/corrodinggames/rts/game/units/am;I)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/corrodinggames/rts/game/units/custom/j;a(Lcom/corrodinggames/rts/game/units/custom/l;ZZ[Lcom/corrodinggames/rts/game/units/custom/at;)V",
                    shift = At.Shift.AFTER
            ),
            require = 1
    )
    private void rustedfabricapi$afterCustomUnitConvert(@Coerce Object action, @Coerce Object targetPoint, @Coerce Object targetUnit, int recursionDepth, CallbackInfoReturnable<Boolean> cir) {
        CustomUnitRuntimeEvents.AFTER_CUSTOM_UNIT_CONVERT.invoker().afterCustomUnitConvert(this, action, targetPoint, targetUnit, recursionDepth);
    }

    @Inject(method = "a(Lcom/corrodinggames/rts/game/units/am;I)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeTurretFireAtTarget(@Coerce Object targetUnit, int turretIndex, CallbackInfo ci) {
        if (CustomUnitRuntimeEvents.BEFORE_TURRET_FIRE_AT_TARGET.invoker().beforeTurretFireAtTarget(this, targetUnit, turretIndex)) {
            ci.cancel();
        }
    }

    @Inject(method = "a(Lcom/corrodinggames/rts/game/units/am;FFILcom/corrodinggames/rts/game/units/custom/bh;I)Lcom/corrodinggames/rts/game/f;", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeFireProjectileAtGround(@Coerce Object targetUnit, float x, float y, int turretIndex, @Coerce Object template, int projectileCount, CallbackInfoReturnable<Object> cir) {
        if (CustomUnitRuntimeEvents.BEFORE_FIRE_PROJECTILE_AT_GROUND.invoker().beforeFireProjectileAtGround(this, targetUnit, x, y, turretIndex, template, projectileCount)) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "a(Lcom/corrodinggames/rts/game/units/am;ILcom/corrodinggames/rts/game/units/custom/bh;FFFF)Lcom/corrodinggames/rts/game/f;", at = @At("RETURN"), require = 1)
    private static void rustedfabricapi$afterProjectileCreatedFromTemplate(@Coerce Object targetUnit, int turretIndex, @Coerce Object template, float x, float y, float height, float direction, CallbackInfoReturnable<Object> cir) {
        CustomUnitRuntimeEvents.AFTER_PROJECTILE_CREATED_FROM_TEMPLATE.invoker().afterProjectileCreatedFromTemplate(cir.getReturnValue(), targetUnit, turretIndex, template, x, y, height, direction);
    }

    @Inject(method = "a(Lcom/corrodinggames/rts/game/f;Lcom/corrodinggames/rts/game/units/am;ILcom/corrodinggames/rts/game/units/custom/bh;FFFF)V", at = @At("RETURN"), require = 1)
    private static void rustedfabricapi$afterProjectileTemplateApplied(@Coerce Object projectile, @Coerce Object targetUnit, int turretIndex, @Coerce Object template, float x, float y, float height, float direction, CallbackInfo ci) {
        CustomUnitRuntimeEvents.AFTER_PROJECTILE_TEMPLATE_APPLIED.invoker().afterProjectileTemplateApplied(projectile, targetUnit, turretIndex, template, x, y, height, direction);
    }

    @Inject(method = "C(Lcom/corrodinggames/rts/game/units/am;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterCustomUnitTransportLoad(@Coerce Object transportedUnit, CallbackInfo ci) {
        CustomUnitRuntimeEvents.AFTER_CUSTOM_UNIT_TRANSPORT_LOAD.invoker().afterCustomUnitTransportLoad(this, transportedUnit);
    }

    @Inject(method = "e(Lcom/corrodinggames/rts/game/units/am;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterCustomUnitTransportUnload(@Coerce Object transportedUnit, CallbackInfo ci) {
        CustomUnitRuntimeEvents.AFTER_CUSTOM_UNIT_TRANSPORT_UNLOAD.invoker().afterCustomUnitTransportUnload(this, transportedUnit);
    }

    @Inject(method = "bu()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterCustomUnitKilled(CallbackInfo ci) {
        CustomUnitRuntimeEvents.AFTER_CUSTOM_UNIT_KILLED.invoker().afterCustomUnitKilled(this);
    }

    @Inject(method = "a()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterCustomUnitRemoved(CallbackInfo ci) {
        CustomUnitRuntimeEvents.AFTER_CUSTOM_UNIT_REMOVED.invoker().afterCustomUnitRemoved(this);
    }

    @Inject(method = "a(Lcom/corrodinggames/rts/game/units/d/j;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterBuildQueueItemComplete(@Coerce Object queueItem, CallbackInfo ci) {
        CustomUnitRuntimeEvents.AFTER_BUILD_QUEUE_ITEM_COMPLETE.invoker().afterBuildQueueItemComplete(this, queueItem);
    }
}
