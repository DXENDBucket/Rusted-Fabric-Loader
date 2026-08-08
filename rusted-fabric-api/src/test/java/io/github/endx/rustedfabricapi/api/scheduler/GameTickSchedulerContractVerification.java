package io.github.endx.rustedfabricapi.api.scheduler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import io.github.endx.rustedfabricapi.api.session.GameSession;
import io.github.endx.rustedfabricapi.api.session.GameSessionRuntime;
import io.github.endx.rustedfabricapi.api.util.Identifier;

/** Ordering, delay, pause, recurrence, reentrancy, failure, owner, and scope checks. */
public final class GameTickSchedulerContractVerification {
    private GameTickSchedulerContractVerification() {
    }

    public static void verify() {
        verifyOrderingAndFailureIsolation();
        verifyDelayRepeatAndReentrancy();
        verifyCancellationAndScopes();
        verifyValidation();
        GameTickScheduler.clearAll();
        GameSessionRuntime.endCurrent();
    }

    private static void verifyOrderingAndFailureIsolation() {
        GameTickScheduler.clearAll();
        List<Integer> order = new ArrayList<Integer>();
        ScheduledGameTask first = GameTickScheduler.schedule(0, () -> order.add(1));
        ScheduledGameTask failed = GameTickScheduler.schedule(0, () -> {
            order.add(2);
            throw new IllegalStateException("intentional scheduler failure");
        });
        ScheduledGameTask third = GameTickScheduler.schedule(0, () -> order.add(3));

        TickExecutionReport report = GameTickScheduler.executeUpdateTick(40);
        require(order.equals(Arrays.asList(1, 2, 3))
                        && report.executedCount() == 3 && report.succeededCount() == 2
                        && report.failedCount() == 1 && report.activeTaskCount() == 0,
                "tick tasks did not retain order/isolate a failing action");
        require(first.state() == GameTaskState.COMPLETED
                        && failed.state() == GameTaskState.FAILED
                        && third.state() == GameTaskState.COMPLETED
                        && failed.lastFailure().isPresent()
                        && first.completion().isDone()
                        && failed.completion().isCompletedExceptionally(),
                "one-shot task handles did not expose final states");

        TickExecutionReport duplicate = GameTickScheduler.executeUpdateTick(40);
        require(duplicate.advancedTicks() == 0L && duplicate.executedCount() == 0,
                "paused/duplicate native tick advanced the scheduler");
    }

    private static void verifyDelayRepeatAndReentrancy() {
        GameTickScheduler.clearAll();
        List<String> calls = new ArrayList<String>();
        AtomicInteger repeats = new AtomicInteger();
        ScheduledGameTask repeating = GameTickScheduler.repeat(
                "scheduler_contract:repeat", 2, 2, GameTaskScope.MAP, () -> {
                    int count = repeats.incrementAndGet();
                    calls.add("repeat-" + count);
                    if (count == 1) {
                        GameTickScheduler.schedule("scheduler_contract:nested", 0,
                                GameTaskScope.MAP, () -> calls.add("nested"));
                    }
                });

        require(GameTickScheduler.executeUpdateTick(100).executedCount() == 0,
                "two-tick initial delay fired one tick early");
        require(GameTickScheduler.executeUpdateTick(101).executedCount() == 1
                        && calls.equals(Arrays.asList("repeat-1")),
                "repeating task did not fire at its initial delay");
        require(GameTickScheduler.executeUpdateTick(102).executedCount() == 1
                        && calls.equals(Arrays.asList("repeat-1", "nested")),
                "task scheduled during dispatch re-entered the same scheduler phase");
        require(GameTickScheduler.executeUpdateTick(103).executedCount() == 1
                        && calls.equals(Arrays.asList("repeat-1", "nested", "repeat-2"))
                        && repeating.executionCount() == 2L,
                "repeating period did not use distinct simulation ticks");
        require(repeating.cancel() && !repeating.cancel()
                        && repeating.state() == GameTaskState.CANCELLED
                        && repeating.completion().isCompletedExceptionally(),
                "repeating task cancellation was not idempotent/observable");

        AtomicReference<ScheduledGameTask> self = new AtomicReference<ScheduledGameTask>();
        self.set(GameTickScheduler.repeat(0, 1, () -> self.get().cancel()));
        GameTickScheduler.executeUpdateTick(104);
        require(self.get().state() == GameTaskState.CANCELLED
                        && self.get().executionCount() == 1L,
                "task cancelled while running was rescheduled");
    }

    private static void verifyCancellationAndScopes() {
        GameTickScheduler.clearAll();
        Identifier ownerA = Identifier.parse("scheduler_contract:owner_a");
        Identifier ownerB = Identifier.parse("scheduler_contract:owner_b");
        GameTickScheduler.schedule(ownerA, 5, GameTaskScope.MAP, () -> { });
        GameTickScheduler.schedule(ownerA, 5, GameTaskScope.GLOBAL, () -> { });
        GameTickScheduler.schedule(ownerB, 5, GameTaskScope.MAP, () -> { });
        require(GameTickScheduler.activeTasks(ownerA).size() == 2
                        && GameTickScheduler.cancelOwner(ownerA) == 2
                        && GameTickScheduler.activeTaskCount() == 1,
                "owner-level task lookup/cancellation was incorrect");
        GameTickScheduler.clearAll();

        GameSessionRuntime.endCurrent();
        GameSessionRuntime.transition(GameSession.Kind.SINGLE_PLAYER);
        ScheduledGameTask map = GameTickScheduler.schedule(
                5, GameTaskScope.MAP, () -> { });
        ScheduledGameTask session = GameTickScheduler.schedule(
                5, GameTaskScope.SESSION, () -> { });
        ScheduledGameTask global = GameTickScheduler.schedule(
                5, GameTaskScope.GLOBAL, () -> { });
        GameTickScheduler.executeUpdateTick(200);
        require(GameTickScheduler.beginMap() == 1
                        && map.state() == GameTaskState.CANCELLED
                        && session.active() && global.active()
                        && !GameTickScheduler.lastNativeTick().isPresent(),
                "successful map boundary did not clear only MAP tasks/reset its tick anchor");

        GameSessionRuntime.endCurrent();
        require(session.state() == GameTaskState.CANCELLED && global.active()
                        && GameTickScheduler.activeTaskCount() == 1,
                "session end did not clear SESSION tasks while preserving GLOBAL tasks");
        GameTickScheduler.clearAll();
    }

    private static void verifyValidation() {
        expectIllegal(() -> GameTickScheduler.schedule(-1, () -> { }),
                "negative tick delay was accepted");
        expectIllegal(() -> GameTickScheduler.repeat(0, 0, () -> { }),
                "zero repeat period was accepted");
        expectIllegal(() -> GameTickScheduler.schedule(
                        "invalid owner", 0, GameTaskScope.MAP, () -> { }),
                "invalid task owner ID was accepted");
    }

    private static void expectIllegal(Runnable action, String message) {
        try {
            action.run();
            throw new AssertionError(message);
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
