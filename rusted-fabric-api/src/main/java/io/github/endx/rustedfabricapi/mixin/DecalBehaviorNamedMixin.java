package io.github.endx.rustedfabricapi.mixin;

import android.graphics.PointF;
import io.github.endx.rustedfabricapi.api.client.render.event.DecalRenderEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.custom.graphics.DecalLayer;
import rustedwarfare.custom.graphics.DecalTemplate;
import rustedwarfare.util.RwArrayList;

import java.util.Collections;
import java.util.List;

@Mixin(targets = "rustedwarfare.custom.graphics.DecalBehavior", remap = false)
public abstract class DecalBehaviorNamedMixin {
    @Inject(method = "drawLayerAtPoint(Lrustedwarfare/custom/CustomUnit;FLrustedwarfare/custom/graphics/DecalLayer;Lrustedwarfare/util/RwArrayList;Landroid/graphics/PointF;)V",
            at = @At("HEAD"), require = 1)
    private static void rustedfabricapi$beforeDecalLayer(CustomUnit unit, float delta,
            DecalLayer layer, RwArrayList decals, PointF point, CallbackInfo ci) {
        DecalRenderEvents.BEFORE_LAYER.invoker().onLayer(
                unit, delta, layer, view(decals));
    }

    @Inject(method = "drawLayerAtPoint(Lrustedwarfare/custom/CustomUnit;FLrustedwarfare/custom/graphics/DecalLayer;Lrustedwarfare/util/RwArrayList;Landroid/graphics/PointF;)V",
            at = @At("RETURN"), require = 1)
    private static void rustedfabricapi$afterDecalLayer(CustomUnit unit, float delta,
            DecalLayer layer, RwArrayList decals, PointF point, CallbackInfo ci) {
        DecalRenderEvents.AFTER_LAYER.invoker().onLayer(
                unit, delta, layer, view(decals));
    }

    @SuppressWarnings("unchecked")
    private static List<DecalTemplate> view(RwArrayList decals) {
        return Collections.unmodifiableList((List<DecalTemplate>) (List<?>) decals);
    }
}
