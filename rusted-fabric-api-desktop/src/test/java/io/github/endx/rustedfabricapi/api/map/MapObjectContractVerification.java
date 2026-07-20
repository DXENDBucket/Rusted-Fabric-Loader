package io.github.endx.rustedfabricapi.api.map;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import io.github.endx.rustedfabricapi.api.map.event.MapObjectEvents;

public final class MapObjectContractVerification {
    private MapObjectContractVerification() {
    }

    public static void verify() {
        Map<String, String> mutable = new LinkedHashMap<String, String>();
        mutable.put("wave", " 12 ");
        mutable.put("scale", "1.5");
        mutable.put("enabled", "YES");
        mutable.put("invalid", "not-a-number");
        MapProperties properties = new MapProperties(mutable);
        mutable.put("wave", "99");
        require(properties.integer("wave").orElse(-1) == 12
                        && properties.decimal("scale").orElse(-1.0D) == 1.5D
                        && properties.flag("enabled").orElse(Boolean.FALSE)
                        && properties.integer("invalid").isEmpty()
                        && properties.get("wave").orElse("").trim().equals("12"),
                "TMX property parsing or defensive copying drifted");
        try {
            properties.asMap().put("other", "value");
            throw new AssertionError("TMX properties were mutable");
        } catch (UnsupportedOperationException expected) {
            // Expected.
        }

        MapObjectSnapshot zone = new MapObjectSnapshot(2, "logic", 0, "spawn_zone", "zone",
                100.0F, 200.0F, 80.0F, 40.0F, 0.0F, -1, -1,
                null, null, properties);
        MapObjectSnapshot marker = new MapObjectSnapshot(2, "logic", 1, "boss", "marker",
                300.0F, 400.0F, 0.0F, 0.0F, 15.0F, 10, 3,
                "units", "units.png", MapProperties.empty());
        require(zone.contains(120.0F, 220.0F) && !zone.contains(99.0F, 220.0F)
                        && zone.intersects(170.0F, 230.0F, 190.0F, 250.0F)
                        && zone.center().x() == 140.0F && zone.center().y() == 220.0F
                        && !zone.tileObject() && marker.tileObject()
                        && marker.gid().orElse(-1) == 10 && marker.tileIndex().orElse(-1) == 3,
                "TMX object geometry or tile identity drifted");

        ArrayList<MapObjectSnapshot> mutableObjects = new ArrayList<MapObjectSnapshot>();
        mutableObjects.add(zone);
        mutableObjects.add(marker);
        MapObjectGroupSnapshot group = new MapObjectGroupSnapshot(2, "logic", 20, 10,
                MapProperties.empty(), mutableObjects);
        mutableObjects.clear();
        MapObjectCatalog catalog = new MapObjectCatalog(List.of(group));
        require(catalog.groupCount() == 1 && catalog.objectCount() == 2
                        && catalog.group("LOGIC").orElseThrow().object("BOSS").orElseThrow() == marker
                        && catalog.named("SPAWN_ZONE").equals(List.of(zone))
                        && catalog.ofType("marker").equals(List.of(marker))
                        && catalog.withProperty("wave").equals(List.of(zone))
                        && catalog.containing(120.0F, 220.0F).equals(List.of(zone))
                        && catalog.intersecting(299.0F, 399.0F, 301.0F, 401.0F)
                                .equals(List.of(marker)),
                "TMX object catalog query drifted");
        try {
            catalog.objects().clear();
            throw new AssertionError("TMX object catalog was mutable");
        } catch (UnsupportedOperationException expected) {
            // Expected.
        }

        AtomicInteger eventObjects = new AtomicInteger();
        RustedFabricEvent.Registration registration = MapObjectEvents.AFTER_LOAD.subscribe(
                loaded -> eventObjects.set(loaded.objectCount()));
        MapObjectEvents.AFTER_LOAD.invoker().afterLoad(catalog);
        require(eventObjects.get() == 2, "TMX object load event was not dispatched");
        registration.close();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
