package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.RenderImageLifecycleEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "rustedwarfare.client.render.SlickGraphicsBackend", remap = false)
public abstract class SlickGraphicsBackendImageNamedMixin {
    @Inject(method = "loadErrorImages()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeLoadErrorImages(CallbackInfo ci) {
        RenderImageLifecycleEvents.BEFORE_LOAD_ERROR_IMAGES.invoker().onEvent(this);
    }

    @Inject(method = "loadErrorImages()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterLoadErrorImages(CallbackInfo ci) {
        RenderImageLifecycleEvents.AFTER_LOAD_ERROR_IMAGES.invoker().onEvent(this);
    }

    @Inject(method = "flushImageDataDiscards()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeFlushImageDataDiscards(CallbackInfo ci) {
        RenderImageLifecycleEvents.BEFORE_FLUSH_IMAGE_DATA_DISCARDS.invoker().onEvent(this);
    }

    @Inject(method = "flushImageDataDiscards()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterFlushImageDataDiscards(CallbackInfo ci) {
        RenderImageLifecycleEvents.AFTER_FLUSH_IMAGE_DATA_DISCARDS.invoker().onEvent(this);
    }

    @Inject(method = "flushPendingImageDataDiscards()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeFlushPendingImageDataDiscards(CallbackInfo ci) {
        RenderImageLifecycleEvents.BEFORE_FLUSH_PENDING_IMAGE_DATA_DISCARDS.invoker().onEvent(this);
    }

    @Inject(method = "flushPendingImageDataDiscards()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterFlushPendingImageDataDiscards(CallbackInfo ci) {
        RenderImageLifecycleEvents.AFTER_FLUSH_PENDING_IMAGE_DATA_DISCARDS.invoker().onEvent(this);
    }

