package io.github.endx.rustedfabricapi.api.client.render;

import java.util.Objects;

import android.graphics.Paint;
import android.graphics.Paint$Align;
import android.graphics.Paint$Style;

/** Immutable color, stroke, antialiasing, and text configuration. */
public final class DrawStyle {
    public static final DrawStyle WHITE_FILL = fill(ArgbColor.WHITE);

    private final int color;
    private final boolean filled;
    private final float strokeWidth;
    private final boolean antiAlias;
    private final float textSize;
    private final TextAlignment textAlignment;

    private DrawStyle(int color, boolean filled, float strokeWidth, boolean antiAlias,
            float textSize, TextAlignment textAlignment) {
        if (!Float.isFinite(strokeWidth) || strokeWidth <= 0.0F) {
            throw new IllegalArgumentException("strokeWidth must be finite and positive");
        }
        if (!Float.isFinite(textSize) || textSize <= 0.0F) {
            throw new IllegalArgumentException("textSize must be finite and positive");
        }
        this.color = color;
        this.filled = filled;
        this.strokeWidth = strokeWidth;
        this.antiAlias = antiAlias;
        this.textSize = textSize;
        this.textAlignment = Objects.requireNonNull(textAlignment, "textAlignment");
    }

    public static DrawStyle fill(int color) {
        return new DrawStyle(color, true, 1.0F, true, 16.0F, TextAlignment.LEFT);
    }

    public static DrawStyle stroke(int color, float width) {
        return new DrawStyle(color, false, width, true, 16.0F, TextAlignment.LEFT);
    }

    public static DrawStyle text(int color, float size) {
        return fill(color).withTextSize(size);
    }

    public int color() { return color; }
    public boolean filled() { return filled; }
    public float strokeWidth() { return strokeWidth; }
    public boolean antiAlias() { return antiAlias; }
    public float textSize() { return textSize; }
    public TextAlignment textAlignment() { return textAlignment; }

    public DrawStyle withColor(int value) {
        return copy(value, filled, strokeWidth, antiAlias, textSize, textAlignment);
    }

    public DrawStyle withAlpha(int alpha) {
        return withColor(ArgbColor.withAlpha(color, alpha));
    }

    public DrawStyle asFill() {
        return copy(color, true, strokeWidth, antiAlias, textSize, textAlignment);
    }

    public DrawStyle asStroke(float width) {
        return copy(color, false, width, antiAlias, textSize, textAlignment);
    }

    public DrawStyle withAntiAlias(boolean value) {
        return copy(color, filled, strokeWidth, value, textSize, textAlignment);
    }

    public DrawStyle withTextSize(float value) {
        return copy(color, filled, strokeWidth, antiAlias, value, textAlignment);
    }

    public DrawStyle withTextAlignment(TextAlignment value) {
        return copy(color, filled, strokeWidth, antiAlias, textSize,
                Objects.requireNonNull(value, "value"));
    }

    private DrawStyle copy(int newColor, boolean newFilled, float newStrokeWidth,
            boolean newAntiAlias, float newTextSize, TextAlignment newTextAlignment) {
        return new DrawStyle(newColor, newFilled, newStrokeWidth, newAntiAlias,
                newTextSize, newTextAlignment);
    }

    Paint createNativePaint() {
        Paint paint = new Paint();
        paint.b(color); // Android Paint#setColor in Rusted Warfare's desktop compatibility class.
        paint.a(antiAlias); // setAntiAlias
        paint.a(strokeWidth); // setStrokeWidth
        paint.a(filled ? Paint$Style.a : Paint$Style.b); // FILL / STROKE
        paint.b(textSize); // setTextSize
        switch (textAlignment) {
            case CENTER:
                paint.a(Paint$Align.b);
                break;
            case RIGHT:
                paint.a(Paint$Align.c);
                break;
            case LEFT:
            default:
                paint.a(Paint$Align.a);
                break;
        }
        return paint;
    }

    @Override
    public String toString() {
        return "DrawStyle{color=0x" + Integer.toHexString(color)
                + ", filled=" + filled
                + ", strokeWidth=" + strokeWidth
                + ", antiAlias=" + antiAlias
                + ", textSize=" + textSize
                + ", textAlignment=" + textAlignment + '}';
    }
}
