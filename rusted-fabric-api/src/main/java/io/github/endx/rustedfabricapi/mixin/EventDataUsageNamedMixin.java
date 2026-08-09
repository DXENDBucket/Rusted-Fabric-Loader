package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.impl.custom.DamageEventDataRuntime;
import io.github.endx.rustedfabricapi.impl.custom.NativeEventDataRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "rustedwarfare.custom.logic.VariableScope$ReadUnitMemoryLogicBoolean", remap = false)
public abstract class EventDataUsageNamedMixin {
    @Inject(method = "name(Ljava/lang/String;)V", at = @At("TAIL"), require = 1)
    private void rustedfabricapi$noticeEnhancedEventDataName(String name, CallbackInfo ci) {
        if (getClass().getName().endsWith("VariableScope$ReadEventMemoryLogicBoolean")) {
            DamageEventDataRuntime.onEventDataNameParsed(name);
            NativeEventDataRuntime.onEventDataNameParsed(name);
        }
    }
}
