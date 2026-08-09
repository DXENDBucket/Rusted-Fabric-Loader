package io.github.endx.rustedfabricapi.verification;

import io.github.endx.rustedfabricapi.api.ini.IniExtensionException;
import io.github.endx.rustedfabricapi.api.ini.action.IniActionEffectDefinition;
import io.github.endx.rustedfabricapi.api.ini.action.IniActionEffects;
import io.github.endx.rustedfabricapi.api.ini.action.IniActionSectionScope;
import io.github.endx.rustedfabricapi.impl.ini.IniActionEffectRuntime;
import rustedwarfare.custom.action.CustomActionConfig;
import rustedwarfare.util.UnitConfig;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

final class IniActionEffectContractVerification {
    private IniActionEffectContractVerification() { }

    static void verify() {
        verifyActionFieldAttachesNativeEffect();
        verifyExclusiveFieldsFailBeforeAttachment();
        verifyScopesAndDuplicateKeys();
    }

    private static void verifyActionFieldAttachesNativeEffect() {
        final float[] decoded = {Float.NaN};
        IniActionEffectDefinition<Float> definition = IniActionEffectDefinition
                .<Float>builder("test_mod", "camera_zoom", "cameraTargetZoom")
                .decoder(context -> Float.valueOf(context.rawValue()))
                .validator((context, value) -> {
                    if (!(value > 0.0F)) throw new IllegalArgumentException("must be positive");
                })
                .handler((context, value) -> decoded[0] = value)
                .build();
        try (IniActionEffects.Registration ignored = IniActionEffects.register(definition)) {
            UnitConfig config = config("[action_focus]\ncameraTargetZoom: 1.25\n");
            CustomActionConfig action = new CustomActionConfig();
            IniActionEffectRuntime.parseAndAttach(new Object(), config,
                    "action_focus", action, "focus", true);
            config.checkUnusedKeys();
            require(action.actionEffects != null && action.actionEffects.size() == 1,
                    "registered INI action field did not attach a native effect");
            require(Float.isNaN(decoded[0]), "action handler ran while parsing the INI");
        }
    }

    private static void verifyExclusiveFieldsFailBeforeAttachment() {
        IniActionEffectDefinition<String> absolute = definition(
                "absolute", "cameraCenterAt", IniActionSectionScope.ACTION_AND_HIDDEN,
                "camera_position");
        IniActionEffectDefinition<String> relative = definition(
                "relative", "cameraCenterBy", IniActionSectionScope.ACTION_AND_HIDDEN,
                "camera_position");
        try (IniActionEffects.Registration ignoredAbsolute = IniActionEffects.register(absolute);
             IniActionEffects.Registration ignoredRelative = IniActionEffects.register(relative)) {
            UnitConfig config = config("[hiddenAction_focus]\n"
                    + "cameraCenterAt: 10,20\n"
                    + "cameraCenterBy: 1,2\n");
            CustomActionConfig action = new CustomActionConfig();
            boolean rejected = false;
            try {
                IniActionEffectRuntime.parseAndAttach(new Object(), config,
                        "hiddenAction_focus", action, "focus", true);
            } catch (IniExtensionException expected) {
                rejected = expected.getMessage().contains("camera_position")
                        && expected.getMessage().contains("cameraCenterBy");
            }
            require(rejected, "mutually exclusive camera movement fields were accepted");
            require(action.actionEffects == null || action.actionEffects.isEmpty(),
                    "partial action effects were attached before validation completed");
        }
    }

    private static void verifyScopesAndDuplicateKeys() {
        IniActionEffectDefinition<String> visible = definition(
                "visible", "sameKey", IniActionSectionScope.ACTION, null);
        IniActionEffectDefinition<String> hidden = definition(
                "hidden", "sameKey", IniActionSectionScope.HIDDEN_ACTION, null);
        try (IniActionEffects.Registration ignoredVisible = IniActionEffects.register(visible);
             IniActionEffects.Registration ignoredHidden = IniActionEffects.register(hidden)) {
            boolean rejected = false;
            try {
                IniActionEffects.register(definition("overlap", "sameKey",
                        IniActionSectionScope.ACTION_AND_HIDDEN, null));
            } catch (IllegalArgumentException expected) {
                rejected = expected.getMessage().contains("sameKey");
            }
            require(rejected, "overlapping INI action keys were registered");
        }
    }

    private static IniActionEffectDefinition<String> definition(
            String id, String key, IniActionSectionScope scope, String group) {
        IniActionEffectDefinition.Builder<String> builder = IniActionEffectDefinition
                .<String>builder("test_mod", id, key)
                .scope(scope)
                .decoder(context -> context.rawValue())
                .handler((context, value) -> { });
        if (group != null) builder.exclusiveGroup(group);
        return builder.build();
    }

    private static UnitConfig config(String text) {
        return new UnitConfig(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)),
                "ini-action-effect-contract.ini");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
