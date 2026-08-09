package io.github.endx.rustedfabricapi.api.client.render;

import java.util.concurrent.atomic.AtomicInteger;

import io.github.endx.rustedfabricapi.api.client.event.HudRenderEvents;
import io.github.endx.rustedfabricapi.api.client.event.WorldRenderEvents;
import io.github.endx.rustedfabricapi.api.client.render.event.ClientImageEvents;
import io.github.endx.rustedfabricapi.api.client.render.event.DecalRenderEvents;
import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import io.github.endx.rustedfabricapi.api.world.WorldPoint;

public final class ClientRenderContractVerification {
    private ClientRenderContractVerification() {
    }

    public static void verify() {
        verifyColorAndStyleValues();
        verifyMaskAlphaFormulas();
        verifyWorldViewportValues();
        verifyImageAndEventContracts();
    }

    private static void verifyMaskAlphaFormulas() {
        AlphaMaskOptions multiply = AlphaMaskOptions.DEFAULT;
        require(multiply.combineAlpha(128, 0.5F) == 64,
                "multiplicative mask alpha drifted");
        AlphaMaskOptions binary = new AlphaMaskOptions(0.4F, false,
                MaskThresholdMode.BINARY, MaskAlphaMode.MIN);
        require(binary.combineAlpha(128, 0.39F) == 0
                        && binary.combineAlpha(128, 0.4F) == 128,
                "binary threshold boundary drifted");
        AlphaMaskOptions normalized = new AlphaMaskOptions(0.25F, false,
                MaskThresholdMode.NORMALIZE, MaskAlphaMode.REPLACE);
        require(normalized.combineAlpha(0, 0.625F) == 128,
                "normalized replacement mask alpha drifted");
        AlphaMaskOptions inverted = new AlphaMaskOptions(0.0F, true,
                MaskThresholdMode.KEEP, MaskAlphaMode.REPLACE);
        require(inverted.combineAlpha(255, 0.25F) == 191,
                "inverted mask alpha drifted");
    }

    private static void verifyWorldViewportValues() {
        WorldViewport viewport = new WorldViewport(100.0F, 50.0F, 400.0F, 300.0F, 2.0F);
        ScreenPosition screen = viewport.worldToScreen(110.0F, 70.0F);
        require(screen.equals(new ScreenPosition(20.0F, 40.0F)),
                "world-to-screen conversion drifted");
        WorldPoint world = viewport.screenToWorld(screen.x(), screen.y());
        require(world.equals(new WorldPoint(110.0F, 70.0F)),
                "screen-to-world conversion did not round trip");
        require(viewport.screenWidth() == 800.0F && viewport.screenHeight() == 600.0F,
                "viewport pixel dimensions drifted");
        require(viewport.worldLengthToPixels(12.0F) == 24.0F
                        && viewport.pixelsToWorldLength(24.0F) == 12.0F,
                "viewport length conversion drifted");
        require(viewport.contains(100.0F, 50.0F)
                        && !viewport.contains(99.0F, 50.0F)
                        && viewport.isVisible(90.0F, 60.0F, 10.0F)
                        && !viewport.isVisible(80.0F, 60.0F, 10.0F),
                "viewport visibility boundary drifted");
        try {
            new WorldViewport(0.0F, 0.0F, 1.0F, 1.0F, 0.0F);
            throw new AssertionError("zero viewport zoom was accepted");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    private static void verifyColorAndStyleValues() {
        int color = ArgbColor.argb(0x12, 0x34, 0x56, 0x78);
        require(color == 0x12345678
                        && ArgbColor.alpha(color) == 0x12
                        && ArgbColor.red(color) == 0x34
                        && ArgbColor.green(color) == 0x56
                        && ArgbColor.blue(color) == 0x78,
                "ARGB component packing drifted");
        require(ArgbColor.multiplyAlpha(0x80ffffff, 0.5F) == 0x40ffffff,
                "ARGB alpha multiplication returned the wrong value");
        try {
            ArgbColor.rgb(256, 0, 0);
            throw new AssertionError("invalid color component was accepted");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }

        DrawStyle base = DrawStyle.text(ArgbColor.WHITE, 18.0F);
        DrawStyle centered = base.withTextAlignment(TextAlignment.CENTER).withAlpha(128);
        require(base.textAlignment() == TextAlignment.LEFT
                        && base.color() == ArgbColor.WHITE
                        && centered.textAlignment() == TextAlignment.CENTER
                        && ArgbColor.alpha(centered.color()) == 128,
                "draw style mutation changed the original or lost a value");
        try {
            DrawStyle.stroke(ArgbColor.WHITE, 0.0F);
            throw new AssertionError("zero-width draw style was accepted");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    private static void verifyImageAndEventContracts() {
        AtomicInteger calls = new AtomicInteger();
        RustedFabricEvent.Registration beforeFirst = ClientImageEvents.BEFORE_LOAD.subscribe(
                (resource, smooth) -> {
                    calls.incrementAndGet();
                    return false;
                });
        RustedFabricEvent.Registration beforeSecond = ClientImageEvents.BEFORE_LOAD.subscribe(
                (resource, smooth) -> {
                    calls.incrementAndGet();
                    return true;
                });
        require(ClientImageEvents.BEFORE_LOAD.invoker().beforeLoad(null, true),
                "client image load cancellation was not aggregated");
        require(calls.get() == 2, "client image load cancellation skipped a listener");
        beforeFirst.close();
        beforeSecond.close();

        calls.set(0);
        RustedFabricEvent.Registration released = ClientImageEvents.AFTER_RELEASE.subscribe(
                (closed, nativeReleased) -> {
                    if (closed == null && !nativeReleased) calls.incrementAndGet();
                });
        ClientImageEvents.AFTER_RELEASE.invoker().afterRelease(null, false);
        require(calls.get() == 1, "client image release event was not dispatched");
        released.close();

        RustedFabricEvent.Registration beforeHud = HudRenderEvents.BEFORE_HUD.subscribe(
                (gameInterface, context) -> calls.addAndGet(10));
        HudRenderEvents.BEFORE_HUD.invoker().draw(null, null);
        require(calls.get() == 11, "typed before-HUD event was not dispatched");
        beforeHud.close();

        RustedFabricEvent.Registration hud = HudRenderEvents.AFTER_HUD.subscribe(
                (gameInterface, context) -> calls.addAndGet(10));
        HudRenderEvents.AFTER_HUD.invoker().draw(null, null);
        require(calls.get() == 21, "typed HUD event was not dispatched");
        hud.close();

        RustedFabricEvent.Registration world = WorldRenderEvents.AFTER_WORLD.subscribe(
                context -> calls.addAndGet(100));
        WorldRenderEvents.AFTER_WORLD.invoker().draw(null);
        require(calls.get() == 121, "typed world event was not dispatched");
        world.close();

        RustedFabricEvent.Registration decal = DecalRenderEvents.BEFORE_LAYER.subscribe(
                (unit, delta, layer, templates) -> calls.addAndGet(1000));
        DecalRenderEvents.BEFORE_LAYER.invoker().onLayer(null, 0.0F, null,
                java.util.Collections.emptyList());
        require(calls.get() == 1121, "typed Decal layer event was not dispatched");
        decal.close();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
