package io.github.endx.rustedfabricapi.api.ai;

import io.github.endx.rustedfabricapi.api.game.TeamView;
import io.github.endx.rustedfabricapi.api.world.WorldPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** One immutable strategic situation assessment from the perspective of an AI team. */
public final class AiStrategicMapSnapshot {
    private final TeamView perspective;
    private final AiTerrainMapSnapshot terrain;
    private final AiWorldSnapshot world;
    private final List<AiTeamPresence> teams;
    private final List<AiInfluenceCell> cells;
    private final List<AiInfluenceCell> frontline;
    private final List<AiStrategicResource> resources;
    private final WorldPoint primaryFront;

    AiStrategicMapSnapshot(TeamView perspective, AiTerrainMapSnapshot terrain,
            AiWorldSnapshot world, List<AiTeamPresence> teams, List<AiInfluenceCell> cells,
            List<AiInfluenceCell> frontline, List<AiStrategicResource> resources,
            WorldPoint primaryFront) {
        this.perspective = perspective;
        this.terrain = terrain;
        this.world = world;
        this.teams = immutable(teams);
        this.cells = immutable(cells);
        this.frontline = immutable(frontline);
        this.resources = immutable(resources);
        this.primaryFront = primaryFront;
    }

    public TeamView perspective() { return perspective; }
    public AiTerrainMapSnapshot terrain() { return terrain; }
    public AiWorldSnapshot world() { return world; }
    public List<AiTeamPresence> teams() { return teams; }
    public List<AiInfluenceCell> cells() { return cells; }
    public List<AiInfluenceCell> frontline() { return frontline; }
    public List<AiStrategicResource> resources() { return resources; }
    public Optional<WorldPoint> primaryFront() { return Optional.ofNullable(primaryFront); }

    public AiInfluenceCell cell(int column, int row) {
        if (column < 0 || row < 0 || column >= terrain.columns() || row >= terrain.rows()) {
            return null;
        }
        return cells.get(row * terrain.columns() + column);
    }

    private static <T> List<T> immutable(List<T> source) {
        return Collections.unmodifiableList(new ArrayList<T>(source));
    }
}
