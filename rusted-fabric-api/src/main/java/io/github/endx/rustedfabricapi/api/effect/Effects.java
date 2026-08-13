package io.github.endx.rustedfabricapi.api.effect;

import io.github.endx.rustedfabricapi.api.client.RustedWarfareClient;
import rustedwarfare.core.GameEngine;
import rustedwarfare.framework.GameObject;
import rustedwarfare.render.effect.BuiltInEffectKind;
import rustedwarfare.render.effect.EffectEngine;
import rustedwarfare.render.effect.EffectInstance;
import rustedwarfare.render.effect.EffectPriority;

import java.util.Objects;
import java.util.Optional;

/** Creation and lifecycle helpers for the game's pooled visual effects. */
public final class Effects {
    private Effects() {
    }

    public static EffectEngine manager() {
        GameEngine engine = RustedWarfareClient.requireEngine();
        if (engine.effectManager == null) {
            throw new IllegalStateException("The visual effect manager is not initialized");
        }
        return engine.effectManager;
    }

    public static int activeCount() {
        return manager().activeEffectCount;
    }

    public static Optional<EffectInstance> line(float startX, float startY, float startHeight,
            float targetX, float targetY, float targetHeight) {
        requireFinite(startX, "startX");
        requireFinite(startY, "startY");
        requireFinite(startHeight, "startHeight");
        requireFinite(targetX, "targetX");
        requireFinite(targetY, "targetY");
        requireFinite(targetHeight, "targetHeight");
        return Optional.ofNullable(manager().createLineEffect(startX, startY, startHeight,
                targetX, targetY, targetHeight));
    }

    /** Creates a line effect through a namespace-stable mutable handle. */
    public static Optional<EffectHandle> lineHandle(float startX, float startY, float startHeight,
            float targetX, float targetY, float targetHeight) {
        return line(startX, startY, startHeight, targetX, targetY, targetHeight)
                .map(EffectHandle::new);
    }

    public static Optional<EffectInstance> light(float x, float y, float height, int argb) {
        requirePosition(x, y, height);
        return Optional.ofNullable(manager().createLightEffect(x, y, height, argb));
    }

    /** Creates a light effect through a namespace-stable mutable handle. */
    public static Optional<EffectHandle> lightHandle(float x, float y, float height, int argb) {
        return light(x, y, height, argb).map(EffectHandle::new);
    }

    public static Optional<EffectInstance> attachedLight(GameObject object, int argb, float size) {
        Objects.requireNonNull(object, "object");
        requireNonNegativeFinite(size, "size");
        return Optional.ofNullable(manager().createAttachedLightEffect(object, argb, size));
    }

    public static Optional<EffectInstance> smallExplosion(float x, float y, float height) {
        requirePosition(x, y, height);
        return Optional.ofNullable(manager().createSmallExplosionBuiltInEffect(x, y, height));
    }

    public static void largeExplosion(float x, float y, float height) {
        requirePosition(x, y, height);
        manager().emitLargeExplosionBuiltInEffect(x, y, height);
    }

    public static Optional<EffectInstance> smoke(float x, float y, float height,
            float direction, int variant) {
        requirePosition(x, y, height);
        requireFinite(direction, "direction");
        return Optional.ofNullable(manager().createSmokeBuiltInEffect(
                x, y, height, direction, variant));
    }

    /** Creates the game's soft resource-pool smoke with a namespace-stable mutable handle. */
    public static Optional<EffectHandle> resourceSmoke(float x, float y, float height, int argb) {
        requirePosition(x, y, height);
        return Optional.ofNullable(manager().createResourcePoolSmokeEffect(x, y, height, argb))
                .map(EffectHandle::new);
    }

    public static Optional<EffectInstance> shockwave(float x, float y, float height, int argb) {
        requirePosition(x, y, height);
        return Optional.ofNullable(manager().createShockwaveBuiltInEffect(x, y, height, argb));
    }

    public static Optional<EffectInstance> create(float x, float y, float height,
            BuiltInEffectKind kind, boolean showInFog, EffectPriority priority) {
        requirePosition(x, y, height);
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(priority, "priority");
        return Optional.ofNullable(manager().createEffect(x, y, height, kind, showInFog, priority));
    }

    public static void requireNextOnScreen() {
        manager().requireNextEffectOnscreen();
    }

    public static void allowNextOffScreen() {
        manager().allowNextEffectOffscreen();
    }

    public static void setNextPriority(EffectPriority priority) {
        manager().setNextEffectPriority(Objects.requireNonNull(priority, "priority"));
    }

    /** Deactivates through the native bookkeeping path, including configured death effects. */
    public static boolean deactivate(EffectInstance effect) {
        Objects.requireNonNull(effect, "effect");
        if (!effect.active) return false;
        effect.deactivateAndEmitDeathEffects();
        return true;
    }

    /** Removes all pooled effects. Call on the render/update thread. */
    public static void clear() {
        manager().removeAll(false);
    }

    private static void requirePosition(float x, float y, float height) {
        requireFinite(x, "x");
        requireFinite(y, "y");
        requireFinite(height, "height");
    }

    private static void requireFinite(float value, String name) {
        if (!Float.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
    }

    private static void requireNonNegativeFinite(float value, String name) {
        requireFinite(value, name);
        if (value < 0.0F) throw new IllegalArgumentException(name + " must be non-negative");
    }
}
