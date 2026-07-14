package io.github.endx.rustedfabricapi.api.event;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.endx.rustedfabricapi.api.multiplayer.MultiplayerCompatibility;
import io.github.endx.rustedfabricapi.api.multiplayer.MultiplayerManifest;

/** Exception-isolated, platform-neutral multiplayer compatibility events. */
public final class MultiplayerCompatibilityEvents {
    public static final ManifestEvent LOCAL_MANIFEST_READY = new ManifestEvent();
    public static final EvaluationEvent COMPATIBILITY_EVALUATED = new EvaluationEvent();

    private MultiplayerCompatibilityEvents() {
    }

    public interface Registration extends AutoCloseable {
        boolean unregister();
        @Override default void close() { unregister(); }
    }

    @FunctionalInterface
    public interface ManifestListener {
        void onManifest(MultiplayerManifest manifest);
    }

    @FunctionalInterface
    public interface EvaluationListener {
        void onEvaluation(MultiplayerCompatibility.Report report);
    }

    public static final class DispatchResult {
        private final int listeners;
        private final int failures;

        private DispatchResult(int listeners, int failures) {
            this.listeners = listeners;
            this.failures = failures;
        }

        public int listenerCount() { return listeners; }
        public int failureCount() { return failures; }
        public boolean succeeded() { return failures == 0; }
    }

    public static final class ManifestEvent {
        private final CopyOnWriteArrayList<ManifestListener> listeners =
                new CopyOnWriteArrayList<>();

        public Registration register(ManifestListener listener) {
            if (listener == null) throw new NullPointerException("listener");
            listeners.add(listener);
            AtomicBoolean active = new AtomicBoolean(true);
            return () -> active.compareAndSet(true, false) && listeners.remove(listener);
        }

        public DispatchResult dispatch(MultiplayerManifest manifest) {
            if (manifest == null) throw new NullPointerException("manifest");
            int failures = 0;
            for (ManifestListener listener : listeners) {
                try { listener.onManifest(manifest); } catch (Throwable ignored) { failures++; }
            }
            return new DispatchResult(listeners.size(), failures);
        }
    }

    public static final class EvaluationEvent {
        private final CopyOnWriteArrayList<EvaluationListener> listeners =
                new CopyOnWriteArrayList<>();

        public Registration register(EvaluationListener listener) {
            if (listener == null) throw new NullPointerException("listener");
            listeners.add(listener);
            AtomicBoolean active = new AtomicBoolean(true);
            return () -> active.compareAndSet(true, false) && listeners.remove(listener);
        }

        public DispatchResult dispatch(MultiplayerCompatibility.Report report) {
            if (report == null) throw new NullPointerException("report");
            int failures = 0;
            for (EvaluationListener listener : listeners) {
                try { listener.onEvaluation(report); } catch (Throwable ignored) { failures++; }
            }
            return new DispatchResult(listeners.size(), failures);
        }
    }
}
