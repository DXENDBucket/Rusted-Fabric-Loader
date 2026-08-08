package io.github.endx.rustedfabricapi.mixin;

import com.ElementDocument;
import io.github.endx.rustedfabricapi.api.client.screen.UiDocumentKind;
import io.github.endx.rustedfabricapi.api.event.UiScriptEvents;
import io.github.endx.rustedfabricapi.internal.client.screen.ScreenRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rustedwarfare.ui.LibRocketUiEngine;
import rustedwarfare.ui.PopupDocumentData;

@Mixin(targets = "rustedwarfare.ui.LibRocketUiEngine", remap = false)
public abstract class LibRocketUiEngineNamedMixin {
    @Inject(method = "HandleEvent(Ljava/lang/String;)V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeUiEventHandled(String event, CallbackInfo ci) {
        UiScriptEvents.BEFORE_UI_EVENT_HANDLED.invoker()
                .beforeUiEventHandled(this, event);
    }

    @Inject(method = "newDocumentLoaded(Lcom/ElementDocument;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterUiDocumentLoaded(ElementDocument document, CallbackInfo ci) {
        UiScriptEvents.AFTER_UI_DOCUMENT_LOADED.invoker()
                .afterUiDocumentLoaded(this, document);
        ScreenRuntime.onPageLoaded((LibRocketUiEngine) (Object) this, document);
    }

    @Inject(method = "newDocumentShown(Lcom/ElementDocument;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterUiDocumentShown(ElementDocument document, CallbackInfo ci) {
        UiScriptEvents.AFTER_UI_DOCUMENT_SHOWN.invoker()
                .afterUiDocumentShown(this, document);
        ScreenRuntime.onPageShown((LibRocketUiEngine) (Object) this, document);
    }

    @Inject(method = "showAlertDocumentData(Lrustedwarfare/ui/PopupDocumentData;)Z",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterAlertShown(PopupDocumentData data,
            CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.TRUE.equals(cir.getReturnValue())) {
            ScreenRuntime.onOverlayShown((LibRocketUiEngine) (Object) this, data,
                    UiDocumentKind.ALERT);
        }
    }

    @Inject(method = "showPopupDocumentData(Lrustedwarfare/ui/PopupDocumentData;)Z",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterPopupShown(PopupDocumentData data,
            CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.TRUE.equals(cir.getReturnValue())) {
            ScreenRuntime.onOverlayShown((LibRocketUiEngine) (Object) this, data,
                    UiDocumentKind.POPUP);
        }
    }
}
