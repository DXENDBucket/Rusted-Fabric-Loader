package io.github.endx.rustedfabricapi.api.ai;

import io.github.endx.rustedfabricapi.api.ai.event.AiControlEvents;
import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import io.github.endx.rustedfabricapi.api.util.Identifier;
import rustedwarfare.ai.AiTeam;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/** Explicit, per-team ownership of native AI replacement. */
public final class AiControllers {
    private static final Map<AiTeam, Assignment> ASSIGNMENTS =
            Collections.synchronizedMap(new WeakHashMap<AiTeam, Assignment>());

    private AiControllers() {
    }

    /**
     * Assigns a controller to exactly one AI team.
     *
     * @throws IllegalStateException when another owner already controls the team
     */
    public static Handle assign(AiTeam team, Identifier owner, AiController controller) {
        if (team == null) throw new IllegalArgumentException("team must not be null");
        if (owner == null) throw new IllegalArgumentException("owner must not be null");
        if (controller == null) throw new IllegalArgumentException("controller must not be null");
        Assignment assignment = new Assignment(owner, controller);
        synchronized (ASSIGNMENTS) {
            Assignment existing = ASSIGNMENTS.get(team);
            if (existing != null) {
                throw new IllegalStateException("AI team " + team.teamId
                        + " is already controlled by " + existing.owner);
            }
            ASSIGNMENTS.put(team, assignment);
        }
        return new Handle(team, assignment);
    }

    public static boolean isAssigned(AiTeam team) {
        return team != null && ASSIGNMENTS.containsKey(team);
    }

    public static Optional<Identifier> owner(AiTeam team) {
        Assignment assignment = team == null ? null : ASSIGNMENTS.get(team);
        return assignment == null ? Optional.empty() : Optional.of(assignment.owner);
    }

    /** Removes every assignment. Intended for session teardown and development reloads. */
    public static void clearAssignments() {
        ASSIGNMENTS.clear();
    }

    /** Internal mapped-runtime entrypoint used by the AI tick Mixin. */
    public static boolean beforeNativeTick(AiTeam team, float delta) {
        AiTickContext context = AiTickContext.capture(team, delta);
        AiControlEvents.BEFORE_TICK.invoker().beforeTick(context);
        Assignment assignment = ASSIGNMENTS.get(team);
        if (assignment == null) return false;
        AiTickDecision decision = assignment.controller.tick(context);
        if (decision == null) {
            throw new IllegalStateException("AI controller " + assignment.owner
                    + " returned null for team " + team.teamId);
        }
        if (decision == AiTickDecision.REPLACE_NATIVE) {
            AiControlEvents.AFTER_TICK.invoker().afterTick(context, AiTickOutcome.CUSTOM);
            return true;
        }
        return false;
    }

    /** Internal mapped-runtime entrypoint used after an original AI tick. */
    public static void afterNativeTick(AiTeam team, float delta) {
        AiControlEvents.AFTER_TICK.invoker().afterTick(
                AiTickContext.capture(team, delta), AiTickOutcome.NATIVE);
    }

    private static final class Assignment {
        final Identifier owner;
        final AiController controller;

        Assignment(Identifier owner, AiController controller) {
            this.owner = owner;
            this.controller = controller;
        }
    }

    public static final class Handle implements RustedFabricEvent.Registration {
        private final AiTeam team;
        private final Assignment assignment;
        private boolean active = true;

        private Handle(AiTeam team, Assignment assignment) {
            this.team = team;
            this.assignment = assignment;
        }

        public Identifier owner() { return assignment.owner; }
        public AiTeam team() { return team; }

        @Override
        public synchronized boolean unregister() {
            if (!active) return false;
            boolean removed;
            synchronized (ASSIGNMENTS) {
                removed = ASSIGNMENTS.get(team) == assignment;
                if (removed) ASSIGNMENTS.remove(team);
            }
            active = false;
            return removed;
        }

        @Override
        public void close() { unregister(); }
    }
}
