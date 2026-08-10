package io.github.endx.vulkanmod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import rustedwarfare.client.render.SlickTransformState;

/** Read-only bridge to the transform snapshot used by Slick draw calls. */
@Mixin(targets = "rustedwarfare.client.render.SlickGraphicsBackend", remap = false)
public interface SlickGraphicsBackendStateAccessor {
    @Accessor("transformState")
    SlickTransformState vulkanmod$getTransformState();
}
