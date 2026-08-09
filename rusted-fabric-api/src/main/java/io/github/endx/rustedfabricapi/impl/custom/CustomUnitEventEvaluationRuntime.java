package io.github.endx.rustedfabricapi.impl.custom;

import io.github.endx.rustedfabricapi.mixin.accessor.LogicBooleanAccessor;
import rustedwarfare.custom.CustomTagList;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.custom.logic.LogicBoolean;
import rustedwarfare.custom.logic.LogicEventContext;
import rustedwarfare.custom.logic.VariableScope;
import rustedwarfare.unit.Unit;

import java.util.function.Supplier;

/** Internal save/restore bridge for native LogicBoolean event context. */
public final class CustomUnitEventEvaluationRuntime {
    private CustomUnitEventEvaluationRuntime() { }

    public static <T> T withContext(CustomUnit actor, Unit source, CustomTagList tags,
                                    VariableScope data, Supplier<T> evaluation) {
        LogicEventContext previous = LogicBooleanAccessor.rustedfabricapi$getActiveEvent();
        LogicEventContext temporary = new LogicEventContext();
        temporary.sourceCustomUnit = actor;
        temporary.eventSourceUnit = source;
        temporary.eventTags = tags;
        temporary.eventData = data;
        LogicBoolean.setContextEventSource(temporary);
        try {
            return evaluation.get();
        } finally {
            if (previous != null) LogicBoolean.setContextEventSource(previous);
            else LogicBoolean.clearContext();
        }
    }
}
