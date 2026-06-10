package io.github.endx.rustedfabricapi.api.event;

public final class CustomUnitRenderEvents {
    private CustomUnitRenderEvents() {
    }

    public static final RustedFabricEvent<AfterGetBodyImage> AFTER_GET_BODY_IMAGE =
            RustedFabricEvent.create(listeners -> (unit, image) -> {
                Object result = image;
                for (AfterGetBodyImage listener : listeners) {
                    result = listener.afterGetBodyImage(unit, result);
                }
                return result;
            });

    public static final RustedFabricEvent<AfterGetZoomedIconImage> AFTER_GET_ZOOMED_ICON_IMAGE =
            RustedFabricEvent.create(listeners -> (unit, image) -> {
                Object result = image;
                for (AfterGetZoomedIconImage listener : listeners) {
                    result = listener.afterGetZoomedIconImage(unit, result);
                }
                return result;
            });

    public static final RustedFabricEvent<AfterGetShadowImage> AFTER_GET_SHADOW_IMAGE =
            RustedFabricEvent.create(listeners -> (unit, image) -> {
                Object result = image;
                for (AfterGetShadowImage listener : listeners) {
                    result = listener.afterGetShadowImage(unit, result);
                }
                return result;
            });

    public static final RustedFabricEvent<AfterGetTurretImage> AFTER_GET_TURRET_IMAGE =
            RustedFabricEvent.create(listeners -> (unit, turretIndex, image) -> {
                Object result = image;
                for (AfterGetTurretImage listener : listeners) {
                    result = listener.afterGetTurretImage(unit, turretIndex, result);
                }
                return result;
            });

    public static final RustedFabricEvent<AfterGetShieldImage> AFTER_GET_SHIELD_IMAGE =
            RustedFabricEvent.create(listeners -> (unit, image) -> {
                Object result = image;
                for (AfterGetShieldImage listener : listeners) {
                    result = listener.afterGetShieldImage(unit, result);
                }
                return result;
            });

    public static final RustedFabricEvent<BeforeFrameSourceRect> BEFORE_FRAME_SOURCE_RECT =
            RustedFabricEvent.create(listeners -> (unit, forShadow) -> {
                for (BeforeFrameSourceRect listener : listeners) {
                    listener.beforeFrameSourceRect(unit, forShadow);
                }
            });

    public static final RustedFabricEvent<AfterFrameSourceRect> AFTER_FRAME_SOURCE_RECT =
            RustedFabricEvent.create(listeners -> (unit, forShadow, rect) -> {
                Object result = rect;
                for (AfterFrameSourceRect listener : listeners) {
                    result = listener.afterFrameSourceRect(unit, forShadow, result);
                }
                return result;
            });

    public static final RustedFabricEvent<AfterImageDestinationRect> AFTER_IMAGE_DESTINATION_RECT =
            RustedFabricEvent.create(listeners -> (unit, rect) -> {
                Object result = rect;
                for (AfterImageDestinationRect listener : listeners) {
                    result = listener.afterImageDestinationRect(unit, result);
                }
                return result;
            });

    public static final RustedFabricEvent<BeforeDrawBackImage> BEFORE_DRAW_BACK_IMAGE =
            RustedFabricEvent.create(listeners -> (unit, renderDelta) -> {
                boolean cancelled = false;
                for (BeforeDrawBackImage listener : listeners) {
                    cancelled |= listener.beforeDrawBackImage(unit, renderDelta);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterDrawBackImage> AFTER_DRAW_BACK_IMAGE =
            RustedFabricEvent.create(listeners -> (unit, renderDelta) -> {
                for (AfterDrawBackImage listener : listeners) {
                    listener.afterDrawBackImage(unit, renderDelta);
                }
            });

    public static final RustedFabricEvent<BeforeDrawOverlay> BEFORE_DRAW_OVERLAY =
            RustedFabricEvent.create(listeners -> (unit, renderDelta) -> {
                boolean cancelled = false;
                for (BeforeDrawOverlay listener : listeners) {
                    cancelled |= listener.beforeDrawOverlay(unit, renderDelta);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterDrawOverlay> AFTER_DRAW_OVERLAY =
            RustedFabricEvent.create(listeners -> (unit, renderDelta) -> {
                for (AfterDrawOverlay listener : listeners) {
                    listener.afterDrawOverlay(unit, renderDelta);
                }
            });

    public static final RustedFabricEvent<BeforeTurretWorldTransform> BEFORE_TURRET_WORLD_TRANSFORM =
            RustedFabricEvent.create(listeners -> (unit, turretIndex, includeHeight) -> {
                for (BeforeTurretWorldTransform listener : listeners) {
                    listener.beforeTurretWorldTransform(unit, turretIndex, includeHeight);
                }
            });

    public static final RustedFabricEvent<AfterTurretWorldTransform> AFTER_TURRET_WORLD_TRANSFORM =
            RustedFabricEvent.create(listeners -> (unit, turretIndex, includeHeight, transform) -> {
                Object result = transform;
                for (AfterTurretWorldTransform listener : listeners) {
                    result = listener.afterTurretWorldTransform(unit, turretIndex, includeHeight, result);
                }
                return result;
            });

    @FunctionalInterface
    public interface AfterGetBodyImage {
        Object afterGetBodyImage(Object unit, Object image);
    }

    @FunctionalInterface
    public interface AfterGetZoomedIconImage {
        Object afterGetZoomedIconImage(Object unit, Object image);
    }

    @FunctionalInterface
    public interface AfterGetShadowImage {
        Object afterGetShadowImage(Object unit, Object image);
    }

    @FunctionalInterface
    public interface AfterGetTurretImage {
        Object afterGetTurretImage(Object unit, int turretIndex, Object image);
    }

    @FunctionalInterface
    public interface AfterGetShieldImage {
        Object afterGetShieldImage(Object unit, Object image);
    }

    @FunctionalInterface
    public interface BeforeFrameSourceRect {
        void beforeFrameSourceRect(Object unit, boolean forShadow);
    }

    @FunctionalInterface
    public interface AfterFrameSourceRect {
        Object afterFrameSourceRect(Object unit, boolean forShadow, Object rect);
    }

    @FunctionalInterface
    public interface AfterImageDestinationRect {
        Object afterImageDestinationRect(Object unit, Object rect);
    }

    @FunctionalInterface
    public interface BeforeDrawBackImage {
        boolean beforeDrawBackImage(Object unit, float renderDelta);
    }

    @FunctionalInterface
    public interface AfterDrawBackImage {
        void afterDrawBackImage(Object unit, float renderDelta);
    }

    @FunctionalInterface
    public interface BeforeDrawOverlay {
        boolean beforeDrawOverlay(Object unit, float renderDelta);
    }

    @FunctionalInterface
    public interface AfterDrawOverlay {
        void afterDrawOverlay(Object unit, float renderDelta);
    }

    @FunctionalInterface
    public interface BeforeTurretWorldTransform {
        void beforeTurretWorldTransform(Object unit, int turretIndex, boolean includeHeight);
    }

    @FunctionalInterface
    public interface AfterTurretWorldTransform {
        Object afterTurretWorldTransform(Object unit, int turretIndex, boolean includeHeight, Object transform);
    }
}
