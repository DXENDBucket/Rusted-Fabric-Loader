package io.github.endx.vulkanmod.render;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Paint$Style;
import android.graphics.Bitmap;
import io.github.endx.vulkanmod.VulkanRuntime;
import io.github.endx.vulkanmod.spi.VulkanBlendMode;
import io.github.endx.vulkanmod.spi.VulkanClipRect;
import io.github.endx.vulkanmod.spi.VulkanColoredQuad;
import io.github.endx.vulkanmod.spi.VulkanDrawState;
import io.github.endx.vulkanmod.spi.VulkanTextureFilter;
import io.github.endx.vulkanmod.spi.VulkanTexturedQuad;
import io.github.endx.vulkanmod.spi.VulkanTransform2D;
import rustedwarfare.client.render.GameImage;
import rustedwarfare.client.render.SlickGraphicsBackend;
import rustedwarfare.render.AndroidGlRenderer;
import rustedwarfare.render.CanvasDrawTarget;
import rustedwarfare.render.DrawTimeCallback;
import rustedwarfare.render.GraphicsEngine;
import rustedwarfare.render.ShaderProgram;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.awt.image.BufferedImage;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import javax.imageio.ImageIO;
import java.util.concurrent.locks.Lock;
import java.util.Arrays;
import java.util.ArrayDeque;
import java.util.Deque;
import rustedwarfare.util.CommonUtils;
import com.corrodinggames.rts.R$drawable;

/**
 * Renderer selected by the game in native Vulkan mode.
 *
 * <p>The root engine records directly into the native Vulkan frame owned by {@link VulkanRuntime}.
 * CPU-backed child engines provide the game's offscreen image semantics without creating a Slick
 * renderer, LWJGL Display, or OpenGL context.</p>
 */
public final class VulkanGraphicsEngine implements GraphicsEngine {
    private final GraphicsEngine delegate;
    private final SlickGraphicsBackend slickDelegate;
    private final GameImage renderTarget;
    private int width;
    private int height;
    private GameImage errorImage;
    private VulkanTransform2D transform = VulkanTransform2D.IDENTITY;
    private VulkanClipRect clip;
    private final Deque<NativeState> stateStack = new ArrayDeque<NativeState>();
    private final CanvasDrawTarget nativeCanvasTarget = new NativeCanvasTarget();

    public VulkanGraphicsEngine() {
        this.delegate = null;
        this.slickDelegate = null;
        this.renderTarget = null;
        VulkanRuntime.onGraphicsEngineInstalled(this);
    }

    private VulkanGraphicsEngine(GameImage renderTarget) {
        if (renderTarget == null) throw new NullPointerException("renderTarget");
        this.delegate = null;
        this.slickDelegate = null;
        this.renderTarget = renderTarget.getRealImage();
        this.width = renderTarget.getWidth();
        this.height = renderTarget.getHeight();
    }

    public SlickGraphicsBackend compatibilityDelegate() {
        return slickDelegate;
    }

    public static GraphicsEngine unwrapCompatibility(GraphicsEngine engine) {
        if (!(engine instanceof VulkanGraphicsEngine)) return engine;
        GraphicsEngine delegate = ((VulkanGraphicsEngine) engine).delegate;
        return delegate == null ? engine : delegate;
    }

    private boolean nativeRoot() {
        return renderTarget == null && VulkanRuntime.isNativeRendererSelected();
    }

    private VulkanDrawState state(Paint paint) {
        return new VulkanDrawState(transform, clip, VulkanBlendMode.NORMAL,
                paint != null && paint.c()
                        ? VulkanTextureFilter.LINEAR : VulkanTextureFilter.NEAREST);
    }

    private static float[] color(int argb) {
        return new float[] {
                ((argb >>> 16) & 255) / 255.0f,
                ((argb >>> 8) & 255) / 255.0f,
                (argb & 255) / 255.0f,
                ((argb >>> 24) & 255) / 255.0f
        };
    }

    private void nativeQuad(float x, float y, float width, float height, Paint paint) {
        if (width < 0.0f || height < 0.0f) return;
        float[] rgba = color(paint == null ? 0xffffffff : paint.e());
        VulkanRuntime.recordNativeColoredQuad(new VulkanColoredQuad(
                x, y, width, height, rgba[0], rgba[1], rgba[2], rgba[3], state(paint)));
    }

