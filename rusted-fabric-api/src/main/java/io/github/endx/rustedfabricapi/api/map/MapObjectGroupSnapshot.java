package io.github.endx.rustedfabricapi.api.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable TMX object-group snapshot. */
public final class MapObjectGroupSnapshot {
    private final int index;
    private final String name;
    private final int width;
    private final int height;
    private final MapProperties properties;
    private final List<MapObjectSnapshot> objects;

    public MapObjectGroupSnapshot(int index, String name, int width, int height,
            MapProperties properties, List<MapObjectSnapshot> objects) {
        if (index < 0 || width < 0 || height < 0) {
            throw new IllegalArgumentException("index, width and height must be non-negative");
        }
        this.index = index;
        this.name = name != null ? name : "";
        this.width = width;
        this.height = height;
        this.properties = Objects.requireNonNull(properties, "properties");
        ArrayList<MapObjectSnapshot> copy = new ArrayList<MapObjectSnapshot>();
        if (objects != null) {
            for (MapObjectSnapshot object : objects) {
                MapObjectSnapshot checked = Objects.requireNonNull(object, "object");
                if (checked.groupIndex() != index) {
                    throw new IllegalArgumentException("object belongs to another group index");
                }
                copy.add(checked);
            }
        }
        this.objects = Collections.unmodifiableList(copy);
    }

    public int index() { return index; }
    public String name() { return name; }
    public Optional<String> optionalName() {
        return name.isEmpty() ? Optional.empty() : Optional.of(name);
    }
    public int width() { return width; }
    public int height() { return height; }
    public MapProperties properties() { return properties; }
    public List<MapObjectSnapshot> objects() { return objects; }

    public Optional<MapObjectSnapshot> object(String name) {
        Objects.requireNonNull(name, "name");
        for (MapObjectSnapshot object : objects) {
            if (object.name().equalsIgnoreCase(name)) return Optional.of(object);
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return "MapObjectGroupSnapshot{" + index + ", name='" + name
                + "', objects=" + objects.size() + '}';
    }
}
