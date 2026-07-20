package io.github.endx.rustedfabricapi.api.client.option;

import java.util.Objects;

/** Result of one atomic API-mediated option transaction. */
public final class ClientOptionUpdateResult {
    private final ClientOptionChangeSet changes;
    private final boolean applied;
    private final boolean cancelled;
    private final boolean persistenceRequested;
    private final boolean persistenceSuccessful;

    ClientOptionUpdateResult(ClientOptionChangeSet changes, boolean applied, boolean cancelled,
            boolean persistenceRequested, boolean persistenceSuccessful) {
        this.changes = Objects.requireNonNull(changes, "changes");
        this.applied = applied;
        this.cancelled = cancelled;
        this.persistenceRequested = persistenceRequested;
        this.persistenceSuccessful = persistenceSuccessful;
    }

    public ClientOptionChangeSet changes() { return changes; }
    public boolean applied() { return applied; }
    public boolean cancelled() { return cancelled; }
    public boolean persistenceRequested() { return persistenceRequested; }
    /** True only when persistence was requested and the native save returned success. */
    public boolean persistenceSuccessful() { return persistenceSuccessful; }
    public boolean restartRequired() { return changes.restartRequired(); }
}
