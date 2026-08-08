package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.unit.status.StatusEffects;
import io.github.endx.rustedfabricapi.api.unit.status.event.StatusEffectEvents;
import io.github.endx.rustedfabricapi.api.client.RustedWarfareClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rustedwarfare.unit.OrderableUnit;
import rustedwarfare.unit.status.UnitStatusEffect;

import java.util.Collections;
import java.util.List;

@Mixin(targets = "rustedwarfare.unit.status.UnitStatusEffectManager", remap = false)
public abstract class UnitStatusEffectManagerNamedMixin {
    @Unique
    private static final ThreadLocal<List<UnitStatusEffect>> rustedfabricapi$beforeUpdate =
            new ThreadLocal<List<UnitStatusEffect>>();

    @Inject(
            method = "addStatusEffect(Lrustedwarfare/unit/OrderableUnit;Lrustedwarfare/unit/status/UnitStatusEffect;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 1
    )
    private static void rustedfabricapi$beforeAdd(@Coerce Object unit, @Coerce Object effect,
                                                  CallbackInfo ci) {
        if (StatusEffectEvents.BEFORE_ADD.invoker().beforeAdd(
                (OrderableUnit) unit, (UnitStatusEffect) effect)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "addStatusEffect(Lrustedwarfare/unit/OrderableUnit;Lrustedwarfare/unit/status/UnitStatusEffect;)V",
            at = @At("RETURN"),
            require = 1
    )
    private static void rustedfabricapi$afterAdd(@Coerce Object unit, @Coerce Object effect,
                                                 CallbackInfo ci) {
        OrderableUnit typedUnit = (OrderableUnit) unit;
        UnitStatusEffect typedEffect = (UnitStatusEffect) effect;
        StatusEffectEvents.AFTER_ADD.invoker().afterAdd(
                typedUnit, typedEffect, StatusEffects.contains(typedUnit, typedEffect));
    }

    @Inject(
            method = "updateStatusEffects(Lrustedwarfare/unit/OrderableUnit;F)V",
            at = @At("HEAD"),
            require = 1
    )
    private static void rustedfabricapi$beforeUpdate(@Coerce Object unit, float delta,
                                                     CallbackInfo ci) {
        OrderableUnit typedUnit = (OrderableUnit) unit;
        List<UnitStatusEffect> before = StatusEffects.active(typedUnit);
        rustedfabricapi$beforeUpdate.set(before);
        StatusEffectEvents.BEFORE_UPDATE.invoker().beforeUpdate(typedUnit, delta, before);
    }

    @Inject(
            method = "updateStatusEffects(Lrustedwarfare/unit/OrderableUnit;F)V",
            at = @At("RETURN"),
            require = 1
    )
    private static void rustedfabricapi$afterUpdate(@Coerce Object unit, float delta,
                                                    CallbackInfo ci) {
        OrderableUnit typedUnit = (OrderableUnit) unit;
        List<UnitStatusEffect> before = rustedfabricapi$beforeUpdate.get();
        rustedfabricapi$beforeUpdate.remove();
        if (before == null) before = Collections.emptyList();
        List<UnitStatusEffect> after = StatusEffects.active(typedUnit);
        int currentTime = RustedWarfareClient.requireEngine().gameTimeMillis;
        for (UnitStatusEffect effect : before) {
            if (!after.contains(effect) && effect.getExpireFrame() <= currentTime) {
                StatusEffectEvents.EXPIRED.invoker().onExpired(typedUnit, effect);
            }
        }
        StatusEffectEvents.AFTER_UPDATE.invoker().afterUpdate(typedUnit, delta, after);
    }
}
