package io.github.endx.vulkanmod;

import android.graphics.Paint;
import android.graphics.Paint$Style;
import android.graphics.Rect;
import android.graphics.RectF;
import io.github.endx.vulkanmod.mixin.SlickGraphicsBackendStateAccessor;
import io.github.endx.vulkanmod.mixin.SlickTransformStateAccessor;
import io.github.endx.vulkanmod.spi.VulkanClipRect;
import io.github.endx.vulkanmod.spi.VulkanColoredQuad;
import io.github.endx.vulkanmod.spi.VulkanDrawState;
import io.github.endx.vulkanmod.spi.VulkanFrameCommands;
import io.github.endx.vulkanmod.spi.VulkanTexturedQuad;
import io.github.endx.vulkanmod.spi.VulkanTransform2D;
import rustedwarfare.client.render.GameImage;
import rustedwarfare.client.render.SlickGraphicsBackend;
import rustedwarfare.client.render.SlickTransformState;

/** Translates the first safe subset of Slick draw calls into ordered Vulkan commands. */
final class SlickRenderCapture {
    private VulkanFrameCommands.Builder builder;
    private SlickGraphicsBackend backend;
    private int width;
    private int height;
    private int commandCount;
    private int rejectedCount;

    void begin(SlickGraphicsBackend source) {
        backend = source;
        width = Math.max(1, source.getWidth());
        height = Math.max(1, source.getHeight());
        builder = VulkanFrameCommands.builder(width, height).clear(0.0f, 0.0f, 0.0f, 1.0f);
        commandCount = 0;
        rejectedCount = 0;
    }

    VulkanFrameCommands finish() {
        if (builder == null) return null;
        VulkanFrameCommands frame = builder.build();
        builder = null;
        backend = null;
        return frame;
    }

    boolean clear(SlickGraphicsBackend source, int argb) {
        if (!ensure(source)) return false;
        float[] color = color(argb);
        if (commandCount == 0) {
            builder.clear(color[0], color[1], color[2], color[3]);
        } else {
            builder.coloredQuad(new VulkanColoredQuad(0.0f, 0.0f, width, height,
                    color[0], color[1], color[2], color[3]));
            commandCount++;
        }
        return true;
    }

    boolean rectangle(SlickGraphicsBackend source, Rect rect, Paint paint) {
        if (rect == null) return reject();
        return rectangle(source, rect.a, rect.b, rect.c, rect.d, paint);
    }

    boolean rectangle(SlickGraphicsBackend source, RectF rect, Paint paint) {
        if (rect == null) return reject();
        return rectangle(source, rect.a, rect.b, rect.c, rect.d, paint);
    }

    private boolean rectangle(SlickGraphicsBackend source, float left, float top,
                              float right, float bottom, Paint paint) {
        if (!ensure(source) || paint == null || paint.d() == Paint$Style.b
                || right < left || bottom < top) return reject();
        float[] color = color(paint.e());
        builder.coloredQuad(new VulkanColoredQuad(left, top, right - left, bottom - top,
                color[0], color[1], color[2], color[3], state(source)));
        commandCount++;
        return true;
    }

    boolean image(SlickGraphicsBackend source, GameImage image, Rect src, RectF dst,
                  Paint paint) {
        if (!ensure(source) || image == null || src == null || dst == null
                || dst.c < dst.a || dst.d < dst.b) return reject();
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        if (imageWidth <= 0 || imageHeight <= 0) return reject();
        long texture = VulkanRuntime.textureForGameImage(image);
        float[] tint = paint == null ? WHITE : color(paint.e());
        builder.texturedQuad(new VulkanTexturedQuad(texture,
                dst.a, dst.b, dst.c - dst.a, dst.d - dst.b,
                src.a / (float) imageWidth, src.b / (float) imageHeight,
                src.c / (float) imageWidth, src.d / (float) imageHeight,
                tint[0], tint[1], tint[2], tint[3], state(source)));
        commandCount++;
        return true;
    }

    boolean imageRaw(SlickGraphicsBackend source, GameImage image, float x, float y,
                     Paint paint) {
        if (image == null) return reject();
        return image(source, image, new Rect(0, 0, image.getWidth(), image.getHeight()),
                new RectF(x, y, x + image.getWidth(), y + image.getHeight()), paint);
    }

    boolean imageCentered(SlickGraphicsBackend source, GameImage image, float x, float y,
                          Paint paint) {
        if (image == null) return reject();
        return imageRaw(source, image, x - image.halfWidth, y - image.halfHeight, paint);
    }

    int commandCount() { return commandCount; }
    int rejectedCount() { return rejectedCount; }

    void unsupported(SlickGraphicsBackend source) {
        if (ensure(source)) rejectedCount++;
    }

    private boolean isCapturing(SlickGraphicsBackend source) {
        return builder != null && backend == source;
    }

    private boolean ensure(SlickGraphicsBackend source) {
        if (source == null || source.renderTargetImage != null) return false;
        if (builder == null) begin(source);
        return isCapturing(source);
    }

    private boolean reject() {
        rejectedCount++;
        return false;
    }

    private static VulkanDrawState state(SlickGraphicsBackend backend) {
        SlickTransformState raw = ((SlickGraphicsBackendStateAccessor) (Object) backend)
                .vulkanmod$getTransformState();
        SlickTransformStateAccessor state = (SlickTransformStateAccessor) (Object) raw;
        VulkanTransform2D transform = VulkanTransform2D.scale(
                state.vulkanmod$getScaleX(), state.vulkanmod$getScaleY())
                .then(VulkanTransform2D.translation(
                        state.vulkanmod$getTranslateX(), state.vulkanmod$getTranslateY()));
        float effectiveRotation = state.vulkanmod$getRotationDegrees() + 90.0f;
        if (Math.abs(effectiveRotation) > 0.0001f) {
            transform = transform.then(VulkanTransform2D.rotationAround(effectiveRotation,
                    state.vulkanmod$getRotationPivotX(), state.vulkanmod$getRotationPivotY()));
        }
        RectF clip = state.vulkanmod$getClipRect();
        VulkanClipRect vulkanClip = clip == null ? null : new VulkanClipRect(
                clip.a, clip.b, Math.max(0.0f, clip.c - clip.a),
                Math.max(0.0f, clip.d - clip.b));
        return new VulkanDrawState(transform, vulkanClip);
    }

    private static float[] color(int argb) {
        return new float[] {
                ((argb >>> 16) & 255) / 255.0f,
                ((argb >>> 8) & 255) / 255.0f,
                (argb & 255) / 255.0f,
                ((argb >>> 24) & 255) / 255.0f
        };
    }

    private static final float[] WHITE = { 1.0f, 1.0f, 1.0f, 1.0f };
}
