package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.TransportEvents;
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
        int result = TransportEvents.MODIFY_TRANSPORT_SLOTS_NEEDED.invoker()
                .modifyTransportSlotsNeeded(this, current != null ? current.intValue() : 1);
        Integer typed = io.github.endx.rustedfabricapi.api.unit.transport.event.TransportEvents
                .MODIFY_SLOTS_NEEDED.invoker().modify((Unit) (Object) this, result);
        cir.setReturnValue(typed);
    }
}
