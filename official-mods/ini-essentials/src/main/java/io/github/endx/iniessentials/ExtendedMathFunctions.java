package io.github.endx.iniessentials;

import io.github.endx.rustedfabricapi.api.logic.LogicNumberFunctionDefinition;
import io.github.endx.rustedfabricapi.api.logic.LogicNumberFunctions;

import java.util.concurrent.atomic.AtomicBoolean;

final class ExtendedMathFunctions {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();

    private ExtendedMathFunctions() { }

    static void register() {
        if (!REGISTERED.compareAndSet(false, true)) return;
        function("pow", 2, a -> (float) StrictMath.pow(a[0], a[1]));
        function("exp", 1, a -> (float) StrictMath.exp(a[0]));
        function("ln", 1, a -> (float) StrictMath.log(a[0]));
        function("log10", 1, a -> (float) StrictMath.log10(a[0]));
        function("log", 2, a -> a[0] > 0.0F && a[1] > 0.0F && a[1] != 1.0F
                ? (float) (StrictMath.log(a[0]) / StrictMath.log(a[1]))
                : Float.NaN);
        function("cbrt", 1, a -> (float) StrictMath.cbrt(a[0]));
        function("abs", 1, a -> Math.abs(a[0]));
        function("floor", 1, a -> (float) StrictMath.floor(a[0]));
        function("ceil", 1, a -> (float) StrictMath.ceil(a[0]));
        function("round", 1, a -> (float) StrictMath.round(a[0]));
        function("sign", 1, a -> Math.signum(a[0]));
        function("clamp", 3, a -> Math.max(a[1], Math.min(a[2], a[0])));
        function("lerp", 3, a -> a[0] + (a[1] - a[0]) * a[2]);
        function("inverse_lerp", 3, a -> (a[2] - a[0]) / (a[1] - a[0]));
        function("hypot", 2, a -> (float) StrictMath.hypot(a[0], a[1]));
        function("atan2", 2, a -> (float) StrictMath.toDegrees(StrictMath.atan2(a[0], a[1])));
        function("atan", 1, a -> (float) StrictMath.toDegrees(StrictMath.atan(a[0])));
        function("asin", 1, a -> (float) StrictMath.toDegrees(StrictMath.asin(a[0])));
        function("acos", 1, a -> (float) StrictMath.toDegrees(StrictMath.acos(a[0])));
        function("tan", 1, a -> (float) StrictMath.tan(StrictMath.toRadians(a[0])));
        function("smoothstep", 3, a -> {
            float t = Math.max(0.0F, Math.min(1.0F, (a[2] - a[0]) / (a[1] - a[0])));
            return t * t * (3.0F - 2.0F * t);
        });
        function("pi", 0, a -> (float) StrictMath.PI);
        function("tau", 0, a -> (float) (StrictMath.PI * 2.0D));
        function("e", 0, a -> (float) StrictMath.E);
    }

    private static void function(String name, int arguments,
                                 LogicNumberFunctionDefinition.Evaluator evaluator) {
        LogicNumberFunctions.register(LogicNumberFunctionDefinition.of(name, arguments, evaluator));
    }
}
