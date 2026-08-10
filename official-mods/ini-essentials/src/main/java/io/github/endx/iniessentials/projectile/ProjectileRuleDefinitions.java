package io.github.endx.iniessentials.projectile;

import io.github.endx.iniessentials.BooleanExpression;
import io.github.endx.iniessentials.IniEssentials;
import io.github.endx.iniessentials.NumericExpression;

import io.github.endx.rustedfabricapi.api.custom.event.CustomUnitEventEvaluation;
import io.github.endx.rustedfabricapi.api.event.RustedCustomUnitRegistryEvents;
import io.github.endx.rustedfabricapi.api.ini.IniApplicationPhase;
import io.github.endx.rustedfabricapi.api.ini.IniExtensionKind;
import io.github.endx.rustedfabricapi.api.ini.IniExtensions;
import io.github.endx.rustedfabricapi.api.ini.IniFieldDefinition;
import io.github.endx.rustedfabricapi.api.ini.IniSectionSelector;
import io.github.endx.rustedfabricapi.api.projectile.event.ProjectileCombatEvents;
import rustedwarfare.custom.CustomProjectileTemplate;
import rustedwarfare.custom.CustomTagList;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.custom.CustomUnitMetadata;
import rustedwarfare.custom.UnitTag;
import rustedwarfare.custom.logic.VariableScope;
import rustedwarfare.game.Projectile;
import rustedwarfare.unit.Unit;
import rustedwarfare.util.UnitConfig;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

/** Dynamic projectile mutators and ordered per-turret projectile replacement rules. */
public final class ProjectileRuleDefinitions {
    private static final String MUTATOR_PREFIX = "mutator";
    private static final String SELECT_PREFIX = "projectileRule_";
    private static final String IF_CONDITION = "ifCondition";
    private static final String DIRECT_MULTIPLIER = "directDamageMultiplier";
    private static final String AREA_MULTIPLIER = "areaDamageMultiplier";
    private static final String SELECT_PROJECTILE = "projectile";
    private static final String IF_TARGET_WITH_TAGS = "ifTargetWithTags";
    private static final String IF_TARGET_WITHOUT_TAGS = "ifTargetWithoutTags";
    private static final String NEVER_TAG = "ini_essentials_internal_never_match";

    private static final Map<Object, MetadataRules> RULES =
            Collections.synchronizedMap(new WeakHashMap<Object, MetadataRules>());

    private ProjectileRuleDefinitions() { }

    public static void register() {
        registerMutatorCondition();
        registerMutatorMultiplier(DIRECT_MULTIPLIER, false);
        registerMutatorMultiplier(AREA_MULTIPLIER, true);
        registerSelectionField(SELECT_PROJECTILE);
        registerSelectionField(IF_CONDITION);
        registerSelectionField(IF_TARGET_WITH_TAGS);
        registerSelectionField(IF_TARGET_WITHOUT_TAGS);

        ProjectileCombatEvents.MODIFY_DAMAGE.register(ProjectileRuleDefinitions::modifyDamage);
        ProjectileCombatEvents.SELECT_TURRET_PROJECTILE.register(
                ProjectileRuleDefinitions::selectProjectile);
        RustedCustomUnitRegistryEvents.AFTER_METADATA_PARSED.register((context, metadata) -> {
            validate(metadata);
            return metadata;
        });
    }

    private static void registerMutatorCondition() {
        IniExtensions.register(IniFieldDefinition
                .<String>builder(IniEssentials.MOD_ID, "mutator_if_condition",
                        IniSectionSelector.prefix("projectile_"), MUTATOR_PREFIX)
                .matchKeyPrefix()
                .applicationPhase(IniApplicationPhase.BEFORE_STATIC_VARIABLES)
                .activatesWhen(context -> mutatorPrefix(context.key(), IF_CONDITION) != null)
                .decoder(context -> context.rawValue().trim())
                .validator((context, value) -> {
                    if (value.isEmpty()) throw new IllegalArgumentException("condition must not be empty");
                })
                .applier(field -> {
                    String prefix = mutatorPrefix(field.source().key(), IF_CONDITION);
                    UnitConfig config = (UnitConfig) field.unitConfig();
                    ensureNativeMutatorCanParse(config, field.source().section(), prefix);
                    MutatorRule rule = mutatorRule(field.metadata(), field.source().section(), prefix);
                    rule.config = config;
                    rule.conditionKey = field.source().key();
                    rule.conditionSource = field.value();
                    IniEssentials.activateSynchronizedRequirement();
                })
                .build());
    }

