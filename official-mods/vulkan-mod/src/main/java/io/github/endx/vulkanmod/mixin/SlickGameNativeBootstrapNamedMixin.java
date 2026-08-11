package io.github.endx.vulkanmod.mixin;

import io.github.endx.vulkanmod.NativeSlickGameBridge;
import io.github.endx.vulkanmod.VulkanRuntime;
import org.newdawn.slick.GameContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rustedwarfare.client.SlickGame;
import rustedwarfare.client.RustedWarfareMain;
import rustedwarfare.core.GameEngine;
import rustedwarfare.ui.LibRocketSlickRenderer;

/** Restores game-system initialization without entering SlickGame's OpenGL setup method. */
@Mixin(SlickGame.class)
public abstract class SlickGameNativeBootstrapNamedMixin implements NativeSlickGameBridge {
    @Shadow GameContainer gameContainer;
    @Shadow RustedWarfareMain main;
    @Shadow GameEngine gameEngine;
    @Shadow int lastDeltaMs;
    @Shadow public abstract void startLoadingThreaded();

    @Override
    public void vulkanmod$bindNativeContainer(GameContainer container) {
        gameContainer = container;
    }

    @Override
    public void vulkanmod$startNativeGameSystems() {
        startLoadingThreaded();
    }

    @Override
    public void vulkanmod$runNativeFrame(int deltaMillis) {
        if (gameEngine == null) gameEngine = GameEngine.getInstance();
        if (gameEngine == null || main == null) return;
        // SlickGame.render normally performs this assignment for the duration of a GL frame.
        // In native mode the Vulkan engine is the permanent window render target instead.
        gameEngine.renderGraphicsEngine = VulkanRuntime.nativeGraphicsEngine();
        lastDeltaMs = Math.max(0, Math.min(deltaMillis, 250));
        float delta = lastDeltaMs * 0.060000002f;
        main.updateTaskQueue(delta);
        if (gameEngine.hasLoadedLevel) {
            gameEngine.gameLoop(delta, lastDeltaMs);
        } else {
            gameEngine.networkEngine.b(delta);
            gameEngine.musicManager.update(delta);
        }
        LibRocketSlickRenderer ui = ((RustedWarfareMainUiAccessor) (Object) main)
                .vulkanmod$getLibRocketRenderer();
        if (ui != null) {
            ui.scriptEngine.update(delta);
            if (!ui.isNoDocumentOrPopupActive()) {
                ui.update();
                ui.render();
                ui.scriptEngine.checkForErrors();
                ui.debug = false;
            }
            ui.postUpdate();
        }
        lastDeltaMs = 0;
    }

    @Inject(method = "a(Ljava/lang/String;Z)V", at = @At("HEAD"),
            cancellable = true, require = 1)
    private void vulkanmod$skipLegacyLoadingFrame(String status, boolean updateText,
                                                   CallbackInfo callback) {
        if (VulkanRuntime.isNativeRendererSelected()) {
            // The original method asks AppGameContainer for a Slick Graphics and performs an
            // immediate WGL swap while the game is loading. Native mode keeps loading progress in
            // the ordinary log until Vulkan's own loading UI is available.
            if (!status.startsWith("Loading units")) {
                System.out.println("[Vulkan Mod/Native Load] " + status);
            }
            callback.cancel();
        }
    }

    @Inject(method = "applyDisplayMode()V", at = @At("HEAD"),
            cancellable = true, require = 1)
    private void vulkanmod$skipLegacyDisplayMode(CallbackInfo callback) {
        if (VulkanRuntime.isNativeRendererSelected()) callback.cancel();
    }
}
