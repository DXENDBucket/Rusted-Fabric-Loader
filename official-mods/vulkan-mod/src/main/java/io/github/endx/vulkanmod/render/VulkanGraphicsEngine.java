package io.github.endx.vulkanmod.render;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import io.github.endx.vulkanmod.VulkanRuntime;
import rustedwarfare.client.render.GameImage;
import rustedwarfare.client.render.SlickGraphicsBackend;
import rustedwarfare.render.AndroidGlRenderer;
import rustedwarfare.render.CanvasDrawTarget;
import rustedwarfare.render.DrawTimeCallback;
import rustedwarfare.render.GraphicsEngine;
import rustedwarfare.render.ShaderProgram;

import java.io.File;
import java.io.InputStream;
import java.util.concurrent.locks.Lock;

/**
 * Renderer selected by the game in native Vulkan mode.
 *
 * <p>The first migration stage deliberately delegates operations to Slick. This changes ownership
 * at the game's {@link GraphicsEngine} boundary now, so individual operations can move to Vulkan
 * without repeatedly patching game call sites. The compatibility delegate is removed only after
 * native images, offscreen targets, fonts and LibRocket no longer require it.</p>
 */
public final class VulkanGraphicsEngine implements GraphicsEngine {
    private final GraphicsEngine delegate;
    private final SlickGraphicsBackend slickDelegate;

    public VulkanGraphicsEngine() {
        this(new SlickGraphicsBackend());
        VulkanRuntime.onGraphicsEngineInstalled();
    }

    private VulkanGraphicsEngine(GraphicsEngine delegate) {
        if (delegate == null) throw new NullPointerException("delegate");
        if (!(delegate instanceof SlickGraphicsBackend)) {
            throw new IllegalArgumentException("Expected Slick compatibility backend, got "
                    + delegate.getClass().getName());
        }
        this.delegate = delegate;
        this.slickDelegate = (SlickGraphicsBackend) delegate;
    }

    public SlickGraphicsBackend compatibilityDelegate() {
        return slickDelegate;
    }

    public static GraphicsEngine unwrapCompatibility(GraphicsEngine engine) {
        return engine instanceof VulkanGraphicsEngine
                ? ((VulkanGraphicsEngine) engine).delegate : engine;
    }

    private static VulkanGraphicsEngine wrap(GraphicsEngine engine) {
        return engine == null ? null : new VulkanGraphicsEngine(engine);
    }

    @Override public GraphicsEngine createBackendForImage(GameImage image) {
        return wrap(delegate.createBackendForImage(image));
    }

    @Override public GraphicsEngine createChildBackendForImage(GameImage image) {
        return wrap(delegate.createChildBackendForImage(image));
    }

