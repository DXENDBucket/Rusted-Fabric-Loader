package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.CoreDebugStatsEvents;
import io.github.endx.rustedfabricapi.api.stats.event.StatisticsEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rustedwarfare.stats.StatsEventDispatcher;
import rustedwarfare.unit.Unit;

@Mixin(targets = "rustedwarfare.stats.StatsEventDispatcher", remap = false)
public abstract class StatsEventDispatcherNamedMixin {
    @Inject(method = "notifyUnitKilled(Lrustedwarfare/unit/Unit;Lrustedwarfare/unit/Unit;)V",
            at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeNotifyUnitKilled(
            @Coerce Object killedUnit, @Coerce Object attackerUnit, CallbackInfo ci) {
        CoreDebugStatsEvents.BEFORE_NOTIFY_UNIT_KILLED.invoker()
                .beforeStatsUnitKilledNotification(this, killedUnit, attackerUnit);
        StatisticsEvents.BEFORE_UNIT_KILLED.invoker().onUnitKilled(
                (StatsEventDispatcher) (Object) this, (Unit) killedUnit, (Unit) attackerUnit);
    }

    @Inject(method = "notifyUnitKilled(Lrustedwarfare/unit/Unit;Lrustedwarfare/unit/Unit;)V",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterNotifyUnitKilled(
            @Coerce Object killedUnit, @Coerce Object attackerUnit, CallbackInfo ci) {
        CoreDebugStatsEvents.AFTER_NOTIFY_UNIT_KILLED.invoker()
                .afterStatsUnitKilledNotification(this, killedUnit, attackerUnit);
        StatisticsEvents.AFTER_UNIT_KILLED.invoker().onUnitKilled(
                (StatsEventDispatcher) (Object) this, (Unit) killedUnit, (Unit) attackerUnit);
    }
}
