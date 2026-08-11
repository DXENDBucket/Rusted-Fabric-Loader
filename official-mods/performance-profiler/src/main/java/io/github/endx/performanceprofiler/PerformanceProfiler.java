package io.github.endx.performanceprofiler;

import io.github.endx.rustedfabricapi.api.client.event.HudRenderEvents;
import io.github.endx.rustedfabricapi.api.client.input.ClientInputEvents;
import io.github.endx.rustedfabricapi.api.client.input.KeyBindings;
import io.github.endx.rustedfabricapi.api.client.input.ModKeyBinding;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/** Official, renderer-independent performance diagnostics. */
public final class PerformanceProfiler implements ClientModInitializer {
    public static final String MOD_ID = "performance_profiler";
    private static final PerformanceProfilerRuntime RUNTIME =
            new PerformanceProfilerRuntime();

    @Override
    public void onInitializeClient() {
        ModKeyBinding hud = KeyBindings.register(MOD_ID + ":toggle_hud",
                "Toggle performance HUD", "Performance Profiler", "F8");
        ModKeyBinding sampling = KeyBindings.register(MOD_ID + ":toggle_sampling",
                "Start/stop performance sampling", "Performance Profiler", "F9");
        RUNTIME.install(FabricLoader.getInstance().getGameDir(), hud, sampling);
        ClientInputEvents.MOUSE_MOVED.register(RUNTIME::pointerMoved);
        ClientInputEvents.MOUSE_DRAGGED.register(RUNTIME::pointerMoved);
        ClientInputEvents.MOUSE_RELEASED.register(RUNTIME::pointerReleased);
        HudRenderEvents.AFTER_HUD.register((gameInterface, context) ->
                RUNTIME.drawHud(context));
        Runtime.getRuntime().addShutdownHook(
                new Thread(RUNTIME::close, "RFL profiler shutdown"));
        System.out.println("[Performance Profiler] Ready: F8 HUD, F9 sampling report");
    }

    public static void frameStarted() {
        RUNTIME.frameStarted();
    }

    public static void frameFinished() {
        RUNTIME.frameFinished();
    }

    public PerformanceProfiler() {
    }
}
