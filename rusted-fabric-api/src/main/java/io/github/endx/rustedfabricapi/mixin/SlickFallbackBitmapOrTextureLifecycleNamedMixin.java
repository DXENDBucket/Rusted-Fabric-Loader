package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.RenderImageLifecycleEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "rustedwarfare.client.render.SlickFallbackBitmapOrTexture", remap = false)
public abstract class SlickFallbackBitmapOrTextureLifecycleNamedMixin {
    @Inject(method = "forceLoad()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeForceLoad(CallbackInfo ci) {
        RenderImageLifecycleEvents.BEFORE_FORCE_LOAD.invoker().onEvent(this);
    }

    @Inject(method = "forceLoad()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterForceLoad(CallbackInfo ci) {
        RenderImageLifecycleEvents.AFTER_FORCE_LOAD.invoker().onEvent(this);
    }

    @Inject(method = "flushPixelBufferToBitmap()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeFlushPixelBufferToBitmap(CallbackInfo ci) {
        RenderImageLifecycleEvents.BEFORE_FLUSH_PIXEL_BUFFER_TO_BITMAP.invoker().onEvent(this);
    }

    @Inject(method = "flushPixelBufferToBitmap()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterFlushPixelBufferToBitmap(CallbackInfo ci) {
        RenderImageLifecycleEvents.AFTER_FLUSH_PIXEL_BUFFER_TO_BITMAP.invoker().onEvent(this);
    }

    @Inject(method = "dropPixelBuffer()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeDropPixelBuffer(CallbackInfo ci) {
        RenderImageLifecycleEvents.BEFORE_DROP_PIXEL_BUFFER.invoker().onEvent(this);
    }

    @Inject(method = "dropPixelBuffer()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterDropPixelBuffer(CallbackInfo ci) {
        RenderImageLifecycleEvents.AFTER_DROP_PIXEL_BUFFER.invoker().onEvent(this);
    }

    @Inject(method = "enableAutoReleaseOnFinalize()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeEnableAutoReleaseOnFinalize(CallbackInfo ci) {
        RenderImageLifecycleEvents.BEFORE_ENABLE_AUTO_RELEASE_ON_FINALIZE.invoker().onEvent(this);
    }

    @Inject(method = "enableAutoReleaseOnFinalize()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterEnableAutoReleaseOnFinalize(CallbackInfo ci) {
        RenderImageLifecycleEvents.AFTER_ENABLE_AUTO_RELEASE_ON_FINALIZE.invoker().onEvent(this);
    }

    @Inject(method = "releaseBitmap()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeReleaseBitmap(CallbackInfo ci) {
        RenderImageLifecycleEvents.BEFORE_RELEASE_BITMAP.invoker().onEvent(this);
    }

    @Inject(method = "releaseBitmap()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterReleaseBitmap(CallbackInfo ci) {
        RenderImageLifecycleEvents.AFTER_RELEASE_BITMAP.invoker().onEvent(this);
    }

    @Inject(method = "checkAndReloadIfFileChanged()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeCheckAndReloadIfFileChanged(CallbackInfo ci) {
        RenderImageLifecycleEvents.BEFORE_CHECK_AND_RELOAD_IF_FILE_CHANGED.invoker().onEvent(this);
    }

    @Inject(method = "checkAndReloadIfFileChanged()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterCheckAndReloadIfFileChanged(CallbackInfo ci) {
        RenderImageLifecycleEvents.AFTER_CHECK_AND_RELOAD_IF_FILE_CHANGED.invoker().onEvent(this);
    }

    @Inject(method = "reloadImage()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeReloadImage(CallbackInfo ci) {
        RenderImageLifecycleEvents.BEFORE_RELOAD_SLICK_IMAGE.invoker().onEvent(this);
    }

    @Inject(method = "reloadImage()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterReloadImage(CallbackInfo ci) {
        RenderImageLifecycleEvents.AFTER_RELOAD_SLICK_IMAGE.invoker().onEvent(this);
    }

    @Inject(method = "setSlickImageData(Lorg/newdawn/slick/opengl/ImageData;Ljava/lang/String;Z)V",
            at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeSetSlickImageData(@Coerce Object imageData, String name, boolean fallback,
                                                         CallbackInfo ci) {
        RenderImageLifecycleEvents.BEFORE_SET_SLICK_IMAGE_DATA.invoker()
                .onEvent(this, imageData, name, fallback);
    }

    @Inject(method = "setSlickImageData(Lorg/newdawn/slick/opengl/ImageData;Ljava/lang/String;Z)V",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterSetSlickImageData(@Coerce Object imageData, String name, boolean fallback,
                                                        CallbackInfo ci) {
        RenderImageLifecycleEvents.AFTER_SET_SLICK_IMAGE_DATA.invoker()
                .onEvent(this, imageData, name, fallback);
    }
}
