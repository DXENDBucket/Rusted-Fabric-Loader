package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.ini.RustedIniDiagnostics;
import io.github.endx.rustedfabricapi.api.event.RustedIniEvents;
import io.github.endx.rustedfabricapi.impl.ini.IniExtensionRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.InputStream;

@Mixin(targets = "rustedwarfare.util.UnitConfig", remap = false)
public abstract class UnitConfigIniNamedMixin {
    @Inject(method = "parseInputStream(Ljava/io/InputStream;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterParseUnitConfig(InputStream inputStream, CallbackInfo ci) {
        IniExtensionRuntime.index(this);
        RustedIniEvents.AFTER_PARSE_UNIT_CONFIG.invoker().afterParseUnitConfig(this, inputStream);
    }

    @Inject(method = "getTrackedValue(Ljava/lang/String;Ljava/lang/String;ZLjava/lang/String;)Ljava/lang/String;", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeTrackedKeyRead(String section, String key, boolean required, String valueType, CallbackInfoReturnable<String> cir) {
        String fallback = IniExtensionRuntime.nativeFallback(this, section, key);
        if (fallback != null) {
            cir.setReturnValue(fallback);
            return;
        }
        if (!RustedIniDiagnostics.isKeyReadTracingEnabled()) {
            return;
        }

        RustedIniEvents.KeyReadContext context = new RustedIniEvents.KeyReadContext(
                this, section, key, valueType, "getTrackedValue", required, null);
        RustedIniEvents.BEFORE_KEY_READ.invoker().beforeKeyRead(context);
        if (context.rawValueOverrideSet()) {
            cir.setReturnValue(asString(context.rawValue()));
        }
    }

    @Inject(method = "getTrackedValue(Ljava/lang/String;Ljava/lang/String;ZLjava/lang/String;)Ljava/lang/String;", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$afterTrackedKeyRead(String section, String key, boolean required, String valueType, CallbackInfoReturnable<String> cir) {
        if (!RustedIniDiagnostics.isKeyReadTracingEnabled()) {
            return;
        }

        RustedIniEvents.KeyReadContext context = new RustedIniEvents.KeyReadContext(
                this, section, key, valueType, "getTrackedValue", required, cir.getReturnValue());
        RustedIniEvents.AFTER_KEY_READ.invoker().afterKeyRead(context);
        if (context.rawValueOverrideSet()) {
            cir.setReturnValue(asString(context.rawValue()));
        }
    }

    @Inject(method = "checkUnusedKeys()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeUnusedKeyCheck(CallbackInfo ci) {
        IniExtensionRuntime.markActiveFieldsRead(this);
        RustedIniEvents.BEFORE_UNUSED_KEY_CHECK.invoker().beforeUnusedKeyCheck(this);
    }

    @Inject(method = "checkUnusedKeys()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterUnusedKeyCheck(CallbackInfo ci) {
        RustedIniEvents.AFTER_UNUSED_KEY_CHECK.invoker().afterUnusedKeyCheck(this);
    }

    private static String asString(Object value) {
        return value != null ? value.toString() : null;
    }
}
