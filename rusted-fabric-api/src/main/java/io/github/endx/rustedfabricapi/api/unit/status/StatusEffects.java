package io.github.endx.rustedfabricapi.api.unit.status;

import io.github.endx.rustedfabricapi.api.client.RustedWarfareClient;
import io.github.endx.rustedfabricapi.api.unit.status.event.StatusEffectEvents;
import io.github.endx.rustedfabricapi.api.util.RustedReflection;
import rustedwarfare.core.GameEngine;
import rustedwarfare.unit.OrderableUnit;
import rustedwarfare.unit.Unit;
import rustedwarfare.unit.action.UnitActionId;
import rustedwarfare.unit.status.MovementSpeedStatusEffect;
import rustedwarfare.unit.status.SpecialActionBlockStatusEffect;
import rustedwarfare.unit.status.UnitStatusEffect;
import rustedwarfare.unit.status.UnitStatusEffectManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Query and mutation helpers for native unit status effects.
 *
 * <p>Mutations must run on the game update thread and, in multiplayer, from deterministic logic
 * shared by every peer.</p>
 */
public final class StatusEffects {
    private StatusEffects() {
    }

    public static List<UnitStatusEffect> active(OrderableUnit unit) {
        Objects.requireNonNull(unit, "unit");
        if (unit.activeStatusEffects == null || unit.activeStatusEffects.isEmpty()) {
            return Collections.emptyList();
        }
        List<UnitStatusEffect> result = new ArrayList<UnitStatusEffect>(unit.activeStatusEffects.size());
        for (Object value : unit.activeStatusEffects) {
            if (value instanceof UnitStatusEffect) result.add((UnitStatusEffect) value);
        }
        return Collections.unmodifiableList(result);
    }

    public static List<StatusEffectSnapshot> snapshot(OrderableUnit unit) {
        List<UnitStatusEffect> active = active(unit);
        List<StatusEffectSnapshot> result = new ArrayList<StatusEffectSnapshot>(active.size());
        for (UnitStatusEffect effect : active) result.add(StatusEffectSnapshot.capture(effect));
        return Collections.unmodifiableList(result);
    }

    public static int count(OrderableUnit unit) {
        return active(unit).size();
    }

    public static boolean contains(OrderableUnit unit, UnitStatusEffect effect) {
        Objects.requireNonNull(unit, "unit");
        return effect != null && unit.activeStatusEffects != null
                && unit.activeStatusEffects.contains(effect);
    }

    public static StatusEffectKind kindOf(UnitStatusEffect effect) {
        Objects.requireNonNull(effect, "effect");
        if (effect instanceof MovementSpeedStatusEffect) return StatusEffectKind.MOVEMENT_SPEED;
        if (effect instanceof SpecialActionBlockStatusEffect) return StatusEffectKind.ACTION_BLOCK;
        return StatusEffectKind.UNKNOWN;
    }

    public static boolean add(OrderableUnit unit, UnitStatusEffect effect) {
        Objects.requireNonNull(unit, "unit");
        Objects.requireNonNull(effect, "effect");
        boolean alreadyPresent = contains(unit, effect);
        UnitStatusEffectManager.addStatusEffect(unit, effect);
        return !alreadyPresent && contains(unit, effect);
    }

    public static MovementSpeedStatusEffect addMovementSpeed(
            OrderableUnit unit, float multiplier, int durationTime) {
        if (!Float.isFinite(multiplier) || multiplier < 0.0F) {
            throw new IllegalArgumentException("multiplier must be finite and non-negative");
        }
        int expiresAt = expirationTime(durationTime);
        MovementSpeedStatusEffect effect = new MovementSpeedStatusEffect();
        RustedReflection.setFieldValue(effect, new String[]{"expireFrame", "a"},
                Integer.valueOf(expiresAt));
        RustedReflection.setFieldValue(effect, new String[]{"speedMultiplier", "b"},
                Float.valueOf(multiplier));
        add(unit, effect);
        return effect;
    }

    public static SpecialActionBlockStatusEffect blockAction(
            OrderableUnit unit, UnitActionId actionId, int durationTime) {
        Objects.requireNonNull(unit, "unit");
        Objects.requireNonNull(actionId, "actionId");
        SpecialActionBlockStatusEffect effect = new SpecialActionBlockStatusEffect(
                expirationTime(durationTime), actionId);
        add(unit, effect);
        return effect;
    }

    public static int remainingActionBlockTime(Unit unit, UnitActionId actionId) {
        return Math.max(0, SpecialActionBlockStatusEffect.getRemainingBlockFrames(
                Objects.requireNonNull(unit, "unit"), Objects.requireNonNull(actionId, "actionId")));
    }

    public static SpecialActionBlockStatusEffect findActionBlock(Unit unit, UnitActionId actionId) {
        return SpecialActionBlockStatusEffect.findSpecialActionBlock(
                Objects.requireNonNull(unit, "unit"), Objects.requireNonNull(actionId, "actionId"));
    }

    public static void expireActionBlocks(Unit unit, UnitActionId actionId) {
        SpecialActionBlockStatusEffect.expireSpecialActionBlocks(
                Objects.requireNonNull(unit, "unit"), Objects.requireNonNull(actionId, "actionId"));
    }

    public static boolean remove(OrderableUnit unit, UnitStatusEffect effect) {
        Objects.requireNonNull(unit, "unit");
        Objects.requireNonNull(effect, "effect");
        if (unit.activeStatusEffects == null || !unit.activeStatusEffects.contains(effect)) return false;
        if (StatusEffectEvents.BEFORE_REMOVE.invoker().beforeRemove(unit, effect)) return false;
        boolean removed = unit.activeStatusEffects.remove(effect);
        if (removed) StatusEffectEvents.AFTER_REMOVE.invoker().afterRemove(unit, effect);
        return removed;
    }

    public static int clear(OrderableUnit unit) {
        int removed = 0;
        for (UnitStatusEffect effect : active(unit)) {
            if (remove(unit, effect)) removed++;
        }
        return removed;
    }

    private static int expirationTime(int durationTime) {
        if (durationTime <= 0) throw new IllegalArgumentException("durationTime must be positive");
        GameEngine engine = RustedWarfareClient.requireEngine();
        long expiresAt = (long) engine.gameTimeMillis + durationTime;
        if (expiresAt > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return (int) expiresAt;
    }
}
