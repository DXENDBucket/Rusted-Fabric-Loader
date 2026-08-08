package io.github.endx.rustedfabricapi.api.unit.attribute;

import io.github.endx.rustedfabricapi.api.unit.attribute.event.UnitStatEvents;
import io.github.endx.rustedfabricapi.api.util.Identifier;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.custom.MutableStatAccessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.WeakHashMap;

/** Typed access to native custom-unit mutable stats and composable runtime modifiers. */
public final class CustomUnitStats {
    private static final Map<CustomUnit, UnitState> STATES = new WeakHashMap<CustomUnit, UnitState>();
    private static final ThreadLocal<List<NativeWrite>> NATIVE_WRITES =
            ThreadLocal.withInitial(ArrayList::new);
    private static boolean serializationSuspended;

    private CustomUnitStats() {
    }

    public static double get(CustomUnit unit, UnitStat stat) {
        Objects.requireNonNull(unit, "unit");
        Objects.requireNonNull(stat, "stat");
        return stat.accessor().read(unit, unit.mutableStats);
    }

    public static UnitStatSnapshot snapshot(CustomUnit unit) {
        Objects.requireNonNull(unit, "unit");
        EnumMap<UnitStat, Double> values = new EnumMap<UnitStat, Double>(UnitStat.class);
        for (UnitStat stat : UnitStat.values()) values.put(stat, get(unit, stat));
        return new UnitStatSnapshot(unit, values);
    }

    /**
     * Sets a runtime value, or the baseline beneath active modifiers for a metadata stat.
     * The mapped native accessor performs the actual write and its normal side effects.
     */
    public static double set(CustomUnit unit, UnitStat stat, double value) {
        Objects.requireNonNull(unit, "unit");
        Objects.requireNonNull(stat, "stat");
        double modified = UnitStatEvents.MODIFY_SET_VALUE.invoker().modify(unit, stat, value);
        modified = stat.normalize(modified);
        double oldEffective;
        double newEffective;
        synchronized (STATES) {
            oldEffective = get(unit, stat);
            StatState state = findState(unit, stat);
            if (state == null || stat.runtimeValue()) {
                stat.accessor().write(unit, modified);
            } else {
                state.baseline = modified;
                apply(unit, stat, state);
            }
            newEffective = get(unit, stat);
        }
        changed(unit, stat, oldEffective, newEffective, UnitStatChangeCause.API_SET);
        return newEffective;
    }

    public static double baseValue(CustomUnit unit, UnitStat stat) {
        Objects.requireNonNull(unit, "unit");
        Objects.requireNonNull(stat, "stat");
        synchronized (STATES) {
            StatState state = findState(unit, stat);
            return state != null ? state.baseline : get(unit, stat);
        }
    }

    public static void addModifier(CustomUnit unit, UnitStat stat, UnitStatModifier modifier) {
        requireModifiable(stat);
        Objects.requireNonNull(unit, "unit");
        Objects.requireNonNull(modifier, "modifier");
        double oldEffective;
        double newEffective;
        synchronized (STATES) {
            oldEffective = get(unit, stat);
            UnitState unitState = STATES.computeIfAbsent(unit, ignored -> new UnitState());
            StatState state = unitState.stats.get(stat);
            if (state == null) {
                state = new StatState(oldEffective);
                unitState.stats.put(stat, state);
            }
            state.modifiers.put(modifier.id(), modifier);
            apply(unit, stat, state);
            newEffective = get(unit, stat);
        }
        changed(unit, stat, oldEffective, newEffective, UnitStatChangeCause.MODIFIER_ADDED);
    }

    public static boolean removeModifier(CustomUnit unit, UnitStat stat, Identifier modifierId) {
        requireModifiable(stat);
        Objects.requireNonNull(unit, "unit");
        Objects.requireNonNull(modifierId, "modifierId");
        double oldEffective;
        double newEffective;
        synchronized (STATES) {
            StatState state = findState(unit, stat);
            if (state == null || state.modifiers.remove(modifierId) == null) return false;
            oldEffective = get(unit, stat);
            if (state.modifiers.isEmpty()) {
                stat.accessor().write(unit, stat.normalize(state.baseline));
                removeState(unit, stat);
            } else {
                apply(unit, stat, state);
            }
            newEffective = get(unit, stat);
        }
        changed(unit, stat, oldEffective, newEffective, UnitStatChangeCause.MODIFIER_REMOVED);
        return true;
    }

