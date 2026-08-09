package io.github.endx.iniessentials;

import io.github.endx.rustedfabricapi.api.custom.event.CustomUnitEventData;
import io.github.endx.rustedfabricapi.api.custom.event.CustomUnitEventEvaluation;
import io.github.endx.rustedfabricapi.api.custom.event.CustomUnitTriggerEvents;
import io.github.endx.rustedfabricapi.api.ini.IniFieldDefinition;
import io.github.endx.rustedfabricapi.api.ini.IniFieldDocumentation;
import io.github.endx.rustedfabricapi.api.ini.IniExtensions;
import io.github.endx.rustedfabricapi.api.ini.IniMultiplayerImpact;
import io.github.endx.rustedfabricapi.api.ini.IniSectionSelector;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.custom.event.CustomUnitEventType;
import rustedwarfare.custom.logic.VariableScope;
import rustedwarfare.unit.Unit;
import rustedwarfare.util.UnitConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

/** Declarative rules applied before the native queued event action list starts. */
final class EventRuleDefinitions {
    private static final String PREFIX = "event_";
    private static final Map<Object, List<Rule>> BY_METADATA = Collections.synchronizedMap(
            new WeakHashMap<Object, List<Rule>>());

    private EventRuleDefinitions() { }

    static void register() {
        IniExtensions.register(IniFieldDefinition
                .<String>builder(IniEssentials.MOD_ID, "event_rule",
                        IniSectionSelector.prefix(PREFIX), "event")
                .decoder(context -> context.rawValue().trim())
                .validator((context, value) -> parseEventType(value))
                .applier(field -> {
                    parseAndStore(field.metadata(), (UnitConfig) field.unitConfig(),
                            field.source().section());
                    IniEssentials.activateSynchronizedRequirement();
                })
                .documentation(new IniFieldDocumentation(
                        "native autoTriggerOnEvent name",
                        "Declares an ordered queued-event rule without reordering native action effects.",
                        "声明一条有序的队列事件规则，不改变原版 action effect 的执行顺序。",
                        "[event_damageGate]\nevent: tookDamage\ncancelEventActions: eventData(name='damage',type='number') < 1",
                        IniMultiplayerImpact.GAMEPLAY_SYNCED))
                .build());

        CustomUnitTriggerEvents.PREPARE_QUEUE.register((unit, eventType, source, tags, scope) ->
                applyRules(unit, eventType, source, tags, scope));
    }

    private static void parseAndStore(Object metadata, UnitConfig config, String section) {
        String phase = optional(config, section, "phase");
        if (phase != null && !"queued".equalsIgnoreCase(phase.trim())) {
            throw new IllegalArgumentException(
                    "event phase currently supports only queued; native before/after phases are event-specific");
        }
        Rule rule = new Rule(parseEventType(required(config, section, "event")),
                BooleanExpression.compile(metadata, optional(config, section, "when"), "true"),
                BooleanExpression.compile(metadata,
                        optional(config, section, "cancelEventActions"), "false"),
                parseNumbers(metadata, optional(config, section, "setEventNumber")),
                parseNumbers(metadata, optional(config, section, "addEventNumber")),
                parseNumbers(metadata, optional(config, section, "multiplyEventNumber")),
                parseBooleans(metadata, optional(config, section, "setEventBoolean")));
        synchronized (BY_METADATA) {
            List<Rule> rules = BY_METADATA.computeIfAbsent(metadata,
                    ignored -> new ArrayList<Rule>());
            rules.add(rule);
        }
    }

    private static boolean applyRules(CustomUnit unit, CustomUnitEventType eventType,
                                      Unit source, rustedwarfare.custom.CustomTagList tags,
                                      VariableScope scope) {
        List<Rule> rules = BY_METADATA.get(unit.unitMetadata);
        if (rules == null || rules.isEmpty()) return false;
        VariableScope dataScope = scope != null ? scope : new VariableScope();
        CustomUnitEventData data = CustomUnitEventData.wrap(dataScope);
        for (Rule rule : rules) {
            if (rule.eventType != eventType) continue;
            boolean cancelled = CustomUnitEventEvaluation.withContext(
                    unit, source, tags, dataScope,
                    () -> rule.apply(unit, data));
            if (cancelled) return true;
        }
        return false;
    }

    private static List<NumberAssignment> parseNumbers(Object metadata, String raw) {
        if (raw == null || raw.trim().isEmpty()) return Collections.emptyList();
        ArrayList<NumberAssignment> result = new ArrayList<NumberAssignment>();
        for (String assignment : splitTopLevel(raw, ';')) {
            int equals = topLevelEquals(assignment);
            if (equals < 1) throw new IllegalArgumentException("expected name=number expression");
            result.add(new NumberAssignment(requiredPart(assignment.substring(0, equals)),
                    NumericExpression.compile(metadata, assignment.substring(equals + 1))));
        }
        return Collections.unmodifiableList(result);
    }

