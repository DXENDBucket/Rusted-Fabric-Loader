package io.github.endx.rustedfabricapi.api.ai;

import io.github.endx.rustedfabricapi.api.map.Maps;
import io.github.endx.rustedfabricapi.api.path.Pathfinding;
import io.github.endx.rustedfabricapi.api.util.RustedReflection;
import io.github.endx.rustedfabricapi.api.world.WorldPoint;
import rustedwarfare.map.MapEngine;
import rustedwarfare.map.MapTile;
import rustedwarfare.path.MovementCostMap;
import rustedwarfare.path.PathEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Static map topology shared by multiple dynamic AI situation snapshots. */
public final class AiTerrainMapSnapshot {
    // Six tiles retain narrow ramps and lava/cliff detours that decide spawn-role assignments;
    // twelve-tile cells merged distinct Two Shores approaches into the same coarse route.
    public static final int DEFAULT_CELL_SIZE_TILES = 6;

    private final int mapWidthTiles;
    private final int mapHeightTiles;
    private final int tileWidth;
    private final int tileHeight;
    private final int cellSizeTiles;
    private final int columns;
    private final int rows;
    private final List<AiTerrainCell> cells;
    private final List<AiResourceSite> resourceSites;

    AiTerrainMapSnapshot(int mapWidthTiles, int mapHeightTiles, int tileWidth, int tileHeight,
            int cellSizeTiles, int columns, int rows, List<AiTerrainCell> cells,
            List<AiResourceSite> resourceSites) {
        this.mapWidthTiles = mapWidthTiles;
        this.mapHeightTiles = mapHeightTiles;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        this.cellSizeTiles = cellSizeTiles;
        this.columns = columns;
        this.rows = rows;
        this.cells = Collections.unmodifiableList(new ArrayList<AiTerrainCell>(cells));
        this.resourceSites = Collections.unmodifiableList(
                new ArrayList<AiResourceSite>(resourceSites));
    }

    public static AiTerrainMapSnapshot captureCurrent() {
        return capture(Maps.requireCurrent(), DEFAULT_CELL_SIZE_TILES);
    }

    public static AiTerrainMapSnapshot captureCurrent(int cellSizeTiles) {
        return capture(Maps.requireCurrent(), cellSizeTiles);
    }

