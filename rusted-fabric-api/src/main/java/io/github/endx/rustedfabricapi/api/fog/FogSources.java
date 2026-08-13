package io.github.endx.rustedfabricapi.api.fog;

import io.github.endx.rustedfabricapi.api.event.GameSessionEvents;
import io.github.endx.rustedfabricapi.api.custom.CustomUnitHandle;
import io.github.endx.rustedfabricapi.api.geometry.GeometryBounds;
import io.github.endx.rustedfabricapi.api.geometry.GeometryMask;
import io.github.endx.rustedfabricapi.api.unit.Teams;
import rustedwarfare.game.Team;
import rustedwarfare.map.MapEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/** Active geometry-backed fog sources reapplied after each native LOS update. */
public final class FogSources {
    /** A negative duration creates a source that remains until cancelled. */
    public static final float PERMANENT = -1.0F;
    private static final Object LOCK = new Object();
    private static final AtomicLong IDS = new AtomicLong();
    private static final ArrayList<Source> ACTIVE = new ArrayList<Source>();

    static {
        GameSessionEvents.SESSION_ENDED.register(session -> clear());
    }

    private FogSources() { }

    public static FogSourceHandle add(Team team, FogOperation operation,
                                      float durationTicks, FogMaskProvider provider) {
        return add(team, operation, durationTicks, true, provider);
    }

    /**
     * Reveals a small moving world-space circle to one exact team while following a custom unit.
     * Reuse and {@link FogSourceHandle#refresh(float) refresh} the returned handle for repeated
     * hits instead of creating one source per hit.
     */
    public static FogSourceHandle revealFollowing(int teamId, float durationTicks,
                                                   CustomUnitHandle followed, float radius) {
        CustomUnitHandle checkedUnit = Objects.requireNonNull(followed, "followed");
        if (!Float.isFinite(radius) || radius <= 0.0F) {
            throw new IllegalArgumentException("radius must be finite and positive");
        }
        Team team = Teams.findById(teamId).orElseThrow(() ->
                new IllegalArgumentException("fog team does not exist: " + teamId));
        FollowingCircleMask mask = new FollowingCircleMask(checkedUnit, radius);
        return add(team, FogOperation.REVEAL, durationTicks, true,
                () -> checkedUnit.alive() ? mask : null);
    }

    /**
     * Adds a source and controls whether its mask can move or otherwise change over time.
     * Static reveal/explore masks are only replayed when the native LOS map is rebuilt;
     * conceal/shroud sources still run every tick so nearby units cannot reveal through them.
     */
    public static FogSourceHandle add(Team team, FogOperation operation,
                                      float durationTicks, boolean dynamic,
                                      FogMaskProvider provider) {
        Objects.requireNonNull(team, "team");
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(provider, "provider");
        if (!Float.isFinite(durationTicks) || durationTicks == 0.0F) {
            throw new IllegalArgumentException("durationTicks must be finite and non-zero");
        }
        Source source = new Source(IDS.incrementAndGet(), team, operation,
                durationTicks, dynamic, provider);
        synchronized (LOCK) { ACTIVE.add(source); }
        return source;
    }

    public static List<FogSourceHandle> active() {
        synchronized (LOCK) {
            return Collections.unmodifiableList(new ArrayList<FogSourceHandle>(ACTIVE));
        }
    }

    public static int clear() {
        synchronized (LOCK) {
            int count = ACTIVE.size();
            for (Source source : ACTIVE) source.active = false;
            ACTIVE.clear();
            return count;
        }
    }

    /** Native bridge invoked after the game's normal LOS refresh. */
    public static void updateAfterNativeLos(MapEngine map, float delta, boolean nativeLosRefresh) {
        if (map == null) return;
        List<Source> snapshot;
        synchronized (LOCK) { snapshot = new ArrayList<Source>(ACTIVE); }
        for (Source source : snapshot) {
            if (!source.active) continue;
            if (source.remaining > 0.0F) {
                source.remaining -= delta;
                if (source.remaining <= 0.0F) {
                    source.cancel();
                    continue;
                }
            }
            if (!map.useFogOfWar) continue;
            boolean mustReapply = source.dynamic || nativeLosRefresh
                    || source.operation == FogOperation.CONCEAL
                    || source.operation == FogOperation.SHROUD;
            if (!mustReapply) continue;
            GeometryMask mask;
            try {
                mask = source.provider.currentMask();
                if (mask != null) FogOfWar.apply(source.team, mask, source.operation);
            } catch (RuntimeException failure) {
                System.err.println("[Rusted Fabric API] Removing failed fog source "
                        + source.id + ": " + failure.getMessage());
                source.cancel();
            }
        }
        synchronized (LOCK) {
            Iterator<Source> iterator = ACTIVE.iterator();
            while (iterator.hasNext()) if (!iterator.next().active) iterator.remove();
        }
    }

    private static final class Source implements FogSourceHandle {
        private final long id;
        private final Team team;
        private final FogOperation operation;
        private final boolean dynamic;
        private final FogMaskProvider provider;
        private volatile boolean active = true;
        private float remaining;
        private Source(long id, Team team, FogOperation operation,
                       float duration, boolean dynamic, FogMaskProvider provider) {
            this.id = id;
            this.team = team;
            this.operation = operation;
            this.remaining = duration;
            this.dynamic = dynamic;
            this.provider = provider;
        }
        @Override public long id() { return id; }
        @Override public Team team() { return team; }
        @Override public FogOperation operation() { return operation; }
        @Override public boolean active() { return active; }
        @Override public float remainingTicks() { return remaining; }
        @Override public boolean refresh(float durationTicks) {
            if (!active) return false;
            if (!Float.isFinite(durationTicks) || durationTicks <= 0.0F) {
                throw new IllegalArgumentException("durationTicks must be finite and positive");
            }
            remaining = durationTicks;
            return true;
        }
        @Override public boolean cancel() {
            if (!active) return false;
            active = false;
            return true;
        }
    }

    private static final class FollowingCircleMask implements GeometryMask {
        private final CustomUnitHandle unit;
        private final float radius;
        private final float radiusSquared;

        private FollowingCircleMask(CustomUnitHandle unit, float radius) {
            this.unit = unit;
            this.radius = radius;
            this.radiusSquared = radius * radius;
        }

        @Override public GeometryBounds bounds() {
            float x = unit.x();
            float y = unit.y();
            return new GeometryBounds(x - radius, y - radius, x + radius, y + radius);
        }

        @Override public boolean contains(float x, float y) {
            float dx = x - unit.x();
            float dy = y - unit.y();
            return dx * dx + dy * dy <= radiusSquared;
        }
    }
}
