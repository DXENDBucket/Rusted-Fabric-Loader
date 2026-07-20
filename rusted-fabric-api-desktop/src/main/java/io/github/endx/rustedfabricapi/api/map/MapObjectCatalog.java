package io.github.endx.rustedfabricapi.api.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable, queryable view of all TMX object groups in one loaded map. */
public final class MapObjectCatalog {
    private final List<MapObjectGroupSnapshot> groups;
    private final List<MapObjectSnapshot> objects;

    public MapObjectCatalog(List<MapObjectGroupSnapshot> groups) {
        ArrayList<MapObjectGroupSnapshot> groupCopy = new ArrayList<MapObjectGroupSnapshot>();
        ArrayList<MapObjectSnapshot> objectCopy = new ArrayList<MapObjectSnapshot>();
        if (groups != null) {
            for (MapObjectGroupSnapshot group : groups) {
                MapObjectGroupSnapshot checked = Objects.requireNonNull(group, "group");
                groupCopy.add(checked);
                objectCopy.addAll(checked.objects());
            }
        }
        this.groups = Collections.unmodifiableList(groupCopy);
        this.objects = Collections.unmodifiableList(objectCopy);
    }

    public List<MapObjectGroupSnapshot> groups() { return groups; }
    public List<MapObjectSnapshot> objects() { return objects; }
    public int groupCount() { return groups.size(); }
    public int objectCount() { return objects.size(); }

    public Optional<MapObjectGroupSnapshot> group(String name) {
        Objects.requireNonNull(name, "name");
        for (MapObjectGroupSnapshot group : groups) {
            if (group.name().equalsIgnoreCase(name)) return Optional.of(group);
        }
        return Optional.empty();
    }

    public List<MapObjectSnapshot> named(String name) {
        Objects.requireNonNull(name, "name");
        return filter(object -> object.name().equalsIgnoreCase(name));
    }

    public List<MapObjectSnapshot> ofType(String type) {
        Objects.requireNonNull(type, "type");
        return filter(object -> object.type().equalsIgnoreCase(type));
    }

    public List<MapObjectSnapshot> withProperty(String key) {
        Objects.requireNonNull(key, "key");
        return filter(object -> object.properties().contains(key));
    }

    public List<MapObjectSnapshot> containing(float worldX, float worldY) {
        return filter(object -> object.contains(worldX, worldY));
    }

    public List<MapObjectSnapshot> intersecting(float left, float top, float right, float bottom) {
        return filter(object -> object.intersects(left, top, right, bottom));
    }

    private List<MapObjectSnapshot> filter(ObjectPredicate predicate) {
        ArrayList<MapObjectSnapshot> result = new ArrayList<MapObjectSnapshot>();
        for (MapObjectSnapshot object : objects) {
            if (predicate.test(object)) result.add(object);
        }
        return Collections.unmodifiableList(result);
    }

    @FunctionalInterface
    private interface ObjectPredicate {
        boolean test(MapObjectSnapshot object);
    }

    @Override
    public String toString() {
        return "MapObjectCatalog{groups=" + groups.size() + ", objects=" + objects.size() + '}';
    }
}
