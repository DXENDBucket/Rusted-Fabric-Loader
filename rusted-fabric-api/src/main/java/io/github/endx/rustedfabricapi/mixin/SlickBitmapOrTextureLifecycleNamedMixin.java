package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.RenderImageLifecycleEvents;
import io.github.endx.rustedfabricapi.internal.client.AndroidShaderCompatibility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "rustedwarfare.client.render.SlickBitmapOrTexture", remap = false)
public abstract class SlickBitmapOrTextureLifecycleNamedMixin {
    @Shadow boolean pixelBufferDiscarded;

    @Shadow public abstract void recreateImageDataFromTexture();

    @Inject(method = "readPixelsFromBitmap()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$rehydrateDiscardedAndroidPixels(CallbackInfo ci) {
        if (!pixelBufferDiscarded
                || !AndroidShaderCompatibility.shouldRehydrateDiscardedPixels()) return;

        // readPixelsFromBitmap normally treats discardPixelBuffer as irreversible. Generated
        // shadows can nevertheless be requested later by lazy CPU team colouring on Android.
        // Restore only this requested texture, then let the original lifecycle discard it again.
        pixelBufferDiscarded = false;
        try {
            recreateImageDataFromTexture();
            AndroidShaderCompatibility.reportDiscardedPixelRehydration();
        } catch (RuntimeException | Error failure) {
            pixelBufferDiscarded = true;
            throw failure;
        }
    }

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

    @Inject(method = "ensureImageDataLoaded()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeEnsureImageDataLoaded(CallbackInfo ci) {
        RenderImageLifecycleEvents.BEFORE_ENSURE_SLICK_IMAGE_DATA_LOADED.invoker().onEvent(this);
    }

    @Inject(method = "ensureImageDataLoaded()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterEnsureImageDataLoaded(CallbackInfo ci) {
        RenderImageLifecycleEvents.AFTER_ENSURE_SLICK_IMAGE_DATA_LOADED.invoker().onEvent(this);
    }

    @Inject(method = "dropImageDataIfAllowed()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeDropImageDataIfAllowed(CallbackInfo ci) {
        RenderImageLifecycleEvents.BEFORE_DROP_SLICK_IMAGE_DATA_IF_ALLOWED.invoker().onEvent(this);
    }

    @Inject(method = "dropImageDataIfAllowed()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterDropImageDataIfAllowed(CallbackInfo ci) {
        RenderImageLifecycleEvents.AFTER_DROP_SLICK_IMAGE_DATA_IF_ALLOWED.invoker().onEvent(this);
    }

    @Inject(method = "recreateImageDataFromTexture()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeRecreateImageDataFromTexture(CallbackInfo ci) {
        RenderImageLifecycleEvents.BEFORE_RECREATE_SLICK_IMAGE_DATA_FROM_TEXTURE.invoker().onEvent(this);
    }

    @Inject(method = "recreateImageDataFromTexture()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterRecreateImageDataFromTexture(CallbackInfo ci) {
        RenderImageLifecycleEvents.AFTER_RECREATE_SLICK_IMAGE_DATA_FROM_TEXTURE.invoker().onEvent(this);
    }

    @Inject(method = "reloadImage()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeReloadImage(CallbackInfo ci) {
        RenderImageLifecycleEvents.BEFORE_RELOAD_SLICK_IMAGE.invoker().onEvent(this);
    }

    @Inject(method = "reloadImage()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterReloadImage(CallbackInfo ci) {
        RenderImageLifecycleEvents.AFTER_RELOAD_SLICK_IMAGE.invoker().onEvent(this);
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
