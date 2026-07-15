package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.CustomUnitRenderEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "rustedwarfare.custom.CustomUnit", remap = false)
public abstract class CustomUnitRenderNamedMixin {
    @Inject(method = "getBodyImage()Lrustedwarfare/client/render/GameImage;", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$afterGetBodyImage(CallbackInfoReturnable<Object> cir) {
        cir.setReturnValue(CustomUnitRenderEvents.AFTER_GET_BODY_IMAGE.invoker().afterGetBodyImage(this, cir.getReturnValue()));
    }

    @Inject(method = "getZoomedOutIconImage()Lrustedwarfare/client/render/GameImage;", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$afterGetZoomedIconImage(CallbackInfoReturnable<Object> cir) {
        cir.setReturnValue(CustomUnitRenderEvents.AFTER_GET_ZOOMED_ICON_IMAGE.invoker().afterGetZoomedIconImage(this, cir.getReturnValue()));
    }

    @Inject(method = "getShadowImage()Lrustedwarfare/client/render/GameImage;", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$afterGetShadowImage(CallbackInfoReturnable<Object> cir) {
        cir.setReturnValue(CustomUnitRenderEvents.AFTER_GET_SHADOW_IMAGE.invoker().afterGetShadowImage(this, cir.getReturnValue()));
    }

    @Inject(method = "getTurretImage(I)Lrustedwarfare/client/render/GameImage;", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$afterGetTurretImage(int turretIndex, CallbackInfoReturnable<Object> cir) {
        cir.setReturnValue(CustomUnitRenderEvents.AFTER_GET_TURRET_IMAGE.invoker().afterGetTurretImage(this, turretIndex, cir.getReturnValue()));
    }

    @Inject(method = "getShieldImage()Lrustedwarfare/client/render/GameImage;", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$afterGetShieldImage(CallbackInfoReturnable<Object> cir) {
        cir.setReturnValue(CustomUnitRenderEvents.AFTER_GET_SHIELD_IMAGE.invoker().afterGetShieldImage(this, cir.getReturnValue()));
    }

    @Inject(method = "getFrameSourceRect(Z)Landroid/graphics/Rect;", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeFrameSourceRect(boolean forShadow, CallbackInfoReturnable<Object> cir) {
        CustomUnitRenderEvents.BEFORE_FRAME_SOURCE_RECT.invoker().beforeFrameSourceRect(this, forShadow);
    }

    @Inject(method = "getFrameSourceRect(Z)Landroid/graphics/Rect;", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$afterFrameSourceRect(boolean forShadow, CallbackInfoReturnable<Object> cir) {
        cir.setReturnValue(CustomUnitRenderEvents.AFTER_FRAME_SOURCE_RECT.invoker().afterFrameSourceRect(this, forShadow, cir.getReturnValue()));
    }

    @Inject(method = "getImageDestinationRect()Landroid/graphics/RectF;", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$afterImageDestinationRect(CallbackInfoReturnable<Object> cir) {
        cir.setReturnValue(CustomUnitRenderEvents.AFTER_IMAGE_DESTINATION_RECT.invoker().afterImageDestinationRect(this, cir.getReturnValue()));
    }

    @Inject(method = "drawPreMainLayer(F)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeDrawBackImage(float renderDelta, CallbackInfo ci) {
        if (CustomUnitRenderEvents.BEFORE_DRAW_BACK_IMAGE.invoker().beforeDrawBackImage(this, renderDelta)) {
            ci.cancel();
        }
    }

    @Inject(method = "drawPreMainLayer(F)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterDrawBackImage(float renderDelta, CallbackInfo ci) {
        CustomUnitRenderEvents.AFTER_DRAW_BACK_IMAGE.invoker().afterDrawBackImage(this, renderDelta);
    }

    @Inject(method = "drawPostOverlayLayer(F)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeDrawOverlay(float renderDelta, CallbackInfo ci) {
        if (CustomUnitRenderEvents.BEFORE_DRAW_OVERLAY.invoker().beforeDrawOverlay(this, renderDelta)) {
            ci.cancel();
        }
    }

    @Inject(method = "drawPostOverlayLayer(F)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterDrawOverlay(float renderDelta, CallbackInfo ci) {
        CustomUnitRenderEvents.AFTER_DRAW_OVERLAY.invoker().afterDrawOverlay(this, renderDelta);
    }

    @Inject(method = "getTurretWorldTransform(IZ)Lrustedwarfare/math/Point3F;", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeTurretWorldTransform(int turretIndex, boolean includeHeight, CallbackInfoReturnable<Object> cir) {
        CustomUnitRenderEvents.BEFORE_TURRET_WORLD_TRANSFORM.invoker().beforeTurretWorldTransform(this, turretIndex, includeHeight);
    }

    @Inject(method = "getTurretWorldTransform(IZ)Lrustedwarfare/math/Point3F;", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$afterTurretWorldTransform(int turretIndex, boolean includeHeight, CallbackInfoReturnable<Object> cir) {
        cir.setReturnValue(CustomUnitRenderEvents.AFTER_TURRET_WORLD_TRANSFORM.invoker().afterTurretWorldTransform(this, turretIndex, includeHeight, cir.getReturnValue()));
    }
}
