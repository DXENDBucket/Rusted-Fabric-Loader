package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.CustomUnitRuntimeEvents;
import io.github.endx.rustedfabricapi.api.custom.event.CustomUnitUpdateContext;
import io.github.endx.rustedfabricapi.api.custom.event.CustomUnitUpdateEvents;
import io.github.endx.rustedfabricapi.impl.custom.PerActionAutoTriggerCooldownRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "rustedwarfare.custom.CustomUnit", remap = false)
public abstract class CustomUnitRuntimeNamedMixin {
    @Shadow private float autoTriggerCooldownTimer;

    @Inject(method = "update(F)V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeCustomUnitUpdate(float deltaFrames, CallbackInfo ci) {
        CustomUnitUpdateEvents.BEFORE_UPDATE.invoker().update(new CustomUnitUpdateContext(
                (rustedwarfare.custom.CustomUnit) (Object) this, deltaFrames));
    }

    @Inject(method = "update(F)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterCustomUnitUpdate(float deltaFrames, CallbackInfo ci) {
        CustomUnitUpdateEvents.AFTER_UPDATE.invoker().update(new CustomUnitUpdateContext(
                (rustedwarfare.custom.CustomUnit) (Object) this, deltaFrames));
    }

    @Inject(method = "b(FZ)V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$updatePerActionAutoTriggerCooldowns(
            float deltaFrames, boolean forceCheck, CallbackInfo ci) {
        if (PerActionAutoTriggerCooldownRuntime.beforeAutoTriggerUpdate(
                (rustedwarfare.custom.CustomUnit) (Object) this, deltaFrames)) {
            autoTriggerCooldownTimer = 0.0F;
        }
    }

