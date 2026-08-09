package io.github.endx.rustedfabricapi.impl.projectile;

import io.github.endx.rustedfabricapi.mixin.accessor.CustomUnitMetadataAccessor;
import io.github.endx.rustedfabricapi.mixin.accessor.EffectTemplateAccessor;
import rustedwarfare.custom.CustomUnitMetadata;
import rustedwarfare.custom.EffectList;
import rustedwarfare.custom.EffectTemplate;
import rustedwarfare.custom.graphics.DecalBehavior;
import rustedwarfare.util.UnitConfig;

import java.util.ArrayList;
import java.util.Optional;

/** Internal bridge to native custom-unit asset parsers and mixin accessors. */
public final class CustomProjectileAssetRuntime {
    private CustomProjectileAssetRuntime() { }

    public static void parseEffects(CustomUnitMetadata metadata, UnitConfig config) {
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

    public static Optional<DecalBehavior> parseDecals(
            CustomUnitMetadata metadata, UnitConfig config) {
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
