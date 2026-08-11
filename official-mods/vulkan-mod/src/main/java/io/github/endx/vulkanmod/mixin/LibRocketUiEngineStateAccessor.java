package io.github.endx.vulkanmod.mixin;

import android.graphics.RectF;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes the active LibRocket scissor to the Vulkan geometry bridge. */
@Mixin(targets = "rustedwarfare.ui.LibRocketUiEngine", remap = false)
public interface LibRocketUiEngineStateAccessor {
    @Accessor("scissorEnabled") boolean vulkanmod$isScissorEnabled();
    @Accessor("scissorEnabled") void vulkanmod$setScissorEnabled(boolean enabled);
    @Accessor("scissorRectF") RectF vulkanmod$getScissorRectF();
}
