package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.RenderImageLifecycleEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "rustedwarfare.client.render.GameImage", remap = false)
public abstract class GameImageLifecycleNamedMixin {
    @Inject(method = "releaseImageData()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeReleaseImageData(CallbackInfo ci) {
        RenderImageLifecycleEvents.BEFORE_RELEASE_IMAGE_DATA.invoker().onEvent(this);
    }

    @Inject(method = "releaseImageData()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterReleaseImageData(CallbackInfo ci) {
        RenderImageLifecycleEvents.AFTER_RELEASE_IMAGE_DATA.invoker().onEvent(this);
    }

    @Inject(method = "discardPixelBuffer()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeDiscardPixelBuffer(CallbackInfo ci) {
        RenderImageLifecycleEvents.BEFORE_DISCARD_PIXEL_BUFFER.invoker().onEvent(this);
    }

    @Inject(method = "discardPixelBuffer()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterDiscardPixelBuffer(CallbackInfo ci) {
        RenderImageLifecycleEvents.AFTER_DISCARD_PIXEL_BUFFER.invoker().onEvent(this);
    }

    @Inject(method = "dropPixelBuffer()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeDropPixelBuffer(CallbackInfo ci) {
        RenderImageLifecycleEvents.BEFORE_DROP_PIXEL_BUFFER.invoker().onEvent(this);
    }

    @Inject(method = "dropPixelBuffer()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterDropPixelBuffer(CallbackInfo ci) {
        RenderImageLifecycleEvents.AFTER_DROP_PIXEL_BUFFER.invoker().onEvent(this);
    }

    @Inject(method = "releaseBitmap()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeReleaseBitmap(CallbackInfo ci) {
        RenderImageLifecycleEvents.BEFORE_RELEASE_BITMAP.invoker().onEvent(this);
    }

    @Inject(method = "releaseBitmap()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterReleaseBitmap(CallbackInfo ci) {
        RenderImageLifecycleEvents.AFTER_RELEASE_BITMAP.invoker().onEvent(this);
    }

    @Inject(method = "flushPixelBufferToBitmap()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeFlushPixelBufferToBitmap(CallbackInfo ci) {
        RenderImageLifecycleEvents.BEFORE_FLUSH_PIXEL_BUFFER_TO_BITMAP.invoker().onEvent(this);
    }

    @Inject(method = "flushPixelBufferToBitmap()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterFlushPixelBufferToBitmap(CallbackInfo ci) {
        RenderImageLifecycleEvents.AFTER_FLUSH_PIXEL_BUFFER_TO_BITMAP.invoker().onEvent(this);
    }

    @Inject(method = "ensureImageDataAvailable()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeEnsureImageDataAvailable(CallbackInfo ci) {
        RenderImageLifecycleEvents.BEFORE_ENSURE_IMAGE_DATA_AVAILABLE.invoker().onEvent(this);
    }

    @Inject(method = "ensureImageDataAvailable()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterEnsureImageDataAvailable(CallbackInfo ci) {
        RenderImageLifecycleEvents.AFTER_ENSURE_IMAGE_DATA_AVAILABLE.invoker().onEvent(this);
    }

    @Inject(method = "forceLoad()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeForceLoad(CallbackInfo ci) {
        RenderImageLifecycleEvents.BEFORE_FORCE_LOAD.invoker().onEvent(this);
    }

    @Inject(method = "forceLoad()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterForceLoad(CallbackInfo ci) {
        RenderImageLifecycleEvents.AFTER_FORCE_LOAD.invoker().onEvent(this);
    }

    @Inject(method = "checkAndReloadIfFileChanged()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeCheckAndReloadIfFileChanged(CallbackInfo ci) {
        RenderImageLifecycleEvents.BEFORE_CHECK_AND_RELOAD_IF_FILE_CHANGED.invoker().onEvent(this);
    }

    @Inject(method = "checkAndReloadIfFileChanged()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterCheckAndReloadIfFileChanged(CallbackInfo ci) {
        RenderImageLifecycleEvents.AFTER_CHECK_AND_RELOAD_IF_FILE_CHANGED.invoker().onEvent(this);
    }

    @Inject(method = "enableAutoReleaseOnFinalize()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeEnableAutoReleaseOnFinalize(CallbackInfo ci) {
        RenderImageLifecycleEvents.BEFORE_ENABLE_AUTO_RELEASE_ON_FINALIZE.invoker().onEvent(this);
    }

    @Inject(method = "enableAutoReleaseOnFinalize()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterEnableAutoReleaseOnFinalize(CallbackInfo ci) {
        RenderImageLifecycleEvents.AFTER_ENABLE_AUTO_RELEASE_ON_FINALIZE.invoker().onEvent(this);
    }
}
