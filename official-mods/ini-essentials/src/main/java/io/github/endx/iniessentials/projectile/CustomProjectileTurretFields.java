package io.github.endx.iniessentials.projectile;

import io.github.endx.iniessentials.BooleanExpression;
import io.github.endx.iniessentials.IniEssentials;

import io.github.endx.rustedfabricapi.api.ini.IniApplicationPhase;
import io.github.endx.rustedfabricapi.api.ini.IniExtensions;
import io.github.endx.rustedfabricapi.api.ini.IniFieldDefinition;
import io.github.endx.rustedfabricapi.api.ini.IniFieldDocumentation;
import io.github.endx.rustedfabricapi.api.ini.IniMultiplayerImpact;
import io.github.endx.rustedfabricapi.api.ini.IniSectionSelector;
import io.github.endx.rustedfabricapi.api.custom.event.CustomUnitEventEvaluation;
import io.github.endx.rustedfabricapi.api.event.RustedCustomUnitRegistryEvents;
import io.github.endx.rustedfabricapi.api.projectile.pattern.TurretProjectilePatternEvents;
import io.github.endx.rustedfabricapi.api.projectile.pattern.TurretProjectilePatternPlan;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.custom.CustomTagList;
import rustedwarfare.custom.TurretTemplate;
import rustedwarfare.custom.UnitTag;
import rustedwarfare.custom.logic.VariableScope;
import rustedwarfare.unit.Unit;
import rustedwarfare.util.UnitConfig;
import rustedwarfare.unit.combat.TurretRuntimeState;
import rustedwarfare.util.CommonUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/** Exact turret integration for independent CustomProjectile patterns. */
public final class CustomProjectileTurretFields {
    private static final String FIELD = "projectilePattern";
    private static final String RULE_PREFIX = "projectilePatternRule_";
    private static final String RULE_PATTERN = "pattern";
    private static final String IF_CONDITION = "ifCondition";
    private static final String IF_TARGET_WITH_TAGS = "ifTargetWithTags";
    private static final String IF_TARGET_WITHOUT_TAGS = "ifTargetWithoutTags";
    private static final String PLACEHOLDER = "iniEssentialsPatternPlaceholder";
    private static final Map<Object, Map<String, DeferredReference>> BY_METADATA =
            Collections.synchronizedMap(
                    new WeakHashMap<Object, Map<String, DeferredReference>>());
    private static final Map<Object, Map<String, LinkedHashMap<String, PatternRule>>> RULES =
            Collections.synchronizedMap(
                    new WeakHashMap<Object, Map<String, LinkedHashMap<String, PatternRule>>>());

    private CustomProjectileTurretFields() { }

    public static void register() {
        IniExtensions.register(IniFieldDefinition
                .<String>builder(
                        IniEssentials.MOD_ID, "turret_projectile_pattern",
                        IniSectionSelector.prefix("turret_"), FIELD)
                .applicationPhase(IniApplicationPhase.BEFORE_STATIC_VARIABLES)
                .decoder(context -> context.rawValue().trim())
                .applier(field -> {
                    UnitConfig config = (UnitConfig) field.unitConfig();
                    ensureStandalonePlaceholder(config, field.source().section());
                    synchronized (BY_METADATA) {
                        BY_METADATA.computeIfAbsent(field.metadata(), ignored ->
                                new LinkedHashMap<String, DeferredReference>())
                                .put(field.source().section(), new DeferredReference(
                                        config, field.source().section(), field.source().key(),
                                        field.value()));
                    }
                    IniEssentials.activateSynchronizedRequirement();
                })
                .documentation(new IniFieldDocumentation(
                        "namespace:path/pattern",
                        "Replaces only this turret's native projectile creation with a CustomProjectile pattern while preserving native firing side effects.",
                        "仅把此炮塔的原版弹体创建替换为 CustomProjectile 弹幕，同时保留原版开火副作用。",
                        "projectilePattern: example:plasma_fan/main",
                        IniMultiplayerImpact.GAMEPLAY_SYNCED))
                .build());

        registerRuleField(RULE_PATTERN);
        registerRuleField(IF_CONDITION);
        registerRuleField(IF_TARGET_WITH_TAGS);
        registerRuleField(IF_TARGET_WITHOUT_TAGS);
        RustedCustomUnitRegistryEvents.AFTER_METADATA_PARSED.register((context, metadata) -> {
            validateRules(metadata);
            return metadata;
        });

        TurretProjectilePatternEvents.PLAN.register(request -> {
            CustomUnit shooter = request.shooter();
            Map<String, DeferredReference> fields = BY_METADATA.get(
                    shooter.unitMetadata);
            Map<String, LinkedHashMap<String, PatternRule>> metadataRules = RULES.get(
                    shooter.unitMetadata);
            if (fields == null && metadataRules == null) return;
            if (shooter.unitMetadata.turretTemplates == null
                    || request.turretIndex() >= shooter.unitMetadata.turretTemplates.length) return;
            TurretTemplate turret = shooter.unitMetadata.turretTemplates[request.turretIndex()];
            CustomProjectileDefinitions.Reference reference = chooseRule(
                    metadataRules, turret, shooter, request.targetUnit().orElse(null));
            if (reference == null && fields != null) {
                DeferredReference field = fields.get(turret.sectionName);
                if (field == null) field = fields.get("turret_" + turret.name);
                if (field != null) reference = field.reference;
            }
            if (reference == null) return;

            CustomProjectileDefinitions.Definition definition = reference.definition();
            CustomProjectileDefinitions.CompiledPattern pattern = definition
                    .requirePattern(reference.patternName()).compileFor(shooter);
            float nativeDirection = nativeTurretDirection(shooter, request.turretIndex());
            float direction = pattern.centerDirection(shooter, nativeDirection);
            float localX = pattern.originOffsetX.evaluate(shooter);
            float localY = pattern.originOffsetY.evaluate(shooter);
            float sin = CommonUtils.fastSin(shooter.direction);
            float cos = CommonUtils.fastCos(shooter.direction);
            float worldX = cos * localY - sin * localX;
            float worldY = sin * localY + cos * localX;

            request.replace(TurretProjectilePatternPlan
                    .builder(definition.projectile(), pattern.resolve(shooter))
                    .aimMode(pattern.aimMode)
                    .centerDirection(direction)
                    .originOffset(worldX, worldY,
                            pattern.originOffsetHeight.evaluate(shooter))
                    .collision(definition.collision().compileFor(shooter).resolve(shooter))
                    .directionDistance(pattern.directionDistance.evaluate(shooter))
                    .build());
        });
    }

