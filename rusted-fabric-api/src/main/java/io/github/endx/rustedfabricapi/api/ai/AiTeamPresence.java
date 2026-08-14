package io.github.endx.rustedfabricapi.api.ai;

import io.github.endx.rustedfabricapi.api.game.TeamView;
import io.github.endx.rustedfabricapi.api.game.UnitView;
import io.github.endx.rustedfabricapi.api.world.WorldPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable coarse distribution summary for one player/team. */
public final class AiTeamPresence {
    private final TeamView team;
    private final AiTeamRelation relation;
    private final List<UnitView> units;
    private final int buildingCount;
    private final int mobileCount;
    private final int flyingCount;
    private final float totalHealth;
    private final float totalMaximumHealth;
    private final WorldPoint centroid;
    private final WorldPoint buildingCentroid;
    private final WorldPoint anchor;
    private final float spreadRadius;
    private final Map<String, Integer> movementCounts;

    AiTeamPresence(TeamView team, AiTeamRelation relation, List<UnitView> units,
            int buildingCount, int mobileCount, int flyingCount, float totalHealth,
            float totalMaximumHealth, WorldPoint centroid, WorldPoint buildingCentroid,
            WorldPoint anchor, float spreadRadius, Map<String, Integer> movementCounts) {
        this.team = team;
        this.relation = relation;
        this.units = Collections.unmodifiableList(new ArrayList<UnitView>(units));
        this.buildingCount = buildingCount;
        this.mobileCount = mobileCount;
        this.flyingCount = flyingCount;
        this.totalHealth = totalHealth;
        this.totalMaximumHealth = totalMaximumHealth;
        this.centroid = centroid;
        this.buildingCentroid = buildingCentroid;
        this.anchor = anchor;
        this.spreadRadius = spreadRadius;
        this.movementCounts = Collections.unmodifiableMap(
                new LinkedHashMap<String, Integer>(movementCounts));
    }

    public TeamView team() { return team; }
    public AiTeamRelation relation() { return relation; }
    public List<UnitView> units() { return units; }
    public int unitCount() { return units.size(); }
    public int buildingCount() { return buildingCount; }
    public int mobileCount() { return mobileCount; }
    public int flyingCount() { return flyingCount; }
    public float totalHealth() { return totalHealth; }
    public float totalMaximumHealth() { return totalMaximumHealth; }
    public WorldPoint centroid() { return centroid; }
    /** Falls back to {@link #centroid()} when this team has no buildings. */
    public WorldPoint buildingCentroid() { return buildingCentroid; }
    /** Center of the densest local building/unit cluster, used as the approximate main position. */
    public WorldPoint anchor() { return anchor; }
    public float spreadRadius() { return spreadRadius; }
    public Map<String, Integer> movementCounts() { return movementCounts; }
}
