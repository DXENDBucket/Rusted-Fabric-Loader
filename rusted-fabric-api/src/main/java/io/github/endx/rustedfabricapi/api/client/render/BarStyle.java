package io.github.endx.rustedfabricapi.api.client.render;

import java.util.Objects;

/** Immutable colors, border and direction used when rasterizing a bar image. */
public final class BarStyle {
    private final int fillColor;
    private final int backgroundColor;
    private final int borderColor;
    private final int borderWidth;
    private final BarDirection direction;

    public BarStyle(int fillColor, int backgroundColor, int borderColor, int borderWidth,
                    BarDirection direction) {
        if (borderWidth < 0) throw new IllegalArgumentException("borderWidth must not be negative");
        this.fillColor = fillColor;
        this.backgroundColor = backgroundColor;
        this.borderColor = borderColor;
        this.borderWidth = borderWidth;
        this.direction = Objects.requireNonNull(direction, "direction");
    }

    public int fillColor() { return fillColor; }
    public int backgroundColor() { return backgroundColor; }
    public int borderColor() { return borderColor; }
    public int borderWidth() { return borderWidth; }
    public BarDirection direction() { return direction; }
}
