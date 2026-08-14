package io.github.endx.rustedfabricapi.api.ai;

import io.github.endx.rustedfabricapi.api.game.TeamView;
import io.github.endx.rustedfabricapi.api.game.Teams;
import rustedwarfare.ai.AiTeam;

/** Complete game-thread context for one AI-team update. */
public final class AiTickContext {
    private final AiTeam aiTeam;
    private final TeamView team;
    private final float delta;
    private AiWorldSnapshot world;
    private AiOrders orders;
    private AiStrategicMapSnapshot strategicMap;

    private AiTickContext(AiTeam aiTeam, float delta) {
        if (aiTeam == null) throw new IllegalArgumentException("aiTeam must not be null");
        if (!Float.isFinite(delta) || delta < 0.0F) {
            throw new IllegalArgumentException("delta must be finite and non-negative");
        }
        this.aiTeam = aiTeam;
        this.team = Teams.view(aiTeam);
        this.delta = delta;
    }

    public static AiTickContext capture(AiTeam aiTeam, float delta) {
        return new AiTickContext(aiTeam, delta);
    }

    /** Native object for mapped APIs that do not yet have a stable view. */
    public AiTeam rawTeam() { return aiTeam; }
    public TeamView team() { return team; }
    public float delta() { return delta; }

    /** Lazily captures the complete, fog-independent unit membership for this tick. */
    public AiWorldSnapshot world() {
        if (world == null) world = AiWorldSnapshot.capture(team);
        return world;
    }

    /** Synchronized command facade restricted to units owned by this AI team. */
    public AiOrders orders() {
        if (orders == null) orders = new AiOrders(aiTeam);
        return orders;
    }

    /**
     * Lazily analyzes terrain, player distribution, influence, front lines, and resource sites.
     * Controllers should retain this snapshot for their own strategic decision interval rather
     * than rebuilding it for every tactical tick.
     */
    public AiStrategicMapSnapshot strategicMap() {
        if (strategicMap == null) strategicMap = AiStrategicMaps.capture(this);
        return strategicMap;
    }
}
