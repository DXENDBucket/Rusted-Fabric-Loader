package io.github.endx.rustedfabricapi.mixin;

import android.graphics.RectF;
import io.github.endx.rustedfabricapi.api.client.event.WorldRenderEvents;
import io.github.endx.rustedfabricapi.api.client.render.WorldDrawContext;
import io.github.endx.rustedfabricapi.api.client.render.WorldLayerDrawContext;
import io.github.endx.rustedfabricapi.api.client.render.WorldViewport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rustedwarfare.core.RustedWarfareGameEngine;
import rustedwarfare.render.CanvasDrawTarget;
import rustedwarfare.render.GraphicsEngine;
import rustedwarfare.util.GameObjectArrayList;

@Mixin(targets = "rustedwarfare.core.RustedWarfareGameEngine", remap = false)
public abstract class WorldRenderNamedMixin {
    @Shadow private GameObjectArrayList visibleWorldObjects;

    @Inject(method = "drawWorld(Lrustedwarfare/render/CanvasDrawTarget;F)V",
            at = @At(value = "INVOKE",
                    target = "Lrustedwarfare/path/PathEngine;drawDebugOverlays(F)V",
                    shift = At.Shift.AFTER), require = 1)
    private void rustedfabricapi$beforeUnitRender(CanvasDrawTarget canvas, float delta,
                                                  CallbackInfo ci) {
        rustedfabricapi$dispatchWorldLayer(WorldRenderEvents.BEFORE_UNITS, delta);
    }

    @Inject(method = "drawWorld(Lrustedwarfare/render/CanvasDrawTarget;F)V",
            at = @At(value = "INVOKE",
                    target = "Lrustedwarfare/render/effect/NoiseCloudOverlay;draw(F)V"),
            require = 1)
    private void rustedfabricapi$afterUnitRender(CanvasDrawTarget canvas, float delta,
                                                 CallbackInfo ci) {
        rustedfabricapi$dispatchWorldLayer(WorldRenderEvents.AFTER_UNITS, delta);
    }

    @Inject(method = "drawWorld(Lrustedwarfare/render/CanvasDrawTarget;F)V",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterWorldRender(CanvasDrawTarget canvas, float delta,
                                                   CallbackInfo ci) {
        RustedWarfareGameEngine engine = (RustedWarfareGameEngine) (Object) this;
        GraphicsEngine graphics = engine.renderGraphicsEngine;
        if (!engine.hasLoadedLevel || graphics == null || !(engine.zoom > 0.0F)
                || !(engine.visibleWorldWidth > 0.0F) || !(engine.visibleWorldHeight > 0.0F)) {
            return;
        }
        WorldViewport viewport = new WorldViewport(engine.viewpointXSnapped,
                engine.viewpointYSnapped, engine.visibleWorldWidth,
                engine.visibleWorldHeight, engine.zoom);
        graphics.save();
        try {
            graphics.setClipRect(new RectF(0.0F, 0.0F,
                    viewport.screenWidth() + 1.0F, viewport.screenHeight() + 1.0F));
            WorldRenderEvents.AFTER_WORLD.invoker().draw(
                    new WorldDrawContext(graphics, viewport, delta));
        } finally {
            graphics.restore();
        }
    }

    private void rustedfabricapi$dispatchWorldLayer(
            io.github.endx.rustedfabricapi.api.event.RustedFabricEvent<WorldRenderEvents.WorldLayer> event,
            float delta) {
        if (!event.hasListeners()) return;
        RustedWarfareGameEngine engine = (RustedWarfareGameEngine) (Object) this;
        GraphicsEngine graphics = engine.renderGraphicsEngine;
        if (graphics == null || visibleWorldObjects == null) return;
        WorldViewport viewport = new WorldViewport(engine.viewpointXSnapped,
                engine.viewpointYSnapped, engine.visibleWorldWidth,
                engine.visibleWorldHeight, engine.zoom);
        event.invoker().draw(new WorldLayerDrawContext(graphics, viewport, delta,
                visibleWorldObjects.getBackingArray(), visibleWorldObjects.size()));
    }
}
