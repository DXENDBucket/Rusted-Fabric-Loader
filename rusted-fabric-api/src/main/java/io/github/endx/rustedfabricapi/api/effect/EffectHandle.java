package io.github.endx.rustedfabricapi.api.effect;

import rustedwarfare.render.effect.EffectInstance;

import java.util.Objects;

/** Namespace-stable handle for configuring one pooled native visual effect. */
public final class EffectHandle {
    private final EffectInstance effect;

    EffectHandle(EffectInstance effect) {
        this.effect = Objects.requireNonNull(effect, "effect");
    }

    public Object identity() { return effect; }
    public boolean active() { return effect.active; }

    public EffectHandle color(int argb) {
        effect.color = argb;
        return this;
    }

    public EffectHandle alpha(float alpha) {
        requireFinite(alpha, "alpha");
        effect.alpha = Math.max(0.0F, Math.min(1.0F, alpha));
        return this;
    }

    public EffectHandle scale(float from, float to) {
        requireNonNegativeFinite(from, "from");
        requireNonNegativeFinite(to, "to");
        effect.scaleFrom = from;
        effect.scaleTo = to;
        return this;
    }

    public EffectHandle velocity(float x, float y, float height) {
        requireFinite(x, "x");
        requireFinite(y, "y");
        requireFinite(height, "height");
        effect.velocityX = x;
        effect.velocityY = y;
        effect.velocityHeight = height;
        return this;
    }

    public EffectHandle lifetime(float frames) {
        requireNonNegativeFinite(frames, "frames");
        effect.lifeMax = frames;
        effect.lifeRemaining = frames;
        return this;
    }

    public EffectHandle fadeOut(boolean fadeOut) {
        effect.fadeOut = fadeOut;
        return this;
    }

    public EffectHandle showInFog(boolean showInFog) {
        effect.showInFog = showInFog;
        return this;
    }

    public boolean deactivate() {
        return Effects.deactivate(effect);
    }

    private static void requireFinite(float value, String name) {
        if (!Float.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
    }

    private static void requireNonNegativeFinite(float value, String name) {
        requireFinite(value, name);
        if (value < 0.0F) throw new IllegalArgumentException(name + " must be non-negative");
    }
}