    @SuppressWarnings("unchecked")
    static AiTerrainMapSnapshot capture(MapEngine map, int cellSizeTiles) {
        if (map == null || !map.mapLoaded) throw new IllegalStateException("No loaded map");
        if (cellSizeTiles < 4 || cellSizeTiles > 64) {
            throw new IllegalArgumentException("cellSizeTiles must be between 4 and 64");
        }
        int columns = (map.width + cellSizeTiles - 1) / cellSizeTiles;
        int rows = (map.height + cellSizeTiles - 1) / cellSizeTiles;
        PathEngine pathEngine = Pathfinding.engine();
        EnumMap<AiMovementDomain, MovementCostMap> costs =
                new EnumMap<AiMovementDomain, MovementCostMap>(AiMovementDomain.class);
        for (AiMovementDomain domain : AiMovementDomain.values()) {
            if (domain != AiMovementDomain.AIR) {
                costs.put(domain, pathEngine.getCostsForMovementType(domain.nativeType()));
            }
        }

        ArrayList<AiTerrainCell> cells = new ArrayList<AiTerrainCell>(columns * rows);
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                int minX = column * cellSizeTiles;
                int minY = row * cellSizeTiles;
                int maxX = Math.min(map.width, minX + cellSizeTiles);
                int maxY = Math.min(map.height, minY + cellSizeTiles);
                cells.add(captureCell(map, pathEngine, costs, column, row,
                        minX, minY, maxX, maxY));
            }
        }
        calculateChokes(cells, columns, rows);

        ArrayList<AiResourceSite> sites = new ArrayList<AiResourceSite>();
        if (map.resourcePoolPoints != null) {
            for (Object point : (List<Object>) map.resourcePoolPoints) {
                int x = pointCoordinate(point, new String[]{"x", "a"});
                int y = pointCoordinate(point, new String[]{"y", "b"});
                if (x >= 0 && y >= 0 && x < map.width && y < map.height) {
                    sites.add(new AiResourceSite(x, y,
                            new WorldPoint(x * map.tileWidth + map.halfTileWidth,
                                    y * map.tileHeight + map.halfTileHeight)));
                }
            }
        }
        sites.sort((first, second) -> {
            int y = Integer.compare(first.tileY(), second.tileY());
            return y != 0 ? y : Integer.compare(first.tileX(), second.tileX());
        });
        return new AiTerrainMapSnapshot(map.width, map.height, map.tileWidth, map.tileHeight,
                cellSizeTiles, columns, rows, cells, sites);
    }

    private static AiTerrainCell captureCell(MapEngine map, PathEngine pathEngine,
            Map<AiMovementDomain, MovementCostMap> costs, int column, int row,
            int minX, int minY, int maxX, int maxY) {
        int tileCount = Math.max(1, (maxX - minX) * (maxY - minY));
        int water = 0;
        int mountain = 0;
        int largeBlocker = 0;
        int lava = 0;
        int buildingBlocked = 0;
        EnumMap<AiMovementDomain, Integer> passable =
                new EnumMap<AiMovementDomain, Integer>(AiMovementDomain.class);
        EnumMap<AiMovementDomain, Map<Integer, Integer>> regions =
                new EnumMap<AiMovementDomain, Map<Integer, Integer>>(AiMovementDomain.class);
        for (AiMovementDomain domain : AiMovementDomain.values()) {
            passable.put(domain, Integer.valueOf(domain == AiMovementDomain.AIR ? tileCount : 0));
            regions.put(domain, new HashMap<Integer, Integer>());
        }
        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                MapTile tile = map.getTileAtTilePositionSafe(x, y);
                if (tile != null) {
                    if (tile.isWater) water++;
                    if (tile.isCliff) mountain++;
                    if (tile.isLargeCliffOrTreeBlocker) largeBlocker++;
                    if (tile.isLava) lava++;
                    if (tile.blocksBuildings) buildingBlocked++;
                }
                for (AiMovementDomain domain : AiMovementDomain.values()) {
                    if (domain == AiMovementDomain.AIR) continue;
                    MovementCostMap cost = costs.get(domain);
                    if (cost == null || pathEngine.isCostMapTileBlockedWithOptions(
                            cost, x, y, true)) continue;
                    passable.put(domain, Integer.valueOf(passable.get(domain).intValue() + 1));
                    int region = connectedRegion(cost, x, y);
                    if (region > 0) regions.get(domain).merge(
                            Integer.valueOf(region), Integer.valueOf(1), Integer::sum);
                }
            }
        }
        EnumMap<AiMovementDomain, Float> fractions =
                new EnumMap<AiMovementDomain, Float>(AiMovementDomain.class);
        EnumMap<AiMovementDomain, Integer> dominant =
                new EnumMap<AiMovementDomain, Integer>(AiMovementDomain.class);
        for (AiMovementDomain domain : AiMovementDomain.values()) {
            fractions.put(domain, Float.valueOf(passable.get(domain).floatValue() / tileCount));
            int bestRegion = -1;
            int bestCount = 0;
            for (Map.Entry<Integer, Integer> entry : regions.get(domain).entrySet()) {
                if (entry.getValue().intValue() > bestCount
                        || entry.getValue().intValue() == bestCount
                        && entry.getKey().intValue() < bestRegion) {
                    bestRegion = entry.getKey().intValue();
                    bestCount = entry.getValue().intValue();
                }
            }
            if (bestRegion > 0) dominant.put(domain, Integer.valueOf(bestRegion));
        }
        EnumMap<AiMovementDomain, WorldPoint> representatives =
                new EnumMap<AiMovementDomain, WorldPoint>(AiMovementDomain.class);
        representatives.put(AiMovementDomain.AIR, new WorldPoint(
                (minX + maxX) * map.tileWidth * 0.5F,
                (minY + maxY) * map.tileHeight * 0.5F));
        float centerTileX = (minX + maxX - 1) * 0.5F;
        float centerTileY = (minY + maxY - 1) * 0.5F;
        for (AiMovementDomain domain : AiMovementDomain.values()) {
            if (domain == AiMovementDomain.AIR || !dominant.containsKey(domain)) continue;
            MovementCostMap cost = costs.get(domain);
            int wantedRegion = dominant.get(domain).intValue();
            int bestX = -1;
            int bestY = -1;
            float bestDistance = Float.POSITIVE_INFINITY;
            for (int x = minX; x < maxX; x++) {
                for (int y = minY; y < maxY; y++) {
                    if (cost == null || pathEngine.isCostMapTileBlockedWithOptions(
                            cost, x, y, true) || connectedRegion(cost, x, y) != wantedRegion) {
                        continue;
                    }
                    float dx = x - centerTileX;
                    float dy = y - centerTileY;
                    float distance = dx * dx + dy * dy;
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        bestX = x;
                        bestY = y;
                    }
                }
            }
            if (bestX >= 0) representatives.put(domain, new WorldPoint(
                    bestX * map.tileWidth + map.halfTileWidth,
                    bestY * map.tileHeight + map.halfTileHeight));
        }
        return new AiTerrainCell(column, row, minX, minY, maxX, maxY,
                map.tileWidth, map.tileHeight, water / (float) tileCount,
                mountain / (float) tileCount, lava / (float) tileCount,
                largeBlocker / (float) tileCount,
                buildingBlocked / (float) tileCount, fractions, dominant,
                representatives);
    }

    private static void calculateChokes(List<AiTerrainCell> cells, int columns, int rows) {
        for (AiTerrainCell cell : cells) {
            float passable = cell.passableFraction(AiMovementDomain.LAND);
            if (passable < 0.15F) {
                cell.setLandChokeScore(0.0F);
                continue;
            }
            int exits = 0;
            int[][] offsets = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            for (int[] offset : offsets) {
                int x = cell.column() + offset[0];
                int y = cell.row() + offset[1];
                if (x < 0 || y < 0 || x >= columns || y >= rows) continue;
                AiTerrainCell neighbor = cells.get(y * columns + x);
                if (neighbor.passableFraction(AiMovementDomain.LAND) >= 0.15F
                        && sharesRegion(cell, neighbor, AiMovementDomain.LAND)) exits++;
            }
            float constrained = exits <= 1 ? 1.0F : exits == 2 ? 0.7F : exits == 3 ? 0.25F : 0.0F;
            float obstruction = 1.0F - Math.min(1.0F, passable);
            cell.setLandChokeScore(Math.min(1.0F, constrained * 0.55F
                    + obstruction * 0.25F + cell.mountainFraction() * 0.3F
                    + cell.largeBlockerFraction() * 0.15F));
        }
    }

    private static boolean sharesRegion(AiTerrainCell first, AiTerrainCell second,
            AiMovementDomain domain) {
        return first.dominantRegion(domain).isPresent()
                && second.dominantRegion(domain).isPresent()
                && first.dominantRegion(domain).getAsInt()
                == second.dominantRegion(domain).getAsInt();
    }

    private static int connectedRegion(MovementCostMap costs, int x, int y) {
        if (costs.isolatedGroups == null) return -1;
        int index = x * costs.height + y;
        return index >= 0 && index < costs.isolatedGroups.length
                ? costs.isolatedGroups[index] : -1;
    }

    private static int pointCoordinate(Object point, String[] names) {
        if (point == null) return -1;
        Object value = RustedReflection.getFieldValue(point, names);
        return value instanceof Number ? ((Number) value).intValue() : -1;
    }

    public int mapWidthTiles() { return mapWidthTiles; }
    public int mapHeightTiles() { return mapHeightTiles; }
    public int tileWidth() { return tileWidth; }
    public int tileHeight() { return tileHeight; }
    public float worldWidth() { return mapWidthTiles * (float) tileWidth; }
    public float worldHeight() { return mapHeightTiles * (float) tileHeight; }
    public int cellSizeTiles() { return cellSizeTiles; }
    public int columns() { return columns; }
    public int rows() { return rows; }
    public List<AiTerrainCell> cells() { return cells; }
    public List<AiResourceSite> resourceSites() { return resourceSites; }

    /** Builds one reusable coarse route-cost field from the supplied world position. */
    public AiTerrainRouteMap routesFrom(WorldPoint origin, AiMovementDomain domain) {
        return new AiTerrainRouteMap(this, origin, domain);
    }

    public AiTerrainCell cell(int column, int row) {
        if (column < 0 || row < 0 || column >= columns || row >= rows) return null;
        return cells.get(row * columns + column);
    }

    public AiTerrainCell cellAtWorld(float x, float y) {
        if (!Float.isFinite(x) || !Float.isFinite(y)
                || x < 0.0F || y < 0.0F || x >= worldWidth() || y >= worldHeight()) return null;
        int column = Math.min(columns - 1, (int) (x / (cellSizeTiles * (float) tileWidth)));
        int row = Math.min(rows - 1, (int) (y / (cellSizeTiles * (float) tileHeight)));
        return cell(column, row);
    }

    public boolean sameRegion(AiTerrainCell first, AiTerrainCell second,
            AiMovementDomain domain) {
        if (first == null || second == null || domain == null) return false;
        if (domain == AiMovementDomain.AIR) return true;
        return sharesRegion(first, second, domain);
    }
}
