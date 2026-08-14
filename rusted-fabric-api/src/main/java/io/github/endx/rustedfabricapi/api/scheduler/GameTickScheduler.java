package io.github.endx.rustedfabricapi.api.scheduler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicLong;

import io.github.endx.rustedfabricapi.api.event.GameSessionEvents;
import io.github.endx.rustedfabricapi.api.util.Identifier;

/** Deterministic, insertion-ordered scheduler advanced by distinct native simulation ticks. */
public final class GameTickScheduler {
    private static final Identifier UNOWNED = Identifier.of("rustedfabric", "unowned_task");
    private static final Object LOCK = new Object();
    private static final AtomicLong SEQUENCE = new AtomicLong();
    private static final LinkedHashMap<Long, ScheduledGameTask> ACTIVE =
            new LinkedHashMap<Long, ScheduledGameTask>();
    private static Integer lastNativeTick;
    private static boolean dispatching;
    private static volatile TickExecutionReport lastReport = new TickExecutionReport(
            0, 0L, Collections.emptyList(), Collections.emptyList(), 0);

    static {
        GameSessionEvents.SESSION_ENDED.register(session -> endSession());
    }

    private GameTickScheduler() {
    }

    public static ScheduledGameTask schedule(int delayTicks, Runnable action) {
        return schedule(UNOWNED, delayTicks, GameTaskScope.MAP, action);
    }

    public static ScheduledGameTask schedule(int delayTicks, GameTaskScope scope,
            Runnable action) {
        return schedule(UNOWNED, delayTicks, scope, action);
    }

    public static ScheduledGameTask schedule(String owner, int delayTicks,
            GameTaskScope scope, Runnable action) {
        return schedule(Identifier.parse(owner), delayTicks, scope, action);
    }

    public static ScheduledGameTask schedule(Identifier owner, int delayTicks,
            GameTaskScope scope, Runnable action) {
        return add(owner, checkedDelay(delayTicks), 0L, scope, action);
    }

    public static ScheduledGameTask repeat(int initialDelayTicks, int periodTicks,
            Runnable action) {
        return repeat(UNOWNED, initialDelayTicks, periodTicks, GameTaskScope.MAP, action);
    }

    public static ScheduledGameTask repeat(int initialDelayTicks, int periodTicks,
            GameTaskScope scope, Runnable action) {
        return repeat(UNOWNED, initialDelayTicks, periodTicks, scope, action);
    }

    public static ScheduledGameTask repeat(String owner, int initialDelayTicks,
            int periodTicks, GameTaskScope scope, Runnable action) {
        return repeat(Identifier.parse(owner), initialDelayTicks, periodTicks, scope, action);
    }

    public static ScheduledGameTask repeat(Identifier owner, int initialDelayTicks,
            int periodTicks, GameTaskScope scope, Runnable action) {
        if (periodTicks <= 0) {
            throw new IllegalArgumentException("periodTicks must be positive");
        }
        return add(owner, checkedDelay(initialDelayTicks), periodTicks, scope, action);
    }

    public static List<ScheduledGameTask> activeTasks() {
        synchronized (LOCK) {
            return Collections.unmodifiableList(
                    new ArrayList<ScheduledGameTask>(ACTIVE.values()));
        }
    }

