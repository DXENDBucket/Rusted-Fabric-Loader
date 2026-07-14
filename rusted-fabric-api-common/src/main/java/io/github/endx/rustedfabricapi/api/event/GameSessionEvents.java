package io.github.endx.rustedfabricapi.api.event;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.endx.rustedfabricapi.api.session.GameSession;

/** Session lifecycle shared by Windows/Android and single-player/multiplayer. */
public final class GameSessionEvents {
    public static final SessionEvent SESSION_STARTED = new SessionEvent();
    public static final SessionEvent SESSION_ENDED = new SessionEvent();

    private GameSessionEvents() {
    }

    @FunctionalInterface
    public interface Listener { void onSession(GameSession session); }

    public interface Registration extends AutoCloseable {
        boolean unregister();
        @Override default void close() { unregister(); }
    }

    public static final class SessionEvent {
        private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();

        public Registration register(Listener listener) {
            Listener checked = Objects.requireNonNull(listener, "listener");
            listeners.add(checked);
            AtomicBoolean active = new AtomicBoolean(true);
            return () -> active.compareAndSet(true, false) && listeners.remove(checked);
        }

        public RuntimeLifecycleEvents.DispatchResult dispatch(GameSession session) {
            Objects.requireNonNull(session, "session");
            int count = 0;
            int failures = 0;
            for (Listener listener : listeners) {
                count++;
                try { listener.onSession(session); } catch (Throwable ignored) { failures++; }
            }
            return RuntimeLifecycleEvents.DispatchResult.of(count, failures);
        }
    }
}
