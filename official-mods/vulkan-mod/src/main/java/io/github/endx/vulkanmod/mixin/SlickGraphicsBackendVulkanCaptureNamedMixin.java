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

    @Inject(method = "drawImageRotated(Lrustedwarfare/client/render/GameImage;FFFLandroid/graphics/Paint;)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void vulkanmod$drawImageRotated(GameImage image, float x, float y, float angle,
                                            Paint paint, CallbackInfo callback) {
        if (VulkanRuntime.captureImageRotated(
                vulkanmod$self(), image, null, x, y, angle, paint)) callback.cancel();
    }

    @Inject(method = "drawImageSectionRotated(Lrustedwarfare/client/render/GameImage;Landroid/graphics/Rect;FFFLandroid/graphics/Paint;)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void vulkanmod$drawImageSectionRotated(GameImage image, Rect src,
                                                   float x, float y, float angle,
                                                   Paint paint, CallbackInfo callback) {
        if (VulkanRuntime.captureImageRotated(
                vulkanmod$self(), image, src, x, y, angle, paint)) callback.cancel();
    }

    @Inject(method = "drawImageTransformed(Lrustedwarfare/client/render/GameImage;FFLandroid/graphics/Paint;FF)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void vulkanmod$drawImageTransformed(GameImage image, float x, float y, Paint paint,
                                                float angle, float scale,
                                                CallbackInfo callback) {
        if (VulkanRuntime.captureImageTransformed(
                vulkanmod$self(), image, x, y, paint, angle, scale)) callback.cancel();
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

    @Inject(method = "drawTiledImage(Lrustedwarfare/client/render/GameImage;Landroid/graphics/Rect;Landroid/graphics/Paint;)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void vulkanmod$drawTiledImage(GameImage image, Rect destination, Paint paint,
                                          CallbackInfo callback) {
        RectF target = destination == null ? null : new RectF(destination);
        if (VulkanRuntime.captureTiledImage(
                vulkanmod$self(), image, target, paint, 0.0f, 0.0f)) callback.cancel();
    }

    @Inject(method = "drawTiledImage(Lrustedwarfare/client/render/GameImage;Landroid/graphics/Rect;Landroid/graphics/Paint;IIII)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void vulkanmod$drawTiledImageOptions(GameImage image, Rect destination, Paint paint,
                                                 int offsetX, int offsetY,
                                                 int ignoredRight, int ignoredBottom,
                                                 CallbackInfo callback) {
        RectF target = destination == null ? null : new RectF(destination);
        if (VulkanRuntime.captureTiledImage(vulkanmod$self(), image, target, paint,
                offsetX, offsetY)) callback.cancel();
    }

    @Inject(method = "drawTiledImage(Lrustedwarfare/client/render/GameImage;Landroid/graphics/RectF;Landroid/graphics/Paint;FFII)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void vulkanmod$drawTiledImageF(GameImage image, RectF destination, Paint paint,
                                          float offsetX, float offsetY,
                                          int ignoredRight, int ignoredBottom,
                                          CallbackInfo callback) {
        if (VulkanRuntime.captureTiledImage(vulkanmod$self(), image, destination, paint,
                offsetX, offsetY)) callback.cancel();
    }

    @Inject(method = "drawText(Ljava/lang/String;FFLandroid/graphics/Paint;)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void vulkanmod$drawText(String text, float x, float y, Paint paint,
                                    CallbackInfo callback) {
        if (VulkanRuntime.captureText(vulkanmod$self(), text, x, y, paint)) callback.cancel();
    }

    @Inject(method = "drawLine(FFFFLandroid/graphics/Paint;)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void vulkanmod$drawLine(float x1, float y1, float x2, float y2, Paint paint,
                                    CallbackInfo callback) {
        if (VulkanRuntime.captureLine(
                vulkanmod$self(), x1, y1, x2, y2, paint)) callback.cancel();
    }

    @Inject(method = "drawLines([FIILandroid/graphics/Paint;)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void vulkanmod$drawLines(float[] points, int offset, int count, Paint paint,
                                     CallbackInfo callback) {
        if (VulkanRuntime.captureLines(
                vulkanmod$self(), points, offset, count, paint)) callback.cancel();
    }

    @Inject(method = {
            "drawCircle(FFFLandroid/graphics/Paint;)V",
            "drawCircleDirect(FFFLandroid/graphics/Paint;)V"
    }, at = @At("HEAD"), cancellable = true, require = 1)
    private void vulkanmod$drawCircle(float x, float y, float radius, Paint paint,
                                      CallbackInfo callback) {
        if (VulkanRuntime.captureCircle(
                vulkanmod$self(), x, y, radius, paint)) callback.cancel();
    }

    @Inject(method = {
            "clearAlphaMap()V"
    }, at = @At("HEAD"), require = 1)
    private void vulkanmod$noteUnsupported(CallbackInfo callback) {
        VulkanRuntime.noteUnsupportedDraw(vulkanmod$self());
    }
}
