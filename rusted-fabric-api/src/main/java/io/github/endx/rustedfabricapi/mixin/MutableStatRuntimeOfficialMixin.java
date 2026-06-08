package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.CustomUnitRuntimeEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.corrodinggames.rts.game.units.custom.av", remap = false)
public abstract class MutableStatRuntimeOfficialMixin {
    @Inject(method = "writeToUnit(Lcom/corrodinggames/rts/game/units/y;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterMutableStatsApplied(@Coerce Object unit, CallbackInfo ci) {
        CustomUnitRuntimeEvents.AFTER_MUTABLE_STATS_APPLIED.invoker().afterMutableStatsApplied(this, unit);
    }
}
