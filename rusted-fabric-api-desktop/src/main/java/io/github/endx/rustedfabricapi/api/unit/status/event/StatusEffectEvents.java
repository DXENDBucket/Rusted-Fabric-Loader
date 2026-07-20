package io.github.endx.rustedfabricapi.api.unit.status.event;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import rustedwarfare.unit.OrderableUnit;
import rustedwarfare.unit.status.UnitStatusEffect;

import java.util.List;

/** Strongly typed native status-effect lifecycle events. */
public final class StatusEffectEvents {
    public static final RustedFabricEvent<BeforeAdd> BEFORE_ADD =
            RustedFabricEvent.create(listeners -> (unit, effect) -> {
                boolean cancelled = false;
                for (BeforeAdd listener : listeners) cancelled |= listener.beforeAdd(unit, effect);
                return cancelled;
            });
    public static final RustedFabricEvent<AfterAdd> AFTER_ADD =
            RustedFabricEvent.create(listeners -> (unit, effect, added) -> {
                for (AfterAdd listener : listeners) listener.afterAdd(unit, effect, added);
            });
    public static final RustedFabricEvent<BeforeRemove> BEFORE_REMOVE =
            RustedFabricEvent.create(listeners -> (unit, effect) -> {
                boolean cancelled = false;
                for (BeforeRemove listener : listeners) cancelled |= listener.beforeRemove(unit, effect);
                return cancelled;
            });
    public static final RustedFabricEvent<AfterRemove> AFTER_REMOVE =
            RustedFabricEvent.create(listeners -> (unit, effect) -> {
                for (AfterRemove listener : listeners) listener.afterRemove(unit, effect);
            });
    public static final RustedFabricEvent<BeforeUpdate> BEFORE_UPDATE =
            RustedFabricEvent.create(listeners -> (unit, delta, effects) -> {
                for (BeforeUpdate listener : listeners) listener.beforeUpdate(unit, delta, effects);
            });
    public static final RustedFabricEvent<AfterUpdate> AFTER_UPDATE =
            RustedFabricEvent.create(listeners -> (unit, delta, effects) -> {
                for (AfterUpdate listener : listeners) listener.afterUpdate(unit, delta, effects);
            });
    public static final RustedFabricEvent<Expired> EXPIRED =
            RustedFabricEvent.create(listeners -> (unit, effect) -> {
                for (Expired listener : listeners) listener.onExpired(unit, effect);
            });

    private StatusEffectEvents() {
    }

    @FunctionalInterface
    public interface BeforeAdd {
        boolean beforeAdd(OrderableUnit unit, UnitStatusEffect effect);
    }

    @FunctionalInterface
    public interface AfterAdd {
        void afterAdd(OrderableUnit unit, UnitStatusEffect effect, boolean added);
    }

    @FunctionalInterface
    public interface BeforeRemove {
        boolean beforeRemove(OrderableUnit unit, UnitStatusEffect effect);
    }

    @FunctionalInterface
    public interface AfterRemove {
        void afterRemove(OrderableUnit unit, UnitStatusEffect effect);
    }

    @FunctionalInterface
    public interface BeforeUpdate {
        void beforeUpdate(OrderableUnit unit, float delta, List<UnitStatusEffect> effects);
    }

    @FunctionalInterface
    public interface AfterUpdate {
        void afterUpdate(OrderableUnit unit, float delta, List<UnitStatusEffect> effects);
    }

    @FunctionalInterface
    public interface Expired {
        void onExpired(OrderableUnit unit, UnitStatusEffect effect);
    }
}
