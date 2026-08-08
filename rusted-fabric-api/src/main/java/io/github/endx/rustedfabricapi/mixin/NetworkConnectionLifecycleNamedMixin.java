package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.networking.event.ConnectionEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rustedwarfare.network.NetworkConnection;

@Mixin(targets = "rustedwarfare.network.NetworkConnection", remap = false)
public abstract class NetworkConnectionLifecycleNamedMixin {
    @Inject(method = "sendDisconnectReasonAndClose(Ljava/lang/String;)V",
            at = @At("HEAD"), require = 1)
    private void rustedfabricapi$connectionClosing(String reason, CallbackInfo ci) {
        ConnectionEvents.CONNECTION_CLOSING.invoker()
                .onClosing((NetworkConnection) (Object) this, reason);
    }
}
