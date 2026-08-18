package io.github.endx.strategicai;

import io.github.endx.rustedfabricapi.api.ai.AiInfluenceCell;
import io.github.endx.rustedfabricapi.api.ai.AiMovementDomain;
import io.github.endx.rustedfabricapi.api.ai.AiStrategicMapSnapshot;
import io.github.endx.rustedfabricapi.api.ai.AiTerrainRouteMap;
import io.github.endx.rustedfabricapi.api.world.WorldPoint;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

/** Live land-contact sectors derived from the API influence grid. */
final class DynamicFrontlineMap {
    private final List<Sector> sectors;
    private final Sector primary;

    private DynamicFrontlineMap(List<Sector> sectors, Sector primary) {
        this.sectors = java.util.Collections.unmodifiableList(sectors);
        this.primary = primary;
    }

    static DynamicFrontlineMap capture(AiStrategicMapSnapshot situation,
            StrategicTeamPlan teamPlan) {
        HashMap<Long, AiInfluenceCell> cells = new HashMap<Long, AiInfluenceCell>();
        for (AiInfluenceCell cell : situation.frontline()) {
            AiMovementDomain domain = cell.frontlineDomain().orElse(null);
            if (domain == AiMovementDomain.AIR || cell.terrain()
                    .passableFraction(AiMovementDomain.LAND) < 0.12F) continue;
            cells.put(key(cell.terrain().column(), cell.terrain().row()), cell);
        }
        WorldPoint own = teamPlan.ownAnchor();
        WorldPoint preferred = teamPlan.preferredFrontierPoint();
        AiTerrainRouteMap routes = own != null
                ? situation.terrain().routesFrom(own, AiMovementDomain.LAND) : null;
        float diagonal = Math.max(1.0F, (float) Math.hypot(
                situation.terrain().worldWidth(), situation.terrain().worldHeight()));
        HashSet<Long> visited = new HashSet<Long>();
        ArrayList<Sector> sectors = new ArrayList<Sector>();
        for (Map.Entry<Long, AiInfluenceCell> entry : cells.entrySet()) {
            if (!visited.add(entry.getKey())) continue;
            ArrayDeque<AiInfluenceCell> open = new ArrayDeque<AiInfluenceCell>();
            open.add(entry.getValue());
            SectorAccumulator accumulator = new SectorAccumulator();
            while (!open.isEmpty()) {
                AiInfluenceCell current = open.removeFirst();
                accumulator.add(current);
                int column = current.terrain().column();
                int row = current.terrain().row();
                for (int y = -1; y <= 1; y++) {
                    for (int x = -1; x <= 1; x++) {
                        if (x == 0 && y == 0) continue;
                        long nextKey = key(column + x, row + y);
                        AiInfluenceCell next = cells.get(nextKey);
                        if (next != null && visited.add(nextKey)) open.addLast(next);
                    }
                }
            }
            WorldPoint point = accumulator.point();
            OptionalDouble cost = routes != null ? routes.costTo(point)
                    : OptionalDouble.empty();
            double routeNorm = cost.isPresent() ? cost.getAsDouble() / diagonal : 2.0D;
            double objectiveNorm = preferred != null
                    ? Math.sqrt(point.distanceSquared(preferred)) / diagonal : 1.0D;
            double score = sectorScore(accumulator.friendly, accumulator.enemy,
                    accumulator.frontQuality(), routeNorm, objectiveNorm,
                    accumulator.cells);
            sectors.add(new Sector(point, accumulator.friendly,
                    accumulator.enemy, accumulator.cells, score));
        }
        sectors.sort(java.util.Comparator.comparingDouble(Sector::score).reversed()
                .thenComparingDouble(value -> value.point().y())
                .thenComparingDouble(value -> value.point().x()));
        return new DynamicFrontlineMap(sectors,
                sectors.isEmpty() ? null : sectors.get(0));
    }

    static double sectorScore(double friendly, double enemy, double frontQuality,
            double routeNorm, double objectiveNorm, int cells) {
        double activity = Math.max(0.0D, friendly) + Math.max(0.0D, enemy);
        double balance = 1.0D - Math.abs(friendly - enemy) / (activity + 1.0D);
        return Math.log1p(activity) * 1.55D
                + Math.max(0.0D, frontQuality) * 1.10D
                + Math.max(0.0D, balance) * 1.25D
                + Math.sqrt(Math.max(1, cells)) * 0.22D
                - Math.max(0.0D, routeNorm) * 0.48D
                - Math.max(0.0D, objectiveNorm) * 0.32D;
    }

    Optional<WorldPoint> primaryPoint() {
        return primary != null ? Optional.of(primary.point()) : Optional.empty();
    }

    int sectorCount() { return sectors.size(); }

    private static long key(int column, int row) {
        return ((long) column << 32) ^ (row & 0xffffffffL);
    }

    static final class Sector {
        private final WorldPoint point;
        private final float friendly;
        private final float enemy;
        private final int cells;
        private final double score;

        Sector(WorldPoint point, float friendly, float enemy, int cells, double score) {
            this.point = point;
            this.friendly = friendly;
            this.enemy = enemy;
            this.cells = cells;
            this.score = score;
        }

        WorldPoint point() { return point; }
        float friendly() { return friendly; }
        float enemy() { return enemy; }
        int cells() { return cells; }
        double score() { return score; }
    }

    private static final class SectorAccumulator {
        float weightedX;
        float weightedY;
        float totalWeight;
        float friendly;
        float enemy;
        float quality;
        int cells;

        void add(AiInfluenceCell cell) {
            float localActivity = cell.friendlyInfluence() + cell.enemyInfluence();
            float weight = Math.max(0.15F, cell.frontlineScore())
                    * (1.0F + (float) Math.sqrt(Math.max(0.0F, localActivity)));
            WorldPoint center = cell.terrain().representativePoint(AiMovementDomain.LAND)
                    .orElse(cell.terrain().center());
            weightedX += center.x() * weight;
            weightedY += center.y() * weight;
            totalWeight += weight;
            friendly += cell.friendlyInfluence();
            enemy += cell.enemyInfluence();
            quality += cell.frontlineScore();
            cells++;
        }

        WorldPoint point() {
            float divisor = Math.max(0.001F, totalWeight);
            return new WorldPoint(weightedX / divisor, weightedY / divisor);
        }

        float frontQuality() { return cells > 0 ? quality / cells : 0.0F; }
    }
}
