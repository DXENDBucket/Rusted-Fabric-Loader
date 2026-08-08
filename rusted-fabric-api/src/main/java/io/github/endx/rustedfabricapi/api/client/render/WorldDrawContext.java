package io.github.endx.rustedfabricapi.api.client.render;

import java.util.Objects;
import java.util.function.Consumer;

import io.github.endx.rustedfabricapi.api.world.WorldPoint;
import rustedwarfare.render.GraphicsEngine;

/**
 * Frame-scoped world-overlay drawing facade.
 *
 * <p>Positions and geometry sizes use world units. Stroke widths, text sizes, padding, and
 * naturally sized images remain screen-pixel sized, so overlays stay readable at every zoom.</p>
 */
public final class WorldDrawContext {
    private final HudDrawContext screen;
    private final WorldViewport viewport;

    public WorldDrawContext(GraphicsEngine graphics, WorldViewport viewport, float delta) {
        this.screen = new HudDrawContext(graphics, delta);
        this.viewport = Objects.requireNonNull(viewport, "viewport");
    }

    public GraphicsEngine graphics() { return screen.graphics(); }
    public float delta() { return screen.delta(); }
    public WorldViewport viewport() { return viewport; }

    /** Screen-space access using the same frame-local paint cache and viewport clip. */
    public HudDrawContext screen() { return screen; }

    public ScreenPosition worldToScreen(float worldX, float worldY) {
        return viewport.worldToScreen(worldX, worldY);
    }

    public ScreenPosition worldToScreen(WorldPoint point) {
        return viewport.worldToScreen(point);
    }

    public WorldPoint screenToWorld(float screenX, float screenY) {
        return viewport.screenToWorld(screenX, screenY);
    }

    public boolean isVisible(float worldX, float worldY, float radius) {
        return viewport.isVisible(worldX, worldY, radius);
    }

    public void drawText(String text, float worldX, float worldY, DrawStyle style) {
        ScreenPosition point = worldToScreen(worldX, worldY);
        screen.drawText(text, point.x(), point.y(), style);
    }

    public void drawTextWithBackground(String text, float worldX, float worldY,
            DrawStyle textStyle, DrawStyle backgroundStyle, float paddingPixels) {
        ScreenPosition point = worldToScreen(worldX, worldY);
        screen.drawTextWithBackground(text, point.x(), point.y(), textStyle,
                backgroundStyle, paddingPixels);
    }

    public int textWidth(String text, DrawStyle style) { return screen.textWidth(text, style); }
    public int textHeight(String text, DrawStyle style) { return screen.textHeight(text, style); }

    public void drawLine(float startWorldX, float startWorldY,
            float endWorldX, float endWorldY, DrawStyle style) {
        ScreenPosition start = worldToScreen(startWorldX, startWorldY);
        ScreenPosition end = worldToScreen(endWorldX, endWorldY);
        screen.drawLine(start.x(), start.y(), end.x(), end.y(), style);
    }

    public void drawRect(float worldX, float worldY, float worldWidth, float worldHeight,
            DrawStyle style) {
        ScreenPosition point = worldToScreen(worldX, worldY);
        screen.drawRect(point.x(), point.y(), pixels(worldWidth, "worldWidth"),
                pixels(worldHeight, "worldHeight"), style);
    }

    public void fillRect(float worldX, float worldY, float worldWidth, float worldHeight,
            int color) {
        drawRect(worldX, worldY, worldWidth, worldHeight, DrawStyle.fill(color));
    }

    public void strokeRect(float worldX, float worldY, float worldWidth, float worldHeight,
            int color, float strokeWidthPixels) {
        drawRect(worldX, worldY, worldWidth, worldHeight,
                DrawStyle.stroke(color, strokeWidthPixels));
    }

    public void drawCircle(float centerWorldX, float centerWorldY, float radiusWorld,
            DrawStyle style) {
        ScreenPosition point = worldToScreen(centerWorldX, centerWorldY);
        screen.drawCircle(point.x(), point.y(), pixels(radiusWorld, "radiusWorld"), style);
    }

    /** Draws an image centered at a world position at its natural screen-pixel size. */
    public void drawImageCentered(ClientImage image, float centerWorldX, float centerWorldY,
            DrawStyle style) {
        ScreenPosition point = worldToScreen(centerWorldX, centerWorldY);
        screen.drawImageCentered(image, point.x(), point.y(), style);
    }

    /** Draws a naturally sized image centered and rotated around a world position. */
    public void drawImageRotated(ClientImage image, float centerWorldX, float centerWorldY,
            float degrees, DrawStyle style) {
        ScreenPosition point = worldToScreen(centerWorldX, centerWorldY);
        screen.drawImageRotated(image, point.x(), point.y(), degrees, style);
    }

    /** Draws an image into a rectangle whose dimensions are measured in world units. */
    public void drawImageInWorld(ClientImage image, float worldX, float worldY,
            float worldWidth, float worldHeight, DrawStyle style) {
        ScreenPosition point = worldToScreen(worldX, worldY);
        screen.drawImageScaled(image, point.x(), point.y(), pixels(worldWidth, "worldWidth"),
                pixels(worldHeight, "worldHeight"), style);
    }

    public void drawImageRegionInWorld(ClientImage image, int sourceX, int sourceY,
            int sourceWidth, int sourceHeight, float worldX, float worldY,
            float worldWidth, float worldHeight, DrawStyle style) {
        ScreenPosition point = worldToScreen(worldX, worldY);
        screen.drawImageRegion(image, sourceX, sourceY, sourceWidth, sourceHeight,
                point.x(), point.y(), pixels(worldWidth, "worldWidth"),
                pixels(worldHeight, "worldHeight"), style);
    }

    /** Restricts drawing to a world-space rectangle and always restores the previous clip. */
    public void withClip(float worldX, float worldY, float worldWidth, float worldHeight,
            Consumer<WorldDrawContext> drawing) {
        Objects.requireNonNull(drawing, "drawing");
        ScreenPosition point = worldToScreen(worldX, worldY);
        screen.withClip(point.x(), point.y(), pixels(worldWidth, "worldWidth"),
                pixels(worldHeight, "worldHeight"), ignored -> drawing.accept(this));
    }

    private float pixels(float worldLength, String name) {
        if (!Float.isFinite(worldLength) || worldLength < 0.0F) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
        return viewport.worldLengthToPixels(worldLength);
    }
}