    private static void registerRuleField(String suffix) {
        IniExtensions.register(IniFieldDefinition
                .<String>builder(IniEssentials.MOD_ID,
                        "turret_projectile_pattern_rule_" + suffix.toLowerCase(java.util.Locale.ROOT),
                        IniSectionSelector.prefix("turret_"), RULE_PREFIX)
                .matchKeyPrefix()
                .applicationPhase(IniApplicationPhase.BEFORE_STATIC_VARIABLES)
                .activatesWhen(context -> ruleName(context.key(), suffix) != null)
                .decoder(context -> context.rawValue().trim())
                .validator((context, value) -> {
                    if (value.isEmpty()) throw new IllegalArgumentException("value must not be empty");
                })
                .applier(field -> {
                    String name = ruleName(field.source().key(), suffix);
                    PatternRule rule = rule(field.metadata(), field.source().section(), name);
                    rule.config = (UnitConfig) field.unitConfig();
                    if (RULE_PATTERN.equals(suffix)) {
                        rule.referenceKey = field.source().key();
                        rule.referenceSource = field.value();
                    } else if (IF_CONDITION.equals(suffix)) {
                        rule.conditionKey = field.source().key();
                        rule.conditionSource = field.value();
                    } else if (IF_TARGET_WITH_TAGS.equals(suffix)) {
                        rule.withTagsKey = field.source().key();
                        rule.withTagsSource = field.value();
                    } else if (IF_TARGET_WITHOUT_TAGS.equals(suffix)) {
                        rule.withoutTagsKey = field.source().key();
                        rule.withoutTagsSource = field.value();
                    }
                    IniEssentials.activateSynchronizedRequirement();
                })
                .build());
    }

    private static void ensureStandalonePlaceholder(UnitConfig config, String turretSection) {
        if (config.hasKey(turretSection, "projectile")) return;
        String section = "projectile_" + PLACEHOLDER;
        config.putIfAbsent(section, "directDamage", "0");
        config.putIfAbsent(section, "life", "1");
        config.putIfAbsent(section, "speed", "0");
        config.putIfAbsent(section, "invisible", "true");
        config.putIfAbsent(turretSection, "projectile", PLACEHOLDER);
    }

    private static CustomProjectileDefinitions.Reference chooseRule(
            Map<String, LinkedHashMap<String, PatternRule>> metadataRules,
            TurretTemplate turret, CustomUnit shooter, Unit target) {
        if (metadataRules == null) return null;
        LinkedHashMap<String, PatternRule> rules = metadataRules.get(turret.sectionName);
        if (rules == null) rules = metadataRules.get("turret_" + turret.name);
        if (rules == null) return null;
        for (PatternRule rule : rules.values()) {
            if (matchesTags(rule.withTags, rule.withoutTags, target)
                    && evaluate(rule.condition, shooter, target)) return rule.reference;
        }
        return null;
    }

    private static boolean evaluate(BooleanExpression expression, CustomUnit actor, Unit target) {
        if (expression == null) return true;
        return CustomUnitEventEvaluation.withContext(actor, target,
                target != null ? target.getRuntimeTags() : null,
                VariableScope.emptyVariableScope,
                () -> Boolean.valueOf(expression.evaluate(actor))).booleanValue();
    }

