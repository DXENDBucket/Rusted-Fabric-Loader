package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.TransportEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rustedwarfare.unit.Unit;

@Mixin(
        targets = {
                "rustedwarfare.unit.OrderableUnit",
                "rustedwarfare.custom.CustomUnit",
                "rustedwarfare.unit.air.DropshipUnit",
                "rustedwarfare.unit.land.HovercraftUnit"
        },
        remap = false
)
public abstract class OrderableTransportStateNamedMixin {
    @Inject(method = "getTransportedUnitCount()I", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$modifyTransportedUnitCount(CallbackInfoReturnable<Integer> cir) {
        Integer current = cir.getReturnValue();
        int result = TransportEvents.MODIFY_TRANSPORTED_UNIT_COUNT.invoker()
                .modifyTransportedUnitCount(this, current != null ? current.intValue() : 0);
        Integer typed = io.github.endx.rustedfabricapi.api.unit.transport.event.TransportEvents
                .MODIFY_CARGO_COUNT.invoker().modify((Unit) (Object) this, result);
        cir.setReturnValue(typed);
    }

    @Inject(method = "isTransportUnloading()Z", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$modifyTransportUnloading(CallbackInfoReturnable<Boolean> cir) {
        boolean result = TransportEvents.MODIFY_TRANSPORT_UNLOADING.invoker()
                .modifyTransportUnloading(this, Boolean.TRUE.equals(cir.getReturnValue()));
        Boolean typed = io.github.endx.rustedfabricapi.api.unit.transport.event.TransportEvents
                .MODIFY_IS_UNLOADING.invoker().modify((Unit) (Object) this, result);
        cir.setReturnValue(Boolean.valueOf(Boolean.TRUE.equals(typed)));
    }
}
