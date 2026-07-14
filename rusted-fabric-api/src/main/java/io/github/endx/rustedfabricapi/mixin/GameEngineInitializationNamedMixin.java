package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.RustedFabricAPIContext;
import io.github.endx.rustedfabricapi.api.RustedFabricRuntime;
import io.github.endx.rustedfabricapi.api.event.RuntimeLifecycleEvents;
import io.github.endx.rustedfabricapi.api.event.MultiplayerCompatibilityEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicBoolean;

/** Windows Fabric bridge for the same portable initialization events used by Android Xposed. */
@Mixin(targets = "rustedwarfare.core.RustedWarfareGameEngine", remap = false)
public abstract class GameEngineInitializationNamedMixin {
    @Unique
    private static final AtomicBoolean rustedfabricapi$engineInitializationStarted =
            new AtomicBoolean();
    @Unique
    private static final AtomicBoolean rustedfabricapi$engineInitializationCompleted =
            new AtomicBoolean();
    @Unique
    private static final AtomicBoolean rustedfabricapi$loaderReady = new AtomicBoolean();
    @Unique
    private static final AtomicBoolean rustedfabricapi$gameReady = new AtomicBoolean();

    @Inject(method = "init(Landroid/content/Context;)V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeEngineInitialization(@Coerce Object androidContext,
                                                            CallbackInfo callback) {
        RustedFabricAPIContext context = RustedFabricRuntime.currentContext().orElse(null);
        if (context != null && rustedfabricapi$loaderReady.compareAndSet(false, true)) {
            RuntimeLifecycleEvents.LOADER_READY.dispatch(context);
            context.multiplayerManifest().ifPresent(
                    MultiplayerCompatibilityEvents.LOCAL_MANIFEST_READY::dispatch);
        }
        if (context != null && rustedfabricapi$engineInitializationStarted.compareAndSet(false, true)) {
            RuntimeLifecycleEvents.BEFORE_ENGINE_INITIALIZATION.dispatch(context);
        }
    }

    @Inject(method = "init(Landroid/content/Context;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterEngineInitialization(@Coerce Object androidContext,
                                                           CallbackInfo callback) {
        RustedFabricAPIContext context = RustedFabricRuntime.currentContext().orElse(null);
        if (context != null && rustedfabricapi$engineInitializationCompleted.compareAndSet(false, true)) {
            RuntimeLifecycleEvents.AFTER_ENGINE_INITIALIZATION.dispatch(context);
        }
        if (context != null && rustedfabricapi$gameReady.compareAndSet(false, true)) {
            RuntimeLifecycleEvents.GAME_READY.dispatch(context);
        }
    }
}
