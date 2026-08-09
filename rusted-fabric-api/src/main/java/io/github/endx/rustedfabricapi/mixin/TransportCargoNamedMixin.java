package io.github.endx.rustedfabricapi.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rustedwarfare.unit.Unit;

@Mixin(
        targets = {
                "rustedwarfare.custom.CustomUnit",
                "rustedwarfare.unit.air.DropshipUnit",
                "rustedwarfare.unit.land.HovercraftUnit"
        },
        remap = false
)
public abstract class TransportCargoNamedMixin {
    @Inject(method = "addUnitToTransport(Lrustedwarfare/unit/Unit;)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeAddUnitToTransport(@Coerce Object transportedUnit, CallbackInfo ci) {
        boolean cancelled = io.github.endx.rustedfabricapi.api.unit.transport.event.TransportEvents
                .BEFORE_LOAD.invoker().beforeCargoChange(
                        (Unit) (Object) this, (Unit) transportedUnit);
        if (cancelled) {
            ci.cancel();
        }
    }

    @Inject(method = "addUnitToTransport(Lrustedwarfare/unit/Unit;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterAddUnitToTransport(@Coerce Object transportedUnit, CallbackInfo ci) {
        io.github.endx.rustedfabricapi.api.unit.transport.event.TransportEvents
                .AFTER_LOAD.invoker().onCargoChange((Unit) (Object) this, (Unit) transportedUnit);
    }

    @Inject(method = "removeUnitFromTransport(Lrustedwarfare/unit/Unit;)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeRemoveUnitFromTransport(@Coerce Object transportedUnit, CallbackInfo ci) {
        boolean cancelled = io.github.endx.rustedfabricapi.api.unit.transport.event.TransportEvents
                .BEFORE_REMOVE.invoker().beforeCargoChange(
                        (Unit) (Object) this, (Unit) transportedUnit);
        if (cancelled) {
            ci.cancel();
        }
    }

    @Inject(method = "removeUnitFromTransport(Lrustedwarfare/unit/Unit;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterRemoveUnitFromTransport(@Coerce Object transportedUnit, CallbackInfo ci) {
        io.github.endx.rustedfabricapi.api.unit.transport.event.TransportEvents
                .AFTER_REMOVE.invoker().onCargoChange((Unit) (Object) this, (Unit) transportedUnit);
    }
}
