package io.github.endx.rustedfabricapi.api;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import io.github.endx.rustedfabricapi.api.session.GameSession;
import io.github.endx.rustedfabricapi.api.session.GameSessionRuntime;

/** Current process runtime context shared by platform backends and portable mods. */
public final class RustedFabricRuntime {
    private static final AtomicReference<RustedFabricAPIContext> CONTEXT = new AtomicReference<>();

    private RustedFabricRuntime() {
    }

    public static void installContext(RustedFabricAPIContext context) {
        CONTEXT.set(Objects.requireNonNull(context, "context"));
    }

    public static Optional<RustedFabricAPIContext> currentContext() {
        return Optional.ofNullable(CONTEXT.get());
    }

    /** Current game session, including the always-supported single-player session. */
    public static Optional<GameSession> currentSession() {
        return GameSessionRuntime.current();
    }

    static void resetForTests() {
        CONTEXT.set(null);
    }
}
