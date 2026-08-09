package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.RustedFabricAPIContext;
import io.github.endx.rustedfabricapi.api.RustedFabricRuntime;
import io.github.endx.rustedfabricapi.api.event.RuntimeLifecycleEvents;
import io.github.endx.rustedfabricapi.api.session.GameSession;
import io.github.endx.rustedfabricapi.api.session.GameSessionRuntime;
import io.github.endx.rustedfabricapi.api.event.MultiplayerCompatibilityEvents;
import io.github.endx.rustedfabricapi.api.multiplayer.MultiplayerRequirements;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicBoolean;

/** Fabric bridge for Loader initialization events on every supported host platform. */
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
            context.multiplayerManifest().map(MultiplayerRequirements::effective).ifPresent(
                    MultiplayerCompatibilityEvents.LOCAL_MANIFEST_READY::dispatch);
        }
        if (rustedfabricapi$engineInitializationStarted.compareAndSet(false, true)) {
            if (context != null) {
                RuntimeLifecycleEvents.BEFORE_ENGINE_INITIALIZATION.dispatch(context);
            }
            io.github.endx.rustedfabricapi.api.client.event.ClientLifecycleEvents.BEFORE_ENGINE_INITIALIZATION
                    .invoker().onEngineInitialization((rustedwarfare.core.GameEngine) (Object) this);
        }
    }

    @Inject(method = "init(Landroid/content/Context;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterEngineInitialization(@Coerce Object androidContext,
                                                           CallbackInfo callback) {
        RustedFabricAPIContext context = RustedFabricRuntime.currentContext().orElse(null);
        if (rustedfabricapi$engineInitializationCompleted.compareAndSet(false, true)) {
            if (context != null) {
                RuntimeLifecycleEvents.AFTER_ENGINE_INITIALIZATION.dispatch(context);
            }
            io.github.endx.rustedfabricapi.api.client.event.ClientLifecycleEvents.AFTER_ENGINE_INITIALIZATION
                    .invoker().onEngineInitialization((rustedwarfare.core.GameEngine) (Object) this);
        }
        if (context != null && rustedfabricapi$gameReady.compareAndSet(false, true)) {
            RuntimeLifecycleEvents.GAME_READY.dispatch(context);
            GameSessionRuntime.transition(GameSession.Kind.SINGLE_PLAYER);
        }
    }
}
