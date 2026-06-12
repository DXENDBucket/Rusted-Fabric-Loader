package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.TransportEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
        targets = {
                "rustedwarfare.custom.CustomUnit",
                "rustedwarfare.unit.air.internal.Obf_b_d",
                "rustedwarfare.unit.land.internal.Obf_e_i"
        },
        remap = false
)
public abstract class TransportCargoNamedMixin {
    @Inject(method = "addUnitToTransport(Lrustedwarfare/unit/Unit;)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeAddUnitToTransport(@Coerce Object transportedUnit, CallbackInfo ci) {
        if (TransportEvents.BEFORE_ADD_UNIT_TO_TRANSPORT.invoker()
                .beforeAddUnitToTransport(this, transportedUnit)) {
            ci.cancel();
        }
    }

    @Inject(method = "addUnitToTransport(Lrustedwarfare/unit/Unit;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterAddUnitToTransport(@Coerce Object transportedUnit, CallbackInfo ci) {
        TransportEvents.AFTER_ADD_UNIT_TO_TRANSPORT.invoker()
                .afterAddUnitToTransport(this, transportedUnit);
    }

    @Inject(method = "removeUnitFromTransport(Lrustedwarfare/unit/Unit;)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeRemoveUnitFromTransport(@Coerce Object transportedUnit, CallbackInfo ci) {
        if (TransportEvents.BEFORE_REMOVE_UNIT_FROM_TRANSPORT.invoker()
                .beforeRemoveUnitFromTransport(this, transportedUnit)) {
            ci.cancel();
        }
    }

    @Inject(method = "removeUnitFromTransport(Lrustedwarfare/unit/Unit;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterRemoveUnitFromTransport(@Coerce Object transportedUnit, CallbackInfo ci) {
        TransportEvents.AFTER_REMOVE_UNIT_FROM_TRANSPORT.invoker()
                .afterRemoveUnitFromTransport(this, transportedUnit);
    }
}
