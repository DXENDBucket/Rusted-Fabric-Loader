package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.TransportEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
        targets = {
                "rustedwarfare.unit.OrderableUnit",
                "rustedwarfare.custom.CustomUnit",
                "rustedwarfare.unit.air.internal.Obf_b_d",
                "rustedwarfare.unit.land.internal.Obf_e_i"
        },
        remap = false
)
public abstract class OrderableTransportStateNamedMixin {
    @Inject(method = "getTransportedUnitCount()I", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$modifyTransportedUnitCount(CallbackInfoReturnable<Integer> cir) {
        Integer current = cir.getReturnValue();
        cir.setReturnValue(TransportEvents.MODIFY_TRANSPORTED_UNIT_COUNT.invoker()
                .modifyTransportedUnitCount(this, current != null ? current.intValue() : 0));
    }

    @Inject(method = "isTransportUnloading()Z", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$modifyTransportUnloading(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(TransportEvents.MODIFY_TRANSPORT_UNLOADING.invoker()
                .modifyTransportUnloading(this, Boolean.TRUE.equals(cir.getReturnValue())));
    }
}
