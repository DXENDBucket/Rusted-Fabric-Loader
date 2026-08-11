package io.github.endx.vulkanmod.mixin;

import io.github.endx.vulkanmod.VulkanRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import rustedwarfare.core.RustedWarfareGameEngine;
import rustedwarfare.render.CanvasDrawTarget;

/** Supplies the native CanvasDrawTarget that SlickGame.render used to install per GL frame. */
@Mixin(RustedWarfareGameEngine.class)
public abstract class RustedWarfareNativeCanvasNamedMixin {
    @ModifyVariable(method = "a(Lrustedwarfare/render/CanvasDrawTarget;F)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 1)
    private CanvasDrawTarget vulkanmod$installNativeCanvas(CanvasDrawTarget canvas) {
        if (canvas != null || !VulkanRuntime.isNativeRendererSelected()
                || VulkanRuntime.nativeGraphicsEngine() == null) {
            return canvas;
        }
        return VulkanRuntime.nativeGraphicsEngine().d();
    }
}
