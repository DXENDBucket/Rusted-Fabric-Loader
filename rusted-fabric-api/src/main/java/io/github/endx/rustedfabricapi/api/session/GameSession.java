package io.github.endx.rustedfabricapi.api.session;

import java.util.Objects;
import java.util.Optional;

import io.github.endx.rustedfabricapi.api.multiplayer.MultiplayerManifest;
import io.github.endx.rustedfabricapi.api.multiplayer.MultiplayerRequirements;

/** Platform-neutral game session exposed in single-player and multiplayer alike. */
public final class GameSession {
    public enum Kind {
        SINGLE_PLAYER,
        MULTIPLAYER_HOST,
        MULTIPLAYER_CLIENT
    }

    private final long sequence;
    private final Kind kind;
    private final long startedAtMillis;
    private final MultiplayerManifest localManifest;

    GameSession(long sequence, Kind kind, long startedAtMillis,
                MultiplayerManifest localManifest) {
        this.sequence = sequence;
        this.kind = Objects.requireNonNull(kind, "kind");
        this.startedAtMillis = startedAtMillis;
        this.localManifest = localManifest;
    }

    public long sequence() { return sequence; }
    public Kind kind() { return kind; }
    public long startedAtMillis() { return startedAtMillis; }
    public boolean multiplayer() { return kind != Kind.SINGLE_PLAYER; }
    public boolean host() { return kind == Kind.MULTIPLAYER_HOST; }
    public Optional<MultiplayerManifest> localManifest() {
        return localManifest != null
                ? Optional.of(MultiplayerRequirements.effective(localManifest))
                : Optional.empty();
    }
}
