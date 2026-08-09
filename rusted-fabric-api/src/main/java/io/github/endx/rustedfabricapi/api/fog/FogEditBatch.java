package io.github.endx.rustedfabricapi.api.fog;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;
import rustedwarfare.core.GameEngine;
import rustedwarfare.game.Team;
import rustedwarfare.map.MapEngine;
import rustedwarfare.map.MapTileRenderCache;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/** Batched fog mutation that refreshes native smooth-fog and minimap caches once. */
public final class FogEditBatch implements AutoCloseable {
    private final MapEngine map;
    private final Team team;
    private final Set<Long> changed = new LinkedHashSet<Long>();
    private boolean closed;

    FogEditBatch(MapEngine map, Team team) {
        this.map = Objects.requireNonNull(map, "map");
        this.team = Objects.requireNonNull(team, "team");
        if (team.fogOfWarMap == null) throw new IllegalStateException("team has no fog map");
    }

    public MapEngine map() { return map; }
    public Team team() { return team; }
    public int changedTiles() { return changed.size(); }

    public boolean apply(int tileX, int tileY, FogOperation operation) {
        requireOpen();
        Objects.requireNonNull(operation, "operation");
        if (!map.isInMapBounds(tileX, tileY)
                || tileX >= team.fogOfWarMap.length
                || tileY >= team.fogOfWarMap[tileX].length) return false;
        int current = team.fogOfWarMap[tileX][tileY] & 0xff;
        int replacement = operation.apply(current);
        if (current == replacement) return false;
        team.fogOfWarMap[tileX][tileY] = (byte) replacement;
        changed.add(Long.valueOf(((long) tileX << 32) | (tileY & 0xffffffffL)));
        return true;
    }

    @Override public void close() {
        if (closed) return;
        closed = true;
        if (changed.isEmpty()) return;
        GameEngine engine = GameEngine.getInstance();
        if (engine == null || engine.playerTeam != team) return;
        map.rebuildFogBuffers();
        MapTileRenderCache renderCache = MapEngine.tileRenderCache;
        for (Long packedValue : changed) {
            long packed = packedValue.longValue();
            int x = (int) (packed >> 32);
            int y = (int) packed;
            map.updateFogNeighborTile(x, y);
            if (renderCache != null) renderCache.markTileDirty(x, y, true);
        }
        if (engine.minimap != null) {
            RustedReflection.setFieldValue(engine.minimap,
                    new String[]{"fogDirty", "O"}, Boolean.TRUE);
        }
    }

    private void requireOpen() {
        if (closed) throw new IllegalStateException("fog edit batch is closed");
    }
}
