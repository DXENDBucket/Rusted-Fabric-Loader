package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.CustomUnitRuntimeEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
        targets = {
                "com.corrodinggames.rts.game.units.custom.a.a.a",
                "com.corrodinggames.rts.game.units.custom.a.a.b",
                "com.corrodinggames.rts.game.units.custom.a.a.d",
                "com.corrodinggames.rts.game.units.custom.a.a.e",
                "com.corrodinggames.rts.game.units.custom.a.a.f",
                "com.corrodinggames.rts.game.units.custom.a.a.g",
                "com.corrodinggames.rts.game.units.custom.a.a.h",
                "com.corrodinggames.rts.game.units.custom.a.a.i",
                "com.corrodinggames.rts.game.units.custom.a.a.j",
                "com.corrodinggames.rts.game.units.custom.a.a.k",
                "com.corrodinggames.rts.game.units.custom.a.a.l",
                "com.corrodinggames.rts.game.units.custom.a.a.m",
                "com.corrodinggames.rts.game.units.custom.a.a.o"
        },
        remap = false
)
public abstract class CustomActionEffectRuntimeOfficialMixin {
    @Inject(method = "a(Lcom/corrodinggames/rts/game/units/custom/j;Lcom/corrodinggames/rts/game/units/a/s;Landroid/graphics/PointF;Lcom/corrodinggames/rts/game/units/am;I)Z", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeCustomActionEffectExecute(@Coerce Object unit, @Coerce Object action, @Coerce Object targetPoint, @Coerce Object targetUnit, int recursionDepth, CallbackInfoReturnable<Boolean> cir) {
        if (CustomUnitRuntimeEvents.BEFORE_CUSTOM_ACTION_EFFECT_EXECUTE.invoker().beforeCustomActionEffectExecute(this, unit, action, targetPoint, targetUnit, recursionDepth)) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }

    @Inject(method = "a(Lcom/corrodinggames/rts/game/units/custom/j;Lcom/corrodinggames/rts/game/units/a/s;Landroid/graphics/PointF;Lcom/corrodinggames/rts/game/units/am;I)Z", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterCustomActionEffectExecute(@Coerce Object unit, @Coerce Object action, @Coerce Object targetPoint, @Coerce Object targetUnit, int recursionDepth, CallbackInfoReturnable<Boolean> cir) {
        CustomUnitRuntimeEvents.AFTER_CUSTOM_ACTION_EFFECT_EXECUTE.invoker().afterCustomActionEffectExecute(this, unit, action, targetPoint, targetUnit, recursionDepth, Boolean.TRUE.equals(cir.getReturnValue()));
    }
}
