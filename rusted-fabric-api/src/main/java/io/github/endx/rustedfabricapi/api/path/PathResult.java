package io.github.endx.rustedfabricapi.api.path;

import io.github.endx.rustedfabricapi.api.map.Maps;
import io.github.endx.rustedfabricapi.api.world.WorldPoint;
import rustedwarfare.map.MapEngine;
import rustedwarfare.path.PathNode;
import rustedwarfare.path.PathRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/** Immutable defensive copy of one completed native path request. */
public final class PathResult {
    private final PathRequest request;
    private final boolean pathAvailable;
    private final boolean failed;
    private final int createdFrame;
    private final int solveFrame;
    private final float elapsedSolveTime;
    private final List<PathStep> steps;

    private PathResult(PathRequest request) {
        this.request = Objects.requireNonNull(request, "request");
        this.failed = request.failed;
        this.createdFrame = request.createdFrame;
        this.solveFrame = request.solveFrame;
        this.elapsedSolveTime = request.elapsedSolveTime;
        LinkedList rawNodes = request.getPathNodes();
        this.pathAvailable = rawNodes != null;
        List<PathStep> copied = new ArrayList<PathStep>(rawNodes != null ? rawNodes.size() : 0);
        MapEngine map = Maps.currentOrNull();
        if (rawNodes != null) {
            for (Object rawNode : rawNodes) {
                if (!(rawNode instanceof PathNode)) continue;
                PathNode node = (PathNode) rawNode;
                WorldPoint center = map != null
                        ? new WorldPoint(node.x * map.tileWidth + map.halfTileWidth,
                                node.y * map.tileHeight + map.halfTileHeight)
                        : null;
                copied.add(new PathStep(node.x, node.y, center));
            }
        }
        this.steps = Collections.unmodifiableList(copied);
    }

    public static PathResult capture(PathRequest request) {
        return new PathResult(request);
    }

    public PathRequest request() { return request; }
    public boolean pathAvailable() { return pathAvailable; }
    public boolean successful() { return pathAvailable; }
    /** Exposes the native request's mapped failure/retry flag. */
    public boolean failed() { return failed; }
    public int createdFrame() { return createdFrame; }
    public int solveFrame() { return solveFrame; }
    public float elapsedSolveTime() { return elapsedSolveTime; }
    public List<PathStep> steps() { return steps; }
}