    private static boolean matchesTags(CustomTagList withTags, CustomTagList withoutTags,
                                       Unit target) {
        if (withTags == null && withoutTags == null) return true;
        if (target == null) return false;
        CustomTagList actual = target.getRuntimeTags();
        return (withoutTags == null || !UnitTag.anyTagMatches(withoutTags, actual))
                && (withTags == null || UnitTag.anyTagMatches(withTags, actual));
    }

    private static PatternRule rule(Object metadata, String section, String name) {
        synchronized (RULES) {
            Map<String, LinkedHashMap<String, PatternRule>> bySection =
                    RULES.computeIfAbsent(metadata, ignored ->
                            new LinkedHashMap<String, LinkedHashMap<String, PatternRule>>());
            return bySection.computeIfAbsent(section, ignored ->
                    new LinkedHashMap<String, PatternRule>())
                    .computeIfAbsent(name, ignored -> new PatternRule());
        }
    }

    private static void validateRules(Object metadata) {
        Map<String, DeferredReference> fields = BY_METADATA.get(metadata);
        if (fields != null) {
            for (DeferredReference field : fields.values()) field.resolve();
        }
        Map<String, LinkedHashMap<String, PatternRule>> bySection = RULES.get(metadata);
        if (bySection == null) return;
        for (Map.Entry<String, LinkedHashMap<String, PatternRule>> section : bySection.entrySet()) {
            for (Map.Entry<String, PatternRule> entry : section.getValue().entrySet()) {
                PatternRule rule = entry.getValue();
                String location = "[" + section.getKey() + "] "
                        + RULE_PREFIX + entry.getKey();
                rule.resolveStaticValues(section.getKey());
                if (rule.reference == null) {
                    throw new IllegalArgumentException(location + " requires _pattern");
                }
                if (rule.conditionSource != null) {
                    String current = rule.config != null && rule.conditionKey != null
                            ? rule.config.getRawValue(section.getKey(), rule.conditionKey) : null;
                    if (current != null) rule.conditionSource = current;
                    rule.condition = BooleanExpression.compile(metadata, rule.conditionSource);
                }
                if (rule.conditionSource == null
                        && rule.withTags == null && rule.withoutTags == null) {
                    throw new IllegalArgumentException(location + " requires at least one condition");
                }
            }
        }
    }

    private static String ruleName(String key, String suffix) {
        String ending = "_" + suffix;
        if (!key.startsWith(RULE_PREFIX) || !key.endsWith(ending)
                || key.length() <= RULE_PREFIX.length() + ending.length()) return null;
        return key.substring(RULE_PREFIX.length(), key.length() - ending.length());
    }

    private static final class PatternRule {
        CustomProjectileDefinitions.Reference reference;
        UnitConfig config;
        String referenceKey;
        String referenceSource;
        String conditionKey;
        String conditionSource;
        BooleanExpression condition;
        String withTagsKey;
        String withTagsSource;
        CustomTagList withTags;
        String withoutTagsKey;
        String withoutTagsSource;
        CustomTagList withoutTags;

        void resolveStaticValues(String section) {
            referenceSource = currentSource(section, referenceKey, referenceSource);
            conditionSource = currentSource(section, conditionKey, conditionSource);
            withTagsSource = currentSource(section, withTagsKey, withTagsSource);
            withoutTagsSource = currentSource(section, withoutTagsKey, withoutTagsSource);
            if (referenceSource != null) {
                reference = CustomProjectileDefinitions.Reference.parse(referenceSource);
                CustomProjectileDefinitions.noteReference(reference);
            }
            if (withTagsSource != null) withTags = UnitTag.parseTagList(withTagsSource);
            if (withoutTagsSource != null) withoutTags = UnitTag.parseTagList(withoutTagsSource);
        }

        private String currentSource(String section, String key, String fallback) {
            if (config == null || key == null) return fallback;
            String current = config.getRawValue(section, key);
            return current != null ? current : fallback;
        }
    }

    private static final class DeferredReference {
        final UnitConfig config;
        final String section;
        final String key;
        final String source;
        CustomProjectileDefinitions.Reference reference;

        DeferredReference(UnitConfig config, String section, String key, String source) {
            this.config = config;
            this.section = section;
            this.key = key;
            this.source = source;
        }

        void resolve() {
            String current = config.getRawValue(section, key);
            reference = CustomProjectileDefinitions.Reference.parse(
                    current != null ? current : source);
            CustomProjectileDefinitions.noteReference(reference);
        }
    }

    private static float nativeTurretDirection(CustomUnit shooter, int turretIndex) {
        if (shooter.turretStates != null && turretIndex < shooter.turretStates.length) {
            TurretRuntimeState state = shooter.turretStates[turretIndex];
            if (state != null) return state.aimAngle;
        }
        return shooter.direction;
    }
}
