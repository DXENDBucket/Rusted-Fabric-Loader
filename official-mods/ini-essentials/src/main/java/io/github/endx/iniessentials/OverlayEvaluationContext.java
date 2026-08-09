package io.github.endx.iniessentials;

import io.github.endx.rustedfabricapi.api.logic.LogicNumberFunctionDefinition;
import io.github.endx.rustedfabricapi.api.logic.LogicNumberFunctions;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/** Frame-local values available while an overlay expression or dynamic string is evaluated. */
final class OverlayEvaluationContext {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();
    private static final ThreadLocal<State> CURRENT = new ThreadLocal<State>();

    private OverlayEvaluationContext() { }

    static void registerFunctions() {
        if (!REGISTERED.compareAndSet(false, true)) return;
        function("overlayindex", state -> state.index);
        function("overlaystableindex", state -> state.stableIndex);
        function("overlaycount", state -> state.count);
        function("overlayrow", state -> state.row);
        function("overlaycolumn", state -> state.column);
        function("overlayslot", state -> state.slot);
        function("overlayunitid", state -> state.unitId);
        function("screenwidth", state -> state.screenWidth);
        function("screenheight", state -> state.screenHeight);
        function("uiscale", state -> state.uiScale);
    }

    static <T> T with(State state, Supplier<T> evaluation) {
        State previous = CURRENT.get();
        CURRENT.set(state);
        try {
            return evaluation.get();
        } finally {
            if (previous != null) CURRENT.set(previous);
            else CURRENT.remove();
        }
    }

    static void with(State state, Runnable evaluation) {
        with(state, () -> {
            evaluation.run();
            return null;
        });
    }

    private static void function(String name, Value value) {
        LogicNumberFunctions.register(LogicNumberFunctionDefinition.of(name, 0, arguments -> {
            State state = CURRENT.get();
            return state != null ? value.read(state) : 0.0F;
        }));
    }

    @FunctionalInterface
    private interface Value {
        float read(State state);
    }

    static final class State {
        final float index;
        final float stableIndex;
        final float count;
        final float row;
        final float column;
        final float slot;
        final float unitId;
        final float screenWidth;
        final float screenHeight;
        final float uiScale;

        State(int index, int stableIndex, int count, int row, int column, int slot,
              long unitId, float screenWidth, float screenHeight, float uiScale) {
            this.index = index;
            this.stableIndex = stableIndex;
            this.count = count;
            this.row = row;
            this.column = column;
            this.slot = slot;
            this.unitId = unitId;
            this.screenWidth = screenWidth;
            this.screenHeight = screenHeight;
            this.uiScale = uiScale;
        }
    }
}
