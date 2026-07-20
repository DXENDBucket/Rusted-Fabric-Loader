package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.TransportEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rustedwarfare.unit.Unit;

@Mixin(
        targets = {
                "rustedwarfare.unit.air.DropshipUnit",
                "rustedwarfare.unit.land.HovercraftUnit"
        },
        remap = false
)
public abstract class BuiltinTransportUnloadingNamedMixin {
    @Inject(method = "startTransportUnloading()V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeStartTransportUnloading(CallbackInfo ci) {
        boolean cancelled = TransportEvents.BEFORE_START_TRANSPORT_UNLOADING.invoker()
                .beforeTransportUnloadingCommand(this);
        cancelled |= io.github.endx.rustedfabricapi.api.unit.transport.event.TransportEvents
                .BEFORE_START_UNLOADING.invoker().beforeCommand((Unit) (Object) this);
        if (cancelled) {
            ci.cancel();
        }
    }

    @Inject(method = "startTransportUnloading()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterStartTransportUnloading(CallbackInfo ci) {
        TransportEvents.AFTER_START_TRANSPORT_UNLOADING.invoker()
                .afterTransportUnloadingCommand(this);
        io.github.endx.rustedfabricapi.api.unit.transport.event.TransportEvents
                .AFTER_START_UNLOADING.invoker().afterCommand((Unit) (Object) this);
    }

    @Inject(method = "stopTransportUnloading()V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeStopTransportUnloading(CallbackInfo ci) {
        boolean cancelled = TransportEvents.BEFORE_STOP_TRANSPORT_UNLOADING.invoker()
                .beforeTransportUnloadingCommand(this);
        cancelled |= io.github.endx.rustedfabricapi.api.unit.transport.event.TransportEvents
                .BEFORE_STOP_UNLOADING.invoker().beforeCommand((Unit) (Object) this);
        if (cancelled) {
            ci.cancel();
        }
    }

    @Inject(method = "stopTransportUnloading()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterStopTransportUnloading(CallbackInfo ci) {
        TransportEvents.AFTER_STOP_TRANSPORT_UNLOADING.invoker()
                .afterTransportUnloadingCommand(this);
        io.github.endx.rustedfabricapi.api.unit.transport.event.TransportEvents
                .AFTER_STOP_UNLOADING.invoker().afterCommand((Unit) (Object) this);
    }
}
