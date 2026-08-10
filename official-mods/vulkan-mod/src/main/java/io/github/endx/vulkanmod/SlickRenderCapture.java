package io.github.endx.vulkanmod;

import android.graphics.Paint;
import android.graphics.Paint$Align;
import android.graphics.Paint$Style;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import io.github.endx.vulkanmod.mixin.SlickGraphicsBackendStateAccessor;
import io.github.endx.vulkanmod.mixin.SlickTransformStateAccessor;
import io.github.endx.vulkanmod.spi.VulkanClipRect;
import io.github.endx.vulkanmod.spi.VulkanBlendMode;
import io.github.endx.vulkanmod.spi.VulkanColoredQuad;
import io.github.endx.vulkanmod.spi.VulkanColoredTriangle;
import io.github.endx.vulkanmod.spi.VulkanDrawState;
import io.github.endx.vulkanmod.spi.VulkanFrameCommands;
import io.github.endx.vulkanmod.spi.VulkanTexturedQuad;
import io.github.endx.vulkanmod.spi.VulkanTexturedTriangle;
import io.github.endx.vulkanmod.spi.VulkanTextureFilter;
import io.github.endx.vulkanmod.spi.VulkanTransform2D;
import rustedwarfare.client.render.GameImage;
import rustedwarfare.client.render.SlickGraphicsBackend;
import rustedwarfare.client.render.SlickTransformState;
import rustedwarfare.render.ShaderBlendMode;
import rustedwarfare.render.ShaderColorFilter;

/** Translates the first safe subset of Slick draw calls into ordered Vulkan commands. */
final class SlickRenderCapture {
    private VulkanFrameCommands.Builder builder;
    private SlickGraphicsBackend backend;
    private int width;
    private int height;
    private int commandCount;
    private int rejectedCount;

    void begin(SlickGraphicsBackend source) {
        begin(Math.max(1, source.getWidth()), Math.max(1, source.getHeight()));
        backend = source;
    }

    void begin(int frameWidth, int frameHeight) {
        backend = null;
        width = Math.max(1, frameWidth);
        height = Math.max(1, frameHeight);
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
            commandCount++;
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
        if (!ensure(source)) return false;
        if (paint == null || right < left || bottom < top) return reject();
        if (paint.d() == Paint$Style.b) {
            return rectangleOutline(source, left, top, right, bottom, paint);
        }
        float[] color = paintColor(paint);
        colored(left, top, right - left, bottom - top, color, state(source, paint));
        if (paint.d() == Paint$Style.c) {
            rectangleOutline(source, left, top, right, bottom, paint);
        }
        return true;
    }

    private boolean rectangleOutline(SlickGraphicsBackend source, float left, float top,
                                     float right, float bottom, Paint paint) {
        float thickness = Math.max(1.0f, paint.g());
        float half = thickness * 0.5f;
        float[] color = paintColor(paint);
        VulkanDrawState drawState = state(source, paint);
        colored(left - half, top - half, right - left + thickness, thickness,
                color, drawState);
        colored(left - half, bottom - half, right - left + thickness, thickness,
                color, drawState);
        colored(left - half, top + half, thickness,
                Math.max(0.0f, bottom - top - thickness), color, drawState);
        colored(right - half, top + half, thickness,
                Math.max(0.0f, bottom - top - thickness), color, drawState);
        return true;
    }

    boolean image(SlickGraphicsBackend source, GameImage image, Rect src, RectF dst,
                  Paint paint) {
        return image(source, image, src, dst, paint, null);
    }

