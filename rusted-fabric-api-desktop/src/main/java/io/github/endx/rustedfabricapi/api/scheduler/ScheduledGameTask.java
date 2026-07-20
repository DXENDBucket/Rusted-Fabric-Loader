package io.github.endx.rustedfabricapi.api.scheduler;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import io.github.endx.rustedfabricapi.api.util.Identifier;

/** Thread-safe cancellation and observation handle for one game-tick task. */
public final class ScheduledGameTask {
    final long sequence;
    final Identifier owner;
    final GameTaskScope scope;
    final Runnable action;
    final long periodTicks;
    final CompletableFuture<Void> completion = new CompletableFuture<Void>();
    volatile long remainingTicks;
    volatile long executionCount;
    volatile GameTaskState state = GameTaskState.PENDING;
    volatile Throwable lastFailure;

    ScheduledGameTask(long sequence, Identifier owner, GameTaskScope scope,
            long delayTicks, long periodTicks, Runnable action) {
        this.sequence = sequence;
        this.owner = Objects.requireNonNull(owner, "owner");
        this.scope = Objects.requireNonNull(scope, "scope");
        this.remainingTicks = delayTicks;
        this.periodTicks = periodTicks;
        this.action = Objects.requireNonNull(action, "action");
    }

    public long sequence() { return sequence; }

    public Identifier owner() { return owner; }

    public GameTaskScope scope() { return scope; }

    public boolean repeating() { return periodTicks > 0L; }

    public long periodTicks() { return periodTicks; }

    public long remainingTicks() { return Math.max(0L, remainingTicks); }

    public long executionCount() { return executionCount; }

    public GameTaskState state() { return state; }

    public boolean active() {
        return state == GameTaskState.PENDING || state == GameTaskState.RUNNING;
    }

    public Optional<Throwable> lastFailure() { return Optional.ofNullable(lastFailure); }

    /** A defensive dependent future; completing it cannot mutate the scheduler's state. */
    public CompletableFuture<Void> completion() { return completion.copy(); }

    public boolean cancel() { return GameTickScheduler.cancel(this); }

    @Override public String toString() {
        return "ScheduledGameTask{" + sequence + ", owner=" + owner + ", scope=" + scope
                + ", state=" + state + ", executions=" + executionCount + '}';
    }
}
