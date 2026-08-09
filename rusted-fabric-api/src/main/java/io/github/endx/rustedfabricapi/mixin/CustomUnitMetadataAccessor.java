package io.github.endx.rustedfabricapi.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import rustedwarfare.custom.CustomUnitMetadata;
import rustedwarfare.custom.EffectTemplate;

import java.util.ArrayList;

/** Internal access to native metadata registries used by independent INI assets. */
@Mixin(value = CustomUnitMetadata.class, remap = false)
public interface CustomUnitMetadataAccessor {
    @Accessor("effectTemplates")
    ArrayList<EffectTemplate> rustedfabricapi$getEffectTemplates();
}