    private static void registerMutatorMultiplier(String suffix, boolean area) {
        IniExtensions.register(IniFieldDefinition
                .<String>builder(IniEssentials.MOD_ID,
                        area ? "mutator_dynamic_area_multiplier"
                                : "mutator_dynamic_direct_multiplier",
                        IniSectionSelector.prefix("projectile_"), MUTATOR_PREFIX)
                .matchKeyPrefix()
                .kind(IniExtensionKind.EXTENDED_FORMAT)
                .applicationPhase(IniApplicationPhase.BEFORE_STATIC_VARIABLES)
                .activatesWhen(context -> {
                    String prefix = mutatorPrefix(context.key(), suffix);
                    if (prefix == null) return false;
                    UnitConfig config = (UnitConfig) context.unitConfig();
                    return !isPlainFloat(context.rawValue())
                            || config.hasKey(context.section(), prefix + IF_CONDITION);
                })
                .decoder(context -> context.rawValue().trim())
                .nativeFallback((context, value) -> "1")
                .applier(field -> {
                    String prefix = mutatorPrefix(field.source().key(), suffix);
                    UnitConfig config = (UnitConfig) field.unitConfig();
                    ensureNativeMutatorCanParse(config, field.source().section(), prefix);
                    MutatorRule rule = mutatorRule(field.metadata(), field.source().section(), prefix);
                    rule.config = config;
                    if (area) {
                        rule.areaMultiplierKey = field.source().key();
                        rule.areaMultiplierSource = field.value();
                    } else {
                        rule.directMultiplierKey = field.source().key();
                        rule.directMultiplierSource = field.value();
                    }
                    IniEssentials.activateSynchronizedRequirement();
                })
                .build());
    }

    private static void registerSelectionField(String suffix) {
        IniExtensions.register(IniFieldDefinition
                .<String>builder(IniEssentials.MOD_ID,
                        "turret_projectile_rule_" + suffix.toLowerCase(Locale.ROOT),
                        IniSectionSelector.prefix("turret_"), SELECT_PREFIX)
                .matchKeyPrefix()
                .applicationPhase(IniApplicationPhase.AFTER_METADATA_PARSED)
                .activatesWhen(context -> selectionName(context.key(), suffix) != null)
                .decoder(context -> context.rawValue().trim())
                .validator((context, value) -> {
                    if (value.isEmpty()) throw new IllegalArgumentException("value must not be empty");
                })
                .applier(field -> {
                    String name = selectionName(field.source().key(), suffix);
                    SelectionRule rule = selectionRule(field.metadata(), field.source().section(), name);
                    if (SELECT_PROJECTILE.equals(suffix)) {
                        rule.projectileName = field.value();
                    } else if (IF_CONDITION.equals(suffix)) {
                        rule.condition = BooleanExpression.compile(field.metadata(), field.value());
                    } else if (IF_TARGET_WITH_TAGS.equals(suffix)) {
                        rule.withTags = UnitTag.parseTagList(field.value());
                    } else if (IF_TARGET_WITHOUT_TAGS.equals(suffix)) {
                        rule.withoutTags = UnitTag.parseTagList(field.value());
                    }
                    IniEssentials.activateSynchronizedRequirement();
                })
                .build());
    }

    private static float modifyDamage(Projectile projectile, Unit target, float originalDamage,
                                      float nativeDamage, float currentDamage, boolean areaHit) {
        if (!(projectile.projectileTemplate instanceof CustomProjectileTemplate)
                || !(projectile.sourceUnit instanceof CustomUnit)) return currentDamage;
        CustomProjectileTemplate template =
                (CustomProjectileTemplate) projectile.projectileTemplate;
        MetadataRules metadataRules = RULES.get(template.unitMetadata);
        if (metadataRules == null) return currentDamage;
        LinkedHashMap<String, MutatorRule> rules = metadataRules.mutators.get(
                "projectile_" + template.name);
        if (rules == null) return currentDamage;

        CustomUnit shooter = (CustomUnit) projectile.sourceUnit;
        float result = currentDamage;
        for (MutatorRule rule : rules.values()) {
            NumericExpression expression = areaHit ? rule.areaMultiplier : rule.directMultiplier;
            if (expression == null || !matchesTags(rule.withTags, rule.withoutTags, target)
                    || !evaluate(rule.condition, shooter, target)) continue;
            float multiplier = evaluate(expression, shooter, target);
            if (!Float.isFinite(multiplier)) {
                throw new IllegalStateException("Projectile mutator produced a non-finite multiplier");
            }
            result *= multiplier;
        }
        return result;
    }

