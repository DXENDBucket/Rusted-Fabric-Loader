package io.github.endx.rustedfabricapi.api.event;

public final class UnitDamageEvents {
    private UnitDamageEvents() {
    }

    public static final RustedFabricEvent<BeforeUnitApplyDamage> BEFORE_UNIT_APPLY_DAMAGE =
            RustedFabricEvent.create(listeners -> (unit, attacker, amount, projectile) -> {
                boolean cancelled = false;
                for (BeforeUnitApplyDamage listener : listeners) {
                    cancelled |= listener.beforeUnitApplyDamage(unit, attacker, amount, projectile);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterUnitApplyDamage> AFTER_UNIT_APPLY_DAMAGE =
            RustedFabricEvent.create(listeners -> (unit, attacker, amount, projectile, appliedAmount) -> {
                for (AfterUnitApplyDamage listener : listeners) {
                    listener.afterUnitApplyDamage(unit, attacker, amount, projectile, appliedAmount);
                }
            });

    public static final RustedFabricEvent<ModifyUnitDamageImmunity> MODIFY_UNIT_DAMAGE_IMMUNITY =
            RustedFabricEvent.create(listeners -> (unit, vanillaResult) -> {
                Boolean result = Boolean.valueOf(vanillaResult);
                for (ModifyUnitDamageImmunity listener : listeners) {
                    Boolean override = listener.modifyUnitDamageImmunity(unit, result.booleanValue());
                    if (override != null) {
                        result = override;
                    }
                }
                return result;
            });

    public static final RustedFabricEvent<BeforeUnitDeathSequence> BEFORE_UNIT_DEATH_SEQUENCE =
            RustedFabricEvent.create(listeners -> unit -> {
                boolean cancelled = false;
                for (BeforeUnitDeathSequence listener : listeners) {
                    cancelled |= listener.beforeUnitDeathSequence(unit);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterUnitDeathSequence> AFTER_UNIT_DEATH_SEQUENCE =
            RustedFabricEvent.create(listeners -> unit -> {
                for (AfterUnitDeathSequence listener : listeners) {
                    listener.afterUnitDeathSequence(unit);
                }
            });

    public static final RustedFabricEvent<ModifyUnitDeathEffectsResult> MODIFY_UNIT_DEATH_EFFECTS_RESULT =
            RustedFabricEvent.create(listeners -> (unit, vanillaKeepObject) -> {
                Boolean result = Boolean.valueOf(vanillaKeepObject);
                for (ModifyUnitDeathEffectsResult listener : listeners) {
                    Boolean override = listener.modifyUnitDeathEffectsResult(unit, result.booleanValue());
                    if (override != null) {
                        result = override;
                    }
                }
                return result;
            });

    @FunctionalInterface
    public interface BeforeUnitApplyDamage {
        boolean beforeUnitApplyDamage(Object unit, Object attacker, float amount, Object projectile);
    }

    @FunctionalInterface
    public interface AfterUnitApplyDamage {
        void afterUnitApplyDamage(Object unit, Object attacker, float amount, Object projectile, float appliedAmount);
    }

    @FunctionalInterface
    public interface ModifyUnitDamageImmunity {
        Boolean modifyUnitDamageImmunity(Object unit, boolean currentResult);
    }

    @FunctionalInterface
    public interface BeforeUnitDeathSequence {
        boolean beforeUnitDeathSequence(Object unit);
    }

    @FunctionalInterface
    public interface AfterUnitDeathSequence {
        void afterUnitDeathSequence(Object unit);
    }

    @FunctionalInterface
    public interface ModifyUnitDeathEffectsResult {
        Boolean modifyUnitDeathEffectsResult(Object unit, boolean currentKeepObject);
    }
}
