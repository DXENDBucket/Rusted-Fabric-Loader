package io.github.endx.vulkanmod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import rustedwarfare.client.RustedWarfareMain;
import rustedwarfare.ui.LibRocketSlickRenderer;

@Mixin(RustedWarfareMain.class)
public interface RustedWarfareMainUiAccessor {
    @Accessor("libRocketRenderer")
    LibRocketSlickRenderer vulkanmod$getLibRocketRenderer();
}
