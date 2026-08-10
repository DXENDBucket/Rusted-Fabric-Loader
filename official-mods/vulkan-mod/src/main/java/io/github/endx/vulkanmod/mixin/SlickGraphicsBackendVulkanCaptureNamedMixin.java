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

    @Inject(method = "drawImageQuadInternal(Lrustedwarfare/client/render/GameImage;FFFFFFFFLandroid/graphics/Paint;)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void vulkanmod$drawImageQuad(GameImage image,
                                         float left, float top, float right, float bottom,
                                         float sourceLeft, float sourceTop,
                                         float sourceRight, float sourceBottom,
                                         Paint paint, CallbackInfo callback) {
        if (VulkanRuntime.captureImageQuad(vulkanmod$self(), image,
                left, top, right, bottom,
                sourceLeft, sourceTop, sourceRight, sourceBottom, paint)) {
            callback.cancel();
        }
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
