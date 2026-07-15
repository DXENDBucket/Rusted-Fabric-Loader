package io.github.endx.rustedfabricapi.api.session;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import io.github.endx.rustedfabricapi.api.RustedFabricRuntime;
import io.github.endx.rustedfabricapi.api.event.GameSessionEvents;
import io.github.endx.rustedfabricapi.api.multiplayer.MultiplayerManifest;

/** Shared session state. Loader backends transition it; portable mods only need to observe it. */
public final class GameSessionRuntime {
    private static final AtomicLong SEQUENCE = new AtomicLong();
    private static final AtomicReference<GameSession> CURRENT = new AtomicReference<>();

    private GameSessionRuntime() {
    }

    public static Optional<GameSession> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    /** Backend entrypoint; repeated transitions to the same kind keep the current session. */
    public static GameSession transition(GameSession.Kind kind) {
        Objects.requireNonNull(kind, "kind");
        for (;;) {
            GameSession previous = CURRENT.get();
            if (previous != null && previous.kind() == kind) return previous;
            MultiplayerManifest manifest = RustedFabricRuntime.currentContext()
                    .flatMap(context -> context.multiplayerManifest()).orElse(null);
            GameSession next = new GameSession(SEQUENCE.incrementAndGet(), kind,
                    System.currentTimeMillis(), manifest);
            if (!CURRENT.compareAndSet(previous, next)) continue;
            if (previous != null) GameSessionEvents.SESSION_ENDED.dispatch(previous);
            GameSessionEvents.SESSION_STARTED.dispatch(next);
            return next;
        }
    }

    public static Optional<GameSession> endCurrent() {
        GameSession ended = CURRENT.getAndSet(null);
        if (ended != null) GameSessionEvents.SESSION_ENDED.dispatch(ended);
        return Optional.ofNullable(ended);
    }
}
