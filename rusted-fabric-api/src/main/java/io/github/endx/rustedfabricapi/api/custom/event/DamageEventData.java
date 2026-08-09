package io.github.endx.rustedfabricapi.api.custom.event;

import io.github.endx.rustedfabricapi.impl.custom.DamageEventDataRuntime;

import java.util.Set;

/** Extra values supplied to the native {@code tookDamage} action event. */
public final class DamageEventData {
    public static final String DAMAGE = "damage";
    public static final String RAW_DAMAGE = "rawDamage";
    public static final String HP_DAMAGE = "hpDamage";
    public static final String SHIELD_DAMAGE = "shieldDamage";
    public static final String REMAINING_DAMAGE = "remainingDamage";
    public static final String HP_BEFORE = "hpBefore";
    public static final String HP_AFTER = "hpAfter";
    public static final String SHIELD_BEFORE = "shieldBefore";
    public static final String SHIELD_AFTER = "shieldAfter";
    public static final String WAS_LETHAL = "wasLethal";

    private DamageEventData() { }

    /**
     * Enables the enhanced native event context.
     *
     * @param onFieldUsed invoked when an INI parses one of the enhanced event-data names; callers
     *                    can use this to activate their multiplayer compatibility requirement
     */
    public static Registration enable(Runnable onFieldUsed) {
        return new Registration(DamageEventDataRuntime.enable(onFieldUsed));
    }

    public static Set<String> fieldNames() {
        return DamageEventDataRuntime.fieldNames();
    }

    public static final class Registration implements AutoCloseable {
        private final AutoCloseable delegate;
        private boolean closed;

        private Registration(AutoCloseable delegate) { this.delegate = delegate; }

        @Override
        public synchronized void close() {
            if (closed) return;
            closed = true;
            try {
                delegate.close();
            } catch (RuntimeException failure) {
                throw failure;
            } catch (Exception failure) {
                throw new IllegalStateException("Failed to disable damage event data", failure);
            }
        }
    }
}
