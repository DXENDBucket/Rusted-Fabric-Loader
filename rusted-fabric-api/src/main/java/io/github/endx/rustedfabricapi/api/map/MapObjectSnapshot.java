package io.github.endx.rustedfabricapi.api.map;

import java.util.Objects;
import java.util.Optional;

import io.github.endx.rustedfabricapi.api.world.WorldPoint;

/** Immutable TMX object after the game has applied its map-scale coordinate conversion. */
public final class MapObjectSnapshot {
    private final int groupIndex;
    private final String groupName;
    private final int index;
    private final String name;
    private final String type;
    private final float x;
    private final float y;
    private final float width;
    private final float height;
    private final float rotation;
    private final int gid;
    private final int tileIndex;
    private final String tilesetName;
    private final String tilesetImageSource;
    private final MapProperties properties;

    public MapObjectSnapshot(int groupIndex, String groupName, int index, String name, String type,
            float x, float y, float width, float height, float rotation, int gid, int tileIndex,
            String tilesetName, String tilesetImageSource, MapProperties properties) {
        if (groupIndex < 0 || index < 0) {
            throw new IllegalArgumentException("groupIndex and index must be non-negative");
        }
        requireFinite(x, "x");
        requireFinite(y, "y");
        requireFinite(width, "width");
        requireFinite(height, "height");
        requireFinite(rotation, "rotation");
        if (width < 0.0F || height < 0.0F) {
            throw new IllegalArgumentException("width and height must be non-negative");
        }
        this.groupIndex = groupIndex;
        this.groupName = clean(groupName);
        this.index = index;
        this.name = clean(name);
        this.type = clean(type);
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.rotation = rotation;
        this.gid = gid;
        this.tileIndex = tileIndex;
        this.tilesetName = clean(tilesetName);
        this.tilesetImageSource = clean(tilesetImageSource);
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    public int groupIndex() { return groupIndex; }
    public String groupName() { return groupName; }
    public int index() { return index; }
    public String name() { return name; }
    public Optional<String> optionalName() { return optional(name); }
    public String type() { return type; }
    public Optional<String> optionalType() { return optional(type); }
    public float x() { return x; }
    public float y() { return y; }
    public float width() { return width; }
    public float height() { return height; }
    public float rotation() { return rotation; }
    public float right() { return x + width; }
    public float bottom() { return y + height; }
    public WorldPoint center() { return new WorldPoint(x + width * 0.5F, y + height * 0.5F); }
    public boolean tileObject() { return gid >= 0; }
    public Optional<Integer> gid() { return gid >= 0 ? Optional.of(gid) : Optional.empty(); }
    public Optional<Integer> tileIndex() {
        return tileIndex >= 0 ? Optional.of(tileIndex) : Optional.empty();
    }
    public Optional<String> tilesetName() { return optional(tilesetName); }
    public Optional<String> tilesetImageSource() { return optional(tilesetImageSource); }
    public MapProperties properties() { return properties; }

    /** Axis-aligned TMX bounds; rotation is reported separately and is not applied here. */
    public boolean contains(float worldX, float worldY) {
        if (!Float.isFinite(worldX) || !Float.isFinite(worldY)) return false;
        if (width == 0.0F && height == 0.0F) return worldX == x && worldY == y;
        return worldX >= x && worldY >= y && worldX <= right() && worldY <= bottom();
    }

    public boolean intersects(float left, float top, float right, float bottom) {
        if (!Float.isFinite(left) || !Float.isFinite(top) || !Float.isFinite(right)
                || !Float.isFinite(bottom) || right < left || bottom < top) return false;
        return this.right() >= left && this.bottom() >= top && x <= right && y <= bottom;
    }

    private static String clean(String value) { return value != null ? value : ""; }
    private static Optional<String> optional(String value) {
        return value.isEmpty() ? Optional.empty() : Optional.of(value);
    }
    private static void requireFinite(float value, String name) {
        if (!Float.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
    }

    @Override
    public String toString() {
        return "MapObjectSnapshot{" + groupIndex + ':' + index + ", name='" + name
                + "', type='" + type + "', bounds=" + x + ',' + y + ',' + width + ',' + height
                + '}';
    }
}
