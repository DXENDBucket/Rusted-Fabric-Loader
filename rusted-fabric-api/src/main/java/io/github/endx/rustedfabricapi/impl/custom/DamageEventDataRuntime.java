package io.github.endx.rustedfabricapi.impl.custom;

import io.github.endx.rustedfabricapi.api.custom.event.CustomUnitEventData;
import io.github.endx.rustedfabricapi.api.custom.event.CustomUnitTriggerEvents;
import io.github.endx.rustedfabricapi.api.custom.event.DamageEventData;
import io.github.endx.rustedfabricapi.api.unit.event.UnitDamageResult;
import rustedwarfare.custom.CustomTagList;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.custom.event.CustomUnitEventType;
import rustedwarfare.custom.logic.VariableScope;
import rustedwarfare.game.Projectile;
import rustedwarfare.unit.Unit;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/** Internal lifecycle bridge between native damage and queued custom-unit event contexts. */
public final class DamageEventDataRuntime {
    private static final Set<String> FIELD_NAMES = Collections.unmodifiableSet(
            new LinkedHashSet<String>(Arrays.asList(
                    DamageEventData.DAMAGE, DamageEventData.RAW_DAMAGE,
                    DamageEventData.HP_DAMAGE, DamageEventData.SHIELD_DAMAGE,
                    DamageEventData.REMAINING_DAMAGE, DamageEventData.HP_BEFORE,
                    DamageEventData.HP_AFTER, DamageEventData.SHIELD_BEFORE,
                    DamageEventData.SHIELD_AFTER, DamageEventData.WAS_LETHAL)));
    private static final Set<String> NORMALIZED_FIELD_NAMES;
    private static final CopyOnWriteArrayList<Runnable> USAGE_CALLBACKS =
            new CopyOnWriteArrayList<Runnable>();
    private static final ThreadLocal<Deque<CustomDamageFrame>> CUSTOM_DAMAGE =
            ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Deque<NativeDamageFrame>> NATIVE_DAMAGE =
            ThreadLocal.withInitial(ArrayDeque::new);

    static {
        LinkedHashSet<String> normalized = new LinkedHashSet<String>();
        for (String name : FIELD_NAMES) normalized.add(normalize(name));
        NORMALIZED_FIELD_NAMES = Collections.unmodifiableSet(normalized);
    }

    private DamageEventDataRuntime() { }

    public static AutoCloseable enable(Runnable callback) {
        Runnable value = Objects.requireNonNull(callback, "callback");
        USAGE_CALLBACKS.add(value);
        return () -> USAGE_CALLBACKS.remove(value);
    }

    public static Set<String> fieldNames() { return FIELD_NAMES; }

    public static void onEventDataNameParsed(String name) {
        if (!NORMALIZED_FIELD_NAMES.contains(normalize(name))) return;
        for (Runnable callback : USAGE_CALLBACKS) callback.run();
    }

    public static void beginCustomDamage(CustomUnit unit, Unit attacker,
                                         float rawDamage, Projectile projectile) {
        if (USAGE_CALLBACKS.isEmpty()) return;
        CUSTOM_DAMAGE.get().push(new CustomDamageFrame(unit, attacker, rawDamage, projectile));
    }

    public static void endCustomDamage(CustomUnit unit) {
        Deque<CustomDamageFrame> frames = CUSTOM_DAMAGE.get();
        if (!frames.isEmpty() && frames.peek().unit == unit) {
            frames.pop();
        } else {
            for (Iterator<CustomDamageFrame> iterator = frames.iterator(); iterator.hasNext(); ) {
                if (iterator.next().unit == unit) {
                    iterator.remove();
                    break;
                }
            }
        }
        if (frames.isEmpty()) CUSTOM_DAMAGE.remove();
    }

    public static VariableScope enrichQueuedEvent(CustomUnit unit,
                                                   CustomUnitEventType eventType,
                                                   Unit source, CustomTagList eventTags,
                                                   VariableScope original) {
        VariableScope result = original;
        CustomDamageFrame frame = findCustomFrame(unit);
        if (!USAGE_CALLBACKS.isEmpty()
                && eventType == CustomUnitEventType.TOOK_DAMAGE && frame != null) {
            if (result == null) result = new VariableScope();
            frame.eventData = result;
            CustomUnitEventData.wrap(result)
                    .putNumber(DamageEventData.RAW_DAMAGE, frame.rawDamage);
        }
        if (CustomUnitTriggerEvents.ENRICH_EVENT_DATA.listenerCount() != 0) {
            if (result == null) result = new VariableScope();
            CustomUnitTriggerEvents.ENRICH_EVENT_DATA.invoker().enrich(
                    unit, eventType, source, eventTags, CustomUnitEventData.wrap(result));
        }
        return result;
    }