    private static int selectProjectile(CustomUnit shooter, Unit target,
                                        rustedwarfare.custom.TurretTemplate turret,
                                        int turretIndex, int nativeIndex, int currentIndex) {
        MetadataRules metadataRules = RULES.get(shooter.unitMetadata);
        if (metadataRules == null) return currentIndex;
        LinkedHashMap<String, SelectionRule> rules = metadataRules.selections.get(turret.sectionName);
        if (rules == null) rules = metadataRules.selections.get("turret_" + turret.name);
        if (rules == null) return currentIndex;

        for (SelectionRule rule : rules.values()) {
            if (!matchesTags(rule.withTags, rule.withoutTags, target)
                    || !evaluate(rule.condition, shooter, target)) continue;
            CustomProjectileTemplate replacement =
                    shooter.unitMetadata.getProjectileTemplateByName(rule.projectileName);
            if (replacement != null) return replacement.projectileIndex;
        }
        return currentIndex;
    }

    private static boolean evaluate(BooleanExpression expression, CustomUnit actor, Unit target) {
        if (expression == null) return true;
        return CustomUnitEventEvaluation.withContext(actor, target,
                target != null ? target.getRuntimeTags() : null,
                VariableScope.emptyVariableScope,
                () -> Boolean.valueOf(expression.evaluate(actor))).booleanValue();
    }

    private static float evaluate(NumericExpression expression, CustomUnit actor, Unit target) {
        return CustomUnitEventEvaluation.withContext(actor, target,
                target != null ? target.getRuntimeTags() : null,
                VariableScope.emptyVariableScope,
                () -> Float.valueOf(expression.evaluate(actor))).floatValue();
    }

    private static boolean matchesTags(CustomTagList withTags, CustomTagList withoutTags,
                                       Unit target) {
        if (withTags == null && withoutTags == null) return true;
        if (target == null) return false;
        CustomTagList actual = target.getRuntimeTags();
        return (withoutTags == null || !UnitTag.anyTagMatches(withoutTags, actual))
                && (withTags == null || UnitTag.anyTagMatches(withTags, actual));
    }

    private static void validate(Object metadataObject) {
        MetadataRules rules = RULES.get(metadataObject);
        if (rules == null) return;
        CustomUnitMetadata metadata = (CustomUnitMetadata) metadataObject;
        for (Map.Entry<String, LinkedHashMap<String, MutatorRule>> section
                : rules.mutators.entrySet()) {
            for (Map.Entry<String, MutatorRule> entry : section.getValue().entrySet()) {
                MutatorRule rule = entry.getValue();
                rule.conditionSource = currentSource(rule.config, section.getKey(),
                        rule.conditionKey, rule.conditionSource);
                rule.directMultiplierSource = currentSource(rule.config, section.getKey(),
                        rule.directMultiplierKey, rule.directMultiplierSource);
                rule.areaMultiplierSource = currentSource(rule.config, section.getKey(),
                        rule.areaMultiplierKey, rule.areaMultiplierSource);
                readNativeTags(rule, rule.config, section.getKey(), entry.getKey());
                if (rule.conditionSource != null) {
                    rule.condition = BooleanExpression.compile(metadata, rule.conditionSource);
                }
                if (rule.directMultiplierSource != null) {
                    rule.directMultiplier = NumericExpression.compile(
                            metadata, rule.directMultiplierSource);
                }
                if (rule.areaMultiplierSource != null) {
                    rule.areaMultiplier = NumericExpression.compile(
                            metadata, rule.areaMultiplierSource);
                }
                if (rule.directMultiplierSource == null && rule.areaMultiplierSource == null) {
                    throw new IllegalArgumentException("[" + section.getKey() + "] "
                            + entry.getKey() + IF_CONDITION
                            + " requires a directDamageMultiplier or areaDamageMultiplier");
                }
            }
        }
        for (Map.Entry<String, LinkedHashMap<String, SelectionRule>> section
                : rules.selections.entrySet()) {
            for (Map.Entry<String, SelectionRule> entry : section.getValue().entrySet()) {
                SelectionRule rule = entry.getValue();
                String location = "[" + section.getKey() + "] projectileRule_" + entry.getKey();
                if (rule.projectileName == null) {
                    throw new IllegalArgumentException(location + " requires _projectile");
                }
                if (rule.condition == null && rule.withTags == null && rule.withoutTags == null) {
                    throw new IllegalArgumentException(location + " requires at least one condition");
                }
                if (metadata.getProjectileTemplateByName(rule.projectileName) == null) {
                    throw new IllegalArgumentException(location + " cannot find projectile: "
                            + rule.projectileName);
                }
            }
        }
    }

