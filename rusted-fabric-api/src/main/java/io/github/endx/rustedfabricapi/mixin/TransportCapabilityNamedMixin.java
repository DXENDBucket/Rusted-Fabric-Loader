package io.github.endx.rustedfabricapi.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rustedwarfare.unit.Unit;

@Mixin(
        targets = {
                "rustedwarfare.unit.Unit",
                "rustedwarfare.custom.CustomUnit",
                "rustedwarfare.unit.air.DropshipUnit",
                "rustedwarfare.unit.land.HovercraftUnit"
        },
        remap = false
)
public abstract class TransportCapabilityNamedMixin {
    @Inject(method = "canTransportUnit(Lrustedwarfare/unit/Unit;Z)Z", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$modifyCanTransportUnit(@Coerce Object candidate, boolean allowPartial,
                                                        CallbackInfoReturnable<Boolean> cir) {
        Boolean typed = io.github.endx.rustedfabricapi.api.unit.transport.event.TransportEvents
                .MODIFY_CAN_LOAD.invoker().modify((Unit) (Object) this, (Unit) candidate,
                        allowPartial, Boolean.TRUE.equals(cir.getReturnValue()));
        cir.setReturnValue(Boolean.valueOf(Boolean.TRUE.equals(typed)));
    }

    @Inject(method = "tryAddUnitToTransport(Lrustedwarfare/unit/Unit;Z)Z", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeTryAddUnitToTransport(@Coerce Object candidate, boolean allowPartial,
                                                             CallbackInfoReturnable<Boolean> cir) {
        boolean cancelled = io.github.endx.rustedfabricapi.api.unit.transport.event.TransportEvents
                .BEFORE_TRY_LOAD.invoker().beforeTryLoad(
                        (Unit) (Object) this, (Unit) candidate, allowPartial);
        if (cancelled) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }

    @Inject(method = "tryAddUnitToTransport(Lrustedwarfare/unit/Unit;Z)Z", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterTryAddUnitToTransport(@Coerce Object candidate, boolean allowPartial,
                                                            CallbackInfoReturnable<Boolean> cir) {
        io.github.endx.rustedfabricapi.api.unit.transport.event.TransportEvents
                .AFTER_TRY_LOAD.invoker().afterTryLoad((Unit) (Object) this, (Unit) candidate,
                        allowPartial, Boolean.TRUE.equals(cir.getReturnValue()));
    }

    @Inject(method = "hasTransportCapacity()Z", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$modifyHasTransportCapacity(CallbackInfoReturnable<Boolean> cir) {
        Boolean typed = io.github.endx.rustedfabricapi.api.unit.transport.event.TransportEvents
                .MODIFY_HAS_CAPACITY.invoker().modify((Unit) (Object) this,
                        Boolean.TRUE.equals(cir.getReturnValue()));
        cir.setReturnValue(Boolean.valueOf(Boolean.TRUE.equals(typed)));
    }

    @Inject(method = "getTransportBarUsedSlots()I", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$modifyTransportBarUsedSlots(CallbackInfoReturnable<Integer> cir) {
        Integer current = cir.getReturnValue();
        Integer typed = io.github.endx.rustedfabricapi.api.unit.transport.event.TransportEvents
                .MODIFY_USED_SLOTS.invoker().modify((Unit) (Object) this,
                        current != null ? current.intValue() : -1);
        cir.setReturnValue(typed);
    }

    @Inject(method = "getTransportBarMaxSlots()I", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$modifyTransportBarMaxSlots(CallbackInfoReturnable<Integer> cir) {
        Integer current = cir.getReturnValue();
        Integer typed = io.github.endx.rustedfabricapi.api.unit.transport.event.TransportEvents
                .MODIFY_MAX_SLOTS.invoker().modify((Unit) (Object) this,
                        current != null ? current.intValue() : -1);
        cir.setReturnValue(typed);
    }
}
