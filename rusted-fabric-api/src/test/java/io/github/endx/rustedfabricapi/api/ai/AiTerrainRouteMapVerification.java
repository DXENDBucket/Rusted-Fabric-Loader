package io.github.endx.rustedfabricapi.api.ai;

import io.github.endx.rustedfabricapi.api.world.WorldPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.OptionalDouble;

public final class AiTerrainRouteMapVerification {
    private AiTerrainRouteMapVerification() {
    }

    public static void main(String[] args) {
        AiTerrainMapSnapshot terrain = terrainWithDetour();
        WorldPoint start = terrain.cell(0, 0).center();
        WorldPoint target = terrain.cell(2, 0).center();
        OptionalDouble land = terrain.routesFrom(start, AiMovementDomain.LAND).costTo(target);
        OptionalDouble air = terrain.routesFrom(start, AiMovementDomain.AIR).costTo(target);
        require(land.isPresent(), "land route failed to use the available detour");
        require(air.isPresent(), "air route unexpectedly disconnected");
        require(land.getAsDouble() > air.getAsDouble() * 1.5D,
                "terrain route cost collapsed to straight-line distance");
        WorldPoint sameCellStart = new WorldPoint(5.0F, 5.0F);
        WorldPoint sameCellTarget = new WorldPoint(85.0F, 5.0F);
        OptionalDouble sameCell = terrain.routesFrom(
                sameCellStart, AiMovementDomain.LAND).costTo(sameCellTarget);
        require(sameCell.isPresent() && Math.abs(sameCell.getAsDouble() - 80.0D) < 0.01D,
                "same-cell route lost exact spawn-position distance");
        System.out.println("AI terrain route-map contracts passed");
    }

    private static AiTerrainMapSnapshot terrainWithDetour() {
        int columns = 3;
        int rows = 2;
        int cellTiles = 12;
        int tileSize = 10;
        ArrayList<AiTerrainCell> cells = new ArrayList<AiTerrainCell>();
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                boolean blocked = row == 0 && column == 1;
                EnumMap<AiMovementDomain, Float> passability =
                        new EnumMap<AiMovementDomain, Float>(AiMovementDomain.class);
                EnumMap<AiMovementDomain, Integer> regions =
                        new EnumMap<AiMovementDomain, Integer>(AiMovementDomain.class);
                EnumMap<AiMovementDomain, WorldPoint> representatives =
                        new EnumMap<AiMovementDomain, WorldPoint>(AiMovementDomain.class);
                for (AiMovementDomain domain : AiMovementDomain.values()) {
                    passability.put(domain, domain == AiMovementDomain.AIR
                            ? 1.0F : blocked ? 0.0F : 1.0F);
                    if (domain != AiMovementDomain.AIR) regions.put(domain, 1);
                    if (domain == AiMovementDomain.AIR || !blocked) {
                        representatives.put(domain, new WorldPoint(
                                (column * cellTiles + cellTiles * 0.5F) * tileSize,
                                (row * cellTiles + cellTiles * 0.5F) * tileSize));
                    }
                }
                int minX = column * cellTiles;
                int minY = row * cellTiles;
                cells.add(new AiTerrainCell(column, row, minX, minY,
                        minX + cellTiles, minY + cellTiles, tileSize, tileSize,
                        0.0F, blocked ? 1.0F : 0.0F, 0.0F, 0.0F,
                        blocked ? 1.0F : 0.0F, passability, regions,
                        representatives));
            }
        }
        return new AiTerrainMapSnapshot(columns * cellTiles, rows * cellTiles,
                tileSize, tileSize, cellTiles, columns, rows, cells,
                Collections.emptyList());
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