    @Inject(method = "queueImageDataDiscard(Lrustedwarfare/client/render/SlickBitmapOrTexture;)V",
            at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeQueueImageDataDiscard(@Coerce Object image, CallbackInfo ci) {
        RenderImageLifecycleEvents.BEFORE_QUEUE_IMAGE_DATA_DISCARD.invoker().onEvent(this, image);
    }

    @Inject(method = "queueImageDataDiscard(Lrustedwarfare/client/render/SlickBitmapOrTexture;)V",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterQueueImageDataDiscard(@Coerce Object image, CallbackInfo ci) {
        RenderImageLifecycleEvents.AFTER_QUEUE_IMAGE_DATA_DISCARD.invoker().onEvent(this, image);
    }

    @Inject(method = "loadImageByResourceId(I)Lrustedwarfare/client/render/GameImage;",
            at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeLoadImageByResourceId(int resourceId,
                                                             CallbackInfoReturnable<Object> cir) {
        RenderImageLifecycleEvents.BEFORE_LOAD_IMAGE_BY_RESOURCE_ID.invoker()
                .onEvent(this, resourceId, false);
    }

    @Inject(method = "loadImageByResourceId(I)Lrustedwarfare/client/render/GameImage;",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterLoadImageByResourceId(int resourceId,
                                                            CallbackInfoReturnable<Object> cir) {
        RenderImageLifecycleEvents.AFTER_LOAD_IMAGE_BY_RESOURCE_ID.invoker()
                .onEvent(this, resourceId, false, cir.getReturnValue());
    }

    @Inject(method = "loadImageByResourceId(IZ)Lrustedwarfare/client/render/GameImage;",
            at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeLoadImageByResourceIdSmooth(int resourceId, boolean smooth,
                                                                   CallbackInfoReturnable<Object> cir) {
        RenderImageLifecycleEvents.BEFORE_LOAD_IMAGE_BY_RESOURCE_ID.invoker()
                .onEvent(this, resourceId, smooth);
    }

    @Inject(method = "loadImageByResourceId(IZ)Lrustedwarfare/client/render/GameImage;",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterLoadImageByResourceIdSmooth(int resourceId, boolean smooth,
                                                                  CallbackInfoReturnable<Object> cir) {
        RenderImageLifecycleEvents.AFTER_LOAD_IMAGE_BY_RESOURCE_ID.invoker()
                .onEvent(this, resourceId, smooth, cir.getReturnValue());
    }

    @Inject(method = "loadSlickImageByResourceId(IZ)Lrustedwarfare/client/render/SlickBitmapOrTexture;",
            at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeLoadSlickImageByResourceId(int resourceId, boolean smooth,
                                                                  CallbackInfoReturnable<Object> cir) {
        RenderImageLifecycleEvents.BEFORE_LOAD_SLICK_IMAGE_BY_RESOURCE_ID.invoker()
                .onEvent(this, resourceId, smooth);
    }

    @Inject(method = "loadSlickImageByResourceId(IZ)Lrustedwarfare/client/render/SlickBitmapOrTexture;",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterLoadSlickImageByResourceId(int resourceId, boolean smooth,
                                                                 CallbackInfoReturnable<Object> cir) {
        RenderImageLifecycleEvents.AFTER_LOAD_SLICK_IMAGE_BY_RESOURCE_ID.invoker()
                .onEvent(this, resourceId, smooth, cir.getReturnValue());
    }

    @Inject(method = "loadSlickImageData(Ljava/io/InputStream;)Lorg/newdawn/slick/opengl/ImageData;",
            at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeLoadSlickImageData(@Coerce Object inputStream,
                                                          CallbackInfoReturnable<Object> cir) {
        RenderImageLifecycleEvents.BEFORE_LOAD_SLICK_IMAGE_DATA.invoker()
                .onEvent(this, inputStream);
    }

    @Inject(method = "loadSlickImageData(Ljava/io/InputStream;)Lorg/newdawn/slick/opengl/ImageData;",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterLoadSlickImageData(@Coerce Object inputStream,
                                                         CallbackInfoReturnable<Object> cir) {
        RenderImageLifecycleEvents.AFTER_LOAD_SLICK_IMAGE_DATA.invoker()
                .onEvent(this, inputStream, cir.getReturnValue());
    }

    @Inject(method = "loadImageFromStream(Ljava/io/InputStream;Z)Lrustedwarfare/client/render/GameImage;",
            at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeLoadImageFromStream(@Coerce Object inputStream, boolean smooth,
                                                           CallbackInfoReturnable<Object> cir) {
        RenderImageLifecycleEvents.BEFORE_LOAD_IMAGE_FROM_STREAM.invoker()
                .onEvent(this, inputStream, smooth);
    }

    @Inject(method = "loadImageFromStream(Ljava/io/InputStream;Z)Lrustedwarfare/client/render/GameImage;",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterLoadImageFromStream(@Coerce Object inputStream, boolean smooth,
                                                          CallbackInfoReturnable<Object> cir) {
        RenderImageLifecycleEvents.AFTER_LOAD_IMAGE_FROM_STREAM.invoker()
                .onEvent(this, inputStream, smooth, cir.getReturnValue());
    }

    @Inject(method = "createSlickImageFromData(Lorg/newdawn/slick/opengl/ImageData;Ljava/lang/String;)Lrustedwarfare/client/render/SlickBitmapOrTexture;",
            at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeCreateSlickImageFromData(@Coerce Object imageData, String name,
                                                                CallbackInfoReturnable<Object> cir) {
        RenderImageLifecycleEvents.BEFORE_CREATE_SLICK_IMAGE_FROM_DATA.invoker()
                .onEvent(this, imageData, name);
    }

    @Inject(method = "createSlickImageFromData(Lorg/newdawn/slick/opengl/ImageData;Ljava/lang/String;)Lrustedwarfare/client/render/SlickBitmapOrTexture;",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterCreateSlickImageFromData(@Coerce Object imageData, String name,
                                                               CallbackInfoReturnable<Object> cir) {
        RenderImageLifecycleEvents.AFTER_CREATE_SLICK_IMAGE_FROM_DATA.invoker()
                .onEvent(this, imageData, name, cir.getReturnValue());
    }

    @Inject(method = "createImage(IIZ)Lrustedwarfare/client/render/GameImage;", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeCreateImage(int width, int height, boolean smooth,
                                                   CallbackInfoReturnable<Object> cir) {
        RenderImageLifecycleEvents.BEFORE_CREATE_IMAGE.invoker()
                .onEvent(this, width, height, smooth, false);
    }

    @Inject(method = "createImage(IIZ)Lrustedwarfare/client/render/GameImage;", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterCreateImage(int width, int height, boolean smooth,
                                                  CallbackInfoReturnable<Object> cir) {
        RenderImageLifecycleEvents.AFTER_CREATE_IMAGE.invoker()
                .onEvent(this, width, height, smooth, false, cir.getReturnValue());
    }

    @Inject(method = "createImageBufferBacked(IIZ)Lrustedwarfare/client/render/GameImage;",
            at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeCreateImageBufferBacked(int width, int height, boolean smooth,
                                                               CallbackInfoReturnable<Object> cir) {
        RenderImageLifecycleEvents.BEFORE_CREATE_IMAGE.invoker()
                .onEvent(this, width, height, smooth, true);
    }

    @Inject(method = "createImageBufferBacked(IIZ)Lrustedwarfare/client/render/GameImage;",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterCreateImageBufferBacked(int width, int height, boolean smooth,
                                                              CallbackInfoReturnable<Object> cir) {
        RenderImageLifecycleEvents.AFTER_CREATE_IMAGE.invoker()
                .onEvent(this, width, height, smooth, true, cir.getReturnValue());
    }
}
