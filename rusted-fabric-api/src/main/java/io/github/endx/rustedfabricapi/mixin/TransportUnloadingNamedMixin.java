package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.TransportEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
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
public abstract class TransportUnloadingNamedMixin {
    @Inject(method = "startTransportUnloading()V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeStartTransportUnloading(CallbackInfo ci) {
        if (TransportEvents.BEFORE_START_TRANSPORT_UNLOADING.invoker()
                .beforeTransportUnloadingCommand(this)) {
            ci.cancel();
        }
    }

    @Inject(method = "startTransportUnloading()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterStartTransportUnloading(CallbackInfo ci) {
        TransportEvents.AFTER_START_TRANSPORT_UNLOADING.invoker()
                .afterTransportUnloadingCommand(this);
    }

    @Inject(method = "stopTransportUnloading()V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeStopTransportUnloading(CallbackInfo ci) {
        if (TransportEvents.BEFORE_STOP_TRANSPORT_UNLOADING.invoker()
                .beforeTransportUnloadingCommand(this)) {
            ci.cancel();
        }
    }

    @Inject(method = "stopTransportUnloading()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterStopTransportUnloading(CallbackInfo ci) {
        TransportEvents.AFTER_STOP_TRANSPORT_UNLOADING.invoker()
                .afterTransportUnloadingCommand(this);
    }
}
