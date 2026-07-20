package io.github.endx.rustedfabricapi.api.game;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/** Stable entry points and snapshot queries for the live unit collection. */
public final class Units {
    private static final String[] TYPES = {
            "rustedwarfare.unit.Unit",
            "com.corrodinggames.rts.game.units.am"
    };
    private static final String[] GAME_OBJECT_TYPES = {
            "rustedwarfare.framework.GameObject",
            "com.corrodinggames.rts.gameFramework.w"
    };

    private Units() {
    }

    public static boolean isUnit(Object value) {
        return value != null && RustedReflection.isAnyClass(value.getClass(), TYPES);
    }

    public static UnitView view(Object unit) {
        if (!isUnit(unit)) {
            throw new IllegalArgumentException("Expected Unit, got " + describe(unit));
        }
        return new UnitView(unit);
    }

    /** Returns an immutable list snapshot. Individual views remain live. */
    public static List<UnitView> active() {
        Object collection = RustedReflection.getStaticFieldValue(TYPES,
                new String[]{"allUnits", "bE"});
        List<UnitView> result = new ArrayList<UnitView>();
        for (Object value : RustedReflection.snapshotIterable(collection)) {
            if (isUnit(value)) result.add(new UnitView(value));
        }
        return Collections.unmodifiableList(result);
    }

    public static List<UnitView> matching(Predicate<? super UnitView> predicate) {
        if (predicate == null) throw new IllegalArgumentException("predicate must not be null");
        List<UnitView> result = new ArrayList<UnitView>();
        for (UnitView unit : active()) {
            if (predicate.test(unit)) result.add(unit);
        }
        return Collections.unmodifiableList(result);
    }

    public static List<UnitView> alive() {
        return matching(UnitView::alive);
    }

    public static List<UnitView> forTeam(TeamView team) {
        if (team == null) throw new IllegalArgumentException("team must not be null");
        return matching(unit -> unit.team().map(value -> value.sameTeam(team)).orElse(false));
    }

    public static List<UnitView> within(float x, float y, float radius) {
        if (Float.isNaN(radius) || radius < 0.0F) {
            throw new IllegalArgumentException("radius must be non-negative");
        }
        final float radiusSquared = radius * radius;
        return matching(unit -> {
            float dx = unit.x() - x;
            float dy = unit.y() - y;
            return dx * dx + dy * dy <= radiusSquared;
        });
    }

    public static Optional<UnitView> byId(long id) {
        Object value = RustedReflection.invokeStatic(GAME_OBJECT_TYPES,
                new String[]{"getUnitById", "a"}, Long.valueOf(id), Boolean.FALSE);
        return value == null ? Optional.empty() : Optional.of(view(value));
    }

    public static Optional<UnitView> byId(long id, boolean includeRemoved) {
        Object value = RustedReflection.invokeStatic(GAME_OBJECT_TYPES,
                new String[]{"getUnitById", "a"}, Long.valueOf(id),
                Boolean.valueOf(includeRemoved));
        return value == null ? Optional.empty() : Optional.of(view(value));
    }

    private static String describe(Object value) {
        return value == null ? "null" : value.getClass().getName();
    }
}
