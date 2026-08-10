package io.github.endx.vulkanmod.mixin;

import io.github.endx.vulkanmod.VulkanRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rustedwarfare.ui.LibRocketSlickRenderer;

/** Invalidates Vulkan UI copies before LibRocket removes their Slick owners. */
@Mixin(targets = "com.LibRocket", remap = false)
public abstract class LibRocketTextureLifecycleNamedMixin {
    @Inject(method = "ReleaseTexture(I)V", at = @At("HEAD"), require = 1)
    private void vulkanmod$beforeReleaseTexture(int textureId, CallbackInfo callback) {
        Object renderer = this;
        if (renderer instanceof LibRocketSlickRenderer) {
            VulkanRuntime.releaseLibRocketTexture(
                    (LibRocketSlickRenderer) renderer, textureId);
        }
    }
}
