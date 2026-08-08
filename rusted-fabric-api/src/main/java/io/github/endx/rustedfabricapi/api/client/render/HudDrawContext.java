package io.github.endx.rustedfabricapi.api.client.render;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import rustedwarfare.client.render.GameImage;
import rustedwarfare.render.GraphicsEngine;

/** Frame-scoped screen-space drawing facade that does not expose Android paint/rectangle types. */
public final class HudDrawContext {
    private final GraphicsEngine graphics;
    private final float delta;
    private final Map<DrawStyle, Paint> paints = new IdentityHashMap<DrawStyle, Paint>();
    private final Map<DrawStyle, Boolean> preparedTextStyles =
            new IdentityHashMap<DrawStyle, Boolean>();

    public HudDrawContext(GraphicsEngine graphics, float delta) {
        this.graphics = Objects.requireNonNull(graphics, "graphics");
        requireFinite(delta, "delta");
        this.delta = delta;
    }

    public GraphicsEngine graphics() { return graphics; }
    public float delta() { return delta; }
    public int width() { return graphics.getWidth(); }
    public int height() { return graphics.getHeight(); }
    public float uiScale() { return graphics.getUiScale(); }

    public void drawText(String text, float x, float y, DrawStyle style) {
        requirePoint(x, y);
        graphics.drawText(Objects.requireNonNull(text, "text"), x, y, textPaint(style));
    }

    public void drawTextWithBackground(String text, float x, float y, DrawStyle textStyle,
            DrawStyle backgroundStyle, float padding) {
        requirePoint(x, y);
        requireNonNegative(padding, "padding");
        graphics.drawTextWithBackground(Objects.requireNonNull(text, "text"), x, y,
                textPaint(textStyle), paint(backgroundStyle), padding);
    }

    public int textWidth(String text, DrawStyle style) {
        return graphics.getTextWidth(Objects.requireNonNull(text, "text"), textPaint(style));
    }

    public int textHeight(String text, DrawStyle style) {
        return graphics.getTextHeight(Objects.requireNonNull(text, "text"), textPaint(style));
    }

    public void drawRect(float x, float y, float width, float height, DrawStyle style) {
        requireRect(x, y, width, height);
        graphics.drawRect(new RectF(x, y, x + width, y + height), paint(style));
    }

    public void fillRect(float x, float y, float width, float height, int color) {
        drawRect(x, y, width, height, DrawStyle.fill(color));
    }

    public void strokeRect(float x, float y, float width, float height,
            int color, float strokeWidth) {
        drawRect(x, y, width, height, DrawStyle.stroke(color, strokeWidth));
    }

    public void drawLine(float startX, float startY, float endX, float endY, DrawStyle style) {
        requirePoint(startX, startY);
        requirePoint(endX, endY);
        graphics.drawLine(startX, startY, endX, endY, paint(style));
    }

    public void drawCircle(float centerX, float centerY, float radius, DrawStyle style) {
        requirePoint(centerX, centerY);
        requireNonNegative(radius, "radius");
        graphics.drawCircle(centerX, centerY, radius, paint(style));
    }

    public void drawImage(ClientImage image, float x, float y) {
        drawImage(image, x, y, DrawStyle.WHITE_FILL);
    }

    public void drawImage(ClientImage image, float x, float y, DrawStyle style) {
        requirePoint(x, y);
        graphics.drawImageRaw(Objects.requireNonNull(image, "image").requireOpen(),
                x, y, paint(style));
    }

    public void drawImageCentered(ClientImage image, float centerX, float centerY,
            DrawStyle style) {
        requirePoint(centerX, centerY);
        graphics.drawImageCentered(Objects.requireNonNull(image, "image").requireOpen(),
                centerX, centerY, paint(style));
    }

    public void drawImageRotated(ClientImage image, float centerX, float centerY,
            float degrees, DrawStyle style) {
        requirePoint(centerX, centerY);
        requireFinite(degrees, "degrees");
        graphics.drawImageRotated(Objects.requireNonNull(image, "image").requireOpen(),
                centerX, centerY, degrees, paint(style));
    }

