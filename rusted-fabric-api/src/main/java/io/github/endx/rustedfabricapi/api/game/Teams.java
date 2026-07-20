package io.github.endx.rustedfabricapi.api.game;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;

/** Stable entry points for team objects. */
public final class Teams {
    private static final String[] TYPES = {
            "rustedwarfare.game.Team",
            "com.corrodinggames.rts.game.n"
    };

    private Teams() {
    }

    public static boolean isTeam(Object value) {
        return value != null && RustedReflection.isAnyClass(value.getClass(), TYPES);
    }

    public static TeamView view(Object team) {
        if (!isTeam(team)) {
            throw new IllegalArgumentException("Expected Team, got " + describe(team));
        }
        return new TeamView(team);
    }

    public static TeamView byId(int id) {
        Object value = RustedReflection.invokeStatic(TYPES, new String[]{"getTeamById", "k"},
                Integer.valueOf(id));
        return value == null ? null : view(value);
    }

    private static String describe(Object value) {
        return value == null ? "null" : value.getClass().getName();
    }
}
