package io.github.endx.rustedfabricapi.api.effect.event;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import rustedwarfare.framework.GameObject;
import rustedwarfare.render.effect.EffectEngine;
import rustedwarfare.render.effect.EffectInstance;

/** Typed callbacks fired after common built-in effects have been fully configured. */
public final class EffectEvents {
    public static final RustedFabricEvent<AfterLine> AFTER_LINE =
            RustedFabricEvent.create(listeners -> (engine, effect, startX, startY, startHeight,
                    targetX, targetY, targetHeight) -> {
                for (AfterLine listener : listeners) {
                    listener.afterLine(engine, effect, startX, startY, startHeight,
                            targetX, targetY, targetHeight);
                }
            });

    public static final RustedFabricEvent<AfterLight> AFTER_LIGHT =
            RustedFabricEvent.create(listeners -> (engine, effect, x, y, height, color) -> {
                for (AfterLight listener : listeners) {
                    listener.afterLight(engine, effect, x, y, height, color);
                }
            });

    public static final RustedFabricEvent<AfterAttachedLight> AFTER_ATTACHED_LIGHT =
            RustedFabricEvent.create(listeners -> (engine, effect, object, color, size) -> {
                for (AfterAttachedLight listener : listeners) {
                    listener.afterAttachedLight(engine, effect, object, color, size);
                }
            });

    private EffectEvents() {
    }

    @FunctionalInterface
    public interface AfterLine {
        void afterLine(EffectEngine engine, EffectInstance effect,
                float startX, float startY, float startHeight,
                float targetX, float targetY, float targetHeight);
    }

    @FunctionalInterface
    public interface AfterLight {
        void afterLight(EffectEngine engine, EffectInstance effect,
                float x, float y, float height, int color);
    }

    @FunctionalInterface
    public interface AfterAttachedLight {
        void afterAttachedLight(EffectEngine engine, EffectInstance effect,
                GameObject object, int color, float size);
    }
}