    private boolean image(SlickGraphicsBackend source, GameImage image, Rect src, RectF dst,
                          Paint paint, VulkanDrawState overrideState) {
        if (!ensure(source)) return false;
        if (image == null || src == null || dst == null
                || dst.c < dst.a || dst.d < dst.b) return reject();
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        if (imageWidth <= 0 || imageHeight <= 0) return reject();
        long texture = VulkanRuntime.textureForGameImage(image);
        float[] tint = paint == null ? WHITE : paintColor(paint);
        builder.texturedQuad(new VulkanTexturedQuad(texture,
                dst.a, dst.b, dst.c - dst.a, dst.d - dst.b,
                src.a / (float) imageWidth, src.b / (float) imageHeight,
                src.c / (float) imageWidth, src.d / (float) imageHeight,
                tint[0], tint[1], tint[2], tint[3],
                overrideState == null ? state(source, paint) : overrideState));
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

    boolean imageRotated(SlickGraphicsBackend source, GameImage image, Rect src,
                         float centerX, float centerY, float angle, Paint paint) {
        if (image == null) return reject();
        Rect sourceRect = src == null
                ? new Rect(0, 0, image.getWidth(), image.getHeight()) : src;
        RectF destination = new RectF(centerX - image.halfWidth,
                centerY - image.halfHeight, centerX + image.halfWidth,
                centerY + image.halfHeight);
        VulkanTransform2D local = VulkanTransform2D.rotationAround(
                angle, centerX, centerY);
        return image(source, image, sourceRect, destination, paint,
                stateWithLocalTransform(source, paint, local));
    }

    boolean imageTransformed(SlickGraphicsBackend source, GameImage image,
                             float x, float y, Paint paint, float angle, float scale) {
        if (image == null || !Float.isFinite(scale) || scale < 0.0f) return reject();
        Rect sourceRect = new Rect(0, 0, image.getWidth(), image.getHeight());
        RectF destination = new RectF(x, y,
                x + image.getWidth() * scale, y + image.getHeight() * scale);
        return image(source, image, sourceRect, destination, paint,
                stateWithLocalTransform(source, paint,
                        VulkanTransform2D.rotationAround(angle, x, y)));
    }

    boolean tiledImage(SlickGraphicsBackend source, GameImage image, RectF destination,
                       Paint paint, float offsetX, float offsetY) {
        if (!ensure(source)) return false;
        if (image == null || destination == null) return reject();
        int tileWidth = image.getWidth();
        int tileHeight = image.getHeight();
        if (tileWidth <= 0 || tileHeight <= 0
                || destination.c <= destination.a || destination.d <= destination.b) {
            return reject();
        }
        float startX = destination.a + positiveModulo(offsetX, tileWidth) - tileWidth;
        float startY = destination.b + positiveModulo(offsetY, tileHeight) - tileHeight;
        int guard = 0;
        for (float y = startY; y < destination.d && guard < 16384; y += tileHeight) {
            for (float x = startX; x < destination.c && guard < 16384; x += tileWidth) {
                float left = Math.max(x, destination.a);
                float top = Math.max(y, destination.b);
                float right = Math.min(x + tileWidth, destination.c);
                float bottom = Math.min(y + tileHeight, destination.d);
                if (right > left && bottom > top) {
                    Rect src = new Rect(Math.round(left - x), Math.round(top - y),
                            Math.round(right - x), Math.round(bottom - y));
                    image(source, image, src, new RectF(left, top, right, bottom), paint);
                }
                guard++;
            }
        }
        return true;
    }

    boolean text(SlickGraphicsBackend source, String text, float x, float y, Paint paint) {
        if (!ensure(source)) return false;
        if (text == null || paint == null) return reject();
        if (text.isEmpty()) return true;
        boolean bold = paint.i() != null && paint.i().a();
        VulkanTextTextureCache.Entry texture = VulkanRuntime.textureForText(
                text, Math.round(paint.k()), bold);
        float left = x;
        if (paint.j() == Paint$Align.b) left -= texture.width * 0.5f;
        else if (paint.j() == Paint$Align.c) left -= texture.width;
        float[] tint = paintColor(paint);
        builder.texturedQuad(new VulkanTexturedQuad(texture.textureHandle,
                left, y - texture.lineHeight, texture.width, texture.height,
                0.0f, 0.0f, 1.0f, 1.0f,
                tint[0], tint[1], tint[2], tint[3], state(source, paint)));
        commandCount++;
        return true;
    }

    boolean line(SlickGraphicsBackend source, float x1, float y1, float x2, float y2,
                 Paint paint) {
        if (!ensure(source)) return false;
        if (paint == null) return reject();
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.hypot(dx, dy);
        float thickness = Math.max(1.0f, paint.g());
        float[] color = paintColor(paint);
        if (length < 0.0001f) {
            colored(x1 - thickness * 0.5f, y1 - thickness * 0.5f,
                    thickness, thickness, color, state(source, paint));
            return true;
        }
        float angle = (float) Math.toDegrees(Math.atan2(dy, dx));
        colored(x1, y1 - thickness * 0.5f, length, thickness, color,
                stateWithLocalTransform(source, paint,
                        VulkanTransform2D.rotationAround(angle, x1, y1)));
        return true;
    }

    boolean lines(SlickGraphicsBackend source, float[] points, int offset, int count,
                  Paint paint) {
        if (!ensure(source)) return false;
        if (points == null || paint == null || count < 0) return reject();
        int start = Math.max(0, offset);
        int availablePoints = Math.max(0, (points.length - start) / 2);
        int pointCount = Math.min(count, availablePoints);
        float size = Math.max(1.0f, paint.g());
        float[] color = paintColor(paint);
        VulkanDrawState drawState = state(source, paint);
        for (int index = 0; index < pointCount; index++) {
            int point = start + index * 2;
            colored(points[point] - size * 0.5f, points[point + 1] - size * 0.5f,
                    size, size, color, drawState);
        }
        return true;
    }

    boolean circle(SlickGraphicsBackend source, float centerX, float centerY,
                   float radius, Paint paint) {
        if (!ensure(source)) return false;
        if (paint == null || !Float.isFinite(radius) || radius < 0.0f) {
            return reject();
        }
        if (radius == 0.0f) return line(source, centerX, centerY, centerX, centerY, paint);
        if (paint.d() == Paint$Style.b) {
            int segments = radius > 100.0f ? 60 : 40;
            float previousX = centerX + radius;
            float previousY = centerY;
            for (int index = 1; index <= segments; index++) {
                double angle = index * Math.PI * 2.0 / segments;
                float nextX = centerX + (float) Math.cos(angle) * radius;
                float nextY = centerY + (float) Math.sin(angle) * radius;
                line(source, previousX, previousY, nextX, nextY, paint);
                previousX = nextX;
                previousY = nextY;
            }
            return true;
        }
        int strips = Math.max(12, Math.min(64, (int) Math.ceil(radius)));
        float stripHeight = radius * 2.0f / strips;
        float[] color = paintColor(paint);
        VulkanDrawState drawState = state(source, paint);
        for (int strip = 0; strip < strips; strip++) {
            float relativeY = -radius + (strip + 0.5f) * stripHeight;
            float halfWidth = (float) Math.sqrt(Math.max(0.0f,
                    radius * radius - relativeY * relativeY));
            colored(centerX - halfWidth, centerY + relativeY - stripHeight * 0.5f,
                    halfWidth * 2.0f, stripHeight, color, drawState);
        }
        return true;
    }

    boolean libRocketGeometry(float[] positions, float[] uvs, int[] packedColors,
                              int[] indices, float translationX, float translationY,
                              long textureHandle, float uScale, float vScale,
                              boolean ignoreVertexColor, float alpha,
                              VulkanClipRect clip) {
        if (builder == null || positions == null || uvs == null
                || packedColors == null || indices == null
                || positions.length % 2 != 0 || uvs.length < positions.length
                || packedColors.length < positions.length / 2
                || indices.length % 3 != 0) return false;
        VulkanDrawState drawState = new VulkanDrawState(VulkanTransform2D.IDENTITY, clip);
        for (int triangleIndex = 0; triangleIndex < indices.length; triangleIndex += 3) {
            float[] trianglePositions = new float[6];
            float[] triangleUvs = textureHandle == 0L ? null : new float[6];
            float[] triangleColors = new float[12];
            for (int vertex = 0; vertex < 3; vertex++) {
                int sourceVertex = indices[triangleIndex + vertex];
                if (sourceVertex < 0 || sourceVertex * 2 + 1 >= positions.length) return false;
                trianglePositions[vertex * 2] = positions[sourceVertex * 2] + translationX;
                trianglePositions[vertex * 2 + 1] = positions[sourceVertex * 2 + 1]
                        + translationY;
                if (triangleUvs != null) {
                    triangleUvs[vertex * 2] = uvs[sourceVertex * 2] * uScale;
                    triangleUvs[vertex * 2 + 1] = uvs[sourceVertex * 2 + 1] * vScale;
                }
                int packed = packedColors[sourceVertex];
                int colorOffset = vertex * 4;
                triangleColors[colorOffset] = ignoreVertexColor ? 1.0f
                        : ((packed >>> 24) & 255) / 255.0f;
                triangleColors[colorOffset + 1] = ignoreVertexColor ? 1.0f
                        : ((packed >>> 16) & 255) / 255.0f;
                triangleColors[colorOffset + 2] = ignoreVertexColor ? 1.0f
                        : ((packed >>> 8) & 255) / 255.0f;
                triangleColors[colorOffset + 3] = (ignoreVertexColor ? 1.0f
                        : (packed & 255) / 255.0f) * alpha;
            }
            if (textureHandle == 0L) {
                builder.coloredTriangle(new VulkanColoredTriangle(
                        trianglePositions, triangleColors, drawState));
            } else {
                builder.texturedTriangle(new VulkanTexturedTriangle(textureHandle,
                        trianglePositions, triangleUvs, triangleColors, drawState));
            }
            commandCount++;
        }
        return true;
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
        if (source == null) return false;
        if (source.renderTargetImage != null) {
            // Offscreen rendering still runs through Slick for now. Any intercepted draw means
            // the next screen-space use must upload the newly rendered pixels, even when the
            // game's public image version was not incremented by its Graphics path.
            VulkanRuntime.invalidateCachedImage(source.renderTargetImage);
            return false;
        }
        if (builder == null) begin(source);
        if (backend == null) backend = source;
        return isCapturing(source);
    }

    private boolean reject() {
        rejectedCount++;
        return false;
    }

    private void colored(float x, float y, float quadWidth, float quadHeight,
                         float[] color, VulkanDrawState drawState) {
        builder.coloredQuad(new VulkanColoredQuad(x, y, quadWidth, quadHeight,
                color[0], color[1], color[2], color[3], drawState));
        commandCount++;
    }

    private static VulkanDrawState state(SlickGraphicsBackend backend, Paint paint) {
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
        return new VulkanDrawState(transform, vulkanClip, blendMode(paint),
                textureFilter(paint));
    }

    private static VulkanDrawState stateWithLocalTransform(
            SlickGraphicsBackend backend, Paint paint, VulkanTransform2D local) {
        VulkanDrawState base = state(backend, paint);
        return new VulkanDrawState(local.then(base.transform()), base.clip(), base.blendMode(),
                base.textureFilter());
    }

    private static float positiveModulo(float value, int divisor) {
        float result = value % divisor;
        return result < 0.0f ? result + divisor : result;
    }

    private static float[] color(int argb) {
        return new float[] {
                ((argb >>> 16) & 255) / 255.0f,
                ((argb >>> 8) & 255) / 255.0f,
                (argb & 255) / 255.0f,
                ((argb >>> 24) & 255) / 255.0f
        };
    }

    private static float[] paintColor(Paint paint) {
        float[] tint = color(paint.e());
        ColorFilter filter = paint.h();
        if (filter instanceof LightingColorFilter) {
            int multiplier = ((LightingColorFilter) filter).a;
            if (multiplier != 0 && multiplier != -1) {
                float[] lighting = color(multiplier);
                for (int channel = 0; channel < 4; channel++) {
                    tint[channel] *= lighting[channel];
                }
            }
        }
        return tint;
    }

    private static VulkanBlendMode blendMode(Paint paint) {
        if (paint == null) return VulkanBlendMode.NORMAL;
        ColorFilter filter = paint.h();
        if (filter instanceof LightingColorFilter) {
            int multiplier = ((LightingColorFilter) filter).a;
            if (multiplier != 0 && multiplier != -1) return VulkanBlendMode.ADDITIVE;
        } else if (filter instanceof ShaderColorFilter) {
            ShaderBlendMode mode = ((ShaderColorFilter) filter).blendMode;
            if (mode == ShaderBlendMode.copy) return VulkanBlendMode.COPY;
            if (mode == ShaderBlendMode.additive) return VulkanBlendMode.MODULATE;
        }
        return VulkanBlendMode.NORMAL;
    }

    private static VulkanTextureFilter textureFilter(Paint paint) {
        return paint != null && paint.c()
                ? VulkanTextureFilter.LINEAR : VulkanTextureFilter.NEAREST;
    }

    private static final float[] WHITE = { 1.0f, 1.0f, 1.0f, 1.0f };
}
