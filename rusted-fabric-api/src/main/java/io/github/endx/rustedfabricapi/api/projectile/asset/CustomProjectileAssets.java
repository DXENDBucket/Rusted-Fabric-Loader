package io.github.endx.rustedfabricapi.api.projectile.asset;

import io.github.endx.rustedfabricapi.impl.projectile.CustomProjectileAssetRuntime;
import rustedwarfare.custom.CustomUnitMetadata;
import rustedwarfare.custom.graphics.DecalBehavior;
import rustedwarfare.util.UnitConfig;

import java.util.Objects;
import java.util.Optional;

/** Native asset loading passes reusable by independent projectile-definition formats. */
public final class CustomProjectileAssets {
    private CustomProjectileAssets() { }

    /** Parses all native {@code [effect_*]} sections and resolves their nested effect lists. */
    public static void parseEffects(CustomUnitMetadata metadata, UnitConfig config) {
        Objects.requireNonNull(metadata, "metadata");
        Objects.requireNonNull(config, "config");
        CustomProjectileAssetRuntime.parseEffects(metadata, config);
    }

    /** Runs the native Decal parser and returns the behavior it attached, when present. */
    public static Optional<DecalBehavior> parseDecals(
            CustomUnitMetadata metadata, UnitConfig config) {
        Objects.requireNonNull(metadata, "metadata");
        Objects.requireNonNull(config, "config");
        return CustomProjectileAssetRuntime.parseDecals(metadata, config);
    }
}
