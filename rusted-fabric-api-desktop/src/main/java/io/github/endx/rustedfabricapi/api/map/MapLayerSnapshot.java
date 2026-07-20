package io.github.endx.rustedfabricapi.api.map;

import java.util.Objects;
import java.util.Optional;

/** Immutable metadata for one loaded TMX tile layer. */
public final class MapLayerSnapshot {
    private final int index;
    private final String name;
    private final int width;
    private final int height;
    private final boolean visible;
    private final boolean groundLayer;
    private final boolean itemsLayer;
    private final boolean hasNonAtlasTiles;
    private final MapProperties properties;

    public MapLayerSnapshot(int index, String name, int width, int height, boolean visible,
            boolean groundLayer, boolean itemsLayer, boolean hasNonAtlasTiles,
            MapProperties properties) {
        if (index < 0 || width < 0 || height < 0) {
            throw new IllegalArgumentException("index, width and height must be non-negative");
        }
        this.index = index;
        this.name = name != null ? name : "";
        this.width = width;
        this.height = height;
        this.visible = visible;
        this.groundLayer = groundLayer;
        this.itemsLayer = itemsLayer;
        this.hasNonAtlasTiles = hasNonAtlasTiles;
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    public int index() { return index; }
    public String name() { return name; }
    public Optional<String> optionalName() {
        return name.isEmpty() ? Optional.empty() : Optional.of(name);
    }
    public int width() { return width; }
    public int height() { return height; }
    public boolean visible() { return visible; }
    public boolean groundLayer() { return groundLayer; }
    public boolean itemsLayer() { return itemsLayer; }
    public boolean hasNonAtlasTiles() { return hasNonAtlasTiles; }
    public MapProperties properties() { return properties; }
    public boolean contains(int tileX, int tileY) {
        return tileX >= 0 && tileY >= 0 && tileX < width && tileY < height;
    }

    @Override
    public String toString() {
        return "MapLayerSnapshot{" + index + ", name='" + name + "', size="
                + width + 'x' + height + '}';
    }
}
