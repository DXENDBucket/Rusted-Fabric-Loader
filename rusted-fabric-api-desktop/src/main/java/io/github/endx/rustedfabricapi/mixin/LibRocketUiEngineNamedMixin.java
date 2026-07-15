package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.UiScriptEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "rustedwarfare.ui.LibRocketUiEngine", remap = false)
public abstract class LibRocketUiEngineNamedMixin {
    @Inject(method = "HandleEvent(Ljava/lang/String;)V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeUiEventHandled(String event, CallbackInfo ci) {
        UiScriptEvents.BEFORE_UI_EVENT_HANDLED.invoker()
                .beforeUiEventHandled(this, event);
    }

    @Inject(method = "newDocumentLoaded(Lcom/ElementDocument;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterUiDocumentLoaded(@Coerce Object document, CallbackInfo ci) {
        UiScriptEvents.AFTER_UI_DOCUMENT_LOADED.invoker()
                .afterUiDocumentLoaded(this, document);
    }

    @Inject(method = "newDocumentShown(Lcom/ElementDocument;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterUiDocumentShown(@Coerce Object document, CallbackInfo ci) {
        UiScriptEvents.AFTER_UI_DOCUMENT_SHOWN.invoker()
                .afterUiDocumentShown(this, document);
    }
}
