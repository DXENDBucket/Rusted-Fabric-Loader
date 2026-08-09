package io.github.endx.rustedfabricapi.api.projectile.spawn;

import java.util.Objects;

/** Explodes after crossing a ground-tile boundary matching a from/to terrain pair. */
public final class TerrainTransitionSpec {
    private static final TerrainTransitionSpec NONE = new TerrainTransitionSpec(null, null);

    private final TerrainKind from;
    private final TerrainKind to;

    private TerrainTransitionSpec(TerrainKind from, TerrainKind to) {
        this.from = from;
        this.to = to;
    }

    public static TerrainTransitionSpec none() { return NONE; }

    public static TerrainTransitionSpec of(TerrainKind from, TerrainKind to) {
        return new TerrainTransitionSpec(Objects.requireNonNull(from, "from"),
                Objects.requireNonNull(to, "to"));
    }

    public boolean enabled() { return from != null; }
    public TerrainKind from() { return from; }
    public TerrainKind to() { return to; }
}
