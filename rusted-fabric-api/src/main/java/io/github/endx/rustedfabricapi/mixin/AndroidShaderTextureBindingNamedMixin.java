package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.internal.client.AndroidShaderCompatibility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rustedwarfare.render.ShaderProgram;

/** Restores auxiliary custom-shader samplers after GL4ES fixed-pipeline rendering. */
@Mixin(targets = "rustedwarfare.client.render.SlickGraphicsBackend", remap = false)
public abstract class AndroidShaderTextureBindingNamedMixin {
    @Inject(method = "updateShaderUniforms(Lrustedwarfare/render/ShaderProgram;)V",
            at = @At("HEAD"), require = 1)
    private void rustedfabricapi$restoreAndroidShaderTextureBindings(
            ShaderProgram shader, CallbackInfo ci) {
        AndroidShaderCompatibility.restoreTextureParametersOnActivation(shader);
    }
}
