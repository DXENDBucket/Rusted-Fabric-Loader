package io.github.endx.iniessentials.projectile;

import io.github.endx.iniessentials.NumericExpression;
import io.github.endx.rustedfabricapi.api.projectile.spawn.ProjectileAimMode;
import rustedwarfare.custom.CustomUnit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** One CustomProjectile reference plus per-emission expression overrides. */
final class CustomProjectileSpawnRequest {
    private static final String[] NUMERIC_KEYS = {
            "count", "centerDirection", "startAngle", "sweepAngle",
            "originSpacing", "lineAngleOffset", "originOffsetX", "originOffsetY",
            "originOffsetHeight", "directionDistance", "speed", "turnSpeed",
            "dx", "dy", "offsetX", "offsetY"
    };

    final CustomProjectileDefinitions.Reference reference;
    private final Map<String, String> numeric;
    private final ProjectileAimMode aimMode;

    private CustomProjectileSpawnRequest(CustomProjectileDefinitions.Reference reference,
                                         Map<String, String> numeric,
                                         ProjectileAimMode aimMode) {
        this.reference = reference;
        this.numeric = numeric;
        this.aimMode = aimMode;
    }

    static CustomProjectileSpawnRequest parse(String raw) {
        String source = raw != null ? raw.trim() : "";
        if (source.isEmpty()) throw new IllegalArgumentException(
                "spawnCustomProjectile reference is empty");
        int opening = firstTopLevelOpening(source);
        String referenceSource = opening >= 0 ? source.substring(0, opening).trim() : source;
        String arguments = null;
        if (opening >= 0) {
            int closing = matchingClosing(source, opening);
            if (closing != source.length() - 1) {
                throw new IllegalArgumentException(
                        "unexpected text after spawnCustomProjectile parameters: "
                                + source.substring(closing + 1));
            }
            arguments = source.substring(opening + 1, closing).trim();
        }

        LinkedHashMap<String, String> numeric = new LinkedHashMap<String, String>();
        ProjectileAimMode aimMode = null;
        if (arguments != null && !arguments.isEmpty()) {
            for (String item : splitTopLevel(arguments)) {
                int equals = topLevelEquals(item);
                if (equals <= 0 || equals == item.length() - 1) {
                    throw new IllegalArgumentException(
                            "spawnCustomProjectile parameter requires name=expression: " + item);
                }
                String rawName = item.substring(0, equals).trim();
                String name = canonicalKey(rawName);
                String value = item.substring(equals + 1).trim();
                if ("aimMode".equals(name)) {
                    if (aimMode != null) throw duplicate(name);
                    aimMode = enumValue(ProjectileAimMode.class, value, "aimMode");
                } else if (numeric.put(name, value) != null) {
                    throw duplicate(name);
                }
            }
        }
        return new CustomProjectileSpawnRequest(
                CustomProjectileDefinitions.Reference.parse(referenceSource),
                Collections.unmodifiableMap(numeric), aimMode);
    }

    Compiled compileForUnit(Object metadata) {
        LinkedHashMap<String, Value> values = new LinkedHashMap<String, Value>();
        for (Map.Entry<String, String> entry : numeric.entrySet()) {
            NumericExpression expression = NumericExpression.compile(metadata, entry.getValue());
            values.put(entry.getKey(), (actor, state) -> expression.evaluate(actor));
        }
        return new Compiled(reference, values, aimMode);
    }

    Compiled compileForProjectile(Object metadata,
                                  CustomProjectileExpression.MemorySchema memory) {
        LinkedHashMap<String, Value> values = new LinkedHashMap<String, Value>();
        for (Map.Entry<String, String> entry : numeric.entrySet()) {
            CustomProjectileExpression.Numeric expression =
                    CustomProjectileExpression.compileNumber(metadata, memory, entry.getValue());
            values.put(entry.getKey(), (actor, state) -> expression.evaluate(state));
        }
        return new Compiled(reference, values, aimMode);
    }

    private static String canonicalKey(String raw) {
        for (String candidate : NUMERIC_KEYS) {
            if (candidate.equalsIgnoreCase(raw)) return candidate;
        }
        if ("aimMode".equalsIgnoreCase(raw)) return "aimMode";
        throw new IllegalArgumentException("unknown spawnCustomProjectile parameter: " + raw);
    }

