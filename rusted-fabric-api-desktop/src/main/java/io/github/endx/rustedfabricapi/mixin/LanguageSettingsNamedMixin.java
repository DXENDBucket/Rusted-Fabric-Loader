package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.text.LanguageEvents;
import io.github.endx.rustedfabricapi.api.text.Translations;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "rustedwarfare.core.LanguageSettings", remap = false)
public abstract class LanguageSettingsNamedMixin {
    @Inject(method = "a()V", at = @At("HEAD"), require = 1)
    private static void rustedfabricapi$beforeLanguageReload(CallbackInfo ci) {
        LanguageEvents.BEFORE_RELOAD.invoker().onReload(Translations.currentLanguage());
    }

    @Inject(method = "a()V", at = @At("RETURN"), require = 1)
    private static void rustedfabricapi$afterLanguageReload(CallbackInfo ci) {
        Translations.invalidateCaches();
        LanguageEvents.AFTER_RELOAD.invoker().onReload(Translations.currentLanguage());
    }
}
