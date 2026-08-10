package io.github.endx.vulkanmod.mixin;

import com.LibRocket$CompiledGeometry;
import io.github.endx.vulkanmod.VulkanRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rustedwarfare.ui.LibRocketSlickRenderer;

/** Replaces LibRocket's immediate-mode triangles once their texture is Vulkan-readable. */
@Mixin(targets = "rustedwarfare.ui.LibRocketSlickRenderer", remap = false)
public abstract class LibRocketSlickRendererVulkanCaptureNamedMixin {
    @Inject(method = "GenerateTexture(I[B)Z", at = @At("HEAD"), require = 1)
    private void vulkanmod$captureGeneratedTexture(
            int textureId, byte[] rgba, CallbackInfoReturnable<Boolean> callback) {
        VulkanRuntime.registerGeneratedLibRocketTexture(
                (LibRocketSlickRenderer) (Object) this, textureId, rgba);
    }

    @Inject(method = "RenderGeometryPossiblyCompiled([F[F[I[IIFFLcom/LibRocket$CompiledGeometry;)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void vulkanmod$renderGeometry(float[] positions, float[] uvs,
                                          int[] colors, int[] indices, int textureId,
                                          float translationX, float translationY,
                                          LibRocket$CompiledGeometry compiled,
                                          CallbackInfo callback) {
        LibRocketSlickRenderer renderer = (LibRocketSlickRenderer) (Object) this;
        if (VulkanRuntime.captureLibRocketGeometry(renderer, positions, uvs, colors, indices,
                textureId, translationX, translationY)) callback.cancel();
    }
}
