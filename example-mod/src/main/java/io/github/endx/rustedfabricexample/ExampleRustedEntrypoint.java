package io.github.endx.rustedfabricexample;

import io.github.endx.rustedfabricapi.api.RustedFabricAPIContext;
import io.github.endx.rustedfabricapi.api.RustedFabricAPIEntrypoint;
import io.github.endx.rustedfabricapi.api.diagnostic.PlatformRuntimeDiagnostics;
import io.github.endx.rustedfabricapi.api.unit.event.UnitEvents;

public final class ExampleRustedEntrypoint extends RustedFabricAPIEntrypoint {
    @Override
    protected void onRustedFabricAPI(RustedFabricAPIContext ctx) {
        PortableInitializationProbe.register(ExampleMod::log);
        UnitEvents.registerAfterUnitAdded(unit -> ExampleMod.log(
                "unit added: id=" + unit.id()
                        + ", hp=" + unit.health() + "/" + unit.maxHealth()
                        + ", movement=" + unit.movementType()));
        ExampleMod.log(ctx.entrypointKey()
                + " contextVersion=" + ctx.contextVersion()
                + ", loaderVersion=" + ctx.loaderVersion()
                + ", gameVersion=" + ctx.gameVersion()
                + ", mappingsVersion=" + ctx.mappingsVersion()
                + ", mappingProfile=" + ctx.mappingProfileId()
                + ", platform=" + ctx.platform()
                + ", runtimeNamespace=" + ctx.runtimeNamespace()
                + ", androidRuntime=" + ctx.androidRuntime()
                + ", capabilities=" + ctx.capabilities()
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
