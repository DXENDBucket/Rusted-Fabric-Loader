package io.github.endx.vulkanmod.mixin;

import io.github.endx.vulkanmod.VulkanRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Gives the opt-in frame test the window only after the legacy frame has been presented. */
@Mixin(targets = "rustedwarfare.client.RustedWarfareAppGameContainer", remap = false)
public abstract class RustedWarfareGameLoopVulkanPresentNamedMixin {
    @Inject(method = "gameLoop()V", at = @At(value = "INVOKE",
            target = "Lorg/lwjgl/opengl/Display;update(Z)V", shift = At.Shift.AFTER), require = 1)
    private void vulkanmod$afterOpenGlPresent(CallbackInfo callback) {
        VulkanRuntime.afterOpenGlPresent();
    }
}
