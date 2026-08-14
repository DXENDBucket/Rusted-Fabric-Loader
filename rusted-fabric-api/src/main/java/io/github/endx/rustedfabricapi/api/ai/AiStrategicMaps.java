package io.github.endx.rustedfabricapi.api.ai;

import io.github.endx.rustedfabricapi.api.game.TeamView;
import io.github.endx.rustedfabricapi.api.game.UnitView;
import io.github.endx.rustedfabricapi.api.world.WorldPoint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Builds dynamic strategic situation snapshots over the cached static terrain grid. */
public final class AiStrategicMaps {
    private static final int INFLUENCE_RADIUS_CELLS = 2;

    private AiStrategicMaps() {
    }

    public static AiStrategicMapSnapshot capture(AiTickContext context) {
        if (context == null) throw new IllegalArgumentException("context must not be null");
        return capture(context, AiTerrainMaps.current());
    }

    static AiStrategicMapSnapshot capture(AiTickContext context,
            AiTerrainMapSnapshot terrain) {
        AiWorldSnapshot world = context.world();
        TeamView perspective = context.team();
        List<AiTeamPresence> presences = teamPresences(world, perspective, terrain);
        InfluenceBuild influence = influence(world, perspective, terrain);
        List<AiStrategicResource> resources = resources(world, perspective, terrain,
                presences, influence.cells, influence.frontline);
        return new AiStrategicMapSnapshot(perspective, terrain, world, presences,
                influence.cells, influence.frontline, resources, influence.primaryFront);
    }

    private static List<AiTeamPresence> teamPresences(AiWorldSnapshot world,
            TeamView perspective, AiTerrainMapSnapshot terrain) {
        IdentityHashMap<Object, TeamBucket> byRawTeam = new IdentityHashMap<Object, TeamBucket>();
        for (UnitView unit : world.all()) {
            TeamView team = unit.team().orElse(null);
            if (team == null) continue;
            TeamBucket bucket = byRawTeam.get(team.raw());
            if (bucket == null) {
                bucket = new TeamBucket(team, relation(perspective, team));
                byRawTeam.put(team.raw(), bucket);
            }
            bucket.units.add(unit);
        }
        ArrayList<AiTeamPresence> result = new ArrayList<AiTeamPresence>();
        for (TeamBucket bucket : byRawTeam.values()) result.add(bucket.finish(terrain));
        result.sort(Comparator.comparingInt(value -> value.team().id()));
        return result;
    }

