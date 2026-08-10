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
    private SlickGraphicsBackend activeGlBackend;
    private int diagnosedOffscreenImages;
    private final java.util.IdentityHashMap<GameImage, Boolean> diagnosedRenderTargets =
            new java.util.IdentityHashMap<GameImage, Boolean>();

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
        if (Boolean.getBoolean("rusted.fabric.vulkan.debugMagentaClear")) {
            builder.clear(0.65f, 0.0f, 0.65f, 1.0f);
        }
        if (Boolean.getBoolean("rusted.fabric.vulkan.debugMarkerQuad")) {
            builder.coloredQuad(new VulkanColoredQuad(32.0f, 32.0f, 224.0f, 160.0f,
                    0.1f, 1.0f, 0.1f, 1.0f));
        }
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

    boolean imageQuad(SlickGraphicsBackend source, GameImage image,
                      float left, float top, float right, float bottom,
                      float sourceLeft, float sourceTop,
                      float sourceRight, float sourceBottom, Paint paint) {
        if (Boolean.getBoolean("rusted.fabric.vulkan.debugRenderTargets")
                && source != null && source.renderTargetImage != null
                && source.renderTargetImage.getWidth() >= 1000
                && diagnosedOffscreenImages++ < 80) {
            System.out.println("[Vulkan Mod] Offscreen image draw target="
                    + source.renderTargetImage.getName() + "("
                    + source.renderTargetImage.getWidth() + "x"
                    + source.renderTargetImage.getHeight() + ") source="
                    + (image == null ? "null" : image.getName())
                    + " src=" + sourceLeft + "," + sourceTop + ".."
                    + sourceRight + "," + sourceBottom
                    + " dst=" + left + "," + top + ".." + right + "," + bottom);
        }
        if (!ensure(source)) return false;
        if (image == null || right < left || bottom < top) return reject();
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        if (imageWidth <= 0 || imageHeight <= 0) return reject();
        if (Boolean.getBoolean("rusted.fabric.vulkan.debugRenderTargets")
                && VulkanRuntime.isRenderTargetImage(image)
                && diagnosedRenderTargets.put(image, Boolean.TRUE) == null) {
            System.out.println("[Vulkan Mod] Render-target draw " + image.getName()
                    + " image=" + imageWidth + "x" + imageHeight
                    + " src=" + sourceLeft + "," + sourceTop + ".."
                    + sourceRight + "," + sourceBottom
                    + " dst=" + left + "," + top + ".." + right + "," + bottom);
        }

        SlickTransformState raw = ((SlickGraphicsBackendStateAccessor) (Object) source)
                .vulkanmod$getTransformState();
        SlickTransformStateAccessor transform = (SlickTransformStateAccessor) (Object) raw;
        float width = right - left;
        float height = bottom - top;
        float rotation = transform.vulkanmod$getRotationDegrees();
        if (rotation != -90.0f) {
            float halfWidth = width * 0.5f;
            float halfHeight = height * 0.5f;
            float relativeX = left + halfWidth - transform.vulkanmod$getRotationPivotX();
            float relativeY = top + halfHeight - transform.vulkanmod$getRotationPivotY();
            if (relativeX > 0.01f || relativeY > 0.01f
                    || relativeX < -0.01f || relativeY < -0.01f) {
                double radians = Math.toRadians(rotation + 180.0f);
                float sine = (float) Math.sin(radians);
                float cosine = (float) Math.cos(radians);
                float rotatedX = cosine * relativeY - sine * relativeX;
                float rotatedY = sine * relativeY + cosine * relativeX;
                left = rotatedX + transform.vulkanmod$getRotationPivotX() - halfWidth;
                top = rotatedY + transform.vulkanmod$getRotationPivotY() - halfHeight;
            }
        }

        float scaledWidth = width * transform.vulkanmod$getScaleX();
        float scaledHeight = height * transform.vulkanmod$getScaleY();
        float screenLeft = left * transform.vulkanmod$getScaleX()
                + transform.vulkanmod$getTranslateX();
        float screenTop = top * transform.vulkanmod$getScaleY()
                + transform.vulkanmod$getTranslateY();
        float effectiveRotation = rotation + 90.0f;
        VulkanTransform2D imageTransform = Math.abs(effectiveRotation) < 0.0001f
                ? VulkanTransform2D.IDENTITY
                : VulkanTransform2D.rotationAround(effectiveRotation,
                        screenLeft + scaledWidth * 0.5f,
                        screenTop + scaledHeight * 0.5f);
        VulkanDrawState drawState = new VulkanDrawState(imageTransform,
                clip(transform), blendMode(paint), textureFilter(paint));
        long texture = VulkanRuntime.textureForGameImage(image);
        float[] tint = paint == null ? WHITE : paintColor(paint);
        builder.texturedQuad(new VulkanTexturedQuad(texture,
                screenLeft, screenTop, scaledWidth, scaledHeight,
                sourceLeft / imageWidth, sourceTop / imageHeight,
                sourceRight / imageWidth, sourceBottom / imageHeight,
                tint[0], tint[1], tint[2], tint[3],
                drawState));
        commandCount++;
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
        // GraphicsEngine follows Canvas.drawPoints here: offset and count are measured in
        // floats, with each consecutive x/y pair describing one square point. MinimapLineBatch
        // deliberately keeps a larger reusable array and passes only its populated float count.
        // Treating count as a point count consumes the stale tail of that array and paints
        // phantom unit markers over the minimap.
        int end = Math.min(points.length, start + count);
        if (((end - start) & 1) != 0) end--;
        float size = Math.max(1.0f, paint.g());
        float[] color = paintColor(paint);
        VulkanDrawState drawState = state(source, paint);
        for (int point = start; point + 1 < end; point += 2) {
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
            activateGlBackend(source);
            // Offscreen rendering still runs through Slick for now. Any intercepted draw means
            // the next screen-space use must upload the newly rendered pixels, even when the
            // game's public image version was not incremented by its Graphics path.
            VulkanRuntime.markRenderTargetImage(source.renderTargetImage);
            return false;
        }
        // Cancelling drawImageQuadInternal also skips applyPaintState(), which normally
        // switches Slick away from the last offscreen FBO and back to the window graphics.
        // Tile-cache rendering frequently leaves an FBO current immediately before its
        // completed image is drawn to the screen. Keep Slick's context state transition even
        // though Vulkan consumes the screen draw; otherwise later GL fallbacks and texture
        // readback can still target the tile-cache FBO and corrupt the cached terrain.
        activateGlBackend(source);
        if (builder == null) begin(source);
        if (backend == null) backend = source;
        return isCapturing(source);
    }

    private void activateGlBackend(SlickGraphicsBackend source) {
        if (activeGlBackend == source) return;
        // Vulkan cancels some of the original screen draws that normally drive Slick's
        // Graphics.setCurrent transitions. Explicitly make each main/FBO backend transition,
        // then pass through applyPaintState so the game's own currentSlickGraphics bookkeeping
        // agrees with the actual GL target. This is especially important for the two-stage map
        // cache (shared tile buffer -> per-region buffer -> screen).
        // The region canvas deliberately leaves its Image.startUse batch pending and relies on
        // the following screen draw to switch Graphics and flush it. Since takeover cancels that
        // screen draw, flush the backend we are leaving before reading its texture on the CPU.
        if (activeGlBackend != null) {
            try {
                activeGlBackend.flush();
            } catch (NullPointerException notInitializedYet) {
                // A short-lived bootstrap backend has no Slick Graphics object. It cannot have
                // a pending FBO batch, so there is nothing useful to flush.
            }
        }
        source.activateGraphicsContext();
        source.applyPaint(null);
        activeGlBackend = source;
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
        return new VulkanDrawState(transform, clip(state), blendMode(paint),
                textureFilter(paint));
    }

    private static VulkanClipRect clip(SlickTransformStateAccessor state) {
        RectF clip = state.vulkanmod$getClipRect();
        return clip == null ? null : new VulkanClipRect(
                clip.a, clip.b, Math.max(0.0f, clip.c - clip.a),
                Math.max(0.0f, clip.d - clip.b));
    }

    private static VulkanDrawState stateWithLocalTransform(
            SlickGraphicsBackend backend, Paint paint, VulkanTransform2D local) {
        VulkanDrawState base = state(backend, paint);
        return new VulkanDrawState(local.then(base.transform()), base.clip(), base.blendMode(),
                base.textureFilter());
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
