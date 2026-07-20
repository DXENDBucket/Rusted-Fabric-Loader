package io.github.endx.rustedfabricapi.api.map;

import io.github.endx.rustedfabricapi.api.client.RustedWarfareClient;
import io.github.endx.rustedfabricapi.api.world.WorldPoint;
import rustedwarfare.game.Team;
import rustedwarfare.core.GameEngine;
import rustedwarfare.map.MapEngine;
import rustedwarfare.map.MapTile;

import java.util.Optional;

/** Typed queries for the currently loaded map. */
public final class Maps {
    private Maps() {
    }

    public static MapEngine currentOrNull() {
        GameEngine engine = RustedWarfareClient.getEngine();
        return engine != null ? engine.tileMap : null;
    }

    public static Optional<MapEngine> current() {
        return Optional.ofNullable(currentOrNull());
    }

    public static MapEngine requireCurrent() {
        MapEngine map = currentOrNull();
        if (map == null) {
            throw new IllegalStateException("No Rusted Warfare map is loaded");
        }
        return map;
    }

    public static boolean isLoaded() {
        MapEngine map = currentOrNull();
        return map != null && map.mapLoaded;
    }

    public static float widthInWorldUnits() {
        return requireCurrent().getMapWidthPixels();
    }

    public static float heightInWorldUnits() {
        return requireCurrent().getMapHeightPixels();
    }

    public static boolean containsTile(int tileX, int tileY) {
        return requireCurrent().isInMapBounds(tileX, tileY);
    }

    /** Raw mapped tile access for advanced operations; prefer {@link MapTiles} snapshots. */
    public static Optional<MapTile> tileAtWorld(float worldX, float worldY) {
        return Optional.ofNullable(requireCurrent().getTileAtWorldPosition(worldX, worldY));
    }

    /** Raw mapped ground-tile access for advanced operations; prefer {@link MapTiles} snapshots. */
    public static Optional<MapTile> tileAt(int tileX, int tileY) {
        return Optional.ofNullable(requireCurrent().getTileAtTilePositionSafe(tileX, tileY));
    }

    public static int worldToTileX(float worldX) {
        requireFinite(worldX, "worldX");
        return (int) Math.floor(requireCurrent().worldToTileX(worldX));
    }

    public static int worldToTileY(float worldY) {
        requireFinite(worldY, "worldY");
        return (int) Math.floor(requireCurrent().worldToTileY(worldY));
    }

    public static WorldPoint tileCenter(int tileX, int tileY) {
        MapEngine map = requireCurrent();
        if (!map.isInMapBounds(tileX, tileY)) {
            throw new IllegalArgumentException("tile is outside the current map");
        }
        return new WorldPoint(tileX * map.tileWidth + map.halfTileWidth,
                tileY * map.tileHeight + map.halfTileHeight);
    }

    public static boolean isVisible(float worldX, float worldY, Team team) {
        requireFinite(worldX, "worldX");
        requireFinite(worldY, "worldY");
        if (team == null) throw new NullPointerException("team");
        return requireCurrent().isWorldPointCurrentlyVisibleToTeam(worldX, worldY, team);
    }

    public static boolean isExplored(int tileX, int tileY, Team team) {
        if (team == null) throw new NullPointerException("team");
        if (!containsTile(tileX, tileY)) return false;
        return requireCurrent().isTileExploredByTeam(team, tileX, tileY);
    }

    /** Reveals an area through the game's normal fog path. Call on the update thread. */
    public static void reveal(float worldX, float worldY, int radius, Team team) {
        requireFinite(worldX, "worldX");
        requireFinite(worldY, "worldY");
        if (radius < 0) throw new IllegalArgumentException("radius must be non-negative");
        if (team == null) throw new NullPointerException("team");
        requireCurrent().revealMapAreaForSingleTeam(worldX, worldY, radius, team, false);
    }

    private static void requireFinite(float value, String name) {
        if (!Float.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
    }
}