    public static void clearModifiers(CustomUnit unit, UnitStat stat) {
        requireModifiable(stat);
        Objects.requireNonNull(unit, "unit");
        double oldEffective;
        double newEffective;
        synchronized (STATES) {
            StatState state = findState(unit, stat);
            if (state == null) return;
            oldEffective = get(unit, stat);
            stat.accessor().write(unit, stat.normalize(state.baseline));
            removeState(unit, stat);
            newEffective = get(unit, stat);
        }
        changed(unit, stat, oldEffective, newEffective, UnitStatChangeCause.MODIFIERS_CLEARED);
    }

    public static List<UnitStatModifier> modifiers(CustomUnit unit, UnitStat stat) {
        Objects.requireNonNull(unit, "unit");
        Objects.requireNonNull(stat, "stat");
        synchronized (STATES) {
            StatState state = findState(unit, stat);
            if (state == null) return Collections.emptyList();
            return Collections.unmodifiableList(new ArrayList<UnitStatModifier>(state.modifiers.values()));
        }
    }

    /** Pure deterministic evaluation helper, also useful to preview a modifier set. */
    public static double evaluate(double baseline, Collection<UnitStatModifier> modifiers) {
        if (!Double.isFinite(baseline)) throw new IllegalArgumentException("baseline must be finite");
        Objects.requireNonNull(modifiers, "modifiers");
        List<UnitStatModifier> sorted = new ArrayList<UnitStatModifier>(modifiers);
        sorted.sort(Comparator.naturalOrder());
        double result = baseline;
        for (UnitStatModifier modifier : sorted) {
            if (modifier.operation() == UnitStatOperation.ADD_VALUE) result += modifier.amount();
        }
        for (UnitStatModifier modifier : sorted) {
            if (modifier.operation() == UnitStatOperation.ADD_MULTIPLIED_BASE) {
                result += baseline * modifier.amount();
            }
        }
        for (UnitStatModifier modifier : sorted) {
            if (modifier.operation() == UnitStatOperation.MULTIPLY_TOTAL) {
                result *= 1.0 + modifier.amount();
            }
        }
        if (!Double.isFinite(result)) throw new IllegalArgumentException("modifier result must be finite");
        return result;
    }

    /** Internal bridge used by the mapped native mutable-stat writer hook. */
    public static void beforeNativeWrite(CustomUnit unit, MutableStatAccessor accessor) {
        UnitStat stat = UnitStat.fromAccessor(accessor);
        if (stat == null) return;
        double oldEffective;
        synchronized (STATES) {
            oldEffective = get(unit, stat);
            StatState state = findState(unit, stat);
            if (state != null) accessor.write(unit, stat.normalize(state.baseline));
        }
        NATIVE_WRITES.get().add(new NativeWrite(unit, stat, oldEffective));
    }

    /** Internal bridge used by the mapped native mutable-stat writer hook. */
    public static void afterNativeWrite(CustomUnit unit, MutableStatAccessor accessor) {
        UnitStat stat = UnitStat.fromAccessor(accessor);
        if (stat == null) return;
        NativeWrite context = popNativeWrite(unit, stat);
        double oldEffective = context != null ? context.oldEffective : get(unit, stat);
        double newEffective;
        synchronized (STATES) {
            StatState state = findState(unit, stat);
            if (state != null) {
                state.baseline = get(unit, stat);
                apply(unit, stat, state);
            }
            newEffective = get(unit, stat);
        }
        changed(unit, stat, oldEffective, newEffective, UnitStatChangeCause.NATIVE_MUTATION);
    }

    /** Internal bridge: exposes unmodified baselines while native metadata is replaced. */
    public static void beforeMetadataApply(CustomUnit unit) {
        synchronized (STATES) {
            UnitState unitState = STATES.get(unit);
            if (unitState == null) return;
            for (Map.Entry<UnitStat, StatState> entry : unitState.stats.entrySet()) {
                entry.getKey().accessor().write(unit, entry.getKey().normalize(entry.getValue().baseline));
            }
        }
    }

    /** Internal bridge: rebases active modifiers on the newly applied metadata. */
    public static void afterMetadataApply(CustomUnit unit) {
        List<Change> changes = new ArrayList<Change>();
        synchronized (STATES) {
            UnitState unitState = STATES.get(unit);
            if (unitState == null) return;
            for (Map.Entry<UnitStat, StatState> entry : unitState.stats.entrySet()) {
                UnitStat stat = entry.getKey();
                StatState state = entry.getValue();
                double oldValue = get(unit, stat);
                state.baseline = oldValue;
                apply(unit, stat, state);
                changes.add(new Change(stat, oldValue, get(unit, stat)));
            }
        }
        for (Change change : changes) {
            changed(unit, change.stat, change.oldValue, change.newValue,
                    UnitStatChangeCause.METADATA_APPLY);
        }
    }

