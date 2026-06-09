package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.MapDiscoveryEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;

@Mixin(targets = "com.corrodinggames.rts.gameFramework.i.a", remap = false)
public abstract class ModManagerMapDiscoveryOfficialMixin {
    @Inject(method = "a([Ljava/lang/String;Ljava/lang/String;)[Ljava/lang/String;", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeExtraMapsForPath(String[] originalMaps, String mapPath, CallbackInfoReturnable<String[]> cir) {
        if (MapDiscoveryEvents.BEFORE_EXTRA_MAPS_FOR_PATH.invoker().beforeExtraMapsForPath(this, originalMaps, mapPath)) {
            cir.setReturnValue(originalMaps);
        }
    }

    @Inject(method = "a([Ljava/lang/String;Ljava/lang/String;)[Ljava/lang/String;", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$afterExtraMapsForPath(String[] originalMaps, String mapPath, CallbackInfoReturnable<String[]> cir) {
        cir.setReturnValue(MapDiscoveryEvents.AFTER_EXTRA_MAPS_FOR_PATH.invoker()
                .afterExtraMapsForPath(this, originalMaps, mapPath, cir.getReturnValue()));
    }

    @Redirect(
            method = "a(Ljava/lang/String;Lcom/corrodinggames/rts/gameFramework/i/b;)V",
            at = @At(value = "INVOKE", target = "Ljava/util/ArrayList;add(Ljava/lang/Object;)Z"),
            require = 1
    )
    private boolean rustedfabricapi$afterExtraMapRecordAdded(ArrayList records, Object extraMapRecord, String originalPath, @Coerce Object modInfo) {
        boolean added = records.add(extraMapRecord);
        if (added) {
            MapDiscoveryEvents.AFTER_EXTRA_MAP_RECORD_ADDED.invoker()
                    .afterExtraMapRecordAdded(this, originalPath, modInfo, extraMapRecord);
        }
        return added;
    }
}
