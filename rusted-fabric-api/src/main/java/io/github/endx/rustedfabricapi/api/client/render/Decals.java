package io.github.endx.rustedfabricapi.api.client.render;

import java.util.Objects;

import io.github.endx.rustedfabricapi.mixin.accessor.DecalBehaviorAccessor;
import rustedwarfare.custom.CustomUnitMetadata;
import rustedwarfare.custom.graphics.DecalTemplate;

/** Mapped access to native Decal definitions owned by custom-unit metadata. */
public final class Decals {
    private Decals() { }

    public static DecalTemplate require(CustomUnitMetadata metadata, String name) {
        String checkedName = Objects.requireNonNull(name, "name").trim();
        if (checkedName.isEmpty()) throw new IllegalArgumentException("decal name must not be empty");
        return DecalBehaviorAccessor.rustedfabricapi$findDecalTemplateStrict(
                Objects.requireNonNull(metadata, "metadata"), checkedName);
    }
}
