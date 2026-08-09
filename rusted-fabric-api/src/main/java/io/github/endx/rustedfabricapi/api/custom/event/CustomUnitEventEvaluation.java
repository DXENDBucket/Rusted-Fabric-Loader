package io.github.endx.rustedfabricapi.api.custom.event;

import io.github.endx.rustedfabricapi.impl.custom.CustomUnitEventEvaluationRuntime;
import rustedwarfare.custom.CustomTagList;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.custom.logic.VariableScope;
import rustedwarfare.unit.Unit;

import java.util.Objects;
import java.util.function.Supplier;

/** Evaluates LogicBoolean expressions with a temporary native {@code eventData(...)} context. */
public final class CustomUnitEventEvaluation {
    private CustomUnitEventEvaluation() { }

    public static <T> T withContext(CustomUnit actor, Unit source, CustomTagList tags,
                                    VariableScope data, Supplier<T> evaluation) {
        return CustomUnitEventEvaluationRuntime.withContext(
                Objects.requireNonNull(actor, "actor"), source, tags,
                Objects.requireNonNull(data, "data"),
                Objects.requireNonNull(evaluation, "evaluation"));
    }
}