    @Redirect(
            method = "a(F[Lrustedwarfare/custom/action/AutoTriggerConfig;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lrustedwarfare/custom/CustomUnit;executeActionWithContext(Lrustedwarfare/unit/action/UnitAction;Landroid/graphics/PointF;Lrustedwarfare/unit/Unit;II)Z"
            ),
            require = 1
    )
    private boolean rustedfabricapi$executeAutoTriggerWithIndependentCooldown(
            rustedwarfare.custom.CustomUnit unit,
            rustedwarfare.unit.action.UnitAction action,
            android.graphics.PointF targetPoint,
            rustedwarfare.unit.Unit targetUnit,
            int recursionDepth, int repeatedCount) {
        return PerActionAutoTriggerCooldownRuntime.execute(unit, action, targetPoint,
                targetUnit, recursionDepth, repeatedCount);
    }

    @Redirect(
            method = "fireProjectileAtGround(Lrustedwarfare/unit/Unit;FFILrustedwarfare/custom/CustomProjectileTemplate;I)Lrustedwarfare/game/Projectile;",
            at = @At(
                    value = "INVOKE",
                    target = "Lrustedwarfare/custom/TurretTemplate;getProjectileIndexForUnit(Lrustedwarfare/custom/CustomUnit;)I"
            ),
            require = 1
    )
    private int rustedfabricapi$selectTurretProjectile(
            rustedwarfare.custom.TurretTemplate turret,
            rustedwarfare.custom.CustomUnit shooter,
            rustedwarfare.unit.Unit targetUnit, float x, float y, int turretIndex,
            rustedwarfare.custom.CustomProjectileTemplate explicitTemplate,
            int projectileCount) {
        int nativeIndex = turret.getProjectileIndexForUnit(shooter);
        return io.github.endx.rustedfabricapi.api.projectile.event.ProjectileCombatEvents
                .SELECT_TURRET_PROJECTILE.invoker()
                .select(shooter, targetUnit, turret, turretIndex, nativeIndex, nativeIndex);
    }

    @Inject(method = "executeCustomAction(Lrustedwarfare/unit/action/UnitAction;Landroid/graphics/PointF;Lrustedwarfare/unit/Unit;I)Z", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeCustomActionExecute(@Coerce Object action, @Coerce Object targetPoint, @Coerce Object targetUnit, int recursionDepth, CallbackInfoReturnable<Boolean> cir) {
        if (CustomUnitRuntimeEvents.BEFORE_CUSTOM_ACTION_EXECUTE.invoker().beforeCustomActionExecute(this, action, targetPoint, targetUnit, recursionDepth)) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }

    @Inject(method = "executeCustomAction(Lrustedwarfare/unit/action/UnitAction;Landroid/graphics/PointF;Lrustedwarfare/unit/Unit;I)Z", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterCustomActionExecute(@Coerce Object action, @Coerce Object targetPoint, @Coerce Object targetUnit, int recursionDepth, CallbackInfoReturnable<Boolean> cir) {
        CustomUnitRuntimeEvents.AFTER_CUSTOM_ACTION_EXECUTE.invoker().afterCustomActionExecute(this, action, targetPoint, targetUnit, recursionDepth, Boolean.TRUE.equals(cir.getReturnValue()));
    }

    @Inject(
            method = "executeCustomAction(Lrustedwarfare/unit/action/UnitAction;Landroid/graphics/PointF;Lrustedwarfare/unit/Unit;I)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lrustedwarfare/custom/CustomUnit;applyUnitMetadataWithStatOverrides(Lrustedwarfare/custom/CustomUnitMetadata;ZZ[Lrustedwarfare/custom/MutableStatAccessor;)V"
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
            method = "executeCustomAction(Lrustedwarfare/unit/action/UnitAction;Landroid/graphics/PointF;Lrustedwarfare/unit/Unit;I)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lrustedwarfare/custom/CustomUnit;applyUnitMetadataWithStatOverrides(Lrustedwarfare/custom/CustomUnitMetadata;ZZ[Lrustedwarfare/custom/MutableStatAccessor;)V",
                    shift = At.Shift.AFTER
            ),
            require = 1
    )
    private void rustedfabricapi$afterCustomUnitConvert(@Coerce Object action, @Coerce Object targetPoint, @Coerce Object targetUnit, int recursionDepth, CallbackInfoReturnable<Boolean> cir) {
        CustomUnitRuntimeEvents.AFTER_CUSTOM_UNIT_CONVERT.invoker().afterCustomUnitConvert(this, action, targetPoint, targetUnit, recursionDepth);
    }

    @Inject(method = "fireTurretAtTarget(Lrustedwarfare/unit/Unit;I)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeTurretFireAtTarget(@Coerce Object targetUnit, int turretIndex, CallbackInfo ci) {
        if (CustomUnitRuntimeEvents.BEFORE_TURRET_FIRE_AT_TARGET.invoker().beforeTurretFireAtTarget(this, targetUnit, turretIndex)) {
            ci.cancel();
        }
    }

    @Inject(method = "fireProjectileAtGround(Lrustedwarfare/unit/Unit;FFILrustedwarfare/custom/CustomProjectileTemplate;I)Lrustedwarfare/game/Projectile;", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeFireProjectileAtGround(@Coerce Object targetUnit, float x, float y, int turretIndex, @Coerce Object template, int projectileCount, CallbackInfoReturnable<Object> cir) {
        if (CustomUnitRuntimeEvents.BEFORE_FIRE_PROJECTILE_AT_GROUND.invoker().beforeFireProjectileAtGround(this, targetUnit, x, y, turretIndex, template, projectileCount)) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "createProjectileFromTemplate(Lrustedwarfare/unit/Unit;ILrustedwarfare/custom/CustomProjectileTemplate;FFFF)Lrustedwarfare/game/Projectile;", at = @At("RETURN"), require = 1)
    private static void rustedfabricapi$afterProjectileCreatedFromTemplate(@Coerce Object targetUnit, int turretIndex, @Coerce Object template, float x, float y, float height, float direction, CallbackInfoReturnable<Object> cir) {
        CustomUnitRuntimeEvents.AFTER_PROJECTILE_CREATED_FROM_TEMPLATE.invoker().afterProjectileCreatedFromTemplate(cir.getReturnValue(), targetUnit, turretIndex, template, x, y, height, direction);
    }

    @Inject(method = "applyProjectileTemplateToProjectile(Lrustedwarfare/game/Projectile;Lrustedwarfare/unit/Unit;ILrustedwarfare/custom/CustomProjectileTemplate;FFFF)V", at = @At("RETURN"), require = 1)
    private static void rustedfabricapi$afterProjectileTemplateApplied(@Coerce Object projectile, @Coerce Object targetUnit, int turretIndex, @Coerce Object template, float x, float y, float height, float direction, CallbackInfo ci) {
        CustomUnitRuntimeEvents.AFTER_PROJECTILE_TEMPLATE_APPLIED.invoker().afterProjectileTemplateApplied(projectile, targetUnit, turretIndex, template, x, y, height, direction);
    }

    @Inject(method = "addUnitToTransport(Lrustedwarfare/unit/Unit;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterCustomUnitTransportLoad(@Coerce Object transportedUnit, CallbackInfo ci) {
        CustomUnitRuntimeEvents.AFTER_CUSTOM_UNIT_TRANSPORT_LOAD.invoker().afterCustomUnitTransportLoad(this, transportedUnit);
    }

    @Inject(method = "removeUnitFromTransport(Lrustedwarfare/unit/Unit;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterCustomUnitTransportUnload(@Coerce Object transportedUnit, CallbackInfo ci) {
        CustomUnitRuntimeEvents.AFTER_CUSTOM_UNIT_TRANSPORT_UNLOAD.invoker().afterCustomUnitTransportUnload(this, transportedUnit);
    }

    @Inject(method = "onKilled()V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeCustomUnitKilled(CallbackInfo ci) {
        boolean cancelled = io.github.endx.rustedfabricapi.api.custom.event.CustomUnitLifecycleEvents.BEFORE_KILLED
                .invoker().beforeUnit((rustedwarfare.custom.CustomUnit) (Object) this);
        if (cancelled) {
            ci.cancel();
        }
    }

    @Inject(method = "onKilled()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterCustomUnitKilled(CallbackInfo ci) {
        CustomUnitRuntimeEvents.AFTER_CUSTOM_UNIT_KILLED.invoker().afterCustomUnitKilled(this);
        io.github.endx.rustedfabricapi.api.custom.event.CustomUnitLifecycleEvents.AFTER_KILLED
                .invoker().afterUnit((rustedwarfare.custom.CustomUnit) (Object) this);
    }

    @Inject(method = "removeFromGame()V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeCustomUnitRemoved(CallbackInfo ci) {
        boolean cancelled = io.github.endx.rustedfabricapi.api.custom.event.CustomUnitLifecycleEvents.BEFORE_REMOVED
                .invoker().beforeUnit((rustedwarfare.custom.CustomUnit) (Object) this);
        if (cancelled) {
            ci.cancel();
        }
    }

    @Inject(method = "removeFromGame()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterCustomUnitRemoved(CallbackInfo ci) {
        CustomUnitRuntimeEvents.AFTER_CUSTOM_UNIT_REMOVED.invoker().afterCustomUnitRemoved(this);
        io.github.endx.rustedfabricapi.api.custom.event.CustomUnitLifecycleEvents.AFTER_REMOVED
                .invoker().afterUnit((rustedwarfare.custom.CustomUnit) (Object) this);
    }

    @Inject(method = "completeBuildQueueItem(Lrustedwarfare/unit/build/BuildQueueItem;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterBuildQueueItemComplete(@Coerce Object queueItem, CallbackInfo ci) {
        CustomUnitRuntimeEvents.AFTER_BUILD_QUEUE_ITEM_COMPLETE.invoker().afterBuildQueueItemComplete(this, queueItem);
    }
}
