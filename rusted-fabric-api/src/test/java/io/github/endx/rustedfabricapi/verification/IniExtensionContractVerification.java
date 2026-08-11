package io.github.endx.rustedfabricapi.verification;

import io.github.endx.rustedfabricapi.api.ini.IniApplicationPhase;
import io.github.endx.rustedfabricapi.api.ini.IniExtensionKind;
import io.github.endx.rustedfabricapi.api.ini.IniExtensions;
import io.github.endx.rustedfabricapi.api.ini.IniFieldDefinition;
import io.github.endx.rustedfabricapi.api.ini.IniSectionSelector;
import io.github.endx.rustedfabricapi.impl.ini.IniExtensionRuntime;
import rustedwarfare.util.UnitConfig;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

final class IniExtensionContractVerification {
    private IniExtensionContractVerification() { }

    static void verify() {
        verifyEmptyRegistryDoesNotInspectUnknownObjects();
        verifyNewKeyIsConsumedAndAppliedOnce();
        verifyNativeValueIsNotActivated();
        verifyExplicitExtendedFormatUsesFallback();
        verifyPrefixKeyMatchingConsumesOnlyActivatedKeys();
        verifySectionDefinitionConsumesOnlyClaimedCompanionKeys();
        verifyAfterMetadataParsedPhase();
        verifyInvalidExtendedValueHasLocation();
    }

    private static void verifyAfterMetadataParsedPhase() {
        final int[] applications = {0};
        IniFieldDefinition<String> definition = IniFieldDefinition
                .<String>builder("test_mod", "late_value",
                        IniSectionSelector.exact("overlay_test"), "scaleX")
                .applicationPhase(IniApplicationPhase.AFTER_METADATA_PARSED)
                .decoder(context -> context.rawValue())
                .applier(field -> applications[0]++)
                .build();
        try (IniExtensions.Registration ignored = IniExtensions.register(definition)) {
            Object metadata = new Object();
            UnitConfig config = config("[overlay_test]\nscaleX: memory.scale\n");
            IniExtensionRuntime.index(config);
            IniExtensionRuntime.markActiveFieldsRead(config);
            IniExtensionRuntime.applyAfterStaticVariables(metadata, config);
            require(applications[0] == 0,
                    "late extension ran before native metadata parsing completed");
            IniExtensionRuntime.applyAfterMetadataParsed(metadata);
            IniExtensionRuntime.applyAfterMetadataParsed(metadata);
            require(applications[0] == 1,
                    "late extension was not applied exactly once after metadata parsing");
        }
    }

    private static void verifyEmptyRegistryDoesNotInspectUnknownObjects() {
        IniExtensionRuntime.index(new Object());
        IniExtensionRuntime.markActiveFieldsRead(new Object());
        require(IniExtensionRuntime.nativeFallback(new Object(), "core", "maxHp") == null,
                "empty extension registry touched the native path");
    }

    private static void verifyNewKeyIsConsumedAndAppliedOnce() {
        final int[] applications = {0};
        IniFieldDefinition<Boolean> definition = IniFieldDefinition
                .<Boolean>builder("test_mod", "allow_negative_hp",
                        IniSectionSelector.exact("core"), "allowNegativeHp")
                .decoder(context -> parseBoolean(context.rawValue()))
                .applier(field -> {
                    if (field.value()) applications[0]++;
                })
                .build();
        try (IniExtensions.Registration ignored = IniExtensions.register(definition)) {
            UnitConfig config = config("[core]\nname: test\nallowNegativeHp: true\n");
            config.getRequiredString("core", "name");
            IniExtensionRuntime.index(config);
            IniExtensionRuntime.markActiveFieldsRead(config);
            IniExtensionRuntime.applyAfterStaticVariables(new Object(), config);
            IniExtensionRuntime.applyAfterStaticVariables(new Object(), config);
            config.checkUnusedKeys();
            require(applications[0] == 1, "active extension was not applied exactly once");
        }
    }

    private static void verifyNativeValueIsNotActivated() {
        IniFieldDefinition<Integer> definition = extendedNumber();
        try (IniExtensions.Registration ignored = IniExtensions.register(definition)) {
            UnitConfig config = config("[core]\nmaxHp: 10\n");
            IniExtensionRuntime.index(config);
            require(IniExtensionRuntime.nativeFallback(config, "core", "maxHp") == null,
                    "ordinary native maxHp value activated an extension");
            require(config.getRequiredInt("core", "maxHp") == 10,
                    "native maxHp reader was changed");
        }
    }

