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
            RustedFabricEvent.create(listeners -> (unit, attacker, amount, projectile, applied) -> {
                for (AfterDamage listener : listeners) {
                    listener.afterDamage(unit, attacker, amount, projectile, applied);
                }
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
        void afterDamage(Unit unit, Unit attacker, float requestedAmount,
                         Projectile projectile, float appliedAmount);
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
