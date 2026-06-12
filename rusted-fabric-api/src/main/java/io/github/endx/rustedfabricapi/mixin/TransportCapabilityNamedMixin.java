package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.TransportEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
        targets = {
                "rustedwarfare.unit.Unit",
                "rustedwarfare.custom.CustomUnit",
                "rustedwarfare.unit.air.internal.Obf_b_d",
                "rustedwarfare.unit.land.internal.Obf_e_i"
        },
        remap = false
)
public abstract class TransportCapabilityNamedMixin {
    @Inject(method = "canTransportUnit(Lrustedwarfare/unit/Unit;Z)Z", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$modifyCanTransportUnit(@Coerce Object candidate, boolean allowPartial,
                                                        CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(TransportEvents.MODIFY_CAN_TRANSPORT_UNIT.invoker()
                .modifyCanTransportUnit(this, candidate, allowPartial, Boolean.TRUE.equals(cir.getReturnValue())));
    }

    @Inject(method = "tryAddUnitToTransport(Lrustedwarfare/unit/Unit;Z)Z", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeTryAddUnitToTransport(@Coerce Object candidate, boolean allowPartial,
                                                             CallbackInfoReturnable<Boolean> cir) {
        if (TransportEvents.BEFORE_TRY_ADD_UNIT_TO_TRANSPORT.invoker()
                .beforeTryAddUnitToTransport(this, candidate, allowPartial)) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }

    @Inject(method = "tryAddUnitToTransport(Lrustedwarfare/unit/Unit;Z)Z", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterTryAddUnitToTransport(@Coerce Object candidate, boolean allowPartial,
                                                            CallbackInfoReturnable<Boolean> cir) {
        TransportEvents.AFTER_TRY_ADD_UNIT_TO_TRANSPORT.invoker()
                .afterTryAddUnitToTransport(this, candidate, allowPartial, Boolean.TRUE.equals(cir.getReturnValue()));
    }

    @Inject(method = "hasTransportCapacity()Z", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$modifyHasTransportCapacity(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(TransportEvents.MODIFY_HAS_TRANSPORT_CAPACITY.invoker()
                .modifyHasTransportCapacity(this, Boolean.TRUE.equals(cir.getReturnValue())));
    }

    @Inject(method = "getTransportBarUsedSlots()I", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$modifyTransportBarUsedSlots(CallbackInfoReturnable<Integer> cir) {
        Integer current = cir.getReturnValue();
        cir.setReturnValue(TransportEvents.MODIFY_TRANSPORT_BAR_USED_SLOTS.invoker()
                .modifyTransportBarSlots(this, current != null ? current.intValue() : -1));
    }

    @Inject(method = "getTransportBarMaxSlots()I", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$modifyTransportBarMaxSlots(CallbackInfoReturnable<Integer> cir) {
        Integer current = cir.getReturnValue();
        cir.setReturnValue(TransportEvents.MODIFY_TRANSPORT_BAR_MAX_SLOTS.invoker()
                .modifyTransportBarSlots(this, current != null ? current.intValue() : -1));
    }
}
