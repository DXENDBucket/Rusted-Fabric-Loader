package io.github.endx.rustedfabricapi.mixin;

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
        Integer typed = io.github.endx.rustedfabricapi.api.unit.transport.event.TransportEvents
                .MODIFY_CARGO_COUNT.invoker().modify((Unit) (Object) this,
                        current != null ? current.intValue() : 0);
        cir.setReturnValue(typed);
    }

    @Inject(method = "isTransportUnloading()Z", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$modifyTransportUnloading(CallbackInfoReturnable<Boolean> cir) {
        Boolean typed = io.github.endx.rustedfabricapi.api.unit.transport.event.TransportEvents
                .MODIFY_IS_UNLOADING.invoker().modify((Unit) (Object) this,
                        Boolean.TRUE.equals(cir.getReturnValue()));
        cir.setReturnValue(Boolean.valueOf(Boolean.TRUE.equals(typed)));
    }
}
