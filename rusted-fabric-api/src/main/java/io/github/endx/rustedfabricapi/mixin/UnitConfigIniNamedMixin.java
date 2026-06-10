package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.RustedIniEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.InputStream;

@Mixin(targets = "rustedwarfare.util.UnitConfig", remap = false)
public abstract class UnitConfigIniNamedMixin {
    @Inject(method = "parseInputStream(Ljava/io/InputStream;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterParseUnitConfig(InputStream inputStream, CallbackInfo ci) {
        RustedIniEvents.AFTER_PARSE_UNIT_CONFIG.invoker().afterParseUnitConfig(this, inputStream);
    }
}
