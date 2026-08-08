package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.lobby.GameSetupSnapshot;
import io.github.endx.rustedfabricapi.api.lobby.LobbyGameSetup;
import io.github.endx.rustedfabricapi.api.lobby.LobbyGameSetupEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rustedwarfare.network.GameSetup;
import rustedwarfare.network.NetworkEngine;

@Mixin(targets = "rustedwarfare.network.NetworkEngine", remap = false)
public abstract class LobbyGameSetupNamedMixin {
    @Inject(method = "applyGameSetup(Lrustedwarfare/network/GameSetup;)V",
            at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeApplyGameSetup(GameSetup setup, CallbackInfo ci) {
        NetworkEngine network = (NetworkEngine) (Object) this;
        GameSetupSnapshot snapshot = LobbyGameSetup.snapshot(network, setup);
        LobbyGameSetupEvents.BEFORE_NATIVE_APPLY.invoker().onApply(network, snapshot);
    }

    @Inject(method = "applyGameSetup(Lrustedwarfare/network/GameSetup;)V",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterApplyGameSetup(GameSetup setup, CallbackInfo ci) {
        NetworkEngine network = (NetworkEngine) (Object) this;
        GameSetupSnapshot snapshot = LobbyGameSetup.snapshot(network, setup);
        LobbyGameSetupEvents.AFTER_NATIVE_APPLY.invoker().onApply(network, snapshot);
    }
}
