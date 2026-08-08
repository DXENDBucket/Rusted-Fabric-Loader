package io.github.endx.rustedfabricapi.api.path;

import io.github.endx.rustedfabricapi.api.map.Maps;
import rustedwarfare.unit.MovementType;

import java.util.Objects;

/** Immutable native path request description. Coordinates are map tiles. */
public final class PathQuery {
    private final MovementType movementType;
    private final int startTileX;
    private final int startTileY;
    private final int endTileX;
    private final int endTileY;
    private final int endRadius;
    private final Float startDirection;
    private final boolean lowPriority;
    private final boolean refreshCosts;

    private PathQuery(MovementType movementType, int startTileX, int startTileY,
            int endTileX, int endTileY, int endRadius, Float startDirection,
            boolean lowPriority, boolean refreshCosts) {
        this.movementType = Objects.requireNonNull(movementType, "movementType");
        requireShortCoordinate(startTileX, "startTileX");
        requireShortCoordinate(startTileY, "startTileY");
        requireShortCoordinate(endTileX, "endTileX");
        requireShortCoordinate(endTileY, "endTileY");
        if (endRadius < 0 || endRadius > Short.MAX_VALUE) {
            throw new IllegalArgumentException("endRadius must be between 0 and " + Short.MAX_VALUE);
        }
        if (startDirection != null && !Float.isFinite(startDirection.floatValue())) {
            throw new IllegalArgumentException("startDirection must be finite");
        }
        this.startTileX = startTileX;
        this.startTileY = startTileY;
        this.endTileX = endTileX;
        this.endTileY = endTileY;
        this.endRadius = endRadius;
        this.startDirection = startDirection;
        this.lowPriority = lowPriority;
        this.refreshCosts = refreshCosts;
    }

    public static PathQuery betweenTiles(MovementType movementType, int startTileX,
            int startTileY, int endTileX, int endTileY) {
        return new PathQuery(movementType, startTileX, startTileY, endTileX, endTileY,
                0, null, false, false);
    }

    public static PathQuery betweenWorld(MovementType movementType, float startX,
            float startY, float endX, float endY) {
        requireFinite(startX, "startX");
        requireFinite(startY, "startY");
        requireFinite(endX, "endX");
        requireFinite(endY, "endY");
        return betweenTiles(movementType, Maps.worldToTileX(startX), Maps.worldToTileY(startY),
                Maps.worldToTileX(endX), Maps.worldToTileY(endY));
    }

    public PathQuery withEndRadius(int endRadius) {
        return new PathQuery(movementType, startTileX, startTileY, endTileX, endTileY,
                endRadius, startDirection, lowPriority, refreshCosts);
    }

    public PathQuery withStartDirection(float startDirection) {
        return new PathQuery(movementType, startTileX, startTileY, endTileX, endTileY,
                endRadius, Float.valueOf(startDirection), lowPriority, refreshCosts);
    }

    public PathQuery lowPriority(boolean lowPriority) {
        return new PathQuery(movementType, startTileX, startTileY, endTileX, endTileY,
                endRadius, startDirection, lowPriority, refreshCosts);
    }

    /** Routes the request through the native forced cost-refresh path before solving. */
    public PathQuery refreshCosts(boolean refreshCosts) {
        return new PathQuery(movementType, startTileX, startTileY, endTileX, endTileY,
                endRadius, startDirection, lowPriority, refreshCosts);
    }

    public MovementType movementType() { return movementType; }
    public int startTileX() { return startTileX; }
    public int startTileY() { return startTileY; }
    public int endTileX() { return endTileX; }
    public int endTileY() { return endTileY; }
    public int endRadius() { return endRadius; }
    public Float startDirection() { return startDirection; }
    public boolean lowPriority() { return lowPriority; }
    public boolean refreshCosts() { return refreshCosts; }

    void validateCurrentMap() {
        if (!Maps.containsTile(startTileX, startTileY)) {
            throw new IllegalArgumentException("start tile is outside the current map");
        }
        if (!Maps.containsTile(endTileX, endTileY)) {
            throw new IllegalArgumentException("end tile is outside the current map");
        }
    }

    private static void requireShortCoordinate(int value, String name) {
        if (value < 0 || value > Short.MAX_VALUE) {
            throw new IllegalArgumentException(name + " must be between 0 and " + Short.MAX_VALUE);
        }
    }

    private static void requireFinite(float value, String name) {
        if (!Float.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
    }
}
