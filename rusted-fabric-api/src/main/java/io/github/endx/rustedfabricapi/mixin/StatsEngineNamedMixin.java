package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.CoreDebugStatsEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "rustedwarfare.stats.StatsEngine", remap = false)
public abstract class StatsEngineNamedMixin {
    @Inject(method = "reset()V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeStatsEngineReset(CallbackInfo ci) {
        if (CoreDebugStatsEvents.BEFORE_STATS_ENGINE_RESET.invoker().beforeStatsEngineLifecycle(this)) {
            ci.cancel();
        }
    }

    @Inject(method = "reset()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterStatsEngineReset(CallbackInfo ci) {
        CoreDebugStatsEvents.AFTER_STATS_ENGINE_RESET.invoker().afterStatsEngineLifecycle(this);
    }

    @Inject(method = "recordPeriodicStatsSnapshot()V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforePeriodicStatsSnapshot(CallbackInfo ci) {
        if (CoreDebugStatsEvents.BEFORE_PERIODIC_STATS_SNAPSHOT.invoker().beforeStatsEngineLifecycle(this)) {
            ci.cancel();
        }
    }

    @Inject(method = "recordPeriodicStatsSnapshot()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterPeriodicStatsSnapshot(CallbackInfo ci) {
        CoreDebugStatsEvents.AFTER_PERIODIC_STATS_SNAPSHOT.invoker().afterStatsEngineLifecycle(this);
    }

    @Inject(method = "finalizeStatsHistory()V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeFinalizeStatsHistory(CallbackInfo ci) {
        if (CoreDebugStatsEvents.BEFORE_FINALIZE_STATS_HISTORY.invoker().beforeStatsEngineLifecycle(this)) {
            ci.cancel();
        }
    }

    @Inject(method = "finalizeStatsHistory()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterFinalizeStatsHistory(CallbackInfo ci) {
        CoreDebugStatsEvents.AFTER_FINALIZE_STATS_HISTORY.invoker().afterStatsEngineLifecycle(this);
    }

    @Inject(method = "recordStatsHistorySnapshot(IZZ)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeStatsHistorySnapshot(int frame, boolean flagA, boolean flagB, CallbackInfo ci) {
        if (CoreDebugStatsEvents.BEFORE_STATS_HISTORY_SNAPSHOT.invoker()
                .beforeStatsHistorySnapshot(this, frame, flagA, flagB)) {
            ci.cancel();
        }
    }

    @Inject(method = "recordStatsHistorySnapshot(IZZ)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterStatsHistorySnapshot(int frame, boolean flagA, boolean flagB, CallbackInfo ci) {
        CoreDebugStatsEvents.AFTER_STATS_HISTORY_SNAPSHOT.invoker()
                .afterStatsHistorySnapshot(this, frame, flagA, flagB);
    }
}