    private static InfluenceBuild influence(AiWorldSnapshot world, TeamView perspective,
            AiTerrainMapSnapshot terrain) {
        int size = terrain.columns() * terrain.rows();
        float[] own = new float[size];
        float[] ally = new float[size];
        float[] enemy = new float[size];
        int[] ownCount = new int[size];
        int[] allyCount = new int[size];
        int[] enemyCount = new int[size];
        AiMovementDomain[] domains = AiMovementDomain.values();
        float[][] domainFriendly = new float[domains.length][size];
        float[][] domainEnemy = new float[domains.length][size];
        for (UnitView unit : world.all()) {
            TeamView owner = unit.team().orElse(null);
            AiTeamRelation relation = owner != null ? relation(perspective, owner)
                    : AiTeamRelation.NEUTRAL;
            if (relation == AiTeamRelation.NEUTRAL) continue;
            AiTerrainCell origin = terrain.cellAtWorld(unit.x(), unit.y());
            if (origin == null) continue;
            int originIndex = origin.row() * terrain.columns() + origin.column();
            if (relation == AiTeamRelation.OWN) ownCount[originIndex]++;
            else if (relation == AiTeamRelation.ALLY) allyCount[originIndex]++;
            else enemyCount[originIndex]++;
            float weight = influenceWeight(unit);
            AiMovementDomain domain = AiMovementDomain.of(unit);
            for (int dy = -INFLUENCE_RADIUS_CELLS; dy <= INFLUENCE_RADIUS_CELLS; dy++) {
                for (int dx = -INFLUENCE_RADIUS_CELLS; dx <= INFLUENCE_RADIUS_CELLS; dx++) {
                    int column = origin.column() + dx;
                    int row = origin.row() + dy;
                    AiTerrainCell target = terrain.cell(column, row);
                    if (target == null) continue;
                    if (domain != AiMovementDomain.AIR && target != origin
                            && !terrain.sameRegion(origin, target, domain)) continue;
                    float distance = (float) Math.sqrt(dx * dx + dy * dy);
                    if (distance > INFLUENCE_RADIUS_CELLS + 0.01F) continue;
                    float contribution = weight / (1.0F + distance);
                    int index = row * terrain.columns() + column;
                    if (relation == AiTeamRelation.OWN) own[index] += contribution;
                    else if (relation == AiTeamRelation.ALLY) ally[index] += contribution;
                    else enemy[index] += contribution;
                    if (relation == AiTeamRelation.ENEMY) {
                        domainEnemy[domain.ordinal()][index] += contribution;
                    } else {
                        domainFriendly[domain.ordinal()][index] += contribution;
                    }
                }
            }
        }

        AiCellControl[] controls = new AiCellControl[size];
        for (int i = 0; i < size; i++) {
            controls[i] = control(own[i] + ally[i], enemy[i]);
        }
        boolean[] front = new boolean[size];
        float[] frontScore = new float[size];
        AiMovementDomain[] frontDomain = new AiMovementDomain[size];
        for (int row = 0; row < terrain.rows(); row++) {
            for (int column = 0; column < terrain.columns(); column++) {
                int index = row * terrain.columns() + column;
                float friendly = own[index] + ally[index];
                if (controls[index] == AiCellControl.CONTESTED) {
                    front[index] = true;
                    frontScore[index] = balance(friendly, enemy[index]);
                    float strongest = -1.0F;
                    for (AiMovementDomain domain : domains) {
                        float combined = domainFriendly[domain.ordinal()][index]
                                + domainEnemy[domain.ordinal()][index];
                        if (combined > strongest) {
                            strongest = combined;
                            frontDomain[index] = domain;
                        }
                    }
                }
                int[][] offsets = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
                for (int[] offset : offsets) {
                    int otherColumn = column + offset[0];
                    int otherRow = row + offset[1];
                    AiTerrainCell here = terrain.cell(column, row);
                    AiTerrainCell there = terrain.cell(otherColumn, otherRow);
                    if (there == null) continue;
                    int other = otherRow * terrain.columns() + otherColumn;
                    for (AiMovementDomain domain : domains) {
                        if (domain != AiMovementDomain.AIR
                                && !terrain.sameRegion(here, there, domain)) continue;
                        AiCellControl hereControl = control(
                                domainFriendly[domain.ordinal()][index],
                                domainEnemy[domain.ordinal()][index]);
                        AiCellControl thereControl = control(
                                domainFriendly[domain.ordinal()][other],
                                domainEnemy[domain.ordinal()][other]);
                        boolean opposing = hereControl == AiCellControl.FRIENDLY
                                && thereControl == AiCellControl.ENEMY
                                || hereControl == AiCellControl.ENEMY
                                && thereControl == AiCellControl.FRIENDLY;
                        if (opposing && frontScore[index] < 0.5F) {
                            front[index] = true;
                            frontScore[index] = 0.5F;
                            frontDomain[index] = domain;
                        }
                    }
                }
            }
        }

        ArrayList<AiInfluenceCell> cells = new ArrayList<AiInfluenceCell>(size);
        ArrayList<AiInfluenceCell> frontline = new ArrayList<AiInfluenceCell>();
        AiInfluenceCell primary = null;
        float primaryScore = -1.0F;
        for (int i = 0; i < size; i++) {
            AiInfluenceCell cell = new AiInfluenceCell(terrain.cells().get(i),
                    ownCount[i], allyCount[i], enemyCount[i], own[i], ally[i], enemy[i],
                    controls[i], front[i], frontScore[i], frontDomain[i]);
            cells.add(cell);
            if (front[i]) {
                frontline.add(cell);
                float score = frontScore[i] * (own[i] + ally[i] + enemy[i]);
                if (score > primaryScore) {
                    primary = cell;
                    primaryScore = score;
                }
            }
        }
        frontline.sort(Comparator.comparingDouble(AiInfluenceCell::frontlineScore).reversed()
                .thenComparingInt(value -> value.terrain().row())
                .thenComparingInt(value -> value.terrain().column()));
        return new InfluenceBuild(cells, frontline,
                primary != null ? primary.terrain().center() : null);
    }

