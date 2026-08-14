package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.internal.client.AndroidShaderCompatibility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Normalizes the small subset of desktop GLSL that known GL4ES drivers miscompile. */
@Mixin(targets = "rustedwarfare.render.ShaderProgram", remap = false)
public abstract class AndroidShaderSourceNamedMixin {
    @Shadow public String name;
    @Shadow public String vertexSource;
    @Shadow public String fragmentSource;

    @Inject(method = "reloadSources()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$repairAndroidShaderSources(CallbackInfo ci) {
        vertexSource = AndroidShaderCompatibility.repairShaderSource(name, vertexSource);
        fragmentSource = AndroidShaderCompatibility.repairShaderSource(name, fragmentSource);
    }
}