    @Override public boolean a() { return delegate.a(); }
    @Override public void a(Context context) { delegate.a(context); }
    @Override public void b() { delegate.b(); }
    @Override public CanvasDrawTarget d() { return delegate.d(); }
    @Override public void a(CanvasDrawTarget target) { delegate.a(target); }
    @Override public void a(AndroidGlRenderer renderer) { delegate.a(renderer); }
    @Override public GameImage a(int resourceId) { return delegate.a(resourceId); }
    @Override public GameImage a(int resourceId, boolean option) {
        return delegate.a(resourceId, option);
    }
    @Override public GameImage loadImageFromStream(InputStream input, boolean option) {
        return delegate.loadImageFromStream(input, option);
    }
    @Override public GameImage createImage(int width, int height, boolean alpha) {
        return delegate.createImage(width, height, alpha);
    }
    @Override public GameImage b(int width, int height, boolean alpha) {
        return delegate.b(width, height, alpha);
    }
    @Override public void e() { delegate.e(); }
    @Override public void drawImageRotated(GameImage image, float x, float y, float angle,
                                           Paint paint) {
        delegate.drawImageRotated(image, x, y, angle, paint);
    }
    @Override public void drawImageSectionRotated(GameImage image, Rect source, float x, float y,
                                                  float angle, Paint paint) {
        delegate.drawImageSectionRotated(image, source, x, y, angle, paint);
    }
    @Override public void drawImage(GameImage image, Rect source, Rect destination, Paint paint) {
        delegate.drawImage(image, source, destination, paint);
    }
    @Override public void drawImage(GameImage image, Rect source, RectF destination, Paint paint) {
        delegate.drawImage(image, source, destination, paint);
    }
    @Override public void drawImageCentered(GameImage image, float x, float y, Paint paint) {
        delegate.drawImageCentered(image, x, y, paint);
    }
    @Override public void drawImageTransformed(GameImage image, float x, float y, Paint paint,
                                               float scale, float angle) {
        delegate.drawImageTransformed(image, x, y, paint, scale, angle);
    }
    @Override public void drawImageRaw(GameImage image, float x, float y, Paint paint) {
        delegate.drawImageRaw(image, x, y, paint);
    }
    @Override public void drawImageDirect(GameImage image, Rect source, Rect destination,
                                          Paint paint) {
        delegate.drawImageDirect(image, source, destination, paint);
    }
    @Override public void drawRect(Rect rect, Paint paint) { delegate.drawRect(rect, paint); }
    @Override public void a(boolean value) { delegate.a(value); }
    @Override public void f() { delegate.f(); }
    @Override public void drawTiledImage(GameImage image, Rect destination, Paint paint) {
        delegate.drawTiledImage(image, destination, paint);
    }
    @Override public void drawTiledImage(GameImage image, Rect destination, Paint paint,
                                         int offsetX, int offsetY, int stepX, int stepY) {
        delegate.drawTiledImage(image, destination, paint, offsetX, offsetY, stepX, stepY);
    }
    @Override public void drawTiledImage(GameImage image, RectF destination, Paint paint,
                                         float offsetX, float offsetY, int stepX, int stepY) {
        delegate.drawTiledImage(image, destination, paint, offsetX, offsetY, stepX, stepY);
    }
    @Override public void drawColor(int color) { delegate.drawColor(color); }
    @Override public void drawColor(int color, PorterDuff.Mode mode) {
        delegate.drawColor(color, mode);
    }
    @Override public void drawTextWithBackground(String text, float x, float y, Paint textPaint,
                                                 Paint backgroundPaint, float padding) {
        delegate.drawTextWithBackground(text, x, y, textPaint, backgroundPaint, padding);
    }
    @Override public void drawText(String text, float x, float y, Paint paint) {
        delegate.drawText(text, x, y, paint);
    }
    @Override public void drawRectDirect(Rect rect, Paint paint) {
        delegate.drawRectDirect(rect, paint);
    }
    @Override public void drawRect(RectF rect, Paint paint) { delegate.drawRect(rect, paint); }
    @Override public void beginFrame() { delegate.beginFrame(); }
    @Override public void endFrame() { delegate.endFrame(); }
    @Override public void drawRectFromSize(Rect rect, Paint paint) {
        delegate.drawRectFromSize(rect, paint);
    }
    @Override public void setClipRect(Rect rect) { delegate.setClipRect(rect); }
    @Override public void setClipRect(RectF rect) { delegate.setClipRect(rect); }
    @Override public void drawCircle(float x, float y, float radius, Paint paint) {
        delegate.drawCircle(x, y, radius, paint);
    }
    @Override public void drawCircleDirect(float x, float y, float radius, Paint paint) {
        delegate.drawCircleDirect(x, y, radius, paint);
    }
    @Override public void drawLines(float[] points, int offset, int count, Paint paint) {
        delegate.drawLines(points, offset, count, paint);
    }
    @Override public void save() { delegate.save(); }
    @Override public void restore() { delegate.restore(); }
    @Override public void saveTransform() { delegate.saveTransform(); }
    @Override public void restoreTransform() { delegate.restoreTransform(); }
    @Override public void rotate(float angle, float pivotX, float pivotY) {
        delegate.rotate(angle, pivotX, pivotY);
    }
    @Override public void scale(float x, float y) { delegate.scale(x, y); }
    @Override public void scaleAround(float x, float y, float pivotX, float pivotY) {
        delegate.scaleAround(x, y, pivotX, pivotY);
    }
    @Override public void translate(float x, float y) { delegate.translate(x, y); }
    @Override public void runDrawTimeCallback(DrawTimeCallback callback) {
        delegate.runDrawTimeCallback(callback);
    }
    @Override public void drawLine(float startX, float startY, float endX, float endY,
                                   Paint paint) {
        delegate.drawLine(startX, startY, endX, endY, paint);
    }
    @Override public int getWidth() { return delegate.getWidth(); }
    @Override public int getHeight() { return delegate.getHeight(); }
    @Override public void setSize(int width, int height) { delegate.setSize(width, height); }
    @Override public void clearAlphaMap() { delegate.clearAlphaMap(); }
    @Override public void prepareFontForPaint(Paint paint) { delegate.prepareFontForPaint(paint); }
    @Override public void flush() { delegate.flush(); }
    @Override public void dispose() { delegate.dispose(); }
    @Override public int getTextHeight(String text, Paint paint) {
        return delegate.getTextHeight(text, paint);
    }
    @Override public int getTextWidth(String text, Paint paint) {
        return delegate.getTextWidth(text, paint);
    }
    @Override public GameImage getErrorImage() { return delegate.getErrorImage(); }
    @Override public void saveImageToFile(GameImage image, File file) {
        delegate.saveImageToFile(image, file);
    }
    @Override public void enterLock(Lock lock) { delegate.enterLock(lock); }
    @Override public void leaveLock(Lock lock) { delegate.leaveLock(lock); }
    @Override public void compileShader(ShaderProgram shader) { delegate.compileShader(shader); }
    @Override public float getUiScale() { return delegate.getUiScale(); }
}
