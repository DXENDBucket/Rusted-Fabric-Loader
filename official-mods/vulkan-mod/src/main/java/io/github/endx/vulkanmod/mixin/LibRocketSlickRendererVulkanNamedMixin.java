package io.github.endx.vulkanmod.mixin;

import com.LibRocket$CompiledGeometry;
import io.github.endx.vulkanmod.VulkanRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rustedwarfare.ui.LibRocketSlickRenderer;

/** Routes LibRocket texture, geometry, and scissor operations into the native renderer. */
@Mixin(targets = "rustedwarfare.ui.LibRocketSlickRenderer", remap = false)
public abstract class LibRocketSlickRendererVulkanNamedMixin {
    @Inject(method = "GenerateTexture(I[B)Z", at = @At("HEAD"),
            cancellable = true, require = 1)
    private void vulkanmod$generateTexture(
            int textureId, byte[] rgba, CallbackInfoReturnable<Boolean> callback) {
        VulkanRuntime.registerGeneratedLibRocketTexture(
                (LibRocketSlickRenderer) (Object) this, textureId, rgba);
        if (VulkanRuntime.isNativeRendererSelected()) callback.setReturnValue(true);
    }

    @Inject(method = "RenderGeometryPossiblyCompiled([F[F[I[IIFFLcom/LibRocket$CompiledGeometry;)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void vulkanmod$renderGeometry(float[] positions, float[] uvs,
                                          int[] colors, int[] indices, int textureId,
                                          float translationX, float translationY,
                                          LibRocket$CompiledGeometry compiled,
                                          CallbackInfo callback) {
        if (!VulkanRuntime.isNativeRendererSelected()) return;
        VulkanRuntime.recordNativeLibRocketGeometry(
                (LibRocketSlickRenderer) (Object) this, positions, uvs,
                colors, indices, textureId, translationX, translationY);
        callback.cancel();
    }

    @Inject(method = "EnableScissorRegion(Z)V", at = @At("HEAD"),
            cancellable = true, require = 1)
    private void vulkanmod$enableNativeScissor(boolean enabled, CallbackInfo callback) {
        if (!VulkanRuntime.isNativeRendererSelected()) return;
        ((LibRocketUiEngineStateAccessor) (Object) this)
                .vulkanmod$setScissorEnabled(enabled);
        callback.cancel();
    }
}
