package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.RustedIniEvents;
import io.github.endx.rustedfabricapi.impl.ini.IniExtensionRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "rustedwarfare.custom.config.ConfigVariableProcessor", remap = false)
public abstract class ConfigVariableProcessorNamedMixin {
    @Inject(method = "processStaticConfigVariables(Lrustedwarfare/custom/CustomUnitMetadata;Lrustedwarfare/util/UnitConfig;)V", at = @At("HEAD"), cancellable = true, require = 1)
    private static void rustedfabricapi$beforeStaticVariables(@Coerce Object metadata, @Coerce Object unitConfig, CallbackInfo ci) {
        IniExtensionRuntime.applyBeforeStaticVariables(metadata, unitConfig);
        if (RustedIniEvents.BEFORE_STATIC_VARIABLES.invoker().beforeStaticVariables(metadata, unitConfig)) {
            ci.cancel();
        }
    }

    @Inject(method = "processStaticConfigVariables(Lrustedwarfare/custom/CustomUnitMetadata;Lrustedwarfare/util/UnitConfig;)V", at = @At("RETURN"), require = 1)
    private static void rustedfabricapi$afterStaticVariables(@Coerce Object metadata, @Coerce Object unitConfig, CallbackInfo ci) {
        IniExtensionRuntime.applyAfterStaticVariables(metadata, unitConfig);
        RustedIniEvents.AFTER_STATIC_VARIABLES.invoker().afterStaticVariables(metadata, unitConfig);
    }
}