    private void nativeImage(GameImage image, Rect source, float left, float top,
                             float right, float bottom, Paint paint,
                             VulkanTransform2D localTransform) {
        if (image == null || source == null || right < left || bottom < top) return;
        GameImage real = image.getRealImage();
        if (real == null) real = image;
        int imageWidth = real.getWidth();
        int imageHeight = real.getHeight();
        if (imageWidth <= 0 || imageHeight <= 0) return;
        long texture = VulkanRuntime.textureForGameImage(real);
        if (texture == 0L) return;
        float[] tint = color(paint == null ? 0xffffffff : paint.e());
        VulkanDrawState base = state(paint);
        VulkanDrawState drawState = localTransform == null
                ? base : new VulkanDrawState(localTransform.then(base.transform()), base.clip(),
                        base.blendMode(), base.textureFilter());
        VulkanRuntime.recordNativeTexturedQuad(new VulkanTexturedQuad(texture,
                left, top, right - left, bottom - top,
                source.a / (float) imageWidth, source.b / (float) imageHeight,
                source.c / (float) imageWidth, source.d / (float) imageHeight,
                tint[0], tint[1], tint[2], tint[3], drawState));
    }

    @Override public GraphicsEngine createBackendForImage(GameImage image) {
        VulkanRuntime.markRenderTargetImage(image);
        return new VulkanGraphicsEngine(image);
    }

    @Override public GraphicsEngine createChildBackendForImage(GameImage image) {
        VulkanRuntime.markRenderTargetImage(image);
        return new VulkanGraphicsEngine(image);
    }

