package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.UiScriptEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "rustedwarfare.ui.LibRocketUiController", remap = false)
public abstract class LibRocketUiControllerNamedMixin {
    @Inject(method = "queuePasswordPromptPopup(Lrustedwarfare/network/PasswordPrompt;)V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforePasswordPromptPopup(@Coerce Object passwordPrompt, CallbackInfo ci) {
        UiScriptEvents.BEFORE_PASSWORD_PROMPT_POPUP.invoker()
                .beforePasswordPromptPopup(this, passwordPrompt);
    }

    @Inject(method = "queuePasswordPromptPopup(Lrustedwarfare/network/PasswordPrompt;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterPasswordPromptPopupQueued(@Coerce Object passwordPrompt, CallbackInfo ci) {
        UiScriptEvents.AFTER_PASSWORD_PROMPT_POPUP_QUEUED.invoker()
                .afterPasswordPromptPopupQueued(this, passwordPrompt);
    }
}
