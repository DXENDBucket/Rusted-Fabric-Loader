package io.github.endx.rustedfabricexample;

import io.github.endx.rustedfabricapi.api.RustedFabricAPIContext;
import io.github.endx.rustedfabricapi.api.RustedFabricAPIEntrypoint;
import io.github.endx.rustedfabricapi.api.diagnostic.PlatformRuntimeDiagnostics;

public final class ExampleRustedEntrypoint extends RustedFabricAPIEntrypoint {
    @Override
    protected void onRustedFabricAPI(RustedFabricAPIContext ctx) {
        ExampleMod.log(ctx.entrypointKey()
                + " contextVersion=" + ctx.contextVersion()
                + ", loaderVersion=" + ctx.loaderVersion()
                + ", gameVersion=" + ctx.gameVersion()
                + ", mappingsVersion=" + ctx.mappingsVersion()
                + ", runtimeNamespace=" + ctx.runtimeNamespace()
                + ", androidRuntime=" + ctx.androidRuntime()
                + ", gameDir=" + ctx.gameDir()
                + ", gameJar=" + ctx.gameJar());
        try {
            ExampleMod.log(ctx.entrypointKey() + " platform=" + PlatformRuntimeDiagnostics.describePlatform());
        } catch (RuntimeException e) {
            ExampleMod.log(ctx.entrypointKey() + " platform diagnostics unavailable: " + e.getMessage());
        }
        ExampleMod.rememberGameDir(ctx.gameDir());
        ExampleMod.logNamedGameTypes(ctx.entrypointKey());
        ExampleMod.startVisibleSettingsTweaks(ctx.entrypointKey());
        ExampleMod.startMainMenuPopup(ctx.entrypointKey());
        ExampleEventProbes.registerMapEntryMessage(ctx.entrypointKey());
        ExampleEventProbes.registerEventProbeMessages(ctx.entrypointKey());
        ExampleMod.registerOverlayRenderer(ctx.entrypointKey());
    }
}
