package io.github.endx.vulkanmod.mixin;

import io.github.endx.vulkanmod.VulkanRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Creates the Vulkan surface only after Slick has created and initialized its desktop window. */
@Mixin(targets = "rustedwarfare.client.SlickGame", remap = false)
public abstract class SlickGameVulkanSurfaceNamedMixin {
    @Inject(method = "init(Lorg/newdawn/slick/GameContainer;)V", at = @At("RETURN"), require = 1)
    private void vulkanmod$afterSlickWindowInitialization(CallbackInfo callback) {
        VulkanRuntime.attachToCurrentWindow();
    }
}