    public void drawImageScaled(ClientImage image, float x, float y, float width, float height,
            DrawStyle style) {
        ClientImage checked = Objects.requireNonNull(image, "image");
        requireRect(x, y, width, height);
        GameImage nativeImage = checked.requireOpen();
        graphics.drawImage(nativeImage, new Rect(0, 0, checked.width(), checked.height()),
                new RectF(x, y, x + width, y + height), paint(style));
    }

    public void drawImageRegion(ClientImage image, int sourceX, int sourceY,
            int sourceWidth, int sourceHeight, float x, float y, float width, float height,
            DrawStyle style) {
        ClientImage checked = Objects.requireNonNull(image, "image");
        requireSourceRect(checked, sourceX, sourceY, sourceWidth, sourceHeight);
        requireRect(x, y, width, height);
        graphics.drawImage(checked.requireOpen(),
                new Rect(sourceX, sourceY, sourceX + sourceWidth, sourceY + sourceHeight),
                new RectF(x, y, x + width, y + height), paint(style));
    }

    /** Restores the previous clip even when drawing throws. */
    public void withClip(float x, float y, float width, float height,
            Consumer<HudDrawContext> drawing) {
        requireRect(x, y, width, height);
        Objects.requireNonNull(drawing, "drawing");
        graphics.save();
        try {
            graphics.setClipRect(new RectF(x, y, x + width, y + height));
            drawing.accept(this);
        } finally {
            graphics.restore();
        }
    }

    /** Applies translate, scale, then rotation and always restores the previous transform. */
    public void transformed(float translateX, float translateY, float scaleX, float scaleY,
            float rotationDegrees, Consumer<HudDrawContext> drawing) {
        requirePoint(translateX, translateY);
        requireFinite(scaleX, "scaleX");
        requireFinite(scaleY, "scaleY");
        requireFinite(rotationDegrees, "rotationDegrees");
        if (scaleX == 0.0F || scaleY == 0.0F) {
            throw new IllegalArgumentException("scale must be non-zero");
        }
        Objects.requireNonNull(drawing, "drawing");
        graphics.saveTransform();
        try {
            graphics.translate(translateX, translateY);
            graphics.scale(scaleX, scaleY);
            if (rotationDegrees != 0.0F) graphics.rotate(rotationDegrees, 0.0F, 0.0F);
            drawing.accept(this);
        } finally {
            graphics.restoreTransform();
        }
    }

    private Paint paint(DrawStyle style) {
        DrawStyle checked = Objects.requireNonNull(style, "style");
        Paint paint = paints.get(checked);
        if (paint == null) {
            paint = checked.createNativePaint();
            paints.put(checked, paint);
        }
        return paint;
    }

    private Paint textPaint(DrawStyle style) {
        DrawStyle checked = Objects.requireNonNull(style, "style");
        Paint paint = paint(checked);
        if (!preparedTextStyles.containsKey(checked)) {
            graphics.prepareFontForPaint(paint);
            preparedTextStyles.put(checked, Boolean.TRUE);
        }
        return paint;
    }

    private static void requireSourceRect(ClientImage image, int x, int y, int width, int height) {
        if (x < 0 || y < 0 || width <= 0 || height <= 0
                || (long) x + width > image.width() || (long) y + height > image.height()) {
            throw new IllegalArgumentException("source rectangle is outside the image");
        }
    }

    private static void requireRect(float x, float y, float width, float height) {
        requirePoint(x, y);
        requireNonNegative(width, "width");
        requireNonNegative(height, "height");
    }

    private static void requirePoint(float x, float y) {
        requireFinite(x, "x");
        requireFinite(y, "y");
    }

    private static void requireNonNegative(float value, String name) {
        requireFinite(value, name);
        if (value < 0.0F) throw new IllegalArgumentException(name + " must be non-negative");
    }

    private static void requireFinite(float value, String name) {
        if (!Float.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
    }
}