    @Override public boolean a() { return renderTarget != null || nativeRoot() || delegate.a(); }
    @Override public void a(Context context) {
        // Desktop native loading resolves resource ids through CommonUtils.resourceIdToPath.
    }
    @Override public void b() {
        errorImage = a(R$drawable.error_outmem, true);
        errorImage.setName("Out of memory");
    }
    @Override public CanvasDrawTarget d() { return nativeRoot() ? nativeCanvasTarget : delegate.d(); }
    @Override public void a(CanvasDrawTarget target) {
        if (!nativeRoot()) delegate.a(target);
    }
    @Override public void a(AndroidGlRenderer renderer) {
        if (!nativeRoot()) delegate.a(renderer);
    }
    @Override public GameImage a(int resourceId) { return a(resourceId, true); }
    @Override public GameImage a(int resourceId, boolean option) {
        String path = CommonUtils.resourceIdToPath(resourceId);
        try (InputStream input = new FileInputStream(path)) {
            GameImage image = loadImageFromStream(input, option);
            image.setName(path);
            return image;
        } catch (IOException failure) {
            throw new IllegalStateException("Could not load native image resource " + path,
                    failure);
        }
    }
    @Override public GameImage loadImageFromStream(InputStream input, boolean option) {
        if (input == null) throw new NullPointerException("input");
        try {
            BufferedImage decoded = ImageIO.read(input);
            if (decoded == null) throw new IOException("unsupported image format");
            int width = decoded.getWidth();
            int height = decoded.getHeight();
            int[] argb = decoded.getRGB(0, 0, width, height, null, 0, width);
            return new VulkanGameImage(width, height, argb);
        } catch (IOException failure) {
            throw new IllegalStateException("Could not decode native image", failure);
        }
    }
    @Override public GameImage createImage(int width, int height, boolean alpha) {
        return VulkanGameImage.empty(width, height);
    }
    @Override public GameImage b(int width, int height, boolean alpha) {
        return VulkanGameImage.empty(width, height);
    }
    @Override public void e() { if (renderTarget == null && !nativeRoot()) delegate.e(); }
    @Override public void drawImageRotated(GameImage image, float x, float y, float angle,
                                           Paint paint) {
        if (renderTarget != null) {
            drawImageCentered(image, x, y, paint);
        } else if (nativeRoot()) {
            float left = x - image.getWidth() * 0.5f;
            float top = y - image.getHeight() * 0.5f;
            nativeImage(image, full(image), left, top, left + image.getWidth(),
                    top + image.getHeight(), paint,
                    VulkanTransform2D.rotationAround(angle, x, y));
        } else delegate.drawImageRotated(image, x, y, angle, paint);
    }
    @Override public void drawImageSectionRotated(GameImage image, Rect source, float x, float y,
                                                  float angle, Paint paint) {
        if (renderTarget != null) {
            int width = source.c - source.a;
            int height = source.d - source.b;
            blit(image, source, Math.round(x - width * 0.5f),
                    Math.round(y - height * 0.5f), width, height);
        } else if (nativeRoot()) {
            float width = source.c - source.a;
            float height = source.d - source.b;
            nativeImage(image, source, x - width * 0.5f, y - height * 0.5f,
                    x + width * 0.5f, y + height * 0.5f, paint,
                    VulkanTransform2D.rotationAround(angle, x, y));
        } else delegate.drawImageSectionRotated(image, source, x, y, angle, paint);
    }
    @Override public void drawImage(GameImage image, Rect source, Rect destination, Paint paint) {
        if (renderTarget != null) {
            blit(image, source, destination.a, destination.b,
                    destination.c - destination.a, destination.d - destination.b);
        } else if (nativeRoot()) nativeImage(image, source, destination.a, destination.b,
                destination.c, destination.d, paint, null);
        else delegate.drawImage(image, source, destination, paint);
    }
    @Override public void drawImage(GameImage image, Rect source, RectF destination, Paint paint) {
        if (renderTarget != null) {
            int left = Math.round(destination.a);
            int top = Math.round(destination.b);
            blit(image, source, left, top, Math.round(destination.c) - left,
                    Math.round(destination.d) - top);
        } else if (nativeRoot()) nativeImage(image, source, destination.a, destination.b,
                destination.c, destination.d, paint, null);
        else delegate.drawImage(image, source, destination, paint);
    }
    @Override public void drawImageCentered(GameImage image, float x, float y, Paint paint) {
        if (renderTarget != null) {
            blit(image, full(image), Math.round(x - image.getWidth() * 0.5f),
                    Math.round(y - image.getHeight() * 0.5f),
                    image.getWidth(), image.getHeight());
        } else if (nativeRoot()) nativeImage(image, full(image),
                x - image.getWidth() * 0.5f, y - image.getHeight() * 0.5f,
                x + image.getWidth() * 0.5f, y + image.getHeight() * 0.5f, paint, null);
        else delegate.drawImageCentered(image, x, y, paint);
    }
    @Override public void drawImageTransformed(GameImage image, float x, float y, Paint paint,
                                               float scale, float angle) {
        if (renderTarget != null) {
            int scaledWidth = Math.max(1, Math.round(image.getWidth() * scale));
            int scaledHeight = Math.max(1, Math.round(image.getHeight() * scale));
            blit(image, full(image), Math.round(x - scaledWidth * 0.5f),
                    Math.round(y - scaledHeight * 0.5f), scaledWidth, scaledHeight);
        } else if (nativeRoot()) {
            float w = image.getWidth() * scale;
            float h = image.getHeight() * scale;
            nativeImage(image, full(image), x - w * 0.5f, y - h * 0.5f,
                    x + w * 0.5f, y + h * 0.5f, paint,
                    VulkanTransform2D.rotationAround(angle, x, y));
        } else delegate.drawImageTransformed(image, x, y, paint, scale, angle);
    }
    @Override public void drawImageRaw(GameImage image, float x, float y, Paint paint) {
        if (renderTarget != null) {
            blit(image, full(image), Math.round(x), Math.round(y),
                    image.getWidth(), image.getHeight());
        } else if (nativeRoot()) nativeImage(image, full(image), x, y,
                x + image.getWidth(), y + image.getHeight(), paint, null);
        else delegate.drawImageRaw(image, x, y, paint);
    }
    @Override public void drawImageDirect(GameImage image, Rect source, Rect destination,
                                          Paint paint) {
        if (renderTarget != null) drawImage(image, source, destination, paint);
        else if (nativeRoot()) drawImage(image, source, destination, paint);
        else delegate.drawImageDirect(image, source, destination, paint);
    }
    @Override public void drawRect(Rect rect, Paint paint) {
        if (renderTarget != null) fillRect(rect.a, rect.b, rect.c, rect.d, paint);
        else if (nativeRoot()) nativeRect(rect.a, rect.b, rect.c, rect.d, paint);
        else delegate.drawRect(rect, paint);
    }
    @Override public void a(boolean value) {
        if (renderTarget == null && !nativeRoot()) delegate.a(value);
    }
    @Override public void f() {
        if (renderTarget == null && !nativeRoot()) delegate.f();
    }
    @Override public void drawTiledImage(GameImage image, Rect destination, Paint paint) {
        if (renderTarget != null) {
            drawTiledImage(image, destination, paint, 0, 0,
                    image.getWidth(), image.getHeight());
        } else if (nativeRoot()) drawTiledImage(image, destination, paint, 0, 0,
                image.getWidth(), image.getHeight());
        else delegate.drawTiledImage(image, destination, paint);
    }
    @Override public void drawTiledImage(GameImage image, Rect destination, Paint paint,
                                         int offsetX, int offsetY, int stepX, int stepY) {
        if (renderTarget != null) {
            int tileWidth = stepX > 0 ? stepX : image.getWidth();
            int tileHeight = stepY > 0 ? stepY : image.getHeight();
            for (int y = destination.b + offsetY; y < destination.d; y += tileHeight) {
                for (int x = destination.a + offsetX; x < destination.c; x += tileWidth) {
                    blit(image, full(image), x, y, image.getWidth(), image.getHeight());
                }
            }
        } else if (nativeRoot()) {
            int tileWidth = stepX > 0 ? stepX : image.getWidth();
            int tileHeight = stepY > 0 ? stepY : image.getHeight();
            for (int y = destination.b + offsetY; y < destination.d; y += tileHeight) {
                for (int x = destination.a + offsetX; x < destination.c; x += tileWidth) {
                    float right = Math.min(destination.c, x + image.getWidth());
                    float bottom = Math.min(destination.d, y + image.getHeight());
                    Rect source = new Rect(0, 0, Math.max(0, Math.round(right - x)),
                            Math.max(0, Math.round(bottom - y)));
                    nativeImage(image, source, x, y, right, bottom, paint, null);
                }
            }
        } else delegate.drawTiledImage(image, destination, paint,
                offsetX, offsetY, stepX, stepY);
    }
    @Override public void drawTiledImage(GameImage image, RectF destination, Paint paint,
                                         float offsetX, float offsetY, int stepX, int stepY) {
        if (renderTarget != null) {
            Rect integer = new Rect(Math.round(destination.a), Math.round(destination.b),
                    Math.round(destination.c), Math.round(destination.d));
            drawTiledImage(image, integer, paint, Math.round(offsetX), Math.round(offsetY),
                    stepX, stepY);
        } else if (nativeRoot()) {
            Rect integer = new Rect(Math.round(destination.a), Math.round(destination.b),
                    Math.round(destination.c), Math.round(destination.d));
            drawTiledImage(image, integer, paint, Math.round(offsetX), Math.round(offsetY),
                    stepX, stepY);
        } else delegate.drawTiledImage(image, destination, paint,
                offsetX, offsetY, stepX, stepY);
    }
    @Override public void drawColor(int color) {
        if (renderTarget != null) fillTarget(color);
        else if (nativeRoot()) VulkanRuntime.clearNativeFrame(color);
        else delegate.drawColor(color);
    }
    @Override public void drawColor(int color, PorterDuff.Mode mode) {
        if (renderTarget != null) fillTarget(color);
        else if (nativeRoot()) VulkanRuntime.clearNativeFrame(color);
        else delegate.drawColor(color, mode);
    }
    @Override public void drawTextWithBackground(String text, float x, float y, Paint textPaint,
                                                 Paint backgroundPaint, float padding) {
        if (nativeRoot()) {
            float width = getTextWidth(text, textPaint);
            float height = getTextHeight(text, textPaint);
            nativeRect(x - padding, y - height - padding,
                    x + width + padding, y + padding, backgroundPaint);
            drawText(text, x, y, textPaint);
        } else delegate.drawTextWithBackground(text, x, y, textPaint,
                backgroundPaint, padding);
    }
    @Override public void drawText(String text, float x, float y, Paint paint) {
        if (nativeRoot()) VulkanRuntime.recordNativeText(text, x, y, paint, state(paint));
        else delegate.drawText(text, x, y, paint);
    }
    @Override public void drawRectDirect(Rect rect, Paint paint) {
        if (renderTarget != null) fillRect(rect.a, rect.b, rect.c, rect.d, paint);
        else if (nativeRoot()) nativeRect(rect.a, rect.b, rect.c, rect.d, paint);
        else delegate.drawRectDirect(rect, paint);
    }
    @Override public void drawRect(RectF rect, Paint paint) {
        if (renderTarget != null) fillRect(Math.round(rect.a), Math.round(rect.b),
                Math.round(rect.c), Math.round(rect.d), paint);
        else if (nativeRoot()) nativeRect(rect.a, rect.b, rect.c, rect.d, paint);
        else delegate.drawRect(rect, paint);
    }
    @Override public void beginFrame() {
        if (renderTarget == null && !nativeRoot()) delegate.beginFrame();
    }
    @Override public void endFrame() {
        if (renderTarget != null) renderTarget.version++;
        else if (!nativeRoot()) delegate.endFrame();
    }
    @Override public void drawRectFromSize(Rect rect, Paint paint) {
        if (nativeRoot()) nativeRect(rect.a, rect.b, rect.a + rect.c, rect.b + rect.d, paint);
        else delegate.drawRectFromSize(rect, paint);
    }
    @Override public void setClipRect(Rect rect) {
        if (nativeRoot()) clip = rect == null ? null : new VulkanClipRect(
                rect.a, rect.b, Math.max(0, rect.c - rect.a), Math.max(0, rect.d - rect.b));
        else delegate.setClipRect(rect);
    }
    @Override public void setClipRect(RectF rect) {
        if (nativeRoot()) clip = rect == null ? null : new VulkanClipRect(
                rect.a, rect.b, Math.max(0, rect.c - rect.a), Math.max(0, rect.d - rect.b));
        else delegate.setClipRect(rect);
    }
    @Override public void drawCircle(float x, float y, float radius, Paint paint) {
        if (nativeRoot()) nativeCircle(x, y, radius, paint);
        else delegate.drawCircle(x, y, radius, paint);
    }
    @Override public void drawCircleDirect(float x, float y, float radius, Paint paint) {
        if (nativeRoot()) nativeCircle(x, y, radius, paint);
        else delegate.drawCircleDirect(x, y, radius, paint);
    }
    @Override public void drawLines(float[] points, int offset, int count, Paint paint) {
        if (nativeRoot()) {
            if (points == null) return;
            int end = Math.min(points.length, Math.max(0, offset) + Math.max(0, count));
            float size = Math.max(1.0f, paint == null ? 1.0f : paint.g());
            for (int i = Math.max(0, offset); i + 1 < end; i += 2) {
                nativeQuad(points[i] - size * 0.5f, points[i + 1] - size * 0.5f,
                        size, size, paint);
            }
        } else delegate.drawLines(points, offset, count, paint);
    }
    @Override public void save() {
        if (nativeRoot()) stateStack.push(new NativeState(transform, clip));
        else delegate.save();
    }
    @Override public void restore() {
        if (nativeRoot()) restoreNativeState();
        else delegate.restore();
    }
    @Override public void saveTransform() { save(); }
    @Override public void restoreTransform() { restore(); }
    @Override public void rotate(float angle, float pivotX, float pivotY) {
        if (nativeRoot()) transform = transform.then(
                VulkanTransform2D.rotationAround(angle, pivotX, pivotY));
        else delegate.rotate(angle, pivotX, pivotY);
    }
    @Override public void scale(float x, float y) {
        if (nativeRoot()) transform = transform.then(VulkanTransform2D.scale(x, y));
        else delegate.scale(x, y);
    }
    @Override public void scaleAround(float x, float y, float pivotX, float pivotY) {
        if (nativeRoot()) transform = transform.then(VulkanTransform2D.translation(-pivotX, -pivotY))
                .then(VulkanTransform2D.scale(x, y))
                .then(VulkanTransform2D.translation(pivotX, pivotY));
        else delegate.scaleAround(x, y, pivotX, pivotY);
    }
    @Override public void translate(float x, float y) {
        if (nativeRoot()) transform = transform.then(VulkanTransform2D.translation(x, y));
        else delegate.translate(x, y);
    }
    @Override public void runDrawTimeCallback(DrawTimeCallback callback) {
        if (nativeRoot()) callback.run(this);
        else delegate.runDrawTimeCallback(callback);
    }
    @Override public void drawLine(float startX, float startY, float endX, float endY,
                                   Paint paint) {
        if (nativeRoot()) nativeLine(startX, startY, endX, endY, paint);
        else delegate.drawLine(startX, startY, endX, endY, paint);
    }
    @Override public int getWidth() {
        if (renderTarget != null) return width;
        if (nativeRoot()) return VulkanRuntime.surfaceInfo().map(info -> info.width()).orElse(1);
        return delegate.getWidth();
    }
    @Override public int getHeight() {
        if (renderTarget != null) return height;
        if (nativeRoot()) return VulkanRuntime.surfaceInfo().map(info -> info.height()).orElse(1);
        return delegate.getHeight();
    }
    @Override public void setSize(int width, int height) {
        if (renderTarget == null && !nativeRoot()) delegate.setSize(width, height);
        else {
            this.width = Math.max(1, width);
            this.height = Math.max(1, height);
        }
    }
    @Override public void clearAlphaMap() {
        if (renderTarget != null) fillTarget(0);
        else if (nativeRoot()) VulkanRuntime.clearNativeFrame(0);
        else delegate.clearAlphaMap();
    }
    @Override public void prepareFontForPaint(Paint paint) {
        // Glyph rasterization happens when VulkanTextTextureCache first sees a string.
    }
    @Override public void flush() {
        if (renderTarget != null) renderTarget.version++;
    }
    @Override public void dispose() {
        if (renderTarget == null && !nativeRoot()) delegate.dispose();
    }
    @Override public int getTextHeight(String text, Paint paint) {
        return fontMetrics(paint).getHeight();
    }
    @Override public int getTextWidth(String text, Paint paint) {
        return fontMetrics(paint).stringWidth(text == null ? "" : text);
    }
    @Override public GameImage getErrorImage() { return errorImage; }
    @Override public void saveImageToFile(GameImage image, File file) {
        if (!nativeRoot()) delegate.saveImageToFile(image, file);
    }
    @Override public void enterLock(Lock lock) { if (!nativeRoot()) delegate.enterLock(lock); }
    @Override public void leaveLock(Lock lock) { if (!nativeRoot()) delegate.leaveLock(lock); }
    @Override public void compileShader(ShaderProgram shader) {
        if (!nativeRoot()) delegate.compileShader(shader);
    }
    @Override public float getUiScale() { return nativeRoot() ? 1.0f : delegate.getUiScale(); }

