package io.github.endx.rustedfabricapi.api.ini.action;

import java.util.List;

public final class ActionEffectOrderContractVerification {
    private ActionEffectOrderContractVerification() { }

    public static void verify() {
        IniActionEffects.Registration normal = IniActionEffects.register(definition(
                "contract_normal", "contractNormal", 0));
        IniActionEffects.Registration early = IniActionEffects.register(definition(
                "contract_early", "contractEarly", 100));
        try {
            List<IniActionEffectDefinition<?>> definitions = IniActionEffects.definitions();
            int earlyIndex = definitions.indexOf(early.definition());
            int normalIndex = definitions.indexOf(normal.definition());
            require(earlyIndex >= 0 && normalIndex >= 0 && earlyIndex < normalIndex,
                    "higher-priority action effect was not ordered first");
        } finally {
            early.close();
            normal.close();
        }
    }

    private static IniActionEffectDefinition<String> definition(
            String fieldId, String key, int priority) {
        return IniActionEffectDefinition.<String>builder("contract", fieldId, key)
                .priority(priority)
                .decoder(context -> context.rawValue())
                .handler((context, value) -> { })
                .build();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