    private static MutatorRule mutatorRule(Object metadata, String section, String prefix) {
        MetadataRules rules = metadataRules(metadata);
        LinkedHashMap<String, MutatorRule> sectionRules = rules.mutators.computeIfAbsent(
                section, ignored -> new LinkedHashMap<String, MutatorRule>());
        return sectionRules.computeIfAbsent(prefix, ignored -> new MutatorRule());
    }

    private static SelectionRule selectionRule(Object metadata, String section, String name) {
        MetadataRules rules = metadataRules(metadata);
        LinkedHashMap<String, SelectionRule> sectionRules = rules.selections.computeIfAbsent(
                section, ignored -> new LinkedHashMap<String, SelectionRule>());
        return sectionRules.computeIfAbsent(name, ignored -> new SelectionRule());
    }

    private static MetadataRules metadataRules(Object metadata) {
        synchronized (RULES) {
            return RULES.computeIfAbsent(metadata, ignored -> new MetadataRules());
        }
    }

    private static void readNativeTags(MutatorRule rule, UnitConfig config,
                                       String section, String prefix) {
        String with = config.getRawValue(section, prefix + "ifUnitWithTags");
        String without = config.getRawValue(section, prefix + "ifUnitWithoutTags");
        if (with != null) rule.withTags = UnitTag.parseTagList(with);
        if (without != null && !NEVER_TAG.equals(without)) {
            rule.withoutTags = UnitTag.parseTagList(without);
        }
    }

    private static void ensureNativeMutatorCanParse(UnitConfig config, String section,
                                                     String prefix) {
        if (!config.hasKey(section, prefix + "ifUnitWithTags")
                && !config.hasKey(section, prefix + "ifUnitWithoutTags")) {
            config.putIfAbsent(section, prefix + "ifUnitWithoutTags", NEVER_TAG);
        }
    }

    private static String mutatorPrefix(String key, String suffix) {
        String ending = "_" + suffix;
        if (!key.startsWith(MUTATOR_PREFIX) || !key.endsWith(ending)
                || key.length() <= MUTATOR_PREFIX.length() + ending.length()) return null;
        return key.substring(0, key.length() - suffix.length());
    }

    private static String selectionName(String key, String suffix) {
        String ending = "_" + suffix;
        if (!key.startsWith(SELECT_PREFIX) || !key.endsWith(ending)
                || key.length() <= SELECT_PREFIX.length() + ending.length()) return null;
        return key.substring(SELECT_PREFIX.length(), key.length() - ending.length());
    }

    private static boolean isPlainFloat(String raw) {
        try {
            Float.parseFloat(raw.trim());
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static String currentSource(UnitConfig config, String section, String key,
                                        String fallback) {
        if (config == null || key == null) return fallback;
        String current = config.getRawValue(section, key);
        return current != null ? current : fallback;
    }

    private static final class MetadataRules {
        final LinkedHashMap<String, LinkedHashMap<String, MutatorRule>> mutators =
                new LinkedHashMap<String, LinkedHashMap<String, MutatorRule>>();
        final LinkedHashMap<String, LinkedHashMap<String, SelectionRule>> selections =
                new LinkedHashMap<String, LinkedHashMap<String, SelectionRule>>();
    }

    private static final class MutatorRule {
        UnitConfig config;
        String conditionKey;
        String directMultiplierKey;
        String areaMultiplierKey;
        String conditionSource;
        String directMultiplierSource;
        String areaMultiplierSource;
        BooleanExpression condition;
        NumericExpression directMultiplier;
        NumericExpression areaMultiplier;
        CustomTagList withTags;
        CustomTagList withoutTags;
    }

    private static final class SelectionRule {
        String projectileName;
        BooleanExpression condition;
        CustomTagList withTags;
        CustomTagList withoutTags;
    }
}