    /** Internal save bridge. Runtime modifiers themselves are intentionally not serialized. */
    public static void suspendForSerialization() {
        synchronized (STATES) {
            if (serializationSuspended) return;
            for (Map.Entry<CustomUnit, UnitState> unitEntry : STATES.entrySet()) {
                CustomUnit unit = unitEntry.getKey();
                if (unit == null) continue;
                for (Map.Entry<UnitStat, StatState> statEntry : unitEntry.getValue().stats.entrySet()) {
                    UnitStat stat = statEntry.getKey();
                    stat.accessor().write(unit, stat.normalize(statEntry.getValue().baseline));
                }
            }
            serializationSuspended = true;
        }
    }

    /** Internal save bridge. */
    public static void resumeAfterSerialization() {
        synchronized (STATES) {
            if (!serializationSuspended) return;
            for (Map.Entry<CustomUnit, UnitState> unitEntry : STATES.entrySet()) {
                CustomUnit unit = unitEntry.getKey();
                if (unit == null) continue;
                for (Map.Entry<UnitStat, StatState> statEntry : unitEntry.getValue().stats.entrySet()) {
                    apply(unit, statEntry.getKey(), statEntry.getValue());
                }
            }
            serializationSuspended = false;
        }
    }

    /** Clears modifier ownership without writing to units that are being discarded. */
    public static void clearRuntime() {
        synchronized (STATES) {
            STATES.clear();
            serializationSuspended = false;
        }
        NATIVE_WRITES.remove();
    }

    private static void apply(CustomUnit unit, UnitStat stat, StatState state) {
        double value = stat.normalize(evaluate(state.baseline, state.modifiers.values()));
        stat.accessor().write(unit, value);
    }

    private static StatState findState(CustomUnit unit, UnitStat stat) {
        UnitState unitState = STATES.get(unit);
        return unitState != null ? unitState.stats.get(stat) : null;
    }

    private static void removeState(CustomUnit unit, UnitStat stat) {
        UnitState unitState = STATES.get(unit);
        if (unitState == null) return;
        unitState.stats.remove(stat);
        if (unitState.stats.isEmpty()) STATES.remove(unit);
    }

    private static void requireModifiable(UnitStat stat) {
        Objects.requireNonNull(stat, "stat");
        if (!stat.supportsModifiers()) {
            throw new IllegalArgumentException("Runtime value does not support modifiers: " + stat);
        }
    }

    private static NativeWrite popNativeWrite(CustomUnit unit, UnitStat stat) {
        List<NativeWrite> writes = NATIVE_WRITES.get();
        for (int index = writes.size() - 1; index >= 0; index--) {
            NativeWrite write = writes.get(index);
            if (write.unit == unit && write.stat == stat) {
                writes.remove(index);
                if (writes.isEmpty()) NATIVE_WRITES.remove();
                return write;
            }
        }
        return null;
    }

    private static void changed(CustomUnit unit, UnitStat stat, double oldValue,
            double newValue, UnitStatChangeCause cause) {
        UnitStatEvents.AFTER_CHANGE.invoker().afterChange(
                unit, stat, oldValue, newValue, cause);
    }

    private static final class UnitState {
        private final EnumMap<UnitStat, StatState> stats =
                new EnumMap<UnitStat, StatState>(UnitStat.class);
    }

    private static final class StatState {
        private double baseline;
        private final TreeMap<Identifier, UnitStatModifier> modifiers =
                new TreeMap<Identifier, UnitStatModifier>();

        private StatState(double baseline) {
            this.baseline = baseline;
        }
    }

    private static final class NativeWrite {
        private final CustomUnit unit;
        private final UnitStat stat;
        private final double oldEffective;

        private NativeWrite(CustomUnit unit, UnitStat stat, double oldEffective) {
            this.unit = unit;
            this.stat = stat;
            this.oldEffective = oldEffective;
        }
    }

    private static final class Change {
        private final UnitStat stat;
        private final double oldValue;
        private final double newValue;

        private Change(UnitStat stat, double oldValue, double newValue) {
            this.stat = stat;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }
    }
}
