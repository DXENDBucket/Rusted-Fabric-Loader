package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.CustomUnitLifecycleEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rustedwarfare.custom.CustomUnitMetadata;
import rustedwarfare.custom.stats.MutableStatAccessor;

@Mixin(targets = "rustedwarfare.custom.CustomUnit", remap = false)
public abstract class CustomUnitLifecycleNamedMixin {
    @Shadow
    public CustomUnitMetadata unitMetadata;

    @Unique
    private Object rustedfabricapi$metadataBeforeApply;

    @Inject(method = "applyUnitMetadataWithStatOverrides(Lrustedwarfare/custom/CustomUnitMetadata;ZZ[Lrustedwarfare/custom/stats/MutableStatAccessor;)V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeUnitMetadataApply(CustomUnitMetadata metadata, boolean conversion, boolean initial, MutableStatAccessor[] statOverrides, CallbackInfo ci) {
        rustedfabricapi$metadataBeforeApply = unitMetadata;
        CustomUnitLifecycleEvents.BEFORE_UNIT_METADATA_APPLY.invoker().beforeUnitMetadataApply(this, rustedfabricapi$metadataBeforeApply, metadata, conversion, initial, statOverrides);
    }

    @Inject(method = "applyUnitMetadataWithStatOverrides(Lrustedwarfare/custom/CustomUnitMetadata;ZZ[Lrustedwarfare/custom/stats/MutableStatAccessor;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterUnitMetadataApply(CustomUnitMetadata metadata, boolean conversion, boolean initial, MutableStatAccessor[] statOverrides, CallbackInfo ci) {
        CustomUnitLifecycleEvents.AFTER_UNIT_METADATA_APPLY.invoker().afterUnitMetadataApply(this, rustedfabricapi$metadataBeforeApply, metadata, conversion, initial, statOverrides);
        rustedfabricapi$metadataBeforeApply = null;
    }
}
