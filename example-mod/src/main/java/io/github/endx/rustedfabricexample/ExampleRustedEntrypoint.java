package io.github.endx.rustedfabricexample;

import io.github.endx.rustedfabricapi.api.RustedFabricAPIContext;
import io.github.endx.rustedfabricapi.api.RustedFabricAPIEntrypoint;

public final class ExampleRustedEntrypoint extends RustedFabricAPIEntrypoint {
    @Override
    protected void onRustedFabricAPI(RustedFabricAPIContext ctx) {
        ExampleMod.log(ctx.entrypointKey()
                + " runtimeNamespace=" + ctx.runtimeNamespace()
                + ", androidRuntime=" + ctx.androidRuntime()
                + ", gameDir=" + ctx.gameDir()
                + ", gameJar=" + ctx.gameJar());
        ExampleMod.rememberGameDir(ctx.gameDir());
        ExampleMod.logNamedGameTypes(ctx.entrypointKey());
        ExampleMod.startVisibleSettingsTweaks(ctx.entrypointKey());
        ExampleMod.startMainMenuPopup(ctx.entrypointKey());
        ExampleEventProbes.registerMapEntryMessage(ctx.entrypointKey());
        ExampleEventProbes.registerEventProbeMessages(ctx.entrypointKey());
        ExampleMod.registerOverlayRenderer(ctx.entrypointKey());
    }
}