    private static IllegalArgumentException duplicate(String name) {
        return new IllegalArgumentException(
                "duplicate spawnCustomProjectile parameter: " + name);
    }

    private static int firstTopLevelOpening(String source) {
        for (int index = 0; index < source.length(); index++) {
            if (source.charAt(index) == '(') return index;
        }
        return -1;
    }

    private static int matchingClosing(String source, int opening) {
        int depth = 0;
        for (int index = opening; index < source.length(); index++) {
            char value = source.charAt(index);
            if (value == '(') depth++;
            else if (value == ')' && --depth == 0) return index;
            if (depth < 0) break;
        }
        throw new IllegalArgumentException("unbalanced spawnCustomProjectile parameters");
    }

    private static List<String> splitTopLevel(String raw) {
        ArrayList<String> result = new ArrayList<String>();
        int parentheses = 0;
        int brackets = 0;
        int start = 0;
        for (int index = 0; index < raw.length(); index++) {
            char value = raw.charAt(index);
            if (value == '(') parentheses++;
            else if (value == ')') parentheses--;
            else if (value == '[') brackets++;
            else if (value == ']') brackets--;
            else if (value == ',' && parentheses == 0 && brackets == 0) {
                String item = raw.substring(start, index).trim();
                if (item.isEmpty()) throw new IllegalArgumentException(
                        "empty spawnCustomProjectile parameter");
                result.add(item);
                start = index + 1;
            }
            if (parentheses < 0 || brackets < 0) throw new IllegalArgumentException(
                    "unbalanced spawnCustomProjectile parameter expression");
        }
        if (parentheses != 0 || brackets != 0) throw new IllegalArgumentException(
                "unbalanced spawnCustomProjectile parameter expression");
        String last = raw.substring(start).trim();
        if (last.isEmpty()) throw new IllegalArgumentException(
                "empty spawnCustomProjectile parameter");
        result.add(last);
        return result;
    }

    private static int topLevelEquals(String raw) {
        int parentheses = 0;
        int brackets = 0;
        for (int index = 0; index < raw.length(); index++) {
            char value = raw.charAt(index);
            if (value == '(') parentheses++;
            else if (value == ')') parentheses--;
            else if (value == '[') brackets++;
            else if (value == ']') brackets--;
            else if (value == '=' && parentheses == 0 && brackets == 0) return index;
        }
        return -1;
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String raw, String label) {
        try {
            return Enum.valueOf(type, raw.trim().replace('-', '_').toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("unknown " + label + ": " + raw);
        }
    }

    private interface Value {
        float evaluate(CustomUnit actor, CustomProjectileState state);
    }

    static final class Compiled {
        final CustomProjectileDefinitions.Reference reference;
        private final Map<String, Value> values;
        final ProjectileAimMode aimMode;

        private Compiled(CustomProjectileDefinitions.Reference reference,
                         Map<String, Value> values, ProjectileAimMode aimMode) {
            this.reference = reference;
            this.values = values;
            this.aimMode = aimMode;
        }

        Resolved resolve(CustomUnit actor, CustomProjectileState state) {
            LinkedHashMap<String, Float> result = new LinkedHashMap<String, Float>();
            for (Map.Entry<String, Value> entry : values.entrySet()) {
                float value = entry.getValue().evaluate(actor, state);
                if (!Float.isFinite(value)) throw new IllegalArgumentException(
                        "spawnCustomProjectile " + entry.getKey() + " produced a non-finite value");
                result.put(entry.getKey(), value);
            }
            return new Resolved(reference, result, aimMode);
        }
    }

    static final class Resolved {
        final CustomProjectileDefinitions.Reference reference;
        private final Map<String, Float> values;
        final ProjectileAimMode aimMode;

        private Resolved(CustomProjectileDefinitions.Reference reference,
                         Map<String, Float> values, ProjectileAimMode aimMode) {
            this.reference = reference;
            this.values = values;
            this.aimMode = aimMode;
        }

        Float value(String key) { return values.get(key); }
        float valueOr(String key, float fallback) {
            Float value = values.get(key);
            return value != null ? value.floatValue() : fallback;
        }
    }
}