    public static List<ScheduledGameTask> activeTasks(Identifier owner) {
        Identifier checked = Objects.requireNonNull(owner, "owner");
        ArrayList<ScheduledGameTask> result = new ArrayList<ScheduledGameTask>();
        synchronized (LOCK) {
            for (ScheduledGameTask task : ACTIVE.values()) {
                if (task.owner.equals(checked)) result.add(task);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public static int activeTaskCount() {
        synchronized (LOCK) { return ACTIVE.size(); }
    }

    public static OptionalInt lastNativeTick() {
        synchronized (LOCK) {
            return lastNativeTick != null ? OptionalInt.of(lastNativeTick.intValue())
                    : OptionalInt.empty();
        }
    }

    public static TickExecutionReport lastReport() { return lastReport; }

    public static int cancelOwner(String owner) {
        return cancelOwner(Identifier.parse(owner));
    }

    public static int cancelOwner(Identifier owner) {
        Identifier checked = Objects.requireNonNull(owner, "owner");
        return cancelMatching(task -> task.owner.equals(checked));
    }

    public static int cancelScope(GameTaskScope scope) {
        GameTaskScope checked = Objects.requireNonNull(scope, "scope");
        return cancelMatching(task -> task.scope == checked);
    }

    /** Cancels every task and resets the native-tick anchor; useful for controlled teardown. */
    public static int clearAll() {
        int cancelled = cancelMatching(task -> true);
        synchronized (LOCK) {
            lastNativeTick = null;
            lastReport = new TickExecutionReport(0, 0L, Collections.emptyList(),
                    Collections.emptyList(), 0);
        }
        return cancelled;
    }

    /** Desktop backend bridge called immediately before successful-map-loaded listeners. */
    public static int beginMap() {
        int cancelled = cancelScope(GameTaskScope.MAP);
        io.github.endx.rustedfabricapi.impl.combat.DeferredDamageRuntime.clear();
        synchronized (LOCK) { lastNativeTick = null; }
        return cancelled;
    }

    /** Backend bridge called once at the update scheduler phase. */
    public static TickExecutionReport executeUpdateTick(int nativeTick) {
        final long advanced;
        final List<ScheduledGameTask> due = new ArrayList<ScheduledGameTask>();
        synchronized (LOCK) {
            if (dispatching) {
                throw new IllegalStateException("Game tick scheduler is already dispatching");
            }
            if (lastNativeTick != null && lastNativeTick.intValue() == nativeTick) {
                TickExecutionReport report = new TickExecutionReport(nativeTick, 0L,
                        Collections.emptyList(), Collections.emptyList(), ACTIVE.size());
                lastReport = report;
                return report;
            }
            if (lastNativeTick == null || nativeTick < lastNativeTick.intValue()) {
                advanced = 1L;
            } else {
                advanced = (long) nativeTick - lastNativeTick.intValue();
            }
            lastNativeTick = Integer.valueOf(nativeTick);
            for (ScheduledGameTask task : ACTIVE.values()) {
                if (task.state != GameTaskState.PENDING) continue;
                task.remainingTicks -= advanced;
                if (task.remainingTicks <= 0L) {
                    task.state = GameTaskState.RUNNING;
                    due.add(task);
                }
            }
            dispatching = true;
        }

        ArrayList<Long> executed = new ArrayList<Long>();
        ArrayList<Long> failed = new ArrayList<Long>();
        try {
            for (ScheduledGameTask task : due) {
                synchronized (LOCK) {
                    if (task.state != GameTaskState.RUNNING) continue;
                    task.executionCount++;
                }
                executed.add(Long.valueOf(task.sequence));
                try {
                    task.action.run();
                    boolean complete = false;
                    synchronized (LOCK) {
                        if (task.state == GameTaskState.RUNNING) {
                            if (task.repeating()) {
                                task.remainingTicks = task.periodTicks;
                                task.state = GameTaskState.PENDING;
                            } else {
                                ACTIVE.remove(Long.valueOf(task.sequence));
                                task.state = GameTaskState.COMPLETED;
                                complete = true;
                            }
                        }
                    }
                    if (complete) task.completion.complete(null);
                } catch (ThreadDeath | VirtualMachineError critical) {
                    fail(task, critical);
                    throw critical;
                } catch (Throwable failure) {
                    failed.add(Long.valueOf(task.sequence));
                    fail(task, failure);
                }
            }
        } finally {
            synchronized (LOCK) {
                for (ScheduledGameTask task : due) {
                    if (task.state == GameTaskState.RUNNING) {
                        task.remainingTicks = 1L;
                        task.state = GameTaskState.PENDING;
                    }
                }
                dispatching = false;
            }
        }

        TickExecutionReport report;
        synchronized (LOCK) {
            report = new TickExecutionReport(nativeTick, advanced, executed, failed, ACTIVE.size());
            lastReport = report;
        }
        return report;
    }

    static boolean cancel(ScheduledGameTask task) {
        Objects.requireNonNull(task, "task");
        boolean cancelled;
        synchronized (LOCK) {
            ScheduledGameTask active = ACTIVE.get(Long.valueOf(task.sequence));
            cancelled = active == task && task.active();
            if (cancelled) {
                ACTIVE.remove(Long.valueOf(task.sequence));
                task.state = GameTaskState.CANCELLED;
            }
        }
        if (cancelled) task.completion.cancel(false);
        return cancelled;
    }

    private static ScheduledGameTask add(Identifier owner, long delayTicks, long periodTicks,
            GameTaskScope scope, Runnable action) {
        ScheduledGameTask task = new ScheduledGameTask(SEQUENCE.incrementAndGet(),
                Objects.requireNonNull(owner, "owner"), Objects.requireNonNull(scope, "scope"),
                delayTicks, periodTicks, Objects.requireNonNull(action, "action"));
        synchronized (LOCK) { ACTIVE.put(Long.valueOf(task.sequence), task); }
        return task;
    }

    private static long checkedDelay(int delayTicks) {
        if (delayTicks < 0) throw new IllegalArgumentException("delayTicks must not be negative");
        return Math.max(1L, (long) delayTicks);
    }

    private static int cancelMatching(TaskPredicate predicate) {
        ArrayList<ScheduledGameTask> cancelled = new ArrayList<ScheduledGameTask>();
        synchronized (LOCK) {
            java.util.Iterator<Map.Entry<Long, ScheduledGameTask>> iterator =
                    ACTIVE.entrySet().iterator();
            while (iterator.hasNext()) {
                ScheduledGameTask task = iterator.next().getValue();
                if (!predicate.test(task) || !task.active()) continue;
                iterator.remove();
                task.state = GameTaskState.CANCELLED;
                cancelled.add(task);
            }
        }
        for (ScheduledGameTask task : cancelled) task.completion.cancel(false);
        return cancelled.size();
    }

    private static void fail(ScheduledGameTask task, Throwable failure) {
        boolean markFailed = false;
        synchronized (LOCK) {
            task.lastFailure = failure;
            if (task.state == GameTaskState.RUNNING) {
                ACTIVE.remove(Long.valueOf(task.sequence));
                task.state = GameTaskState.FAILED;
                markFailed = true;
            }
        }
        if (markFailed) task.completion.completeExceptionally(failure);
    }

    private static void endSession() {
        cancelScope(GameTaskScope.MAP);
        cancelScope(GameTaskScope.SESSION);
        io.github.endx.rustedfabricapi.impl.combat.DeferredDamageRuntime.clear();
        synchronized (LOCK) { lastNativeTick = null; }
    }

    @FunctionalInterface
    private interface TaskPredicate { boolean test(ScheduledGameTask task); }
}
