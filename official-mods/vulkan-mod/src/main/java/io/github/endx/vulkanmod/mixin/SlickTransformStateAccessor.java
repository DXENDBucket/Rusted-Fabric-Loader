package io.github.endx.vulkanmod.mixin;

import android.graphics.RectF;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes Slick's accumulated transform without changing it. */
@Mixin(targets = "rustedwarfare.client.render.SlickTransformState", remap = false)
public interface SlickTransformStateAccessor {
    @Accessor("translateX") float vulkanmod$getTranslateX();
    @Accessor("translateY") float vulkanmod$getTranslateY();
    @Accessor("rotationDegrees") float vulkanmod$getRotationDegrees();
    @Accessor("scaleX") float vulkanmod$getScaleX();
    @Accessor("scaleY") float vulkanmod$getScaleY();
    @Accessor("clipRect") RectF vulkanmod$getClipRect();
    @Accessor("rotationPivotX") float vulkanmod$getRotationPivotX();
    @Accessor("rotationPivotY") float vulkanmod$getRotationPivotY();
}
