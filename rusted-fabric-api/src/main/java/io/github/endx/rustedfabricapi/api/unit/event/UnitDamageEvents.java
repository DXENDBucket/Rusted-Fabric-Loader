package io.github.endx.rustedfabricapi.api.unit.event;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import rustedwarfare.game.Projectile;
import rustedwarfare.unit.Unit;

/** Strongly typed damage and death boundaries for desktop units. */
public final class UnitDamageEvents {
    public static final RustedFabricEvent<BeforeDamage> BEFORE_DAMAGE =
            RustedFabricEvent.create(listeners -> (unit, attacker, amount, projectile) -> {
                boolean cancelled = false;
                for (BeforeDamage listener : listeners) {
                    cancelled |= listener.beforeDamage(unit, attacker, amount, projectile);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterDamage> AFTER_DAMAGE =
            RustedFabricEvent.create(listeners -> (unit, attacker, amount, projectile, remaining) -> {
                for (AfterDamage listener : listeners) {
                    listener.afterDamage(unit, attacker, amount, projectile, remaining);
                }
            });

    /** Complete before/after values for one finished native damage application. */
    public static final RustedFabricEvent<AfterDamageResult> AFTER_DAMAGE_RESULT =
            RustedFabricEvent.create(listeners -> result -> {
                for (AfterDamageResult listener : listeners) listener.afterDamage(result);
            });

    /**
     * Modifies the health value at the native lethal-damage clamp.
     *
     * <p>The event is only invoked when the game would call {@code setHp(0)} because hull damage
     * exceeded current HP. Returning {@code null} preserves the current value in the listener
     * chain. With no replacement, native zero-clamping remains unchanged.</p>
     */
    public static final RustedFabricEvent<ModifyLethalHealth> MODIFY_LETHAL_HEALTH =
            RustedFabricEvent.create(listeners -> (unit, attacker, requestedAmount, projectile,
                                                    nativeValue, unclampedValue, currentValue) -> {
                Float result = Float.valueOf(currentValue);
                for (ModifyLethalHealth listener : listeners) {
                    Float replacement = listener.modify(unit, attacker, requestedAmount, projectile,
                            nativeValue, unclampedValue, result.floatValue());
                    if (replacement != null) result = replacement;
                }
                return result;
            });

    public static final RustedFabricEvent<ModifyBoolean> MODIFY_DAMAGE_IMMUNITY = modifyEvent();
    public static final RustedFabricEvent<BeforeDeath> BEFORE_DEATH =
            RustedFabricEvent.create(listeners -> unit -> {
                boolean cancelled = false;
                for (BeforeDeath listener : listeners) {
                    cancelled |= listener.beforeDeath(unit);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterDeath> AFTER_DEATH =
            RustedFabricEvent.create(listeners -> unit -> {
                for (AfterDeath listener : listeners) {
                    listener.afterDeath(unit);
                }
            });

    public static final RustedFabricEvent<ModifyBoolean> MODIFY_KEEP_OBJECT_AFTER_DEATH = modifyEvent();

    private UnitDamageEvents() {
    }

    private static RustedFabricEvent<ModifyBoolean> modifyEvent() {
        return RustedFabricEvent.create(listeners -> (unit, currentValue) -> {
            Boolean result = Boolean.valueOf(currentValue);
            for (ModifyBoolean listener : listeners) {
                Boolean replacement = listener.modify(unit, result.booleanValue());
                if (replacement != null) {
                    result = replacement;
                }
            }
            return result;
        });
    }

    @FunctionalInterface
    public interface BeforeDamage {
        /** Return {@code true} to cancel the damage and make the game report zero applied damage. */
        boolean beforeDamage(Unit unit, Unit attacker, float amount, Projectile projectile);
    }

    @FunctionalInterface
    public interface AfterDamage {
        /**
         * @param nativeRemainingAmount the native return value: damage left after shield/hull
         *                              processing, not the amount applied to HP
         */
        void afterDamage(Unit unit, Unit attacker, float requestedAmount,
                         Projectile projectile, float nativeRemainingAmount);
    }

    @FunctionalInterface
    public interface AfterDamageResult {
        void afterDamage(UnitDamageResult result);
    }

    @FunctionalInterface
    public interface ModifyLethalHealth {
        /**
         * @param nativeValue the game's normal clamped value (currently zero)
         * @param unclampedValue the HP produced by applying the same native damage math without
         *                       its zero floor
         * @param currentValue the result from earlier listeners, initially {@code nativeValue}
         * @return a replacement health value, or {@code null} to keep {@code currentValue}
         */
        Float modify(Unit unit, Unit attacker, float requestedAmount, Projectile projectile,
                     float nativeValue, float unclampedValue, float currentValue);
    }

    @FunctionalInterface
    public interface ModifyBoolean {
        /** Return {@code null} to retain {@code currentValue}. */
        Boolean modify(Unit unit, boolean currentValue);
    }

    @FunctionalInterface
    public interface BeforeDeath {
        /** Return {@code true} to cancel the death sequence. */
        boolean beforeDeath(Unit unit);
    }

    @FunctionalInterface
    public interface AfterDeath {
        void afterDeath(Unit unit);
    }
}
