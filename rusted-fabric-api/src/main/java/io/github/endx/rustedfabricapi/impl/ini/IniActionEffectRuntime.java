package io.github.endx.rustedfabricapi.impl.ini;

import android.graphics.PointF;
import io.github.endx.rustedfabricapi.api.ini.IniExtensionException;
import io.github.endx.rustedfabricapi.api.ini.action.IniActionEffectDefinition;
import io.github.endx.rustedfabricapi.api.ini.action.IniActionEffects;
import io.github.endx.rustedfabricapi.api.ini.action.IniActionExecutionContext;
import io.github.endx.rustedfabricapi.api.ini.action.IniActionFieldContext;
import io.github.endx.rustedfabricapi.api.world.WorldPoint;
import io.github.endx.rustedfabricapi.api.util.RustedReflection;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.custom.action.CustomActionConfig;
import rustedwarfare.custom.action.effect.CustomActionEffect;
import rustedwarfare.unit.Unit;
import rustedwarfare.unit.action.UnitAction;
import rustedwarfare.util.RwArrayList;
import rustedwarfare.util.UnitConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Internal parser/execution bridge for registered INI action effects. */
public final class IniActionEffectRuntime {
    private IniActionEffectRuntime() { }

    public static void parseAndAttach(Object rawMetadata, Object rawConfig,
                                      String section, Object rawActionConfig,
                                      String actionName, boolean customAction) {
        if (!customAction || !IniActionEffects.hasDefinitions()) return;
        UnitConfig config = (UnitConfig) rawConfig;
        CustomActionConfig actionConfig = (CustomActionConfig) rawActionConfig;
        boolean hidden = section.startsWith("hiddenAction_");
        if (!hidden && !section.startsWith("action_")) return;

        List<Parsed<?>> parsed = new ArrayList<Parsed<?>>();
        Map<String, IniActionEffectDefinition<?>> exclusive =
                new HashMap<String, IniActionEffectDefinition<?>>();
        for (IniActionEffectDefinition<?> definition : IniActionEffects.definitions()) {
            if (!definition.scope().accepts(hidden)) continue;
            String rawValue = config.getString(section, definition.key(), null);
            if (rawValue == null) continue;
            IniActionFieldContext context = new IniActionFieldContext(rawMetadata, config, section,
                    actionName, hidden, definition.key(), rawValue);
            Parsed<?> value = decode(definition, context);
            String group = definition.exclusiveGroup();
            if (group != null) {
                IniActionEffectDefinition<?> previous = exclusive.put(group, definition);
                if (previous != null) {
                    throw new IniExtensionException(location(context) + " conflicts with "
                            + previous.key() + " in exclusive group " + group);
                }
            }
            parsed.add(value);
        }
        if (parsed.isEmpty()) return;
        if (actionConfig.actionEffects == null) actionConfig.actionEffects = new RwArrayList();
        for (Parsed<?> value : parsed) actionConfig.actionEffects.add(effect(value));
    }

    private static <T> Parsed<T> decode(IniActionEffectDefinition<T> definition,
                                        IniActionFieldContext context) {
        try {
            T value = definition.decode(context);
            definition.validate(context, value);
            return new Parsed<T>(definition, context, value);
        } catch (IniExtensionException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new IniExtensionException(location(context) + " failed decode/validation for "
                    + definition.qualifiedId() + ": " + failure.getMessage(), failure);
        }
    }

    private static <T> CustomActionEffect effectTyped(Parsed<T> parsed) {
        return new RegisteredActionEffect<T>(parsed);
    }

    @SuppressWarnings("unchecked")
    private static CustomActionEffect effect(Parsed<?> parsed) {
        return effectTyped((Parsed<Object>) parsed);
    }

    private static String location(IniActionFieldContext context) {
        return "[" + context.section() + "] " + context.key() + "=" + context.rawValue();
    }

    private static final class Parsed<T> {
        private final IniActionEffectDefinition<T> definition;
        private final IniActionFieldContext context;
        private final T value;

        private Parsed(IniActionEffectDefinition<T> definition,
                       IniActionFieldContext context, T value) {
            this.definition = definition;
            this.context = context;
            this.value = value;
        }
    }

    private static final class RegisteredActionEffect<T> extends CustomActionEffect {
        private final Parsed<T> parsed;

        private RegisteredActionEffect(Parsed<T> parsed) { this.parsed = parsed; }

        @Override
        public boolean execute(CustomUnit actor, UnitAction action, PointF targetPoint,
                               Unit targetUnit, int recursionDepth) {
            WorldPoint point = targetPoint != null
                    ? new WorldPoint(
                            RustedReflection.getFloatField(targetPoint,
                                    new String[]{"x", "a"}),
                            RustedReflection.getFloatField(targetPoint,
                                    new String[]{"y", "b"}))
                    : null;
            IniActionExecutionContext context = new IniActionExecutionContext(
                    actor, action, point, targetUnit, recursionDepth);
            try {
                parsed.definition.execute(context, parsed.value);
                return true;
            } catch (RuntimeException failure) {
                throw failure;
            } catch (Exception failure) {
                throw new IllegalStateException("INI action effect "
                        + parsed.definition.qualifiedId() + " failed at ["
                        + parsed.context.section() + "]", failure);
            }
        }
    }
}
