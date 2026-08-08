package io.github.endx.rustedfabricapi.api.unit.build;

import android.graphics.PointF;
import rustedwarfare.unit.Unit;
import rustedwarfare.unit.action.UnitAction;
import rustedwarfare.unit.build.BuildQueueHost;
import rustedwarfare.unit.build.BuildQueueItem;
import rustedwarfare.unit.build.FactoryQueueManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Typed query and mutation helpers for the game's build queues. */
public final class BuildQueues {
    private BuildQueues() {
    }

    public static List<BuildQueueItem> snapshot(FactoryQueueManager queue) {
        Objects.requireNonNull(queue, "queue");
        return snapshotValues(queue.getQueueItems());
    }

    public static List<BuildQueueItem> snapshot(BuildQueueHost host) {
        Objects.requireNonNull(host, "host");
        return snapshotValues(host.getBuildQueueItems());
    }

    public static Optional<BuildQueueItem> current(FactoryQueueManager queue) {
        return Optional.ofNullable(Objects.requireNonNull(queue, "queue").getCurrentQueueItem());
    }

    public static Optional<BuildQueueItem> current(BuildQueueHost host) {
        return Optional.ofNullable(Objects.requireNonNull(host, "host").getCurrentBuildQueueItem());
    }

    /** Applies an action through the queue's normal path. Call on the update thread. */
    public static BuildQueueItem apply(FactoryQueueManager queue, UnitAction action, boolean front) {
        return Objects.requireNonNull(queue, "queue").applyQueueActionWithTarget(
                Objects.requireNonNull(action, "action"), front, null, null);
    }

    public static BuildQueueItem applyAt(FactoryQueueManager queue, UnitAction action, boolean front,
                                         float worldX, float worldY, Unit targetUnit) {
        requireFinite(worldX, "worldX");
        requireFinite(worldY, "worldY");
        return Objects.requireNonNull(queue, "queue").applyQueueActionWithTarget(
                Objects.requireNonNull(action, "action"), front,
                new PointF(worldX, worldY), targetUnit);
    }

    public static BuildQueueItem applyTo(FactoryQueueManager queue, UnitAction action, boolean front,
                                         Unit targetUnit) {
        return Objects.requireNonNull(queue, "queue").applyQueueActionWithTarget(
                Objects.requireNonNull(action, "action"), front, null,
                Objects.requireNonNull(targetUnit, "targetUnit"));
    }

    /** Clears a queue and optionally refunds it. Call on the update thread. */
    public static void clear(FactoryQueueManager queue, boolean refund) {
        Objects.requireNonNull(queue, "queue").clearQueueAndRefund(refund);
    }

    public static float progress(FactoryQueueManager queue) {
        return Objects.requireNonNull(queue, "queue").buildProgress;
    }

    public static void setRallyPoint(BuildQueueHost host, float worldX, float worldY) {
        requireFinite(worldX, "worldX");
        requireFinite(worldY, "worldY");
        Objects.requireNonNull(host, "host").setBuildQueueRallyPoint(new PointF(worldX, worldY));
    }

    public static void completeCurrentInstantly(BuildQueueHost host) {
        Objects.requireNonNull(host, "host").forceBuildQueueProgressComplete();
    }

    private static List<BuildQueueItem> snapshotValues(Iterable<?> source) {
        if (source == null) {
            return Collections.emptyList();
        }
        ArrayList<BuildQueueItem> result = new ArrayList<BuildQueueItem>();
        for (Object value : source) {
            if (value instanceof BuildQueueItem) {
                result.add((BuildQueueItem) value);
            }
        }
        return Collections.unmodifiableList(result);
    }

    private static void requireFinite(float value, String name) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }
}
