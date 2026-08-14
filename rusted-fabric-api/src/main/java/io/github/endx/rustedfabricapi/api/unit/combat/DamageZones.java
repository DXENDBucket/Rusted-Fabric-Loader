package io.github.endx.rustedfabricapi.api.unit.combat;

import io.github.endx.rustedfabricapi.api.game.TeamView;
import io.github.endx.rustedfabricapi.api.game.UnitView;
import io.github.endx.rustedfabricapi.api.game.Units;
import io.github.endx.rustedfabricapi.api.scheduler.GameTaskScope;
import io.github.endx.rustedfabricapi.api.scheduler.GameTickScheduler;
import io.github.endx.rustedfabricapi.api.scheduler.ScheduledGameTask;
import io.github.endx.rustedfabricapi.api.util.Identifier;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/** Deterministic native-damage areas that pulse without spawning helper units or projectiles. */
public final class DamageZones {
    private static final Identifier OWNER = Identifier.of("rustedfabricapi", "damage_zone");

    private DamageZones() { }

    public static Handle create(UnitView source, float x, float y, float radius,
                                int durationTicks, int intervalTicks, float centerDamage,
                                Falloff falloff, String... damageTags) {
        Objects.requireNonNull(source, "source");
        TeamView sourceTeam = source.team().orElseThrow(
                () -> new IllegalArgumentException("source must belong to a team"));
        requireFinite(x, "x");
        requireFinite(y, "y");
        if (!Float.isFinite(radius) || radius <= 0.0F) {
            throw new IllegalArgumentException("radius must be finite and positive");
        }
        if (durationTicks <= 0 || intervalTicks <= 0 || durationTicks < intervalTicks) {
            throw new IllegalArgumentException(
                    "duration and interval must be positive, with duration >= interval");
        }
        if (!Float.isFinite(centerDamage) || centerDamage < 0.0F) {
            throw new IllegalArgumentException("centerDamage must be finite and non-negative");
        }
        String[] checkedTags = checkedTags(damageTags);
        int pulses = durationTicks / intervalTicks;
        Zone zone = new Zone(source, sourceTeam, x, y, radius, centerDamage,
                Objects.requireNonNull(falloff, "falloff"), checkedTags, pulses);
        AtomicReference<ScheduledGameTask> task = new AtomicReference<ScheduledGameTask>();
        ScheduledGameTask scheduled = GameTickScheduler.repeat(OWNER, intervalTicks,
                intervalTicks, GameTaskScope.MAP, () -> {
                    zone.pulse();
                    if (--zone.remainingPulses == 0) {
                        ScheduledGameTask current = task.get();
                        if (current != null) current.cancel();
                    }
                });
        task.set(scheduled);
        return new Handle(scheduled, pulses);
    }

    /** Pure falloff helper: {@code distance=0} is full damage and the radius edge is zero. */
    public static float multiplier(Falloff falloff, float distance, float radius) {
        Objects.requireNonNull(falloff, "falloff");
        if (!Float.isFinite(distance) || distance < 0.0F
                || !Float.isFinite(radius) || radius <= 0.0F) {
            throw new IllegalArgumentException("distance/radius must be finite and valid");
        }
        if (distance >= radius) return 0.0F;
        return falloff == Falloff.NONE ? 1.0F : Math.max(0.0F, 1.0F - distance / radius);
    }

    public enum Falloff {
        NONE,
        LINEAR
    }

    public static final class Handle {
        private final ScheduledGameTask task;
        private final int totalPulses;

        private Handle(ScheduledGameTask task, int totalPulses) {
            this.task = task;
            this.totalPulses = totalPulses;
        }

        public boolean active() { return task.active(); }
        public long completedPulses() { return Math.min(totalPulses, task.executionCount()); }
        public int totalPulses() { return totalPulses; }
        public boolean cancel() { return task.cancel(); }
    }

    private static final class Zone {
        private final UnitView source;
        private final TeamView sourceTeam;
        private final float x;
        private final float y;
        private final float radius;
        private final float centerDamage;
        private final Falloff falloff;
        private final String[] tags;
        private int remainingPulses;

        private Zone(UnitView source, TeamView sourceTeam, float x, float y, float radius,
                     float centerDamage, Falloff falloff, String[] tags, int remainingPulses) {
            this.source = source;
            this.sourceTeam = sourceTeam;
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.centerDamage = centerDamage;
            this.falloff = falloff;
            this.tags = tags;
            this.remainingPulses = remainingPulses;
        }

        private void pulse() {
            if (!(centerDamage > 0.0F)) return;
            for (UnitView target : Units.within(x, y, radius)) {
                if (!target.alive()) continue;
                TeamView targetTeam = target.team().orElse(null);
                if (targetTeam == null || !sourceTeam.enemyOf(targetTeam)) continue;
                float dx = target.x() - x;
                float dy = target.y() - y;
                float amount = centerDamage * multiplier(
                        falloff, (float) Math.sqrt(dx * dx + dy * dy), radius);
                if (amount > 0.0F) TaggedDamage.apply(target, source, amount, tags);
            }
        }
    }

    private static String[] checkedTags(String[] tags) {
        Objects.requireNonNull(tags, "damageTags");
        String[] copy = Arrays.copyOf(tags, tags.length);
        for (int index = 0; index < copy.length; index++) {
            String tag = Objects.requireNonNull(copy[index], "damageTags contains null").trim();
            if (tag.isEmpty()) throw new IllegalArgumentException("damage tag must not be blank");
            copy[index] = tag;
        }
        return copy;
    }

    private static void requireFinite(float value, String name) {
        if (!Float.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
    }
}
