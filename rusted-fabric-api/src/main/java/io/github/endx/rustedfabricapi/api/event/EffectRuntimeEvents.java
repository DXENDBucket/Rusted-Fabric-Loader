package io.github.endx.rustedfabricapi.api.event;

public final class EffectRuntimeEvents {
    public static final RustedFabricEvent<AfterLineEffectCreated> AFTER_LINE_EFFECT_CREATED =
            RustedFabricEvent.create(listeners -> (engine, effect, startX, startY, startHeight,
                                                   targetX, targetY, targetHeight) -> {
                for (AfterLineEffectCreated listener : listeners) {
                    listener.afterLineEffectCreated(engine, effect, startX, startY, startHeight,
                            targetX, targetY, targetHeight);
                }
            });

    public static final RustedFabricEvent<AfterLightEffectCreated> AFTER_LIGHT_EFFECT_CREATED =
            RustedFabricEvent.create(listeners -> (engine, effect, x, y, height, color) -> {
                for (AfterLightEffectCreated listener : listeners) {
                    listener.afterLightEffectCreated(engine, effect, x, y, height, color);
                }
            });

    public static final RustedFabricEvent<AfterAttachedLightEffectCreated> AFTER_ATTACHED_LIGHT_EFFECT_CREATED =
            RustedFabricEvent.create(listeners -> (engine, effect, object, color, size) -> {
                for (AfterAttachedLightEffectCreated listener : listeners) {
                    listener.afterAttachedLightEffectCreated(engine, effect, object, color, size);
                }
            });

    private EffectRuntimeEvents() {
    }

    @FunctionalInterface
    public interface AfterLineEffectCreated {
        void afterLineEffectCreated(Object engine, Object effect, float startX, float startY, float startHeight,
                                    float targetX, float targetY, float targetHeight);
    }

    @FunctionalInterface
    public interface AfterLightEffectCreated {
        void afterLightEffectCreated(Object engine, Object effect, float x, float y, float height, int color);
    }

    @FunctionalInterface
    public interface AfterAttachedLightEffectCreated {
        void afterAttachedLightEffectCreated(Object engine, Object effect, Object object, int color, float size);
    }
}