    private static List<AiStrategicResource> resources(AiWorldSnapshot world,
            TeamView perspective, AiTerrainMapSnapshot terrain,
            List<AiTeamPresence> teams, List<AiInfluenceCell> cells,
            List<AiInfluenceCell> frontline) {
        AiTeamPresence ownPresence = null;
        ArrayList<AiTeamPresence> enemies = new ArrayList<AiTeamPresence>();
        for (AiTeamPresence presence : teams) {
            if (presence.relation() == AiTeamRelation.OWN) ownPresence = presence;
            else if (presence.relation() == AiTeamRelation.ENEMY) enemies.add(presence);
        }
        WorldPoint ownAnchor = ownPresence != null ? ownPresence.anchor()
                : new WorldPoint(terrain.worldWidth() * 0.5F, terrain.worldHeight() * 0.5F);
        AiTerrainCell ownAnchorCell = terrain.cellAtWorld(ownAnchor.x(), ownAnchor.y());
        float diagonal = Math.max(1.0F, (float) Math.hypot(
                terrain.worldWidth(), terrain.worldHeight()));
        float occupationRadius = Math.max(terrain.tileWidth(), terrain.tileHeight()) * 1.8F;
        float occupationRadiusSquared = occupationRadius * occupationRadius;
        ArrayList<AiStrategicResource> result = new ArrayList<AiStrategicResource>();
        for (AiResourceSite site : terrain.resourceSites()) {
            UnitView occupant = null;
            float occupantDistance = occupationRadiusSquared;
            for (UnitView unit : world.all()) {
                if (!unit.building()) continue;
                float dx = unit.x() - site.center().x();
                float dy = unit.y() - site.center().y();
                float distance = dx * dx + dy * dy;
                if (distance <= occupantDistance) {
                    occupant = unit;
                    occupantDistance = distance;
                }
            }
            AiResourceControl resourceControl = resourceControl(perspective, occupant);
            AiTerrainCell terrainCell = terrain.cellAtWorld(site.center().x(), site.center().y());
            AiInfluenceCell local = terrainCell != null
                    ? cells.get(terrainCell.row() * terrain.columns() + terrainCell.column()) : null;
            float friendly = local != null ? local.friendlyInfluence() : 0.0F;
            float enemy = local != null ? local.enemyInfluence() : 0.0F;
            AiCellControl localControl = local != null ? local.control() : AiCellControl.EMPTY;
            EnumSet<AiMovementDomain> reachableDomains =
                    EnumSet.noneOf(AiMovementDomain.class);
            for (AiMovementDomain domain : AiMovementDomain.values()) {
                if (terrain.sameRegion(ownAnchorCell, terrainCell, domain)) {
                    reachableDomains.add(domain);
                }
            }
            boolean landReachable = reachableDomains.contains(AiMovementDomain.LAND);
            float ownDistance = distance(ownAnchor, site.center());
            float enemyDistance = diagonal;
            for (AiTeamPresence presence : enemies) {
                enemyDistance = Math.min(enemyDistance,
                        distance(presence.anchor(), site.center()));
            }
            float proximity = clamp01(1.0F - ownDistance / diagonal);
            float pressure = enemy / (friendly + enemy + 1.0F);
            float front = nearestFrontScore(site.center(), frontline, diagonal);
            Objective objective = objective(resourceControl, localControl, landReachable,
                    proximity, pressure, front, enemyDistance < ownDistance * 1.1F);
            result.add(new AiStrategicResource(site, occupant, resourceControl, localControl,
                    friendly, enemy, reachableDomains, objective.kind, objective.priority));
        }
        result.sort(Comparator.comparingDouble(AiStrategicResource::priority).reversed()
                .thenComparingInt(value -> value.site().tileY())
                .thenComparingInt(value -> value.site().tileX()));
        return result;
    }

