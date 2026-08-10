package io.github.endx.vulkanmod.mixin;

import io.github.endx.vulkanmod.VulkanRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Drops cached Vulkan copies before Slick replaces or releases their source pixels. */
@Mixin(targets = "rustedwarfare.client.render.SlickBitmapOrTexture", remap = false)
public abstract class SlickBitmapVulkanTextureLifecycleNamedMixin {
    @Inject(method = "reloadFromImageData()V", at = @At("HEAD"), require = 1)
    private void vulkanmod$beforeReloadFromImageData(CallbackInfo callback) {
        VulkanRuntime.invalidateCachedImage(this);
    }

    @Inject(method = "releaseBitmap()V", at = @At("HEAD"), require = 1)
    private void vulkanmod$beforeReleaseBitmap(CallbackInfo callback) {
        VulkanRuntime.invalidateCachedImage(this);
    }

    @Inject(method = "reloadImage()V", at = @At("HEAD"), require = 1)
    private void vulkanmod$beforeReloadImage(CallbackInfo callback) {
        VulkanRuntime.invalidateCachedImage(this);
    }
}
