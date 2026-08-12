package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.CustomUnitEvents;
import io.github.endx.rustedfabricapi.internal.development.DevelopmentReloadRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "rustedwarfare.custom.CustomUnitLoader", remap = false)
public abstract class CustomUnitLoaderNamedMixin {
    @Inject(method = "checkAndReloadChangedCustomUnits()V", at = @At("HEAD"), require = 1)
    private static void rustedfabricapi$syncEditableContentBeforeSandboxReload(CallbackInfo ci) {
        DevelopmentReloadRuntime.synchronizeBeforeExternalNativeReload();
    }

    @Inject(
            method = "loadAllCustomUnitConfigs()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lrustedwarfare/custom/CustomUnitLoader;readAllCustomUnitConfigs(Ljava/lang/String;IZLrustedwarfare/mod/ModInfo;Ljava/lang/String;Ljava/lang/String;)V",
                    ordinal = 0
            ),
            require = 1
    )
    private static void rustedfabricapi$beforeNativeCustomUnitLoad(CallbackInfo ci) {
        CustomUnitEvents.BEFORE_NATIVE_CUSTOM_UNIT_LOAD.invoker().beforeNativeCustomUnitLoad();
        io.github.endx.rustedfabricapi.api.custom.event.CustomUnitRegistryEvents.BEFORE_NATIVE_LOAD
                .invoker().onLoadPhase(io.github.endx.rustedfabricapi.api.custom.CustomUnits.pendingTypes());
    }

    @Inject(
            method = "loadAllCustomUnitConfigs()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lrustedwarfare/custom/CustomUnitLoader;enableAllLoadedCustomUnitTypes(Z)Ljava/lang/String;"
            ),
            require = 1
    )
    private static void rustedfabricapi$afterNativeCustomUnitParseBeforeEnable(CallbackInfo ci) {
        CustomUnitEvents.AFTER_NATIVE_CUSTOM_UNIT_PARSE_BEFORE_ENABLE.invoker().afterNativeCustomUnitParseBeforeEnable();
        io.github.endx.rustedfabricapi.api.custom.event.CustomUnitRegistryEvents.AFTER_PARSE_BEFORE_ENABLE
                .invoker().onLoadPhase(io.github.endx.rustedfabricapi.api.custom.CustomUnits.pendingTypes());
    }

    @Inject(
            method = "enableAllLoadedCustomUnitTypes(Z)Ljava/lang/String;",
            at = @At(
                    value = "INVOKE",
                    target = "Lrustedwarfare/custom/CustomUnitLoader;rebuildActiveUnitTypeRegistrySynchronized()V"
            ),
            require = 1
    )
    private static void rustedfabricapi$beforeCustomUnitRegistryRebuild(boolean includeDisabledMods, CallbackInfoReturnable<String> cir) {
        CustomUnitEvents.BEFORE_CUSTOM_UNIT_REGISTRY_REBUILD.invoker().beforeCustomUnitRegistryRebuild(includeDisabledMods);
        io.github.endx.rustedfabricapi.api.custom.event.CustomUnitRegistryEvents.BEFORE_REGISTRY_REBUILD
                .invoker().beforeRebuild(includeDisabledMods,
                        io.github.endx.rustedfabricapi.api.custom.CustomUnits.pendingTypes());
    }

    @Inject(
            method = "rebuildCustomUnitLookupAndActionLinksInternal()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lrustedwarfare/custom/CustomUnitLoader;setupUnitTypeActionsAndBuildLinks(Lrustedwarfare/unit/UnitType;)V",
                    ordinal = 0
            ),
            require = 1
    )
    private static void rustedfabricapi$afterCustomUnitOverrideAndReplace(CallbackInfo ci) {
        CustomUnitEvents.AFTER_CUSTOM_UNIT_OVERRIDE_AND_REPLACE.invoker().afterCustomUnitOverrideAndReplace();
        io.github.endx.rustedfabricapi.api.custom.event.CustomUnitRegistryEvents.AFTER_OVERRIDE_AND_REPLACE
                .invoker().onRegistryPhase(io.github.endx.rustedfabricapi.api.custom.CustomUnits.activeTypes());
    }

    @Inject(method = "rebuildCustomUnitLookupAndActionLinksInternal()V", at = @At("RETURN"), require = 1)
    private static void rustedfabricapi$afterCustomUnitLinkGraphBuilt(CallbackInfo ci) {
        CustomUnitEvents.AFTER_CUSTOM_UNIT_LINK_GRAPH_BUILT.invoker().afterCustomUnitLinkGraphBuilt();
        io.github.endx.rustedfabricapi.api.custom.event.CustomUnitRegistryEvents.AFTER_ACTION_LINKS_BUILT
                .invoker().onRegistryPhase(io.github.endx.rustedfabricapi.api.custom.CustomUnits.activeTypes());
    }
}
