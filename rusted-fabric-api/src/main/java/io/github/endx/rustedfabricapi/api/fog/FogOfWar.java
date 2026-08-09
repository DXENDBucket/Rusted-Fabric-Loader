package io.github.endx.rustedfabricapi.api.fog;

import io.github.endx.rustedfabricapi.api.geometry.GeometryBounds;
import io.github.endx.rustedfabricapi.api.geometry.GeometryMask;
import rustedwarfare.core.GameEngine;
import rustedwarfare.game.Team;
import rustedwarfare.map.MapEngine;

import java.util.Objects;
import java.util.Arrays;

/** Stable read and mutation operations over the native per-team fog grid. */
public final class FogOfWar {
    private FogOfWar() { }

    public static FogMode mode() {
        MapEngine map = requireMap();
        if (!map.useFogOfWar) return FogMode.OFF;
        return map.useLineOfSightFog ? FogMode.LINE_OF_SIGHT : FogMode.BASIC;
    }

    /**
     * Changes the native global fog mode. Existing team fog is retained; newly allocated maps use
     * {@code initialState}. This is a synchronized gameplay mutation and must run on every peer.
     */
    public static void setMode(FogMode mode, FogState initialState) {
        FogMode checkedMode = Objects.requireNonNull(mode, "mode");
        FogState checkedInitial = Objects.requireNonNull(initialState, "initialState");
        MapEngine map = requireMap();
        map.useFogOfWar = checkedMode != FogMode.OFF;
        map.useLineOfSightFog = checkedMode == FogMode.LINE_OF_SIGHT;
        if (checkedMode != FogMode.OFF) {
            for (Object value : Team.b(true)) {
                if (!(value instanceof Team)) continue;
                Team team = (Team) value;
                if (team.fogOfWarMap == null || team.fogOfWarMap.length != map.width
                        || (map.width > 0 && team.fogOfWarMap[0].length != map.height)) {
                    team.fogMapWidth = map.width;
                    team.fogMapHeight = map.height;
                    team.fogOfWarMap = new byte[map.width][map.height];
                    for (byte[] column : team.fogOfWarMap) {
                        Arrays.fill(column, (byte) checkedInitial.nativeLevel());
                    }
                }
            }
        }
        map.rebuildFogBuffers();
        map.updateFogAllTeams();
        GameEngine engine = GameEngine.getInstance();
        if (engine.minimap != null) {
            io.github.endx.rustedfabricapi.api.util.RustedReflection.setFieldValue(
                    engine.minimap, new String[]{"fogDirty", "O"}, Boolean.TRUE);
        }
    }

    public static int nativeLevel(Team team, int tileX, int tileY) {
        Team checked = requireTeamMap(team);
        if (tileX < 0 || tileY < 0 || tileX >= checked.fogOfWarMap.length
                || tileY >= checked.fogOfWarMap[tileX].length) {
            throw new IndexOutOfBoundsException("fog tile outside team map: " + tileX + "," + tileY);
        }
        return checked.fogOfWarMap[tileX][tileY] & 0xff;
    }

    public static FogState state(Team team, int tileX, int tileY) {
        return FogState.classify(nativeLevel(team, tileX, tileY));
    }

    public static boolean isExplored(Team team, int tileX, int tileY) {
        return requireMap().isTileExploredByTeam(requireTeamMap(team), tileX, tileY);
    }

    public static boolean isVisible(Team team, int tileX, int tileY) {
        return requireMap().isTileCurrentlyVisibleToTeam(tileX, tileY, requireTeamMap(team));
    }

    public static FogEditBatch edit(Team team) {
        return new FogEditBatch(requireMap(), requireTeamMap(team));
    }

    public static FogEditResult apply(Team team, GeometryMask worldMask,
                                      FogOperation operation) {
        Objects.requireNonNull(worldMask, "worldMask");
        Objects.requireNonNull(operation, "operation");
        MapEngine map = requireMap();
        Team checkedTeam = requireTeamMap(team);
        GeometryBounds bounds = worldMask.bounds();
        int minX = clamp((int) Math.floor(bounds.minX() * map.invTileWidth), 0, map.width - 1);
        int maxX = clamp((int) Math.floor(bounds.maxX() * map.invTileWidth), 0, map.width - 1);
        int minY = clamp((int) Math.floor(bounds.minY() * map.invTileHeight), 0, map.height - 1);
        int maxY = clamp((int) Math.floor(bounds.maxY() * map.invTileHeight), 0, map.height - 1);
        int visited = 0;
        int changed;
        try (FogEditBatch batch = new FogEditBatch(map, checkedTeam)) {
            for (int x = minX; x <= maxX; x++) {
                float worldX = x * map.tileWidth + map.halfTileWidth;
                for (int y = minY; y <= maxY; y++) {
                    float worldY = y * map.tileHeight + map.halfTileHeight;
                    if (!worldMask.contains(worldX, worldY)) continue;
                    visited++;
                    batch.apply(x, y, operation);
                }
            }
            changed = batch.changedTiles();
        }
        return new FogEditResult(visited, changed);
    }

    public static void revealCircle(Team team, float worldX, float worldY, int tileRadius,
                                    boolean includeSharedAllies) {
        if (tileRadius <= 0) throw new IllegalArgumentException("tileRadius must be positive");
        MapEngine map = requireMap();
        if (includeSharedAllies) map.revealMapArea(worldX, worldY, tileRadius, team, true);
        else map.revealMapAreaForSingleTeam(worldX, worldY, tileRadius, team, true);
    }

    private static MapEngine requireMap() {
        GameEngine engine = GameEngine.getInstance();
        if (engine == null || engine.tileMap == null || !engine.tileMap.mapLoaded) {
            throw new IllegalStateException("no map is currently loaded");
        }
        return engine.tileMap;
    }

    private static Team requireTeamMap(Team team) {
        Team checked = Objects.requireNonNull(team, "team");
        if (checked.fogOfWarMap == null) throw new IllegalStateException("team has no fog map");
        return checked;
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
