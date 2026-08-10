package io.github.endx.vulkanmod.mixin;

import io.github.endx.vulkanmod.VulkanRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Gives the opt-in frame test the window only after the legacy frame has been presented. */
@Mixin(targets = "rustedwarfare.client.RustedWarfareAppGameContainer", remap = false)
public abstract class RustedWarfareGameLoopVulkanPresentNamedMixin {
    private boolean vulkanmod$hiddenRenderConfigured;

    @Inject(method = "gameLoop()V", at = @At("HEAD"), require = 1)
    private void vulkanmod$beforeOpenGlFrame(CallbackInfo callback) {
        if (!vulkanmod$hiddenRenderConfigured
                && Boolean.getBoolean("rusted.fabric.vulkan.renderWhenHidden")) {
            vulkanmod$hiddenRenderConfigured = true;
            try {
                getClass().getMethod("setUpdateOnlyWhenVisible", boolean.class)
                        .invoke(this, false);
                getClass().getMethod("setAlwaysRender", boolean.class).invoke(this, true);
            } catch (ReflectiveOperationException failure) {
                throw new IllegalStateException("Could not enable hidden Vulkan test rendering",
                        failure);
            }
        }
        VulkanRuntime.beforeOpenGlFrame();
    }

    @Inject(method = "gameLoop()V", at = @At(value = "INVOKE",
            target = "Lorg/lwjgl/opengl/Display;update(Z)V", shift = At.Shift.AFTER), require = 1)
    private void vulkanmod$afterOpenGlPresent(CallbackInfo callback) {
        VulkanRuntime.afterOpenGlPresent();
    }

    @Inject(method = "destroy()V", at = @At("HEAD"), require = 1)
    private void vulkanmod$beforeWindowDestroy(CallbackInfo callback) {
        VulkanRuntime.shutdown();
    }
}
