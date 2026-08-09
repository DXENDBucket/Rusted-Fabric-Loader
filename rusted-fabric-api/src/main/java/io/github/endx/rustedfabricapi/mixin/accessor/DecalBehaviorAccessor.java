package io.github.endx.rustedfabricapi.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import rustedwarfare.custom.CustomUnitMetadata;
import rustedwarfare.custom.graphics.DecalTemplate;

@Mixin(targets = "rustedwarfare.custom.graphics.DecalBehavior", remap = false)
public interface DecalBehaviorAccessor {
    @Invoker("findDecalTemplateStrict")
    static DecalTemplate rustedfabricapi$findDecalTemplateStrict(
            CustomUnitMetadata metadata, String name) {
        throw new AssertionError("Mixin invoker was not applied");
    }
}
