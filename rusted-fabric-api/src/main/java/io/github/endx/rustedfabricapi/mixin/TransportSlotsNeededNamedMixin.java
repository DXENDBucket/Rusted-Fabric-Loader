package io.github.endx.rustedfabricapi.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rustedwarfare.unit.Unit;

@Mixin(
        targets = {
                "rustedwarfare.unit.Unit",
                "rustedwarfare.custom.CustomUnit",
                "rustedwarfare.unit.land.ExperimentalHoverTankUnit",
                "rustedwarfare.unit.land.ExperimentalTankUnit"
        },
        remap = false
)
public abstract class TransportSlotsNeededNamedMixin {
    @Inject(method = "getTransportSlotsNeeded()I", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$modifyTransportSlotsNeeded(CallbackInfoReturnable<Integer> cir) {
        Integer current = cir.getReturnValue();
        Integer typed = io.github.endx.rustedfabricapi.api.unit.transport.event.TransportEvents
                .MODIFY_SLOTS_NEEDED.invoker().modify((Unit) (Object) this,
                        current != null ? current.intValue() : 1);
        cir.setReturnValue(typed);
    }
}
