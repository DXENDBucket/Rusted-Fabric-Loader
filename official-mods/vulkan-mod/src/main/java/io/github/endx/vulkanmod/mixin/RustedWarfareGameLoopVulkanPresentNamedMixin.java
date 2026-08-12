package io.github.endx.vulkanmod.mixin;

import io.github.endx.vulkanmod.VulkanRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Gives the opt-in frame test the window only after the legacy frame has been presented. */
@Mixin(targets = "rustedwarfare.client.RustedWarfareAppGameContainer", remap = false)
public abstract class RustedWarfareGameLoopVulkanPresentNamedMixin {
    @Inject(method = "gameLoop()V", at = @At("HEAD"), cancellable = true, require = 1)
    private void vulkanmod$beforeOpenGlFrame(CallbackInfo callback) {
        if (VulkanRuntime.isNativeRendererSelected()) {
            if (!VulkanRuntime.runNativeBootstrapFrame()) {
                setRunning(false);
            }
            callback.cancel();
            return;
        }
    }

    /** Drives the isolated compatibility-surface frame test after an ordinary OpenGL present. */
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
