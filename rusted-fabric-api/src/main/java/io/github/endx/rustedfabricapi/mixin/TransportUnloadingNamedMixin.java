package io.github.endx.rustedfabricapi.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rustedwarfare.unit.Unit;

@Mixin(
        targets = {
                "rustedwarfare.custom.CustomUnit"
        },
        remap = false
)
public abstract class TransportUnloadingNamedMixin {
    @Inject(method = "startTransportUnloading()V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeStartTransportUnloading(CallbackInfo ci) {
        boolean cancelled = io.github.endx.rustedfabricapi.api.unit.transport.event.TransportEvents
                .BEFORE_START_UNLOADING.invoker().beforeCommand((Unit) (Object) this);
        if (cancelled) {
            ci.cancel();
        }
    }

    @Inject(method = "startTransportUnloading()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterStartTransportUnloading(CallbackInfo ci) {
        io.github.endx.rustedfabricapi.api.unit.transport.event.TransportEvents
                .AFTER_START_UNLOADING.invoker().afterCommand((Unit) (Object) this);
    }

    @Inject(method = "stopTransportUnloading()V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeStopTransportUnloading(CallbackInfo ci) {
        boolean cancelled = io.github.endx.rustedfabricapi.api.unit.transport.event.TransportEvents
                .BEFORE_STOP_UNLOADING.invoker().beforeCommand((Unit) (Object) this);
        if (cancelled) {
            ci.cancel();
        }
    }

    @Inject(method = "stopTransportUnloading()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterStopTransportUnloading(CallbackInfo ci) {
        io.github.endx.rustedfabricapi.api.unit.transport.event.TransportEvents
                .AFTER_STOP_UNLOADING.invoker().afterCommand((Unit) (Object) this);
    }
}
