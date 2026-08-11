package io.github.endx.vulkanmod.mixin;

import io.github.endx.vulkanmod.VulkanRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rustedwarfare.io.VirtualFileSystemBackend;
import rustedwarfare.io.VirtualFileSystemRegistry;
import rustedwarfare.ui.SlickLibRocketTextureHolder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/** Decodes LibRocket images on the CPU instead of constructing an OpenGL-backed Slick Image. */
@Mixin(SlickLibRocketTextureHolder.class)
public abstract class SlickLibRocketTextureHolderNativeNamedMixin {
    @Inject(method = "loadTexture()Z", at = @At("HEAD"), cancellable = true, require = 1)
    private void vulkanmod$loadNativeTexture(CallbackInfoReturnable<Boolean> callback) {
        if (!VulkanRuntime.isNativeRendererSelected()) return;
        SlickLibRocketTextureHolder holder = (SlickLibRocketTextureHolder) (Object) this;
        try (InputStream input = vulkanmod$open(holder.path)) {
            if (input == null) {
                callback.setReturnValue(false);
                return;
            }
            BufferedImage image = ImageIO.read(input);
            if (image == null) {
                callback.setReturnValue(false);
                return;
            }
            int width = image.getWidth();
            int height = image.getHeight();
            int[] argb = image.getRGB(0, 0, width, height, null, 0, width);
            byte[] rgba = new byte[Math.multiplyExact(Math.multiplyExact(width, height), 4)];
            for (int index = 0; index < argb.length; index++) {
                int color = argb[index];
                int offset = index * 4;
                rgba[offset] = (byte) (color >>> 16);
                rgba[offset + 1] = (byte) (color >>> 8);
                rgba[offset + 2] = (byte) color;
                rgba[offset + 3] = (byte) (color >>> 24);
            }
            holder.width = width;
            holder.height = height;
            VulkanRuntime.registerNativeLibRocketTexture(holder, width, height, rgba);
            callback.setReturnValue(true);
        } catch (IOException | RuntimeException failure) {
            System.err.println("[Vulkan Mod] Could not decode LibRocket texture "
                    + holder.path + ": " + failure.getMessage());
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "remove()V", at = @At("HEAD"), cancellable = true, require = 1)
    private void vulkanmod$removeNativeTexture(CallbackInfo callback) {
        if (VulkanRuntime.isNativeRendererSelected()) {
            VulkanRuntime.releaseNativeLibRocketTexture(this);
            callback.cancel();
        }
    }

    private static InputStream vulkanmod$open(String path) throws IOException {
        VirtualFileSystemBackend backend = VirtualFileSystemRegistry.getBackendForPath(path);
        return backend == null ? new FileInputStream(path) : backend.openInputStream(path, true);
    }
}
