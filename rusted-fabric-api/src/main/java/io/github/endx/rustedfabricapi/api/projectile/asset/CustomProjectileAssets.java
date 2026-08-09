package io.github.endx.rustedfabricapi.api.projectile.asset;

import io.github.endx.rustedfabricapi.mixin.CustomUnitMetadataAccessor;
import io.github.endx.rustedfabricapi.mixin.EffectTemplateAccessor;
import rustedwarfare.custom.CustomUnitMetadata;
import rustedwarfare.custom.EffectList;
import rustedwarfare.custom.EffectTemplate;
import rustedwarfare.custom.graphics.DecalBehavior;
import rustedwarfare.util.UnitConfig;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

/** Native asset loading passes reusable by independent projectile-definition formats. */
public final class CustomProjectileAssets {
    private CustomProjectileAssets() { }

    /** Parses all native {@code [effect_*]} sections and resolves their nested effect lists. */
    public static void parseEffects(CustomUnitMetadata metadata, UnitConfig config) {
        Objects.requireNonNull(metadata, "metadata");
        Objects.requireNonNull(config, "config");
        ArrayList<EffectTemplate> effects =
                ((CustomUnitMetadataAccessor) (Object) metadata)
                        .rustedfabricapi$getEffectTemplates();
        for (Object rawSection : config.getNonMetaSectionsWithPrefix("effect_")) {
            String section = String.valueOf(rawSection);
            String name = section.substring("effect_".length());
            EffectTemplate effect = EffectTemplateAccessor.rustedfabricapi$create(name);
            effect.parseFromConfig(metadata, config, section);
            effects.add(effect);
        }
        for (EffectTemplate effect : effects) {
            EffectTemplateAccessor access = (EffectTemplateAccessor) (Object) effect;
            resolve(access.rustedfabricapi$getAlsoEmitEffects());
            resolve(access.rustedfabricapi$getAlsoEmitEffectsOnDeath());
            resolve(access.rustedfabricapi$getIfSpawnFailsEmitEffects());
            resolve(access.rustedfabricapi$getTrailEffect());
        }
    }

    /** Runs the native Decal parser and returns the behavior it attached, when present. */
    public static Optional<DecalBehavior> parseDecals(
            CustomUnitMetadata metadata, UnitConfig config) {
        Objects.requireNonNull(metadata, "metadata");
        Objects.requireNonNull(config, "config");
        int existing = metadata.renderBehaviors.size();
        DecalBehavior.parseDecalSections(metadata, config);
        for (int i = existing; i < metadata.renderBehaviors.size(); i++) {
            Object behavior = metadata.renderBehaviors.get(i);
            if (behavior instanceof DecalBehavior) {
                return Optional.of((DecalBehavior) behavior);
            }
        }
        return Optional.empty();
    }

    private static void resolve(EffectList effects) {
        if (effects != null) effects.resolveEffectReferences();
    }
}