    private static List<BooleanAssignment> parseBooleans(Object metadata, String raw) {
        if (raw == null || raw.trim().isEmpty()) return Collections.emptyList();
        ArrayList<BooleanAssignment> result = new ArrayList<BooleanAssignment>();
        for (String assignment : splitTopLevel(raw, ';')) {
            int equals = topLevelEquals(assignment);
            if (equals < 1) throw new IllegalArgumentException("expected name=LogicBoolean");
            result.add(new BooleanAssignment(requiredPart(assignment.substring(0, equals)),
                    BooleanExpression.compile(metadata, assignment.substring(equals + 1))));
        }
        return Collections.unmodifiableList(result);
    }

    private static List<String> splitTopLevel(String raw, char delimiter) {
        ArrayList<String> result = new ArrayList<String>();
        int depth = 0;
        int start = 0;
        for (int index = 0; index < raw.length(); index++) {
            char value = raw.charAt(index);
            if (value == '(' || value == '[') depth++;
            else if (value == ')' || value == ']') depth--;
            else if (value == delimiter && depth == 0) {
                result.add(requiredPart(raw.substring(start, index)));
                start = index + 1;
            }
            if (depth < 0) throw new IllegalArgumentException("unbalanced event expression");
        }
        if (depth != 0) throw new IllegalArgumentException("unbalanced event expression");
        result.add(requiredPart(raw.substring(start)));
        return result;
    }

    private static int topLevelEquals(String raw) {
        int depth = 0;
        for (int index = 0; index < raw.length(); index++) {
            char value = raw.charAt(index);
            if (value == '(' || value == '[') depth++;
            else if (value == ')' || value == ']') depth--;
            else if (value == '=' && depth == 0) return index;
        }
        return -1;
    }

    private static CustomUnitEventType parseEventType(String raw) {
        String normalized = normalize(raw);
        for (CustomUnitEventType value : CustomUnitEventType.values()) {
            if (normalize(value.name()).equals(normalized)) return value;
        }
        throw new IllegalArgumentException("unknown custom-unit event: " + raw);
    }

    private static String normalize(String raw) {
        return raw.trim().toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
    }

    private static String required(UnitConfig config, String section, String key) {
        String value = optional(config, section, key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("[" + section + "] requires " + key);
        }
        return value;
    }

    private static String optional(UnitConfig config, String section, String key) {
        return config.getString(section, key, null);
    }

    private static String requiredPart(String raw) {
        String value = raw.trim();
        if (value.isEmpty()) throw new IllegalArgumentException("empty event assignment");
        return value;
    }

    private static final class Rule {
        private final CustomUnitEventType eventType;
        private final BooleanExpression when;
        private final BooleanExpression cancel;
        private final List<NumberAssignment> setNumbers;
        private final List<NumberAssignment> addNumbers;
        private final List<NumberAssignment> multiplyNumbers;
        private final List<BooleanAssignment> setBooleans;

        private Rule(CustomUnitEventType eventType, BooleanExpression when,
                     BooleanExpression cancel, List<NumberAssignment> setNumbers,
                     List<NumberAssignment> addNumbers, List<NumberAssignment> multiplyNumbers,
                     List<BooleanAssignment> setBooleans) {
            this.eventType = eventType;
            this.when = when;
            this.cancel = cancel;
            this.setNumbers = setNumbers;
            this.addNumbers = addNumbers;
            this.multiplyNumbers = multiplyNumbers;
            this.setBooleans = setBooleans;
        }

        private boolean apply(CustomUnit unit, CustomUnitEventData data) {
            if (!when.evaluate(unit)) return false;
            for (NumberAssignment assignment : setNumbers) {
                data.putNumber(assignment.name, assignment.value.evaluate(unit));
            }
            for (NumberAssignment assignment : addNumbers) {
                data.putNumber(assignment.name,
                        data.getNumber(assignment.name, 0.0) + assignment.value.evaluate(unit));
            }
            for (NumberAssignment assignment : multiplyNumbers) {
                data.putNumber(assignment.name,
                        data.getNumber(assignment.name, 1.0) * assignment.value.evaluate(unit));
            }
            for (BooleanAssignment assignment : setBooleans) {
                data.putBoolean(assignment.name, assignment.value.evaluate(unit));
            }
            return cancel.evaluate(unit);
        }
    }

    private static final class NumberAssignment {
        private final String name;
        private final NumericExpression value;
        private NumberAssignment(String name, NumericExpression value) {
            this.name = name;
            this.value = value;
        }
    }

    private static final class BooleanAssignment {
        private final String name;
        private final BooleanExpression value;
        private BooleanAssignment(String name, BooleanExpression value) {
            this.name = name;
            this.value = value;
        }
    }
}
