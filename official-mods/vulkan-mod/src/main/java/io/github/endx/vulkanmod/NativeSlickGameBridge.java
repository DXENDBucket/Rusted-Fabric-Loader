package io.github.endx.vulkanmod;

import org.newdawn.slick.GameContainer;

/** Named-game bridge whose field and method references are remapped with the game jar. */
public interface NativeSlickGameBridge {
    void vulkanmod$bindNativeContainer(GameContainer container);
    void vulkanmod$startNativeGameSystems();
    void vulkanmod$runNativeFrame(int deltaMillis);
}
