package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.RenderImageLifecycleEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "rustedwarfare.client.render.LazyTeamColorImage", remap = false)
public abstract class LazyTeamColorImageLifecycleNamedMixin {
    @Inject(method = "loadLazyColoredImage(Z)V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeLoadLazyColoredImage(boolean allowShader, CallbackInfo ci) {
        RenderImageLifecycleEvents.BEFORE_LOAD_LAZY_TEAM_COLOR_IMAGE.invoker()
                .onEvent(this, allowShader);
    }

    @Inject(method = "loadLazyColoredImage(Z)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterLoadLazyColoredImage(boolean allowShader, CallbackInfo ci) {
        RenderImageLifecycleEvents.AFTER_LOAD_LAZY_TEAM_COLOR_IMAGE.invoker()
                .onEvent(this, allowShader);
    }

    @Inject(method = "forceLoad()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeForceLoad(CallbackInfo ci) {
        RenderImageLifecycleEvents.BEFORE_FORCE_LOAD.invoker().onEvent(this);
    }

    @Inject(method = "forceLoad()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterForceLoad(CallbackInfo ci) {
        RenderImageLifecycleEvents.AFTER_FORCE_LOAD.invoker().onEvent(this);
    }
}
