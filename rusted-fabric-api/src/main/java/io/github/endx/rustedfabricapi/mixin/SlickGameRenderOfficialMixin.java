package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.GameLifecycleEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.corrodinggames.rts.java.u", remap = false)
public abstract class SlickGameRenderOfficialMixin {
    @Inject(method = "render(Lorg/newdawn/slick/GameContainer;Lorg/newdawn/slick/Graphics;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterFrameRender(CallbackInfo ci) {
        GameLifecycleEvents.AFTER_FRAME_RENDER.invoker().afterFrameRender(this);
    }
}
