package io.github.endx.rustedfabricapi.api.unit.status;

import io.github.endx.rustedfabricapi.api.client.RustedWarfareClient;
import io.github.endx.rustedfabricapi.api.util.RustedReflection;
import rustedwarfare.core.GameEngine;
import rustedwarfare.unit.action.UnitActionId;
import rustedwarfare.unit.status.MovementSpeedStatusEffect;
import rustedwarfare.unit.status.SpecialActionBlockStatusEffect;
import rustedwarfare.unit.status.UnitStatusEffect;

import java.util.Objects;

/** Immutable view of an active native status effect. Time values use the simulation clock. */
public final class StatusEffectSnapshot {
    private final UnitStatusEffect effect;
    private final StatusEffectKind kind;
    private final int expiresAtTime;
    private final int remainingTime;
    private final float movementSpeedMultiplier;
    private final int startedAtTime;
    private final UnitActionId blockedActionId;

    private StatusEffectSnapshot(UnitStatusEffect effect, int currentTime) {
        this.effect = Objects.requireNonNull(effect, "effect");
        this.kind = StatusEffects.kindOf(effect);
        this.expiresAtTime = effect.getExpireFrame();
        this.remainingTime = Math.max(0, expiresAtTime - currentTime);
        if (effect instanceof MovementSpeedStatusEffect) {
            this.movementSpeedMultiplier = RustedReflection.getFloatField(
                    effect, new String[]{"speedMultiplier", "b"});
        } else {
            this.movementSpeedMultiplier = Float.NaN;
        }
        if (effect instanceof SpecialActionBlockStatusEffect) {
            this.startedAtTime = RustedReflection.getIntField(
                    effect, new String[]{"startFrame", "b"});
            this.blockedActionId = (UnitActionId) RustedReflection.getFieldValue(
                    effect, new String[]{"blockedActionId", "c"});
        } else {
            this.startedAtTime = -1;
            this.blockedActionId = null;
        }
    }

    public static StatusEffectSnapshot capture(UnitStatusEffect effect) {
        return new StatusEffectSnapshot(effect, currentTime());
    }

    /** Captures using an explicit simulation time, useful for replay and offline tooling. */
    public static StatusEffectSnapshot capture(UnitStatusEffect effect, int currentTime) {
        return new StatusEffectSnapshot(effect, currentTime);
    }

    public UnitStatusEffect effect() { return effect; }
    public StatusEffectKind kind() { return kind; }
    public int expiresAtTime() { return expiresAtTime; }
    public int remainingTime() { return remainingTime; }
    public boolean expired() { return remainingTime <= 0; }
    public float movementSpeedMultiplier() { return movementSpeedMultiplier; }
    public int startedAtTime() { return startedAtTime; }
    public UnitActionId blockedActionId() { return blockedActionId; }

    private static int currentTime() {
        GameEngine engine = RustedWarfareClient.getEngine();
        return engine != null ? engine.gameTimeMillis : 0;
    }
}
