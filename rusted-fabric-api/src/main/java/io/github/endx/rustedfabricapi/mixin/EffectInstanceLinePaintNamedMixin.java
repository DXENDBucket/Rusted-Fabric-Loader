package io.github.endx.rustedfabricapi.mixin;

import android.graphics.Paint;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import rustedwarfare.render.GraphicsEngine;
import rustedwarfare.render.UniquePaint;

/** Makes native line effects honor their per-instance color and alpha. */
@Mixin(targets = "rustedwarfare.render.effect.EffectInstance", remap = false)
public abstract class EffectInstanceLinePaintNamedMixin {
    @Shadow public UniquePaint drawPaint;

    @Redirect(
            method = "draw(Lrustedwarfare/core/GameEngine;Z)Z",
            at = @At(value = "INVOKE",
                    target = "Lrustedwarfare/render/GraphicsEngine;drawLine(FFFFLandroid/graphics/Paint;)V"),
            require = 1
    )
    private void rustedfabricapi$useInstancePaintForLine(GraphicsEngine graphics,
                                                         float startX, float startY,
                                                         float endX, float endY,
                                                         Paint ignoredSharedPaint) {
        graphics.drawLine(startX, startY, endX, endY, drawPaint);
    }
}
