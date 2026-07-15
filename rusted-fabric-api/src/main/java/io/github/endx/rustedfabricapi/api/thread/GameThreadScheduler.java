package io.github.endx.rustedfabricapi.api.thread;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

import io.github.endx.rustedfabricapi.api.RustedFabricCapabilities;
import io.github.endx.rustedfabricapi.api.RustedFabricRuntime;

/** Queues isolated work for the next mapped update or render phase. */
public final class GameThreadScheduler {
    private static final ConcurrentLinkedQueue<Task> UPDATE_TASKS =
            new ConcurrentLinkedQueue<Task>();
    private static final ConcurrentLinkedQueue<Task> RENDER_TASKS =
            new ConcurrentLinkedQueue<Task>();
    private static final AtomicReference<Thread> UPDATE_THREAD = new AtomicReference<Thread>();
    private static final AtomicReference<Thread> RENDER_THREAD = new AtomicReference<Thread>();

    private GameThreadScheduler() {
    }

    public static CompletableFuture<Void> onNextUpdate(Runnable action) {
        requireLifecycleBackend();
        return enqueue(UPDATE_TASKS, action);
    }

    public static CompletableFuture<Void> onNextRender(Runnable action) {
        requireLifecycleBackend();
        return enqueue(RENDER_TASKS, action);
    }

    public static boolean isUpdateThread() {
        return Thread.currentThread() == UPDATE_THREAD.get();
    }

    public static boolean isRenderThread() {
        return Thread.currentThread() == RENDER_THREAD.get();
    }

    public static int pendingUpdateTasks() {
        return UPDATE_TASKS.size();
    }

    public static int pendingRenderTasks() {
        return RENDER_TASKS.size();
    }

    /** Backend bridge. Mod code should schedule work through {@link #onNextUpdate(Runnable)}. */
    public static void executeUpdatePhase() {
        UPDATE_THREAD.set(Thread.currentThread());
        drain(UPDATE_TASKS);
    }

    /** Backend bridge. Mod code should schedule work through {@link #onNextRender(Runnable)}. */
    public static void executeRenderPhase() {
        RENDER_THREAD.set(Thread.currentThread());
        drain(RENDER_TASKS);
    }

    private static CompletableFuture<Void> enqueue(ConcurrentLinkedQueue<Task> queue,
                                                    Runnable action) {
        CompletableFuture<Void> completion = new CompletableFuture<Void>();
        queue.add(new Task(Objects.requireNonNull(action, "action"), completion));
        return completion;
    }

    private static void drain(ConcurrentLinkedQueue<Task> queue) {
        int count = queue.size();
        for (int index = 0; index < count; index++) {
            Task task = queue.poll();
            if (task == null) break;
            try {
                task.action.run();
                task.completion.complete(null);
            } catch (ThreadDeath | VirtualMachineError critical) {
                task.completion.completeExceptionally(critical);
                throw critical;
            } catch (Throwable failure) {
                task.completion.completeExceptionally(failure);
            }
        }
    }

    private static void requireLifecycleBackend() {
        boolean available = RustedFabricRuntime.currentContext()
                .map(context -> context.hasCapability(RustedFabricCapabilities.GAME_LIFECYCLE))
                .orElse(false);
        if (!available) {
            throw new UnsupportedOperationException(
                    "Mapped game update/render scheduling is unavailable on this backend");
        }
    }

    private static final class Task {
        final Runnable action;
        final CompletableFuture<Void> completion;

        Task(Runnable action, CompletableFuture<Void> completion) {
            this.action = action;
            this.completion = completion;
        }
    }
}