    private static void verifyExplicitExtendedFormatUsesFallback() {
        final int[] decoded = {0};
        IniFieldDefinition<Integer> definition = IniFieldDefinition
                .<Integer>builder("test_mod", "hp_suffix",
                        IniSectionSelector.exact("core"), "maxHp")
                .kind(IniExtensionKind.EXTENDED_FORMAT)
                .activatesWhen(context -> context.rawValue().endsWith("hp"))
                .decoder(context -> Integer.valueOf(
                        context.rawValue().substring(0, context.rawValue().length() - 2)))
                .nativeFallback((context, value) -> value.toString())
                .applier(field -> decoded[0] = field.value())
                .build();
        try (IniExtensions.Registration ignored = IniExtensions.register(definition)) {
            UnitConfig config = config("[core]\nmaxHp: 25hp\n");
            IniExtensionRuntime.index(config);
            require("25".equals(IniExtensionRuntime.nativeFallback(config, "core", "maxHp")),
                    "active extended format did not provide its native fallback");
            IniExtensionRuntime.markActiveFieldsRead(config);
            IniExtensionRuntime.applyAfterStaticVariables(new Object(), config);
            config.checkUnusedKeys();
            require(decoded[0] == 25, "extended format did not decode or apply");
        }
    }

    private static void verifyInvalidExtendedValueHasLocation() {
        IniFieldDefinition<Integer> definition = extendedNumber();
        try (IniExtensions.Registration ignored = IniExtensions.register(definition)) {
            boolean contextual = false;
            try {
                IniExtensionRuntime.index(config("[core]\nmaxHp: badhp\n"));
            } catch (IllegalArgumentException expected) {
                contextual = expected.getMessage().contains("[core] maxHp=badhp")
                        && expected.getMessage().contains("test_mod:hp_suffix");
            }
            require(contextual, "invalid extension value lacked section, key, and owner context");
        }
    }

    private static void verifyPrefixKeyMatchingConsumesOnlyActivatedKeys() {
        final String[] appliedKey = {null};
        IniFieldDefinition<String> definition = IniFieldDefinition
                .<String>builder("test_mod", "mutator_condition",
                        IniSectionSelector.prefix("projectile_"), "mutator")
                .matchKeyPrefix()
                .activatesWhen(context -> context.key().endsWith("_ifCondition"))
                .decoder(context -> context.rawValue())
                .applier(field -> appliedKey[0] = field.source().key())
                .build();
        try (IniExtensions.Registration ignored = IniExtensions.register(definition)) {
            UnitConfig config = config("[projectile_main]\n"
                    + "mutatorArmour_ifCondition: memory.enabled\n"
                    + "mutatorArmour_nativeField: untouched\n");
            IniExtensionRuntime.index(config);
            IniExtensionRuntime.markActiveFieldsRead(config);
            IniExtensionRuntime.applyAfterStaticVariables(new Object(), config);
            require("mutatorArmour_ifCondition".equals(appliedKey[0]),
                    "prefix key definition did not apply the activated concrete key");
            require(IniExtensionRuntime.nativeFallback(
                    config, "projectile_main", "mutatorArmour_nativeField") == null,
                    "inactive sibling prefix key was intercepted");
        }
    }

    private static void verifySectionDefinitionConsumesOnlyClaimedCompanionKeys() {
        IniFieldDefinition<String> definition = IniFieldDefinition
                .<String>builder("test_mod", "geometry_definition",
                        IniSectionSelector.prefix("geometry_"), "type")
                .applicationPhase(IniApplicationPhase.AFTER_METADATA_PARSED)
                .claimsKeys("radius", "sweepAngle")
                .decoder(context -> context.rawValue())
                .build();
        try (IniExtensions.Registration ignored = IniExtensions.register(definition)) {
            UnitConfig valid = config("[geometry_light]\n"
                    + "type: sector\nradius: 240\nsweepAngle: 30\n");
            IniExtensionRuntime.markActiveFieldsRead(valid);
            valid.checkUnusedKeys();

            UnitConfig invalid = config("[geometry_light]\n"
                    + "type: sector\nradius: 240\nmisspelledRadius: 30\n");
            IniExtensionRuntime.markActiveFieldsRead(invalid);
            boolean rejected = false;
            try {
                invalid.checkUnusedKeys();
            } catch (RuntimeException expected) {
                rejected = expected.getMessage().contains("misspelledRadius");
            }
            require(rejected, "section definition hid an unclaimed companion key");
        }
    }

    private static IniFieldDefinition<Integer> extendedNumber() {
        return IniFieldDefinition.<Integer>builder("test_mod", "hp_suffix",
                        IniSectionSelector.exact("core"), "maxHp")
                .kind(IniExtensionKind.EXTENDED_FORMAT)
                .activatesWhen(context -> context.rawValue().endsWith("hp"))
                .decoder(context -> Integer.valueOf(
                        context.rawValue().substring(0, context.rawValue().length() - 2)))
                .nativeFallback((context, value) -> value.toString())
                .build();
    }

    private static UnitConfig config(String text) {
        return new UnitConfig(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)),
                "ini-extension-contract.ini");
    }

    private static Boolean parseBoolean(String value) {
        if ("true".equalsIgnoreCase(value)) return Boolean.TRUE;
        if ("false".equalsIgnoreCase(value)) return Boolean.FALSE;
        throw new IllegalArgumentException("expected true or false");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
