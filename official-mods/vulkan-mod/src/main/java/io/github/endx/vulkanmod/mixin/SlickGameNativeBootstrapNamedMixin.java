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
import rustedwarfare.client.DesktopAppFramework;
import rustedwarfare.client.render.GameImage;
import io.github.endx.vulkanmod.spi.VulkanInputEvent;
import org.lwjgl.input.Keyboard;
import com.corrodinggames.rts.R$drawable;
import org.spongepowered.asm.mixin.Unique;

/** Restores game-system initialization without entering SlickGame's OpenGL setup method. */
@Mixin(SlickGame.class)
public abstract class SlickGameNativeBootstrapNamedMixin implements NativeSlickGameBridge {
    @Shadow GameContainer gameContainer;
    @Shadow RustedWarfareMain main;
    @Shadow GameEngine gameEngine;
    @Shadow DesktopAppFramework appFramework;
    @Shadow int lastDeltaMs;
    @Shadow boolean finishedInitialLoad;
    @Shadow public abstract void startLoadingThreaded();
    @Unique private int vulkanmod$pointerX;
    @Unique private int vulkanmod$pointerY;
    @Unique private final boolean[] vulkanmod$buttons = new boolean[3];
    @Unique private final java.util.HashSet<Integer> vulkanmod$keys =
            new java.util.HashSet<Integer>();
    @Unique private GameImage vulkanmod$pointerImage;
    @Unique private boolean vulkanmod$nativeInputReady;

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
        vulkanmod$ensureNativePointer();
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
            if (!vulkanmod$nativeInputReady && finishedInitialLoad
                    && ui.getActiveDocument() != null) {
                // Win32 begins queuing pointer motion as soon as its HWND is visible, while
                // LibRocket's Java renderer can already exist before its native context and first
                // document are ready. Calling processMouseMove in that interval dereferences a
                // null native context. An active document proves setup/loadDocument completed.
                vulkanmod$nativeInputReady = true;
                System.out.println("[Vulkan Mod] Native input enabled after the first UI document");
            }
        }
        // Slick installs this image as the native cursor during its skipped OpenGL init. Draw the
        // same asset last in native mode so menus and the game retain Rusted Warfare's pointer.
        if (vulkanmod$pointerImage != null) {
            VulkanRuntime.drawNativePointer(
                    vulkanmod$pointerImage, vulkanmod$pointerX, vulkanmod$pointerY);
        }
        lastDeltaMs = 0;
    }

    @Unique
    private void vulkanmod$ensureNativePointer() {
        if (vulkanmod$pointerImage != null || VulkanRuntime.nativeGraphicsEngine() == null) return;
        try {
            vulkanmod$pointerImage = VulkanRuntime.nativeGraphicsEngine()
                    .a(R$drawable.pointer, true);
            System.out.println("[Vulkan Mod] Native pointer loaded: "
                    + vulkanmod$pointerImage.getWidth() + "x"
                    + vulkanmod$pointerImage.getHeight());
        } catch (RuntimeException failure) {
            System.out.println("[Vulkan Mod] Could not load the native pointer image: "
                    + failure.getClass().getSimpleName() + ": " + failure.getMessage());
        }
    }

    @Override
    public void vulkanmod$handleNativeInput(VulkanInputEvent event) {
        if (event == null) return;
        if (!vulkanmod$nativeInputReady) {
            // Retain the latest pointer position so the software cursor does not jump when input
            // becomes active, but never enter Slick/LibRocket callbacks during native startup.
            switch (event.type()) {
                case POINTER_MOVE:
                case BUTTON_DOWN:
                case BUTTON_UP:
                case WHEEL:
                    vulkanmod$pointerX = event.x();
                    vulkanmod$pointerY = event.y();
                    break;
                case FOCUS_LOST:
                    vulkanmod$clearNativeInputState();
                    break;
                default:
                    break;
            }
            return;
        }
        switch (event.type()) {
            case POINTER_MOVE: {
                int oldX = vulkanmod$pointerX;
                int oldY = vulkanmod$pointerY;
                vulkanmod$pointerX = event.x();
                vulkanmod$pointerY = event.y();
                if (vulkanmod$anyButtonDown()) {
                    vulkanmod$self().mouseDragged(
                            oldX, oldY, vulkanmod$pointerX, vulkanmod$pointerY);
                } else {
                    vulkanmod$self().mouseMoved(
                            oldX, oldY, vulkanmod$pointerX, vulkanmod$pointerY);
                }
                break;
            }
            case BUTTON_DOWN:
                vulkanmod$pointerX = event.x();
                vulkanmod$pointerY = event.y();
                if (event.code() >= 0 && event.code() < vulkanmod$buttons.length) {
                    vulkanmod$buttons[event.code()] = true;
                }
                vulkanmod$self().mousePressed(event.code(), event.x(), event.y());
                break;
            case BUTTON_UP:
                vulkanmod$pointerX = event.x();
                vulkanmod$pointerY = event.y();
                vulkanmod$self().mouseReleased(event.code(), event.x(), event.y());
                if (event.code() >= 0 && event.code() < vulkanmod$buttons.length) {
                    vulkanmod$buttons[event.code()] = false;
                }
                break;
            case WHEEL:
                vulkanmod$pointerX = event.x();
                vulkanmod$pointerY = event.y();
                vulkanmod$self().mouseWheelMoved(event.value());
                break;
            case KEY_DOWN: {
                int slickKey = vulkanmod$toSlickKey(event.code());
                if (slickKey != Keyboard.KEY_NONE && vulkanmod$keys.add(slickKey)) {
                    vulkanmod$self().keyPressed(slickKey, '\0');
                }
                break;
            }
            case KEY_UP: {
                int slickKey = vulkanmod$toSlickKey(event.code());
                if (slickKey != Keyboard.KEY_NONE) {
                    vulkanmod$keys.remove(slickKey);
                    vulkanmod$self().keyReleased(slickKey, '\0');
                }
                break;
            }
            case CHARACTER:
                if (!Character.isISOControl(event.character())) {
                    vulkanmod$self().keyPressed(Keyboard.KEY_NONE, event.character());
                    vulkanmod$self().keyReleased(Keyboard.KEY_NONE, event.character());
                }
                break;
            case FOCUS_LOST:
                vulkanmod$releaseAllInput();
                break;
            default:
                break;
        }
    }

    @Override
    public void vulkanmod$syncNativeResolution(int width, int height) {
        int nativeWidth = Math.max(1, width);
        int nativeHeight = Math.max(1, height);
        if (appFramework != null) {
            appFramework.width = nativeWidth;
            appFramework.height = nativeHeight;
        }
        if (gameEngine != null) {
            gameEngine.updateWindowResolution(nativeWidth, nativeHeight);
            gameEngine.refreshPaintSizesIfScaleChanged();
        }
        if (main != null) {
            LibRocketSlickRenderer ui = ((RustedWarfareMainUiAccessor) (Object) main)
                    .vulkanmod$getLibRocketRenderer();
            if (ui != null) ui.setDimensionsWrap(nativeWidth, nativeHeight);
        }
    }

    @Unique
    private boolean vulkanmod$anyButtonDown() {
        for (boolean down : vulkanmod$buttons) if (down) return true;
        return false;
    }

    @Unique
    private void vulkanmod$releaseAllInput() {
        for (int button = 0; button < vulkanmod$buttons.length; button++) {
            if (vulkanmod$buttons[button]) {
                vulkanmod$self().mouseReleased(
                        button, vulkanmod$pointerX, vulkanmod$pointerY);
                vulkanmod$buttons[button] = false;
            }
        }
        for (int key : new java.util.ArrayList<Integer>(vulkanmod$keys)) {
            vulkanmod$self().keyReleased(key, '\0');
        }
        vulkanmod$keys.clear();
    }

    @Unique
    private void vulkanmod$clearNativeInputState() {
        java.util.Arrays.fill(vulkanmod$buttons, false);
        vulkanmod$keys.clear();
    }

    @Unique
    private SlickGame vulkanmod$self() {
        return (SlickGame) (Object) this;
    }

    @Unique
    private static int vulkanmod$toSlickKey(int virtualKey) {
        if (virtualKey >= 'A' && virtualKey <= 'Z') {
            final int[] letters = {
                    Keyboard.KEY_A, Keyboard.KEY_B, Keyboard.KEY_C, Keyboard.KEY_D,
                    Keyboard.KEY_E, Keyboard.KEY_F, Keyboard.KEY_G, Keyboard.KEY_H,
                    Keyboard.KEY_I, Keyboard.KEY_J, Keyboard.KEY_K, Keyboard.KEY_L,
                    Keyboard.KEY_M, Keyboard.KEY_N, Keyboard.KEY_O, Keyboard.KEY_P,
                    Keyboard.KEY_Q, Keyboard.KEY_R, Keyboard.KEY_S, Keyboard.KEY_T,
                    Keyboard.KEY_U, Keyboard.KEY_V, Keyboard.KEY_W, Keyboard.KEY_X,
                    Keyboard.KEY_Y, Keyboard.KEY_Z
            };
            return letters[virtualKey - 'A'];
        }
        if (virtualKey >= '0' && virtualKey <= '9') {
            final int[] digits = {
                    Keyboard.KEY_0, Keyboard.KEY_1, Keyboard.KEY_2, Keyboard.KEY_3,
                    Keyboard.KEY_4, Keyboard.KEY_5, Keyboard.KEY_6, Keyboard.KEY_7,
                    Keyboard.KEY_8, Keyboard.KEY_9
            };
            return digits[virtualKey - '0'];
        }
        if (virtualKey >= 0x70 && virtualKey <= 0x79) {
            return Keyboard.KEY_F1 + virtualKey - 0x70;
        }
        switch (virtualKey) {
            case 0x08: return Keyboard.KEY_BACK;
            case 0x09: return Keyboard.KEY_TAB;
            case 0x0D: return Keyboard.KEY_RETURN;
            case 0x10: return Keyboard.KEY_LSHIFT;
            case 0x11: return Keyboard.KEY_LCONTROL;
            case 0x12: return Keyboard.KEY_LMENU;
            case 0x13: return Keyboard.KEY_PAUSE;
            case 0x14: return Keyboard.KEY_CAPITAL;
            case 0x1B: return Keyboard.KEY_ESCAPE;
            case 0x20: return Keyboard.KEY_SPACE;
            case 0x21: return Keyboard.KEY_PRIOR;
            case 0x22: return Keyboard.KEY_NEXT;
            case 0x23: return Keyboard.KEY_END;
            case 0x24: return Keyboard.KEY_HOME;
            case 0x25: return Keyboard.KEY_LEFT;
            case 0x26: return Keyboard.KEY_UP;
            case 0x27: return Keyboard.KEY_RIGHT;
            case 0x28: return Keyboard.KEY_DOWN;
            case 0x2D: return Keyboard.KEY_INSERT;
            case 0x2E: return Keyboard.KEY_DELETE;
            case 0x7A: return Keyboard.KEY_F11;
            case 0x7B: return Keyboard.KEY_F12;
            default: return Keyboard.KEY_NONE;
        }
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
