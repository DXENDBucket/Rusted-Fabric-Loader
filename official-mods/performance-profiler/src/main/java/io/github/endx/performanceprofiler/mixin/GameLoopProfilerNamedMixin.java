package io.github.endx.performanceprofiler.mixin;

import io.github.endx.performanceprofiler.PerformanceProfiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rustedwarfare.core.RustedWarfareGameEngine;

/** Measures the same mapped game-loop boundary for both Slick/OpenGL and native Vulkan. */
@Mixin(RustedWarfareGameEngine.class)
public abstract class GameLoopProfilerNamedMixin {
    @Inject(method = "gameLoop(FI)V", at = @At("HEAD"), require = 1)
    private void performanceprofiler$frameStarted(float delta, int deltaMillis,
                                                   CallbackInfo callback) {
        PerformanceProfiler.frameStarted();
    }

    @Inject(method = "gameLoop(FI)V", at = @At("RETURN"), require = 1)
    private void performanceprofiler$frameFinished(float delta, int deltaMillis,
                                                    CallbackInfo callback) {
        PerformanceProfiler.frameFinished();
    }
}
