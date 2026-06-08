package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.CustomUnitEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.corrodinggames.rts.game.units.custom.ag", remap = false)
public abstract class CustomUnitLoaderOfficialMixin {
    @Inject(
            method = "h()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/corrodinggames/rts/game/units/custom/ag;a(Ljava/lang/String;IZLcom/corrodinggames/rts/gameFramework/i/b;Ljava/lang/String;Ljava/lang/String;)V",
                    ordinal = 0
            ),
            require = 1
    )
    private static void rustedfabricapi$beforeNativeCustomUnitLoad(CallbackInfo ci) {
        CustomUnitEvents.BEFORE_NATIVE_CUSTOM_UNIT_LOAD.invoker().beforeNativeCustomUnitLoad();
    }

    @Inject(
            method = "h()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/corrodinggames/rts/game/units/custom/ag;b(Z)Ljava/lang/String;"
            ),
            require = 1
    )
    private static void rustedfabricapi$afterNativeCustomUnitParseBeforeEnable(CallbackInfo ci) {
        CustomUnitEvents.AFTER_NATIVE_CUSTOM_UNIT_PARSE_BEFORE_ENABLE.invoker().afterNativeCustomUnitParseBeforeEnable();
    }

    @Inject(
            method = "b(Z)Ljava/lang/String;",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/corrodinggames/rts/game/units/custom/ag;e()V"
            ),
            require = 1
    )
    private static void rustedfabricapi$beforeCustomUnitRegistryRebuild(boolean includeDisabledMods, CallbackInfoReturnable<String> cir) {
        CustomUnitEvents.BEFORE_CUSTOM_UNIT_REGISTRY_REBUILD.invoker().beforeCustomUnitRegistryRebuild(includeDisabledMods);
    }

    @Inject(
            method = "o()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/corrodinggames/rts/game/units/custom/ag;a(Lcom/corrodinggames/rts/game/units/as;)V",
                    ordinal = 0
            ),
            require = 1
    )
    private static void rustedfabricapi$afterCustomUnitOverrideAndReplace(CallbackInfo ci) {
        CustomUnitEvents.AFTER_CUSTOM_UNIT_OVERRIDE_AND_REPLACE.invoker().afterCustomUnitOverrideAndReplace();
    }

    @Inject(method = "o()V", at = @At("RETURN"), require = 1)
    private static void rustedfabricapi$afterCustomUnitLinkGraphBuilt(CallbackInfo ci) {
        CustomUnitEvents.AFTER_CUSTOM_UNIT_LINK_GRAPH_BUILT.invoker().afterCustomUnitLinkGraphBuilt();
    }
}
