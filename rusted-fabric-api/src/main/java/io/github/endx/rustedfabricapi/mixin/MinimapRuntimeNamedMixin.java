package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.client.minimap.MinimapEvents;
import io.github.endx.rustedfabricapi.api.client.minimap.MinimapMarkerKind;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rustedwarfare.ui.Minimap;
import rustedwarfare.ui.MinimapMarkerType;
import rustedwarfare.unit.Unit;

@Mixin(targets = "rustedwarfare.ui.Minimap", remap = false)
public abstract class MinimapRuntimeNamedMixin {
    @Inject(method = "addDrawMarker(IILrustedwarfare/ui/MinimapMarkerType;)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeMarker(int x, int y, MinimapMarkerType type, CallbackInfo ci) {
        MinimapMarkerKind kind = MinimapMarkerKind.fromNative(type);
        if (MinimapEvents.BEFORE_MARKER.invoker().beforeMarker(rustedfabricapi$self(), x, y, kind)) ci.cancel();
    }

    @Inject(method = "addDrawMarker(IILrustedwarfare/ui/MinimapMarkerType;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterMarker(int x, int y, MinimapMarkerType type, CallbackInfo ci) {
        MinimapEvents.AFTER_MARKER.invoker().afterMarker(rustedfabricapi$self(), x, y, MinimapMarkerKind.fromNative(type));
    }

    @Inject(method = "addUnitScanPulse(IIFLrustedwarfare/unit/Unit;)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforePulse(int x, int y, float amount, Unit source, CallbackInfo ci) {
        if (MinimapEvents.BEFORE_SCAN_PULSE.invoker().beforeScanPulse(rustedfabricapi$self(), x, y, amount, source)) ci.cancel();
    }

    @Inject(method = "addUnitScanPulse(IIFLrustedwarfare/unit/Unit;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterPulse(int x, int y, float amount, Unit source, CallbackInfo ci) {
        MinimapEvents.AFTER_SCAN_PULSE.invoker().afterScanPulse(rustedfabricapi$self(), x, y, amount, source);
    }

    @Unique
    private Minimap rustedfabricapi$self() {
        return (Minimap) (Object) this;
    }
}
