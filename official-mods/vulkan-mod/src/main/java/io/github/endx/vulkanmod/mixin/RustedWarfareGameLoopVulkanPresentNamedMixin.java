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

    @Inject(method = "gameLoop()V", at = @At("HEAD"), cancellable = true, require = 1)
    private void vulkanmod$beforeOpenGlFrame(CallbackInfo callback) {
        if (VulkanRuntime.isNativeRendererSelected()) {
            if (!VulkanRuntime.runNativeBootstrapFrame()) {
                setRunning(false);
            }
            callback.cancel();
            return;
        }
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

    // Keep the parent HWND responsive before entering acquire/submit/present. In particular this
    // lets LWJGL dispatch focus and restore messages before the Vulkan child surface does any WSI
    // work after an Alt-Tab.
    @Inject(method = "gameLoop()V", at = @At("TAIL"), require = 1)
    private void vulkanmod$afterOpenGlPresent(CallbackInfo callback) {
        if (VulkanRuntime.isNativeRendererSelected()) return;
        VulkanRuntime.afterOpenGlPresent();
    }

    @Inject(method = "destroy()V", at = @At("HEAD"), require = 1)
    private void vulkanmod$beforeWindowDestroy(CallbackInfo callback) {
        VulkanRuntime.shutdown();
    }

    private void setRunning(boolean value) {
        Class<?> current = getClass();
        while (current != null) {
            try {
                java.lang.reflect.Field field = current.getDeclaredField("running");
                field.setAccessible(true);
                field.setBoolean(this, value);
                return;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            } catch (IllegalAccessException failure) {
                throw new IllegalStateException("Could not stop native game loop", failure);
            }
        }
        throw new IllegalStateException("Could not find Slick running state");
    }
}
