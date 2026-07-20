package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.client.input.KeyBindings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rustedwarfare.input.InputBindingRegistry;

@Mixin(value = InputBindingRegistry.class, remap = false)
public abstract class InputBindingRegistryNamedMixin {
    @Inject(method = "<init>()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$installModBindings(CallbackInfo callback) {
        KeyBindings.onRegistryCreated((InputBindingRegistry) (Object) this);
    }
}
