package io.github.endx.iniessentials.client;

import io.github.endx.iniessentials.IniEssentials;
import io.github.endx.rustedfabricapi.api.client.event.ProductionActionNameColorEvents;
import io.github.endx.rustedfabricapi.api.ini.IniApplicationPhase;
import io.github.endx.rustedfabricapi.api.ini.IniExtensions;
import io.github.endx.rustedfabricapi.api.ini.IniFieldDefinition;
import io.github.endx.rustedfabricapi.api.ini.IniFieldDocumentation;
import io.github.endx.rustedfabricapi.api.ini.IniMultiplayerImpact;
import io.github.endx.rustedfabricapi.api.ini.IniSectionSelector;
import rustedwarfare.custom.CustomUnitMetadata;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Optional per-unit colors for queue-unit action labels in the native production menu. */
public final class ProductionNameColorFields {
    private static final Map<Object, ColorChoice> CHOICES =
            Collections.synchronizedMap(new WeakHashMap<Object, ColorChoice>());
    private static final Pattern COMPARISON = Pattern.compile(
            "^\\s*(-?(?:\\d+(?:\\.\\d*)?|\\.\\d+))\\s*(==|!=|>=|<=|>|<)\\s*"
                    + "(-?(?:\\d+(?:\\.\\d*)?|\\.\\d+))\\s*$");

    private ProductionNameColorFields() { }

    public static void register() {
        IniExtensions.register(IniFieldDefinition
                .<ColorChoice>builder(IniEssentials.MOD_ID, "production_name_color",
                        IniSectionSelector.exact("core"), "productionNameColor")
                .applicationPhase(IniApplicationPhase.AFTER_METADATA_PARSED)
                // Do not read this key through UnitConfig again here: field discovery itself is
                // reached from UnitConfig's tracked-read hook. The final metadata is available to
                // the applier, where inherited techLevel is authoritative.
                .decoder(context -> new ColorChoice(context.rawValue()))
                .validator((context, value) -> value.resolve(3))
                .applier(field -> CHOICES.put(field.metadata(), field.value()))
                .documentation(new IniFieldDocumentation(
                        "static ARGB color or if(comparison,color,color)",
                        "Overrides this unit's queue label color without changing techLevel; inherited ${core.techLevel}, nested if expressions, and native fallback are supported.",
                        "覆盖生产队列中的单位名称颜色且不改变 techLevel；支持继承后的 ${core.techLevel}、嵌套 if 表达式和 native 原版回退。",
                        "productionNameColor: if(${core.techLevel}==3,#E65A5A,native)",
                        IniMultiplayerImpact.CLIENT_ONLY))
                .build());
        ProductionActionNameColorEvents.RESOLVE.register((action, producedType, currentColor) -> {
            if (!(producedType instanceof CustomUnitMetadata)) return currentColor;
            ColorChoice choice = CHOICES.get(producedType);
            Integer configured = choice != null
                    ? choice.resolve(producedType.getTechLevel()) : null;
            return configured != null ? configured : currentColor;
        });
    }

    private static boolean startsWithIf(String value) {
        return value.trim().regionMatches(true, 0, "if(", 0, 3);
    }

    private static Integer parseExpression(String source) {
        String value = source.trim();
        if ("native".equalsIgnoreCase(value)) return null;
        if (!startsWithIf(value) || !value.endsWith(")")) return parseHexColor(value);

        List<String> arguments = splitTopLevel(value.substring(3, value.length() - 1));
        if (arguments.size() != 3) {
            throw new IllegalArgumentException(
                    "productionNameColor if(...) requires condition, true color, false color: "
                            + source);
        }
        return evaluateComparison(arguments.get(0))
                ? parseExpression(arguments.get(1))
                : parseExpression(arguments.get(2));
    }

    private static List<String> splitTopLevel(String source) {
        List<String> parts = new ArrayList<String>();
        int depth = 0;
        int start = 0;
        for (int index = 0; index < source.length(); index++) {
            char character = source.charAt(index);
            if (character == '(') depth++;
            else if (character == ')') {
                depth--;
                if (depth < 0) throw new IllegalArgumentException(
                        "Unbalanced productionNameColor expression: " + source);
            } else if (character == ',' && depth == 0) {
                parts.add(source.substring(start, index).trim());
                start = index + 1;
            }
        }
        if (depth != 0) throw new IllegalArgumentException(
                "Unbalanced productionNameColor expression: " + source);
        parts.add(source.substring(start).trim());
        return parts;
    }

    private static boolean evaluateComparison(String source) {
        Matcher matcher = COMPARISON.matcher(source);
        if (!matcher.matches()) throw new IllegalArgumentException(
                "productionNameColor condition must compare two static numbers: " + source);
        double left = Double.parseDouble(matcher.group(1));
        double right = Double.parseDouble(matcher.group(3));
        String operator = matcher.group(2);
        if ("==".equals(operator)) return Double.compare(left, right) == 0;
        if ("!=".equals(operator)) return Double.compare(left, right) != 0;
        if (">=".equals(operator)) return left >= right;
        if ("<=".equals(operator)) return left <= right;
        if (">".equals(operator)) return left > right;
        return left < right;
    }

    private static int parseHexColor(String source) {
        String value = source.trim();
        if (!value.startsWith("#") || (value.length() != 7 && value.length() != 9)) {
            throw new IllegalArgumentException(
                    "productionNameColor expression branches require #RRGGBB, #AARRGGBB, native, or nested if(...): "
                            + source);
        }
        long parsed = Long.parseLong(value.substring(1), 16);
        if (value.length() == 7) parsed |= 0xff000000L;
        return (int) parsed;
    }

    private static final class ColorChoice {
        final String expression;
        private int cachedTechLevel = Integer.MIN_VALUE;
        private Integer cachedColor;

        ColorChoice(String expression) {
            this.expression = expression;
        }

        synchronized Integer resolve(int techLevel) {
            if (cachedTechLevel != techLevel) {
                cachedColor = parseExpression(expression.replace(
                        "${core.techLevel}", Integer.toString(techLevel)));
                cachedTechLevel = techLevel;
            }
            return cachedColor;
        }
    }
}
