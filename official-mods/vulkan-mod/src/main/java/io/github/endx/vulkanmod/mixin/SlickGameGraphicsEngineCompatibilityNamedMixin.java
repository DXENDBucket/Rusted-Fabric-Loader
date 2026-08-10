package io.github.endx.vulkanmod.mixin;

import io.github.endx.vulkanmod.render.VulkanGraphicsEngine;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import rustedwarfare.client.SlickGame;
import rustedwarfare.core.GameEngine;
import rustedwarfare.render.GraphicsEngine;

/** Supplies SlickGame's remaining hard cast with the migration delegate. */
@Mixin(SlickGame.class)
public abstract class SlickGameGraphicsEngineCompatibilityNamedMixin {
    @Redirect(method = "render(Lorg/newdawn/slick/GameContainer;Lorg/newdawn/slick/Graphics;)V",
            at = @At(value = "FIELD",
                    target = "Lrustedwarfare/core/GameEngine;renderGraphicsEngine:Lrustedwarfare/render/GraphicsEngine;",
                    opcode = Opcodes.GETFIELD), require = 1, allow = 1)
    private GraphicsEngine vulkanmod$unwrapCompatibilityRenderer(GameEngine engine) {
        return VulkanGraphicsEngine.unwrapCompatibility(engine.renderGraphicsEngine);
    }
}
