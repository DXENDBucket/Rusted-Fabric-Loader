package io.github.endx.rustedfabricapi.api.event;

import io.github.endx.rustedfabricapi.api.RustedFabricAPIContext;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/** Platform-neutral process events available to both JVM Jar and Android DEX mods. */
public final class RuntimeLifecycleEvents {
    public static final EngineInitializationEvent BEFORE_ENGINE_INITIALIZATION =
            new EngineInitializationEvent();
    public static final EngineInitializationEvent AFTER_ENGINE_INITIALIZATION =
            new EngineInitializationEvent();

    private RuntimeLifecycleEvents() {
    }

    @FunctionalInterface
    public interface EngineInitializationListener {
        void onEngineInitialization(RustedFabricAPIContext context);
    }

    public interface Registration extends AutoCloseable {
        boolean unregister();

        @Override
        default void close() {
            unregister();
        }
    }

    public static final class DispatchResult {
        private final int listenerCount;
        private final int failureCount;

        private DispatchResult(int listenerCount, int failureCount) {
            this.listenerCount = listenerCount;
            this.failureCount = failureCount;
        }

        public int listenerCount() {
            return listenerCount;
        }

        public int failureCount() {
            return failureCount;
        }

        public boolean succeeded() {
            return failureCount == 0;
        }
    }

    public static final class EngineInitializationEvent {
        private final CopyOnWriteArrayList<EngineInitializationListener> listeners =
                new CopyOnWriteArrayList<>();

        private EngineInitializationEvent() {
        }

        public Registration register(EngineInitializationListener listener) {
            EngineInitializationListener checked = Objects.requireNonNull(listener, "listener");
            listeners.add(checked);
            AtomicBoolean active = new AtomicBoolean(true);
            return () -> active.compareAndSet(true, false) && listeners.remove(checked);
        }

        /** Called by a loader backend. Listener failures are counted and never escape. */
        public DispatchResult dispatch(RustedFabricAPIContext context) {
            Objects.requireNonNull(context, "context");
            int failures = 0;
            int count = 0;
            for (EngineInitializationListener listener : listeners) {
                count++;
                try {
                    listener.onEngineInitialization(context);
                } catch (Throwable ignored) {
                    failures++;
                }
            }
            return new DispatchResult(count, failures);
        }

        public int listenerCount() {
            return listeners.size();
        }
    }
}
