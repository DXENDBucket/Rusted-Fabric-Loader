package io.github.endx.rustedfabricapi.api.ai;

import io.github.endx.rustedfabricapi.api.world.WorldPoint;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Comparator;
import java.util.OptionalDouble;
import java.util.PriorityQueue;

/** Reusable coarse route-cost field over one immutable terrain snapshot. */
public final class AiTerrainRouteMap {
    private static final float MINIMUM_PASSABLE_FRACTION = 0.12F;
    private final AiTerrainMapSnapshot terrain;
    private final AiMovementDomain domain;
    private final int originColumn;
    private final int originRow;
    private final WorldPoint origin;
    private final float[] costs;
    private final int[] predecessors;

    AiTerrainRouteMap(AiTerrainMapSnapshot terrain, WorldPoint origin,
            AiMovementDomain domain) {
        if (terrain == null) throw new IllegalArgumentException("terrain must not be null");
        if (origin == null) throw new IllegalArgumentException("origin must not be null");
        if (domain == null) throw new IllegalArgumentException("domain must not be null");
        this.terrain = terrain;
        this.domain = domain;
        this.origin = origin;
        AiTerrainCell start = terrain.cellAtWorld(origin.x(), origin.y());
        this.originColumn = start != null ? start.column() : -1;
        this.originRow = start != null ? start.row() : -1;
        this.costs = new float[terrain.columns() * terrain.rows()];
        this.predecessors = new int[costs.length];
        Arrays.fill(costs, Float.POSITIVE_INFINITY);
        Arrays.fill(predecessors, -1);
        if (start != null) calculate(start);
    }

    public AiTerrainMapSnapshot terrain() { return terrain; }
    public AiMovementDomain domain() { return domain; }
    public int originColumn() { return originColumn; }
    public int originRow() { return originRow; }

    /** Empty means the destination is outside the map or disconnected for this domain. */
    public OptionalDouble costTo(WorldPoint destination) {
        if (destination == null) return OptionalDouble.empty();
        AiTerrainCell cell = terrain.cellAtWorld(destination.x(), destination.y());
        if (cell == null) return OptionalDouble.empty();
        float value = costs[index(cell)];
        if (!Float.isFinite(value)) return OptionalDouble.empty();
        AiTerrainCell start = terrain.cell(originColumn, originRow);
        if (start == cell) {
            return OptionalDouble.of(Math.hypot(
                    destination.x() - origin.x(), destination.y() - origin.y()));
        }
        if (start != null) {
            value += distance(origin, start.center());
            value += distance(destination, cell.center());
        }
        return OptionalDouble.of(value);
    }

    public boolean reaches(WorldPoint destination) {
        return costTo(destination).isPresent();
    }

    /** Coarse route polyline snapped to actual passable tiles in every crossed cell. */
    public List<WorldPoint> pathTo(WorldPoint destination) {
        if (destination == null) return Collections.emptyList();
        AiTerrainCell target = terrain.cellAtWorld(destination.x(), destination.y());
        if (target == null || !Float.isFinite(costs[index(target)])) {
            return Collections.emptyList();
        }
        ArrayList<AiTerrainCell> reverse = new ArrayList<AiTerrainCell>();
        int current = index(target);
        while (current >= 0) {
            reverse.add(terrain.cells().get(current));
            if (current == originRow * terrain.columns() + originColumn) break;
            current = predecessors[current];
        }
        if (reverse.isEmpty() || current < 0) return Collections.emptyList();
        Collections.reverse(reverse);
        ArrayList<WorldPoint> result = new ArrayList<WorldPoint>();
        result.add(origin);
        for (AiTerrainCell cell : reverse) {
            WorldPoint point = cell.representativePoint(domain).orElse(cell.center());
            if (result.get(result.size() - 1).distanceSquared(point) > 1.0F) result.add(point);
        }
        WorldPoint endpoint = target.representativePoint(domain).orElse(destination);
        if (result.get(result.size() - 1).distanceSquared(endpoint) > 1.0F) {
            result.add(endpoint);
        }
        return Collections.unmodifiableList(result);
    }

    /** Returns a passable route point the requested travel distance before the destination. */
    public java.util.Optional<WorldPoint> pointBefore(
            WorldPoint destination, float distanceBefore) {
        List<WorldPoint> path = pathTo(destination);
        if (path.isEmpty()) return java.util.Optional.empty();
        float remaining = Math.max(0.0F, distanceBefore);
        for (int index = path.size() - 1; index > 0; index--) {
            WorldPoint to = path.get(index);
            WorldPoint from = path.get(index - 1);
            float segment = (float) Math.sqrt(to.distanceSquared(from));
            if (segment >= remaining && segment > 0.001F) {
                float amount = remaining / segment;
                return java.util.Optional.of(new WorldPoint(
                        to.x() + (from.x() - to.x()) * amount,
                        to.y() + (from.y() - to.y()) * amount));
            }
            remaining -= segment;
        }
        return java.util.Optional.of(path.get(0));
    }

    private void calculate(AiTerrainCell start) {
        int startIndex = index(start);
        costs[startIndex] = 0.0F;
        PriorityQueue<Node> open = new PriorityQueue<Node>(Comparator
                .comparingDouble((Node value) -> value.cost)
                .thenComparingInt(value -> value.index));
        open.add(new Node(startIndex, 0.0F));
        int[][] offsets = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        while (!open.isEmpty()) {
            Node current = open.remove();
            if (current.cost != costs[current.index]) continue;
            AiTerrainCell from = terrain.cells().get(current.index);
            for (int[] offset : offsets) {
                AiTerrainCell to = terrain.cell(
                        from.column() + offset[0], from.row() + offset[1]);
                if (!canCross(from, to)) continue;
                float next = current.cost + edgeCost(from, to);
                int nextIndex = index(to);
                if (next >= costs[nextIndex]) continue;
                costs[nextIndex] = next;
                predecessors[nextIndex] = current.index;
                open.add(new Node(nextIndex, next));
            }
        }
    }

    private boolean canCross(AiTerrainCell from, AiTerrainCell to) {
        if (to == null) return false;
        if (domain != AiMovementDomain.AIR
                && (from.passableFraction(domain) < MINIMUM_PASSABLE_FRACTION
                || to.passableFraction(domain) < MINIMUM_PASSABLE_FRACTION)) return false;
        return domain == AiMovementDomain.AIR || terrain.sameRegion(from, to, domain);
    }

    private float edgeCost(AiTerrainCell from, AiTerrainCell to) {
        float dx = to.center().x() - from.center().x();
        float dy = to.center().y() - from.center().y();
        float distance = (float) Math.hypot(dx, dy);
        if (domain == AiMovementDomain.AIR) return distance;
        float passable = (from.passableFraction(domain)
                + to.passableFraction(domain)) * 0.5F;
        float terrainPenalty = 1.0F
                + (from.mountainFraction() + to.mountainFraction()) * 0.7F
                + (from.largeBlockerFraction() + to.largeBlockerFraction()) * 0.45F;
        return distance * terrainPenalty / (0.30F + passable * 0.70F);
    }

    private int index(AiTerrainCell cell) {
        return cell.row() * terrain.columns() + cell.column();
    }

    private static float distance(WorldPoint first, WorldPoint second) {
        return (float) Math.hypot(first.x() - second.x(), first.y() - second.y());
    }

    private static final class Node {
        final int index;
        final float cost;

        Node(int index, float cost) {
            this.index = index;
            this.cost = cost;
        }
    }
}
