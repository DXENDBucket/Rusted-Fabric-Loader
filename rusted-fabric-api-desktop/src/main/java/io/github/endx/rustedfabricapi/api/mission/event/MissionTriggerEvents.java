package io.github.endx.rustedfabricapi.api.mission.event;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import rustedwarfare.mission.MissionEngine;
import rustedwarfare.mission.MissionTrigger;

/** Typed activation boundary for parsed map mission triggers. */
public final class MissionTriggerEvents {
    public static final RustedFabricEvent<BeforeActivate> BEFORE_ACTIVATE =
            RustedFabricEvent.create(listeners -> (engine, trigger) -> {
                boolean cancelled = false;
                for (BeforeActivate listener : listeners) {
                    cancelled |= listener.beforeActivate(engine, trigger);
                }
                return cancelled;
            });
    public static final RustedFabricEvent<AfterActivate> AFTER_ACTIVATE =
            RustedFabricEvent.create(listeners -> (engine, trigger) -> {
                for (AfterActivate listener : listeners) listener.afterActivate(engine, trigger);
            });

    private MissionTriggerEvents() {
    }

    @FunctionalInterface
    public interface BeforeActivate {
        boolean beforeActivate(MissionEngine engine, MissionTrigger trigger);
    }

    @FunctionalInterface
    public interface AfterActivate {
        void afterActivate(MissionEngine engine, MissionTrigger trigger);
    }
}
