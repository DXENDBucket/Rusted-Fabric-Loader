package io.github.endx.vulkanmod.mixin;

import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import io.github.endx.vulkanmod.VulkanRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rustedwarfare.client.render.GameImage;
import rustedwarfare.client.render.SlickGraphicsBackend;

/** Captures a conservative subset of top-level Slick drawing during takeover_test. */
@Mixin(targets = "rustedwarfare.client.render.SlickGraphicsBackend", remap = false)
public abstract class SlickGraphicsBackendVulkanCaptureNamedMixin {
    private SlickGraphicsBackend vulkanmod$self() {
        return (SlickGraphicsBackend) (Object) this;
    }

    @Inject(method = "drawColor(I)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void vulkanmod$drawColor(int color, CallbackInfo callback) {
        if (VulkanRuntime.captureClear(vulkanmod$self(), color)) callback.cancel();
    }

    @Inject(method = "drawRect(Landroid/graphics/Rect;Landroid/graphics/Paint;)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void vulkanmod$drawRect(Rect rect, Paint paint, CallbackInfo callback) {
        if (VulkanRuntime.captureRect(vulkanmod$self(), rect, paint)) callback.cancel();
    }

    @Inject(method = "drawRectDirect(Landroid/graphics/Rect;Landroid/graphics/Paint;)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void vulkanmod$drawRectDirect(Rect rect, Paint paint, CallbackInfo callback) {
        if (VulkanRuntime.captureRect(vulkanmod$self(), rect, paint)) callback.cancel();
    }

    @Inject(method = "drawRect(Landroid/graphics/RectF;Landroid/graphics/Paint;)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void vulkanmod$drawRectF(RectF rect, Paint paint, CallbackInfo callback) {
        if (VulkanRuntime.captureRect(vulkanmod$self(), rect, paint)) callback.cancel();
    }

    @Inject(method = "drawImageRaw(Lrustedwarfare/client/render/GameImage;FFLandroid/graphics/Paint;)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void vulkanmod$drawImageRaw(GameImage image, float x, float y, Paint paint,
                                        CallbackInfo callback) {
        if (VulkanRuntime.captureImageRaw(vulkanmod$self(), image, x, y, paint)) {
            callback.cancel();
        }
    }

    @Inject(method = "drawImageCentered(Lrustedwarfare/client/render/GameImage;FFLandroid/graphics/Paint;)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void vulkanmod$drawImageCentered(GameImage image, float x, float y, Paint paint,
                                             CallbackInfo callback) {
        if (VulkanRuntime.captureImageCentered(vulkanmod$self(), image, x, y, paint)) {
            callback.cancel();
        }
    }

    @Inject(method = "drawImage(Lrustedwarfare/client/render/GameImage;Landroid/graphics/Rect;Landroid/graphics/RectF;Landroid/graphics/Paint;)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void vulkanmod$drawImageRectF(GameImage image, Rect src, RectF dst, Paint paint,
                                          CallbackInfo callback) {
        if (VulkanRuntime.captureImage(vulkanmod$self(), image, src, dst, paint)) {
            callback.cancel();
        }
    }

    @Inject(method = "drawImage(Lrustedwarfare/client/render/GameImage;Landroid/graphics/Rect;Landroid/graphics/Rect;Landroid/graphics/Paint;)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void vulkanmod$drawImageRect(GameImage image, Rect src, Rect dst, Paint paint,
                                         CallbackInfo callback) {
        RectF destination = dst == null ? null : new RectF(dst);
        if (VulkanRuntime.captureImage(vulkanmod$self(), image, src, destination, paint)) {
            callback.cancel();
        }
    }

    @Inject(method = "drawImageDirect(Lrustedwarfare/client/render/GameImage;Landroid/graphics/Rect;Landroid/graphics/Rect;Landroid/graphics/Paint;)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void vulkanmod$drawImageDirect(GameImage image, Rect src, Rect dst, Paint paint,
                                           CallbackInfo callback) {
        RectF destination = dst == null ? null : new RectF(dst);
        if (VulkanRuntime.captureImage(vulkanmod$self(), image, src, destination, paint)) {
            callback.cancel();
        }
    }

    @Inject(method = {
            "drawText(Ljava/lang/String;FFLandroid/graphics/Paint;)V",
            "drawCircle(FFFLandroid/graphics/Paint;)V",
            "drawCircleDirect(FFFLandroid/graphics/Paint;)V",
            "drawLine(FFFFLandroid/graphics/Paint;)V",
            "drawLines([FIILandroid/graphics/Paint;)V"
    }, at = @At("HEAD"), require = 1)
    private void vulkanmod$noteUnsupported(CallbackInfo callback) {
        VulkanRuntime.noteUnsupportedDraw(vulkanmod$self());
    }
}