    private static Objective objective(AiResourceControl control, AiCellControl localControl,
            boolean reachable, float proximity, float pressure, float front,
            boolean enemyCloser) {
        AiResourceObjectiveKind kind;
        float priority;
        switch (control) {
            case OWN:
                kind = AiResourceObjectiveKind.DEFEND;
                priority = pressure * 0.75F + front * 0.25F;
                break;
            case ALLY:
                kind = AiResourceObjectiveKind.SUPPORT;
                priority = pressure * 0.7F + front * 0.3F;
                break;
            case ENEMY:
                kind = AiResourceObjectiveKind.DENY;
                priority = proximity * 0.4F + front * 0.35F + (1.0F - pressure) * 0.25F;
                break;
            case NEUTRAL:
                kind = AiResourceObjectiveKind.LOCK_DOWN;
                priority = proximity * 0.35F + front * 0.35F + pressure * 0.3F;
                break;
            case UNCLAIMED:
            default:
                boolean lockDown = enemyCloser || localControl == AiCellControl.CONTESTED
                        || pressure > 0.45F;
                kind = lockDown ? AiResourceObjectiveKind.LOCK_DOWN
                        : AiResourceObjectiveKind.CAPTURE;
                priority = lockDown
                        ? proximity * 0.3F + front * 0.4F + pressure * 0.3F
                        : proximity * 0.45F + (1.0F - pressure) * 0.35F + front * 0.2F;
                break;
        }
        if (!reachable) priority *= 0.35F;
        return new Objective(kind, clamp01(priority));
    }

    private static float nearestFrontScore(WorldPoint point,
            List<AiInfluenceCell> frontline, float diagonal) {
        float best = 0.0F;
        for (AiInfluenceCell cell : frontline) {
            float closeness = 1.0F - distance(point, cell.terrain().center()) / diagonal;
            best = Math.max(best, clamp01(closeness) * Math.max(0.5F, cell.frontlineScore()));
        }
        return best;
    }

    private static AiResourceControl resourceControl(TeamView perspective, UnitView occupant) {
        if (occupant == null) return AiResourceControl.UNCLAIMED;
        TeamView owner = occupant.team().orElse(null);
        if (owner == null) return AiResourceControl.NEUTRAL;
        switch (relation(perspective, owner)) {
            case OWN: return AiResourceControl.OWN;
            case ALLY: return AiResourceControl.ALLY;
            case ENEMY: return AiResourceControl.ENEMY;
            default: return AiResourceControl.NEUTRAL;
        }
    }

    private static AiCellControl control(float friendly, float enemy) {
        if (friendly < 0.001F && enemy < 0.001F) return AiCellControl.EMPTY;
        if (enemy < 0.001F || friendly >= enemy * 1.5F) return AiCellControl.FRIENDLY;
        if (friendly < 0.001F || enemy >= friendly * 1.5F) return AiCellControl.ENEMY;
        return AiCellControl.CONTESTED;
    }

    private static float balance(float first, float second) {
        float maximum = Math.max(first, second);
        return maximum > 0.0F ? Math.min(first, second) / maximum : 0.0F;
    }

    private static AiTeamRelation relation(TeamView perspective, TeamView team) {
        if (team.sameTeam(perspective)) return AiTeamRelation.OWN;
        if (perspective.enemyOf(team)) return AiTeamRelation.ENEMY;
        if (perspective.alliedWith(team)) return AiTeamRelation.ALLY;
        return AiTeamRelation.NEUTRAL;
    }