    private static FontMetrics fontMetrics(Paint paint) {
        int size = Math.max(1, Math.round(paint == null ? 16.0f : paint.k()));
        boolean bold = paint != null && paint.i() != null && paint.i().a();
        BufferedImage target = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = target.createGraphics();
        try {
            Font font = new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, size);
            return graphics.getFontMetrics(font);
        } finally {
            graphics.dispose();
        }
    }

    private void nativeRect(float left, float top, float right, float bottom, Paint paint) {
        if (paint == null || right < left || bottom < top) return;
        if (paint.d() != Paint$Style.b) {
            nativeQuad(left, top, right - left, bottom - top, paint);
        }
        if (paint.d() == Paint$Style.b || paint.d() == Paint$Style.c) {
            float thickness = Math.max(1.0f, paint.g());
            nativeQuad(left, top, right - left, thickness, paint);
            nativeQuad(left, bottom - thickness, right - left, thickness, paint);
            nativeQuad(left, top + thickness, thickness,
                    Math.max(0.0f, bottom - top - thickness * 2.0f), paint);
            nativeQuad(right - thickness, top + thickness, thickness,
                    Math.max(0.0f, bottom - top - thickness * 2.0f), paint);
        }
    }

    private void nativeLine(float x1, float y1, float x2, float y2, Paint paint) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.hypot(dx, dy);
        float thickness = Math.max(1.0f, paint == null ? 1.0f : paint.g());
        if (length < 0.0001f) {
            nativeQuad(x1 - thickness * 0.5f, y1 - thickness * 0.5f,
                    thickness, thickness, paint);
            return;
        }
        VulkanTransform2D previous = transform;
        try {
            transform = transform.then(VulkanTransform2D.rotationAround(
                    (float) Math.toDegrees(Math.atan2(dy, dx)), x1, y1));
            nativeQuad(x1, y1 - thickness * 0.5f, length, thickness, paint);
        } finally {
            transform = previous;
        }
    }

    private void nativeCircle(float x, float y, float radius, Paint paint) {
        if (radius < 0.0f || !Float.isFinite(radius)) return;
        int segments = Math.max(16, Math.min(64, (int) Math.ceil(radius)));
        if (paint != null && paint.d() == Paint$Style.b) {
            float previousX = x + radius;
            float previousY = y;
            for (int i = 1; i <= segments; i++) {
                double angle = i * Math.PI * 2.0 / segments;
                float nextX = x + (float) Math.cos(angle) * radius;
                float nextY = y + (float) Math.sin(angle) * radius;
                nativeLine(previousX, previousY, nextX, nextY, paint);
                previousX = nextX;
                previousY = nextY;
            }
            return;
        }
        float stripHeight = radius * 2.0f / segments;
        for (int i = 0; i < segments; i++) {
            float relativeY = -radius + (i + 0.5f) * stripHeight;
            float halfWidth = (float) Math.sqrt(Math.max(0.0f,
                    radius * radius - relativeY * relativeY));
            nativeQuad(x - halfWidth, y + relativeY - stripHeight * 0.5f,
                    halfWidth * 2.0f, stripHeight, paint);
        }
    }

    private void restoreNativeState() {
        if (stateStack.isEmpty()) {
            transform = VulkanTransform2D.IDENTITY;
            clip = null;
            return;
        }
        NativeState restored = stateStack.pop();
        transform = restored.transform;
        clip = restored.clip;
    }

    private static final class NativeState {
        final VulkanTransform2D transform;
        final VulkanClipRect clip;

        NativeState(VulkanTransform2D transform, VulkanClipRect clip) {
            this.transform = transform;
            this.clip = clip;
        }
    }

    private final class NativeCanvasTarget implements CanvasDrawTarget {
        @Override public void a(boolean value) { VulkanGraphicsEngine.this.a(value); }
        @Override public boolean c() { return true; }
        @Override public void clipRect(Rect rect) { setClipRect(rect); }
        @Override public void clipRect(RectF rect) { setClipRect(rect); }
        @Override public void drawImage(GameImage image, float x, float y, Paint paint) {
            drawImageRaw(image, x, y, paint);
        }
        @Override public void drawImage(GameImage image, Rect source, Rect destination,
                                        Paint paint) {
            VulkanGraphicsEngine.this.drawImage(image, source, destination, paint);
        }
        @Override public void drawImage(GameImage image, Rect source, RectF destination,
                                        Paint paint) {
            VulkanGraphicsEngine.this.drawImage(image, source, destination, paint);
        }
        @Override public void drawCircle(float x, float y, float radius, Paint paint) {
            VulkanGraphicsEngine.this.drawCircle(x, y, radius, paint);
        }
        @Override public void drawColor(int color, PorterDuff.Mode mode) {
            VulkanGraphicsEngine.this.drawColor(color, mode);
        }
        @Override public void drawColor(int color) { VulkanGraphicsEngine.this.drawColor(color); }
        @Override public void drawLine(float x1, float y1, float x2, float y2, Paint paint) {
            VulkanGraphicsEngine.this.drawLine(x1, y1, x2, y2, paint);
        }
        @Override public void drawLines(float[] points, int offset, int count, Paint paint) {
            VulkanGraphicsEngine.this.drawLines(points, offset, count, paint);
        }
        @Override public void drawRect(Rect rect, Paint paint) {
            VulkanGraphicsEngine.this.drawRect(rect, paint);
        }
        @Override public void drawRect(RectF rect, Paint paint) {
            VulkanGraphicsEngine.this.drawRect(rect, paint);
        }
        @Override public void drawText(String text, float x, float y, Paint paint) {
            VulkanGraphicsEngine.this.drawText(text, x, y, paint);
        }
        @Override public void restore() { VulkanGraphicsEngine.this.restore(); }
        @Override public void rotate(float angle, float x, float y) {
            VulkanGraphicsEngine.this.rotate(angle, x, y);
        }
        @Override public void save() { VulkanGraphicsEngine.this.save(); }
        @Override public void scale(float x, float y) { VulkanGraphicsEngine.this.scale(x, y); }
        @Override public void scale(float x, float y, float pivotX, float pivotY) {
            VulkanGraphicsEngine.this.scaleAround(x, y, pivotX, pivotY);
        }
        @Override public void setBitmapFromImage(GameImage image) { }
        @Override public void translate(float x, float y) {
            VulkanGraphicsEngine.this.translate(x, y);
        }
        @Override public void runDrawTimeCallback(DrawTimeCallback callback) {
            VulkanGraphicsEngine.this.runDrawTimeCallback(callback);
        }
        @Override public void flushBitmap(Bitmap bitmap) { }
        @Override public void enterLock(Lock lock) { VulkanGraphicsEngine.this.enterLock(lock); }
        @Override public void leaveLock(Lock lock) { VulkanGraphicsEngine.this.leaveLock(lock); }
        @Override public boolean compileShader(ShaderProgram shader) {
            VulkanGraphicsEngine.this.compileShader(shader);
            return true;
        }
    }

    private void fillTarget(int argb) {
        renderTarget.ensurePixelBuffer();
        if (renderTarget.pixelBuffer == null) {
            renderTarget.pixelBuffer = new int[Math.multiplyExact(
                    renderTarget.getWidth(), renderTarget.getHeight())];
        }
        Arrays.fill(renderTarget.pixelBuffer, argb);
        renderTarget.version++;
    }

    private void fillRect(int left, int top, int right, int bottom, Paint paint) {
        renderTarget.ensurePixelBuffer();
        int[] pixels = renderTarget.pixelBuffer;
        if (pixels == null) return;
        int imageWidth = renderTarget.getWidth();
        int imageHeight = renderTarget.getHeight();
        int x0 = Math.max(0, Math.min(imageWidth, left));
        int y0 = Math.max(0, Math.min(imageHeight, top));
        int x1 = Math.max(x0, Math.min(imageWidth, right));
        int y1 = Math.max(y0, Math.min(imageHeight, bottom));
        int color = paint == null ? 0xffffffff : paint.e();
        for (int y = y0; y < y1; y++) {
            Arrays.fill(pixels, y * imageWidth + x0, y * imageWidth + x1, color);
        }
        renderTarget.version++;
    }

    private static Rect full(GameImage image) {
        return new Rect(0, 0, image.getWidth(), image.getHeight());
    }

    private void blit(GameImage sourceImage, Rect source, int destinationX, int destinationY,
                      int destinationWidth, int destinationHeight) {
        if (sourceImage == null || source == null || destinationWidth <= 0
                || destinationHeight <= 0) return;
        GameImage sourceImageReal = sourceImage.getRealImage();
        if (sourceImageReal == null) sourceImageReal = sourceImage;
        sourceImageReal.ensurePixelBuffer();
        renderTarget.ensurePixelBuffer();
        int[] destinationPixels = renderTarget.pixelBuffer;
        if (destinationPixels == null) return;
        int targetWidth = renderTarget.getWidth();
        int targetHeight = renderTarget.getHeight();
        int sourceWidth = Math.max(1, source.c - source.a);
        int sourceHeight = Math.max(1, source.d - source.b);
        int startX = Math.max(0, -destinationX);
        int startY = Math.max(0, -destinationY);
        int endX = Math.min(destinationWidth, targetWidth - destinationX);
        int endY = Math.min(destinationHeight, targetHeight - destinationY);
        for (int y = startY; y < endY; y++) {
            int sourceY = source.b + (int) ((long) y * sourceHeight / destinationHeight);
            int targetOffset = (destinationY + y) * targetWidth + destinationX;
            for (int x = startX; x < endX; x++) {
                int sourceX = source.a + (int) ((long) x * sourceWidth / destinationWidth);
                destinationPixels[targetOffset + x] = sourceImageReal.getPixel(sourceX, sourceY);
            }
        }
        renderTarget.version++;
    }
}
