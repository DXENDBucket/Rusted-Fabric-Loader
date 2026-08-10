package io.github.endx.vulkanmod.mixin;

import io.github.endx.vulkanmod.VulkanRuntime;
import io.github.endx.vulkanmod.render.VulkanGraphicsEngine;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rustedwarfare.client.RustedWarfareMain;
import rustedwarfare.core.GameEngine;

/** Installs the selected renderer class before GameEngine reflectively constructs it. */
@Mixin(RustedWarfareMain.class)
public abstract class RustedWarfareMainRendererSelectionNamedMixin {
    @Inject(method = "initializeGameSystems()V", at = @At(value = "FIELD",
            target = "Lrustedwarfare/core/GameEngine;gameEngineClass:Ljava/lang/Class;",
            opcode = Opcodes.PUTSTATIC, ordinal = 1, shift = At.Shift.AFTER), require = 1)
    private void vulkanmod$selectGraphicsEngine(CallbackInfo callback) {
        if (VulkanRuntime.isNativeRendererSelected()) {
            GameEngine.gameEngineClass = VulkanGraphicsEngine.class;
        }
    }
}
