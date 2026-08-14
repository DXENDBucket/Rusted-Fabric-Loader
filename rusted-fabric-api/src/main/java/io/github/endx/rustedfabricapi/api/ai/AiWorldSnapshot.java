package io.github.endx.rustedfabricapi.api.ai;

import io.github.endx.rustedfabricapi.api.game.TeamView;
import io.github.endx.rustedfabricapi.api.game.UnitView;
import io.github.endx.rustedfabricapi.api.game.Units;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Immutable membership snapshot of every active unit, grouped relative to one AI team.
 * Unit views remain live, matching the rest of the game-view API.
 */
public final class AiWorldSnapshot {
    private static final Comparator<UnitView> STABLE_ID_ORDER =
            Comparator.comparingLong(UnitView::id);

    private final TeamView perspective;
    private final List<UnitView> all;
    private final List<UnitView> own;
    private final List<UnitView> allies;
    private final List<UnitView> enemies;
    private final List<UnitView> neutral;

    private AiWorldSnapshot(TeamView perspective, List<UnitView> all,
            List<UnitView> own, List<UnitView> allies, List<UnitView> enemies,
            List<UnitView> neutral) {
        this.perspective = perspective;
        this.all = immutable(all);
        this.own = immutable(own);
        this.allies = immutable(allies);
        this.enemies = immutable(enemies);
        this.neutral = immutable(neutral);
    }

    public static AiWorldSnapshot capture(TeamView perspective) {
        if (perspective == null) throw new IllegalArgumentException("perspective must not be null");
        List<UnitView> all = new ArrayList<UnitView>();
        List<UnitView> own = new ArrayList<UnitView>();
        List<UnitView> allies = new ArrayList<UnitView>();
        List<UnitView> enemies = new ArrayList<UnitView>();
        List<UnitView> neutral = new ArrayList<UnitView>();
        for (UnitView unit : Units.active()) {
            if (!unit.alive()) continue;
            all.add(unit);
            TeamView owner = unit.team().orElse(null);
            if (owner == null) {
                neutral.add(unit);
            } else if (owner.sameTeam(perspective)) {
                own.add(unit);
            } else if (perspective.enemyOf(owner)) {
                enemies.add(unit);
            } else if (perspective.alliedWith(owner)) {
                allies.add(unit);
            } else {
                neutral.add(unit);
            }
        }
        return new AiWorldSnapshot(perspective, all, own, allies, enemies, neutral);
    }

    /** Always true: this API intentionally exposes the complete simulation state, not fog memory. */
    public boolean omniscient() { return true; }
    public TeamView perspective() { return perspective; }
    public List<UnitView> all() { return all; }
    public List<UnitView> own() { return own; }
    /** Allied teams excluding the perspective team itself. */
    public List<UnitView> allies() { return allies; }
    public List<UnitView> enemies() { return enemies; }
    public List<UnitView> neutral() { return neutral; }

    private static List<UnitView> immutable(List<UnitView> source) {
        source.sort(STABLE_ID_ORDER);
        return Collections.unmodifiableList(source);
    }
}
