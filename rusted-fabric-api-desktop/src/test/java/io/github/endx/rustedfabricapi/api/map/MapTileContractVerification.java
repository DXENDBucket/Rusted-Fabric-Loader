package io.github.endx.rustedfabricapi.api.map;

import java.util.Map;

public final class MapTileContractVerification {
    private MapTileContractVerification() {
    }

    public static void verify() {
        MapLayerSnapshot layer = new MapLayerSnapshot(3, "Ground", 40, 30,
                true, true, false, false,
                new MapProperties(Map.of("role", "terrain")));
        require(layer.index() == 3 && layer.name().equals("Ground")
                        && layer.width() == 40 && layer.height() == 30
                        && layer.visible() && layer.groundLayer() && !layer.itemsLayer()
                        && layer.contains(39, 29) && !layer.contains(40, 29)
                        && layer.properties().get("role").orElse("").equals("terrain"),
                "map layer snapshot lost a value");

        MapTileSnapshot tile = new MapTileSnapshot(3, "Ground", 4, 5, 20, 20,
                "terrain", "terrain.png", 24, 7, 11, 65_535,
                true, true, false, false, true, -1,
                false, true, 3, new MapProperties(Map.of("damage", "2.5")));
        require(tile.layerIndex() == 3 && tile.tileX() == 4 && tile.tileY() == 5
                        && tile.worldX() == 80.0F && tile.worldY() == 100.0F
                        && tile.center().x() == 90.0F && tile.center().y() == 110.0F
                        && tile.tilesetName().orElse("").equals("terrain")
                        && tile.globalTileId() == 24 && tile.localTileIndex() == 7
                        && tile.atlasIndex() == 11 && tile.registeredTileId() == 65_535
                        && tile.water() && tile.waterBridge() && !tile.lava() && !tile.cliff()
                        && tile.resourcePool() && tile.pathingCost() == -1
                        && !tile.largeCliffOrTreeBlocker() && tile.blocksBuildings()
                        && tile.variantCount() == 3
                        && tile.properties().decimal("damage").orElse(-1.0D) == 2.5D,
                "map tile snapshot lost a value");

        expectIllegal(() -> new MapLayerSnapshot(-1, "bad", 1, 1,
                true, false, false, false, MapProperties.empty()));
        expectIllegal(() -> new MapTileSnapshot(0, "bad", 0, 0, 0, 20,
                null, null, 0, 0, 0, 0,
                false, false, false, false, false, 0,
                false, false, 0, MapProperties.empty()));
    }

    private static void expectIllegal(Runnable action) {
        try {
            action.run();
            throw new AssertionError("invalid map snapshot was accepted");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