    private static float influenceWeight(UnitView unit) {
        float durability = Math.max(1.0F, unit.maxHealth() + unit.maxShield());
        float health = unit.maxHealth() > 0.0F
                ? clamp01(unit.health() / unit.maxHealth()) : 1.0F;
        float weight = (float) Math.sqrt(durability) * (0.25F + health * 0.75F);
        if (unit.building()) weight *= 0.65F;
        return weight;
    }

    private static float distance(WorldPoint first, WorldPoint second) {
        return (float) Math.sqrt(first.distanceSquared(second));
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static final class InfluenceBuild {
        final List<AiInfluenceCell> cells;
        final List<AiInfluenceCell> frontline;
        final WorldPoint primaryFront;

        InfluenceBuild(List<AiInfluenceCell> cells, List<AiInfluenceCell> frontline,
                WorldPoint primaryFront) {
            this.cells = cells;
            this.frontline = frontline;
            this.primaryFront = primaryFront;
        }
    }

    private static final class Objective {
        final AiResourceObjectiveKind kind;
        final float priority;

        Objective(AiResourceObjectiveKind kind, float priority) {
            this.kind = kind;
            this.priority = priority;
        }
    }

    private static final class TeamBucket {
        final TeamView team;
        final AiTeamRelation relation;
        final ArrayList<UnitView> units = new ArrayList<UnitView>();

        TeamBucket(TeamView team, AiTeamRelation relation) {
            this.team = team;
            this.relation = relation;
        }

        AiTeamPresence finish(AiTerrainMapSnapshot terrain) {
            units.sort(Comparator.comparingLong(UnitView::id));
            float x = 0.0F;
            float y = 0.0F;
            float buildingX = 0.0F;
            float buildingY = 0.0F;
            float health = 0.0F;
            float maximumHealth = 0.0F;
            int buildings = 0;
            int flying = 0;
            TreeMap<String, Integer> movements = new TreeMap<String, Integer>();
            int cellCount = terrain.columns() * terrain.rows();
            int[] localUnits = new int[cellCount];
            int[] localBuildings = new int[cellCount];
            for (UnitView unit : units) {
                x += unit.x();
                y += unit.y();
                health += Math.max(0.0F, unit.health());
                maximumHealth += Math.max(0.0F, unit.maxHealth());
                if (unit.building()) {
                    buildings++;
                    buildingX += unit.x();
                    buildingY += unit.y();
                }
                if (unit.flying()) flying++;
                movements.merge(unit.movementType(), Integer.valueOf(1), Integer::sum);
                AiTerrainCell cell = terrain.cellAtWorld(unit.x(), unit.y());
                if (cell != null) {
                    int index = cell.row() * terrain.columns() + cell.column();
                    localUnits[index]++;
                    if (unit.building()) localBuildings[index]++;
                }
            }
            int count = Math.max(1, units.size());
            WorldPoint centroid = new WorldPoint(x / count, y / count);
            WorldPoint buildingCentroid = buildings > 0
                    ? new WorldPoint(buildingX / buildings, buildingY / buildings) : centroid;
            int anchorIndex = -1;
            int anchorScore = -1;
            for (int i = 0; i < cellCount; i++) {
                int score = localBuildings[i] * 4 + localUnits[i];
                if (score > anchorScore) {
                    anchorIndex = i;
                    anchorScore = score;
                }
            }
            WorldPoint anchor = anchorIndex >= 0
                    ? terrain.cells().get(anchorIndex).center() : centroid;
            float spread = 0.0F;
            for (UnitView unit : units) {
                float dx = unit.x() - centroid.x();
                float dy = unit.y() - centroid.y();
                spread = Math.max(spread, (float) Math.hypot(dx, dy));
            }
            return new AiTeamPresence(team, relation, units, buildings,
                    units.size() - buildings, flying, health, maximumHealth,
                    centroid, buildingCentroid, anchor, spread,
                    new LinkedHashMap<String, Integer>(movements));
        }
    }
}
