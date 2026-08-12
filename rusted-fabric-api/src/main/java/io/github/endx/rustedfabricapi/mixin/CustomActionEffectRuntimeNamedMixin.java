package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.CustomUnitRuntimeEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.custom.action.effect.CustomActionEffect;
import rustedwarfare.unit.Unit;
import rustedwarfare.unit.action.UnitAction;

@Mixin(
        targets = {
                "rustedwarfare.custom.action.effect.ResourceModificationActionEffect",
                "rustedwarfare.custom.action.effect.WaypointActionEffect",
                "rustedwarfare.custom.action.effect.AnimationActionEffect",
                "rustedwarfare.custom.action.effect.AttachmentActionEffect",
                "rustedwarfare.custom.action.effect.MessageActionEffect",
                "rustedwarfare.custom.action.effect.ResourceConversionActionEffect",
                "rustedwarfare.custom.action.effect.MemoryAndTargetActionEffect",
                "rustedwarfare.custom.action.effect.SendMessageActionEffect",
                "rustedwarfare.custom.action.effect.SelfMutationActionEffect",
                "rustedwarfare.custom.action.effect.SpawnUnitsActionEffect",
                "rustedwarfare.custom.action.effect.TagModificationActionEffect",
                "rustedwarfare.custom.action.effect.TakeResourcesActionEffect",
                "rustedwarfare.custom.action.effect.TransportActionEffect"
        },
        remap = false
)
public abstract class CustomActionEffectRuntimeNamedMixin {
    private static final String[] RUSTED_FABRIC_TARGET_X_FIELDS = {"x", "a"};
    private static final String[] RUSTED_FABRIC_TARGET_Y_FIELDS = {"y", "b"};

    @Inject(method = "execute(Lrustedwarfare/custom/CustomUnit;Lrustedwarfare/unit/action/UnitAction;Landroid/graphics/PointF;Lrustedwarfare/unit/Unit;I)Z", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeCustomActionEffectExecute(@Coerce Object unit, @Coerce Object action, @Coerce Object targetPoint, @Coerce Object targetUnit, int recursionDepth, CallbackInfoReturnable<Boolean> cir) {
        boolean cancelled = CustomUnitRuntimeEvents.BEFORE_CUSTOM_ACTION_EFFECT_EXECUTE.invoker()
                .beforeCustomActionEffectExecute(this, unit, action, targetPoint, targetUnit, recursionDepth);
        io.github.endx.rustedfabricapi.api.event.RustedFabricEvent<io.github.endx.rustedfabricapi.api.custom.action.event.CustomActionEffectEvents.BeforeExecute> typedEvent =
                io.github.endx.rustedfabricapi.api.custom.action.event.CustomActionEffectEvents.BEFORE_EXECUTE;
        if (typedEvent.hasListeners()) {
            cancelled |= typedEvent.invoker().beforeExecute(
                        (CustomActionEffect) (Object) this,
                        (CustomUnit) unit,
                        (UnitAction) action,
                        rustedfabricapi$targetCoordinate(targetPoint, RUSTED_FABRIC_TARGET_X_FIELDS),
                        rustedfabricapi$targetCoordinate(targetPoint, RUSTED_FABRIC_TARGET_Y_FIELDS),
                        targetPoint != null,
                        (Unit) targetUnit,
                        recursionDepth);
        }
        if (cancelled) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }

    @Inject(method = "execute(Lrustedwarfare/custom/CustomUnit;Lrustedwarfare/unit/action/UnitAction;Landroid/graphics/PointF;Lrustedwarfare/unit/Unit;I)Z", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterCustomActionEffectExecute(@Coerce Object unit, @Coerce Object action, @Coerce Object targetPoint, @Coerce Object targetUnit, int recursionDepth, CallbackInfoReturnable<Boolean> cir) {
        CustomUnitRuntimeEvents.AFTER_CUSTOM_ACTION_EFFECT_EXECUTE.invoker().afterCustomActionEffectExecute(this, unit, action, targetPoint, targetUnit, recursionDepth, Boolean.TRUE.equals(cir.getReturnValue()));
        io.github.endx.rustedfabricapi.api.event.RustedFabricEvent<io.github.endx.rustedfabricapi.api.custom.action.event.CustomActionEffectEvents.AfterExecute> typedEvent =
                io.github.endx.rustedfabricapi.api.custom.action.event.CustomActionEffectEvents.AFTER_EXECUTE;
        if (typedEvent.hasListeners()) {
            typedEvent.invoker().afterExecute(
                        (CustomActionEffect) (Object) this,
                        (CustomUnit) unit,
                        (UnitAction) action,
                        rustedfabricapi$targetCoordinate(targetPoint, RUSTED_FABRIC_TARGET_X_FIELDS),
                        rustedfabricapi$targetCoordinate(targetPoint, RUSTED_FABRIC_TARGET_Y_FIELDS),
                        targetPoint != null,
                        (Unit) targetUnit,
                        recursionDepth,
                        Boolean.TRUE.equals(cir.getReturnValue()));
        }
    }

    private static float rustedfabricapi$targetCoordinate(Object targetPoint,
                                                           String[] fieldNames) {
        return targetPoint == null ? Float.NaN
                : io.github.endx.rustedfabricapi.api.util.RustedReflection.getFloatField(
                        targetPoint, fieldNames);
    }
}