    public static void beginNativeDamage(Unit unit, Unit attacker,
                                         float requestedDamage, Projectile projectile) {
        NativeDamageFrame nativeFrame = new NativeDamageFrame(unit, attacker, projectile,
                requestedDamage, unit.hp, unit.shield);
        NATIVE_DAMAGE.get().push(nativeFrame);
        CustomDamageFrame customFrame = findCustomFrame(unit);
        if (customFrame != null && customFrame.eventData != null) {
            customFrame.nativeFrame = nativeFrame;
            CustomUnitEventData.wrap(customFrame.eventData)
                    .putNumber(DamageEventData.DAMAGE, requestedDamage)
                    .putNumber(DamageEventData.HP_BEFORE, nativeFrame.hpBefore)
                    .putNumber(DamageEventData.SHIELD_BEFORE, nativeFrame.shieldBefore);
        }
    }

    public static UnitDamageResult endNativeDamage(Unit unit, float nativeRemainingDamage) {
        NativeDamageFrame frame = popNativeFrame(unit);
        UnitDamageResult result = new UnitDamageResult(unit, frame.attacker, frame.projectile,
                frame.requestedDamage, nativeRemainingDamage,
                frame.hpBefore, unit.hp, frame.shieldBefore, unit.shield);
        CustomDamageFrame customFrame = findCustomFrame(unit);
        if (customFrame != null && customFrame.nativeFrame == frame
                && customFrame.eventData != null) {
            CustomUnitEventData.wrap(customFrame.eventData)
                    .putNumber(DamageEventData.DAMAGE, result.requestedDamage())
                    .putNumber(DamageEventData.HP_DAMAGE, result.hpDamage())
                    .putNumber(DamageEventData.SHIELD_DAMAGE, result.shieldDamage())
                    .putNumber(DamageEventData.REMAINING_DAMAGE,
                            result.nativeRemainingDamage())
                    .putNumber(DamageEventData.HP_BEFORE, result.hpBefore())
                    .putNumber(DamageEventData.HP_AFTER, result.hpAfter())
                    .putNumber(DamageEventData.SHIELD_BEFORE, result.shieldBefore())
                    .putNumber(DamageEventData.SHIELD_AFTER, result.shieldAfter())
                    .putBoolean(DamageEventData.WAS_LETHAL, result.wasLethal());
        }
        return result;
    }

    private static CustomDamageFrame findCustomFrame(Unit unit) {
        for (CustomDamageFrame frame : CUSTOM_DAMAGE.get()) {
            if (frame.unit == unit) return frame;
        }
        return null;
    }

    private static NativeDamageFrame popNativeFrame(Unit unit) {
        Deque<NativeDamageFrame> frames = NATIVE_DAMAGE.get();
        NativeDamageFrame result = null;
        if (!frames.isEmpty() && frames.peek().unit == unit) {
            result = frames.pop();
        } else {
            for (Iterator<NativeDamageFrame> iterator = frames.iterator(); iterator.hasNext(); ) {
                NativeDamageFrame frame = iterator.next();
                if (frame.unit == unit) {
                    result = frame;
                    iterator.remove();
                    break;
                }
            }
        }
        if (frames.isEmpty()) NATIVE_DAMAGE.remove();
        if (result == null) {
            throw new IllegalStateException("Missing native damage frame for " + unit);
        }
        return result;
    }

    private static String normalize(String name) {
        return Objects.requireNonNull(name, "name").trim().toLowerCase(Locale.ROOT);
    }

    private static final class CustomDamageFrame {
        private final CustomUnit unit;
        @SuppressWarnings("unused") private final Unit attacker;
        private final float rawDamage;
        @SuppressWarnings("unused") private final Projectile projectile;
        private VariableScope eventData;
        private NativeDamageFrame nativeFrame;

        private CustomDamageFrame(CustomUnit unit, Unit attacker,
                                  float rawDamage, Projectile projectile) {
            this.unit = unit;
            this.attacker = attacker;
            this.rawDamage = rawDamage;
            this.projectile = projectile;
        }
    }

    private static final class NativeDamageFrame {
        private final Unit unit;
        private final Unit attacker;
        private final Projectile projectile;
        private final float requestedDamage;
        private final float hpBefore;
        private final float shieldBefore;

        private NativeDamageFrame(Unit unit, Unit attacker, Projectile projectile,
                                  float requestedDamage, float hpBefore, float shieldBefore) {
            this.unit = unit;
            this.attacker = attacker;
            this.projectile = projectile;
            this.requestedDamage = requestedDamage;
            this.hpBefore = hpBefore;
            this.shieldBefore = shieldBefore;
        }
    }
}
