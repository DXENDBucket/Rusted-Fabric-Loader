package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.CustomUnitRuntimeEvents;
import io.github.endx.rustedfabricapi.api.unit.attribute.CustomUnitStats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.custom.MutableStatAccessor;

@Mixin(targets = "rustedwarfare.custom.MutableStatCachedWriterElement", remap = false)
public abstract class MutableStatRuntimeNamedMixin {
    @Shadow
    public MutableStatAccessor statAccessor;

    @Inject(method = "writeToUnit(Lrustedwarfare/unit/OrderableUnit;)V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeMutableStatsApplied(@Coerce Object unit, CallbackInfo ci) {
        if (unit instanceof CustomUnit) {
            CustomUnitStats.beforeNativeWrite((CustomUnit) unit, statAccessor);
        }
    }

    @Inject(method = "writeToUnit(Lrustedwarfare/unit/OrderableUnit;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterMutableStatsApplied(@Coerce Object unit, CallbackInfo ci) {
        if (unit instanceof CustomUnit) {
            CustomUnitStats.afterNativeWrite((CustomUnit) unit, statAccessor);
        }
        CustomUnitRuntimeEvents.AFTER_MUTABLE_STATS_APPLIED.invoker().afterMutableStatsApplied(this, unit);
    }
}
