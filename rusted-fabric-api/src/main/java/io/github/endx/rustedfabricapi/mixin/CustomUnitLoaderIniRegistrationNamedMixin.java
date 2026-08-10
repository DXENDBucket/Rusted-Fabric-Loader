package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.RustedCustomUnitRegistry;
import io.github.endx.rustedfabricapi.api.event.RustedCustomUnitRegistryEvents;
import io.github.endx.rustedfabricapi.api.event.RustedIniEvents;
import io.github.endx.rustedfabricapi.impl.ini.IniExtensionRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.InputStream;

@Mixin(targets = "rustedwarfare.custom.CustomUnitLoader", remap = false)
public abstract class CustomUnitLoaderIniRegistrationNamedMixin {
    @Inject(method = "parseCustomUnitMetadata(Ljava/lang/String;Ljava/io/InputStream;JLrustedwarfare/mod/ModInfo;Lrustedwarfare/io/NamedInputStream;Ljava/lang/String;Ljava/lang/String;)Lrustedwarfare/custom/CustomUnitMetadata;", at = @At("HEAD"), cancellable = true, require = 1)
    private static void rustedfabricapi$beforeParseStream(String unitId, InputStream inputStream, long sourceTimestamp, @Coerce Object modInfo, @Coerce Object namedInputStream, String resourceRoot, String templateRoot, CallbackInfoReturnable<Object> cir) {
        if (RustedCustomUnitRegistry.isJavaRegistrationParseActive()) {
            return;
        }

        RustedIniEvents.ParseStreamContext context = new RustedIniEvents.ParseStreamContext(
                unitId, inputStream, sourceTimestamp, modInfo, namedInputStream, resourceRoot, templateRoot);
        RustedIniEvents.BEFORE_PARSE_STREAM.invoker().beforeParseStream(context);
        if (context.cancelled()) {
            cir.setReturnValue(context.metadataOverride());
        }
    }

    @Inject(method = "parseCustomUnitMetadata(Ljava/lang/String;Ljava/io/InputStream;JLrustedwarfare/mod/ModInfo;Lrustedwarfare/io/NamedInputStream;Ljava/lang/String;Ljava/lang/String;)Lrustedwarfare/custom/CustomUnitMetadata;", at = @At("RETURN"), cancellable = true, require = 1)
    private static void rustedfabricapi$afterMetadataParsed(String unitId, InputStream inputStream, long sourceTimestamp, @Coerce Object modInfo, @Coerce Object namedInputStream, String resourceRoot, String templateRoot, CallbackInfoReturnable<Object> cir) {
        if (RustedCustomUnitRegistry.isJavaRegistrationParseActive()) {
            return;
        }

        RustedIniEvents.ParseStreamContext context = new RustedIniEvents.ParseStreamContext(
                unitId, inputStream, sourceTimestamp, modInfo, namedInputStream, resourceRoot, templateRoot);
        Object metadata = cir.getReturnValue();
        if (metadata != null) {
            IniExtensionRuntime.applyAfterMetadataParsed(metadata);
        }
        Object replacement = RustedCustomUnitRegistryEvents.AFTER_METADATA_PARSED.invoker()
                .afterMetadataParsed(context, metadata);
        if (replacement != metadata) {
            RustedCustomUnitRegistry.replacePendingCustomUnitType(metadata, replacement);
            cir.setReturnValue(replacement);
        }
    }

    @Inject(method = "applyCoreCopyFrom(Lrustedwarfare/custom/CustomUnitMetadata;Lrustedwarfare/util/UnitConfig;Lrustedwarfare/util/UnitConfig;Ljava/lang/String;I)V", at = @At("HEAD"), cancellable = true, require = 1)
    private static void rustedfabricapi$beforeCopyFrom(@Coerce Object metadata, @Coerce Object targetConfig, @Coerce Object sourceConfig, String copyFromPath, int recursionDepth, CallbackInfo ci) {
        if (RustedIniEvents.BEFORE_COPY_FROM.invoker().beforeCopyFrom(metadata, targetConfig, sourceConfig, copyFromPath, recursionDepth)) {
            ci.cancel();
        }
    }

    @Inject(method = "applyCoreCopyFrom(Lrustedwarfare/custom/CustomUnitMetadata;Lrustedwarfare/util/UnitConfig;Lrustedwarfare/util/UnitConfig;Ljava/lang/String;I)V", at = @At("RETURN"), require = 1)
    private static void rustedfabricapi$afterCopyFrom(@Coerce Object metadata, @Coerce Object targetConfig, @Coerce Object sourceConfig, String copyFromPath, int recursionDepth, CallbackInfo ci) {
        IniExtensionRuntime.index(targetConfig);
        RustedIniEvents.AFTER_COPY_FROM.invoker().afterCopyFrom(metadata, targetConfig, sourceConfig, copyFromPath, recursionDepth);
    }

    @Inject(method = "enableAllLoadedCustomUnitTypes(Z)Ljava/lang/String;", at = @At("HEAD"), cancellable = true, require = 1)
    private static void rustedfabricapi$beforeCommit(boolean includeDisabledMods, CallbackInfoReturnable<String> cir) {
        if (RustedCustomUnitRegistry.isJavaRegistrationCommitActive()) {
            return;
        }

        if (RustedCustomUnitRegistryEvents.BEFORE_COMMIT.invoker()
                .beforeCommit(RustedCustomUnitRegistry.getPendingCustomUnitTypesSnapshot(), includeDisabledMods)) {
            cir.setReturnValue(RustedCustomUnitRegistry.getLastCustomUnitLoadError());
        }
    }

    @Inject(method = "enableAllLoadedCustomUnitTypes(Z)Ljava/lang/String;", at = @At("RETURN"), require = 1)
    private static void rustedfabricapi$afterCommit(boolean includeDisabledMods, CallbackInfoReturnable<String> cir) {
        if (RustedCustomUnitRegistry.isJavaRegistrationCommitActive()) {
            return;
        }

        RustedCustomUnitRegistryEvents.AFTER_COMMIT.invoker().afterCommit(
                RustedCustomUnitRegistry.getActiveCustomUnitTypesSnapshot(),
                cir.getReturnValue(),
                RustedCustomUnitRegistry.getUnitTypeReplacementMapSnapshot());
    }

    @Inject(method = "rebuildCustomUnitLookupAndActionLinks()V", at = @At("RETURN"), require = 1)
    private static void rustedfabricapi$afterRebuildLinks(CallbackInfo ci) {
        RustedCustomUnitRegistryEvents.AFTER_REBUILD_LINKS.invoker().afterRebuildLinks(
                RustedCustomUnitRegistry.getActiveCustomUnitTypesSnapshot(),
                RustedCustomUnitRegistry.getUnitTypeReplacementMapSnapshot());
    }

    @Inject(method = "validateCustomUnitLookupAndActionLinks(Z)Z", at = @At("RETURN"), cancellable = true, require = 1)
    private static void rustedfabricapi$afterValidateLinks(boolean strict, CallbackInfoReturnable<Boolean> cir) {
        boolean result = RustedCustomUnitRegistryEvents.AFTER_VALIDATE_LINKS.invoker()
                .afterValidateLinks(strict, Boolean.TRUE.equals(cir.getReturnValue()));
        cir.setReturnValue(Boolean.valueOf(result));
    }
}
