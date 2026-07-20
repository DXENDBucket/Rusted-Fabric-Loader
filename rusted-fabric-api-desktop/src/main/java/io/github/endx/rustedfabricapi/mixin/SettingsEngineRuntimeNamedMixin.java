package io.github.endx.rustedfabricapi.mixin;

import java.util.Objects;

import io.github.endx.rustedfabricapi.api.client.option.ClientOption;
import io.github.endx.rustedfabricapi.api.client.option.ClientOptionChange;
import io.github.endx.rustedfabricapi.api.client.option.ClientOptions;
import io.github.endx.rustedfabricapi.api.client.option.event.ClientOptionEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rustedwarfare.core.SettingsEngine;

/** Observes supported reflective setting writes and the native persistence boundary. */
@Mixin(targets = "rustedwarfare.core.SettingsEngine", remap = false)
public abstract class SettingsEngineRuntimeNamedMixin {
    @Unique private ClientOption<?> rustedfabricapi$dynamicOption;
    @Unique private Object rustedfabricapi$dynamicPreviousValue;

    @Inject(method = "setValueDynamic(Ljava/lang/String;Ljava/lang/String;)Z",
            at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeDynamicChange(String name, String value,
            CallbackInfoReturnable<Boolean> cir) {
        SettingsEngine settings = (SettingsEngine) (Object) this;
        rustedfabricapi$dynamicOption = ClientOptions.findByNativeName(name).orElse(null);
        rustedfabricapi$dynamicPreviousValue = rustedfabricapi$dynamicOption != null
                ? rustedfabricapi$dynamicOption.get(settings) : null;
    }

    @Inject(method = "setValueDynamic(Ljava/lang/String;Ljava/lang/String;)Z",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterDynamicChange(String name, String value,
            CallbackInfoReturnable<Boolean> cir) {
        ClientOption<?> option = rustedfabricapi$dynamicOption;
        if (option == null || !Boolean.TRUE.equals(cir.getReturnValue())) return;
        SettingsEngine settings = (SettingsEngine) (Object) this;
        Object current = option.get(settings);
        if (!Objects.equals(rustedfabricapi$dynamicPreviousValue, current)) {
            ClientOptionEvents.AFTER_NATIVE_DYNAMIC_CHANGE.invoker().afterChange(
                    settings, rustedfabricapi$change(option,
                            rustedfabricapi$dynamicPreviousValue, current));
        }
    }

    @Unique
    @SuppressWarnings("unchecked")
    private static <T> ClientOptionChange<T> rustedfabricapi$change(
            ClientOption<?> option, Object previous, Object current) {
        ClientOption<T> typed = (ClientOption<T>) option;
        return ClientOptionChange.of(typed, (T) previous, (T) current);
    }

    @Inject(method = "save()Z", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeSave(CallbackInfoReturnable<Boolean> cir) {
        ClientOptionEvents.BEFORE_NATIVE_SAVE.invoker()
                .beforeSave((SettingsEngine) (Object) this);
    }

    @Inject(method = "save()Z", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterSave(CallbackInfoReturnable<Boolean> cir) {
        ClientOptionEvents.AFTER_NATIVE_SAVE.invoker().afterSave(
                (SettingsEngine) (Object) this,
                Boolean.TRUE.equals(cir.getReturnValue()));
    }
}
