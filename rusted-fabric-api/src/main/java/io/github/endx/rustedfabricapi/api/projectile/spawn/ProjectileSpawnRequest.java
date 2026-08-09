package io.github.endx.rustedfabricapi.api.projectile.spawn;

import java.util.Objects;

/** Mutable event request for replacing or cancelling one API-driven projectile spawn. */
public final class ProjectileSpawnRequest {
    private final ProjectileSpawnSpec originalSpec;
    private ProjectileSpawnSpec spec;
    private boolean cancelled;

    ProjectileSpawnRequest(ProjectileSpawnSpec spec) {
        this.originalSpec = Objects.requireNonNull(spec, "spec");
        this.spec = spec;
    }

    public ProjectileSpawnSpec originalSpec() { return originalSpec; }
    public ProjectileSpawnSpec spec() { return spec; }
    public boolean cancelled() { return cancelled; }

    /** Replaces the current spec seen by later listeners. */
    public void replace(ProjectileSpawnSpec value) {
        spec = Objects.requireNonNull(value, "spec");
    }

    /** Cancels creation. Later listeners are still notified and can inspect the request. */
    public void cancel() { cancelled = true; }
}
