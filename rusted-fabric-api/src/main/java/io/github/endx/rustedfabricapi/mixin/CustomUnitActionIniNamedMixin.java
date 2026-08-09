package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.impl.ini.IniActionEffectRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(targets = "rustedwarfare.custom.CustomUnitLoader", remap = false)
public abstract class CustomUnitActionIniNamedMixin {
    @ModifyArgs(
            method = "parseCustomActionSection(Lrustedwarfare/custom/CustomUnitMetadata;Lrustedwarfare/util/UnitConfig;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZZ)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lrustedwarfare/custom/action/effect/MessageActionEffect;parseAndAddMessageEffects(Lrustedwarfare/custom/CustomUnitMetadata;Lrustedwarfare/util/UnitConfig;Ljava/lang/String;Ljava/lang/String;Lrustedwarfare/custom/action/CustomActionConfig;Ljava/lang/String;Z)V"
            ),
            require = 1
    )
    private static void rustedfabricapi$parseJavaActionEffects(Args args) {
        IniActionEffectRuntime.parseAndAttach(
                args.get(0), args.get(1), (String) args.get(2), args.get(4),
                (String) args.get(5), (Boolean) args.get(6));
    }
}
