package io.github.endx.rustedfabricapi.api.path;

import io.github.endx.rustedfabricapi.api.client.RustedWarfareClient;
import io.github.endx.rustedfabricapi.api.map.Maps;
import io.github.endx.rustedfabricapi.api.path.event.PathEvents;
import io.github.endx.rustedfabricapi.api.util.RustedReflection;
import rustedwarfare.core.GameEngine;
import rustedwarfare.path.MovementCostMap;
import rustedwarfare.path.PathEngine;
import rustedwarfare.path.PathRequest;
import rustedwarfare.unit.MovementType;
import rustedwarfare.unit.OrderableUnit;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.WeakHashMap;

/** Native path-cost queries and asynchronous route requests. */
public final class Pathfinding {
    private static final Map<PathRequest, PathRequestHandle> HANDLES =
            Collections.synchronizedMap(new IdentityHashMap<PathRequest, PathRequestHandle>());
    private static final Map<PathRequest, Boolean> COMPLETED =
            Collections.synchronizedMap(new WeakHashMap<PathRequest, Boolean>());

    private Pathfinding() {
    }

    public static PathEngine engine() {
        GameEngine engine = RustedWarfareClient.requireEngine();
        if (engine.pathfindingEngine == null) {
            throw new IllegalStateException("Native path engine is unavailable");
        }
        return engine.pathfindingEngine;
    }

    public static boolean isBlocked(MovementType movementType, int tileX, int tileY) {
        return engine().isTileBlocked(Objects.requireNonNull(movementType, "movementType"),
                tileX, tileY);
    }

    public static boolean isBlockedIgnoringBuildings(MovementType movementType,
            int tileX, int tileY) {
        return engine().isTileBlockedIgnoringBuildingCost(
                Objects.requireNonNull(movementType, "movementType"), tileX, tileY);
    }

    public static TilePathingSnapshot snapshot(MovementType movementType, int tileX, int tileY) {
        Objects.requireNonNull(movementType, "movementType");
        PathEngine engine = engine();
        MovementCostMap costs = requireCosts(engine, movementType);
        boolean inBounds = Maps.containsTile(tileX, tileY);
        boolean blocked = !inBounds || engine.isCostMapTileBlocked(costs, tileX, tileY);
        boolean terrainBlocked = !inBounds
                || engine.isCostMapTileBlockedWithOptions(costs, tileX, tileY, true);
        int totalCost = inBounds ? engine.getTileTotalPathCost(costs, tileX, tileY) : -1;
        int clearance = inBounds ? engine.getTileClearanceCost(costs, tileX, tileY) : -1;
        int region = inBounds ? connectedRegion(costs, tileX, tileY) : -1;
        return new TilePathingSnapshot(movementType, tileX, tileY, blocked,
                terrainBlocked, totalCost, clearance, region, connectedRegionSize(costs, region));
    }

    /** Returns empty when this movement cost map has no connected-region data. */
    public static Optional<Boolean> sameConnectedRegion(MovementType movementType,
            int firstTileX, int firstTileY, int secondTileX, int secondTileY) {
        TilePathingSnapshot first = snapshot(movementType, firstTileX, firstTileY);
        TilePathingSnapshot second = snapshot(movementType, secondTileX, secondTileY);
        if (first.blocked() || second.blocked()) return Optional.of(Boolean.FALSE);
        if (!first.connectedRegion().isPresent() || !second.connectedRegion().isPresent()) {
            return Optional.empty();
        }
        return Optional.of(Boolean.valueOf(first.connectedRegion().getAsInt()
                == second.connectedRegion().getAsInt()));
    }

    /** Queues through the game's solver. Call on the update thread. */
    public static PathRequestHandle submit(PathQuery query) {
        Objects.requireNonNull(query, "query").validateCurrentMap();
        PathEngine engine = engine();
        if (!RustedReflection.getBooleanField(engine, new String[]{"isRunning", "p"})) {
            throw new IllegalStateException("Native path engine is not running");
        }
        PathRequest request = engine.createPathRequest(true);
        request.setStart(query.movementType(), (short) query.startTileX(),
                (short) query.startTileY(), query.startDirection(), query.lowPriority());
        request.setEnd((short) query.endTileX(), (short) query.endTileY(),
                (short) query.endRadius());
        request.returnPathInMultiplayer = true;
        PathRequestHandle handle = new PathRequestHandle(query, request);
        HANDLES.put(request, handle);
        try {
            engine.queuePathRequest(request, query.refreshCosts());
        } catch (RuntimeException failure) {
            HANDLES.remove(request);
            handle.future().completeExceptionally(failure);
            throw failure;
        }
        return handle;
    }

    /** Marks one movement cost map dirty through the native throttled refresh path. */
    public static void markCostsDirty(MovementType movementType, boolean immediate) {
        PathEngine engine = engine();
        engine.markCostMapDirty(requireCosts(engine,
                Objects.requireNonNull(movementType, "movementType")), immediate);
    }

    /** Rebuilds dynamic costs near a unit after an out-of-band placement change. */
    public static void rebuildAround(OrderableUnit unit) {
        engine().rebuildCostsAroundUnit(Objects.requireNonNull(unit, "unit"));
    }

    /** Internal mapped hook; also observes requests created by the base game. */
    public static void onNativeQueuing(PathEngine engine, PathRequest request,
            boolean refreshCosts) {
        COMPLETED.remove(request);
        PathEvents.QUEUING.invoker().onQueuing(engine, request, refreshCosts);
    }

    /** Internal mapped hook; also observes requests created by the base game. */
    public static void onNativeQueued(PathEngine engine, PathRequest request,
            boolean refreshCosts) {
        PathEvents.QUEUED.invoker().onQueued(engine, request, refreshCosts);
    }

    /** Internal mapped hook; deduplicates completion when the engine revisits a solved request. */
    public static void onNativeSolved(PathEngine engine, PathRequest request) {
        synchronized (COMPLETED) {
            if (COMPLETED.put(request, Boolean.TRUE) != null) return;
        }
        PathResult result = PathResult.capture(request);
        PathRequestHandle handle = HANDLES.remove(request);
        if (handle != null) handle.complete(result);
        PathEvents.SOLVED.invoker().onSolved(engine, request, result);
    }

    /** Clears only Loader-owned request bookkeeping when a map is replaced. */
    public static void clearRuntime() {
        IllegalStateException failure = new IllegalStateException("Path request invalidated by map change");
        synchronized (HANDLES) {
            for (PathRequestHandle handle : HANDLES.values()) {
                handle.future().completeExceptionally(failure);
            }
            HANDLES.clear();
        }
        COMPLETED.clear();
    }

    private static MovementCostMap requireCosts(PathEngine engine, MovementType movementType) {
        MovementCostMap costs = engine.getCostsForMovementType(movementType);
        if (costs == null) {
            throw new IllegalArgumentException("No native cost map for movement type: " + movementType);
        }
        return costs;
    }

    private static int connectedRegion(MovementCostMap costs, int tileX, int tileY) {
        if (costs.isolatedGroups == null) return -1;
        int index = tileX * costs.height + tileY;
        if (index < 0 || index >= costs.isolatedGroups.length) return -1;
        return costs.isolatedGroups[index];
    }

    private static int connectedRegionSize(MovementCostMap costs, int region) {
        if (region <= 0 || costs.isolatedGroupSizes == null) return -1;
        Object size = costs.isolatedGroupSizes.get(Short.valueOf((short) region));
        if (!(size instanceof Number)) size = costs.isolatedGroupSizes.get(Integer.valueOf(region));
        return size instanceof Number ? ((Number) size).intValue() : -1;
    }
}
