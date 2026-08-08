package io.github.endx.rustedfabricapi.mixin;

import java.util.HashSet;
import java.util.OptionalInt;
import java.util.Set;

import io.github.endx.rustedfabricapi.api.client.input.ClientInputEvents;
import io.github.endx.rustedfabricapi.api.client.input.InputKeys;
import io.github.endx.rustedfabricapi.api.client.input.InputModifiers;
import io.github.endx.rustedfabricapi.api.client.input.KeyboardAction;
import io.github.endx.rustedfabricapi.api.client.input.KeyboardInput;
import io.github.endx.rustedfabricapi.api.client.input.MouseButton;
import io.github.endx.rustedfabricapi.api.client.input.PointerAction;
import io.github.endx.rustedfabricapi.api.client.input.PointerInput;
import io.github.endx.rustedfabricapi.api.world.WorldPoint;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rustedwarfare.core.GameEngine;

@Mixin(targets = "rustedwarfare.client.SlickGame", remap = false)
public abstract class SlickInputNamedMixin {
    @Shadow private float inputScale;
    @Shadow private float pointerX;
    @Shadow private float pointerY;
    @Shadow abstract boolean isLibRocketInputActive();

    @Unique private float rustedfabricapi$pointerBeforeX;
    @Unique private float rustedfabricapi$pointerBeforeY;
    @Unique private boolean rustedfabricapi$uiActiveBefore;
    @Unique private boolean rustedfabricapi$keyRepeated;
    @Unique private Set<Integer> rustedfabricapi$pressedKeys;

    @Inject(method = "keyPressed(IC)V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeKeyPressed(int keyCode, char character, CallbackInfo ci) {
        rustedfabricapi$uiActiveBefore = isLibRocketInputActive();
        if (rustedfabricapi$pressedKeys == null) {
            rustedfabricapi$pressedKeys = new HashSet<Integer>();
        }
        rustedfabricapi$keyRepeated = !rustedfabricapi$pressedKeys.add(Integer.valueOf(keyCode));
    }

    @Inject(method = "keyPressed(IC)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterKeyPressed(int keyCode, char character, CallbackInfo ci) {
        ClientInputEvents.KEY_PRESSED.invoker().onKeyboardInput(
                rustedfabricapi$keyboard(KeyboardAction.PRESS, keyCode, character,
                        rustedfabricapi$keyRepeated));
    }

    @Inject(method = "keyReleased(IC)V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeKeyReleased(int keyCode, char character, CallbackInfo ci) {
        rustedfabricapi$uiActiveBefore = isLibRocketInputActive();
    }

    @Inject(method = "keyReleased(IC)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterKeyReleased(int keyCode, char character, CallbackInfo ci) {
        if (rustedfabricapi$pressedKeys != null) {
            rustedfabricapi$pressedKeys.remove(Integer.valueOf(keyCode));
        }
        ClientInputEvents.KEY_RELEASED.invoker().onKeyboardInput(
                rustedfabricapi$keyboard(KeyboardAction.RELEASE, keyCode, character, false));
    }

    @Inject(method = "mousePressed(III)V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeMousePressed(int button, int x, int y, CallbackInfo ci) {
        rustedfabricapi$capturePointerBefore();
    }

    @Inject(method = "mousePressed(III)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterMousePressed(int button, int x, int y, CallbackInfo ci) {
        ClientInputEvents.MOUSE_PRESSED.invoker().onPointerInput(
                rustedfabricapi$pointer(PointerAction.PRESS, button, x, y, 0));
    }

    @Inject(method = "mouseReleased(III)V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeMouseReleased(int button, int x, int y, CallbackInfo ci) {
        rustedfabricapi$capturePointerBefore();
    }

    @Inject(method = "mouseReleased(III)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterMouseReleased(int button, int x, int y, CallbackInfo ci) {
        ClientInputEvents.MOUSE_RELEASED.invoker().onPointerInput(
                rustedfabricapi$pointer(PointerAction.RELEASE, button, x, y, 0));
    }

    @Inject(method = "mouseMoved(IIII)V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeMouseMoved(int oldX, int oldY, int newX, int newY,
                                                  CallbackInfo ci) {
        rustedfabricapi$capturePointerBefore();
    }

    @Inject(method = "mouseMoved(IIII)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterMouseMoved(int oldX, int oldY, int newX, int newY,
                                                 CallbackInfo ci) {
        ClientInputEvents.MOUSE_MOVED.invoker().onPointerInput(
                rustedfabricapi$pointer(PointerAction.MOVE, -1, newX, newY, 0));
    }

    @Inject(method = "mouseDragged(IIII)V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeMouseDragged(int oldX, int oldY, int newX, int newY,
                                                    CallbackInfo ci) {
        rustedfabricapi$capturePointerBefore();
    }

    @Inject(method = "mouseDragged(IIII)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterMouseDragged(int oldX, int oldY, int newX, int newY,
                                                   CallbackInfo ci) {
        ClientInputEvents.MOUSE_DRAGGED.invoker().onPointerInput(
                rustedfabricapi$pointer(PointerAction.DRAG, -1, newX, newY, 0));
    }

    @Inject(method = "mouseWheelMoved(I)V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeMouseWheelMoved(int delta, CallbackInfo ci) {
        rustedfabricapi$capturePointerBefore();
    }

    @Inject(method = "mouseWheelMoved(I)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterMouseWheelMoved(int delta, CallbackInfo ci) {
        int rawX = Math.round(pointerX * rustedfabricapi$safeInputScale());
        int rawY = Math.round(pointerY * rustedfabricapi$safeInputScale());
        ClientInputEvents.MOUSE_SCROLLED.invoker().onPointerInput(
                rustedfabricapi$pointer(PointerAction.SCROLL, -1, rawX, rawY, delta));
    }

    @Unique
    private void rustedfabricapi$capturePointerBefore() {
        rustedfabricapi$pointerBeforeX = pointerX;
        rustedfabricapi$pointerBeforeY = pointerY;
        rustedfabricapi$uiActiveBefore = isLibRocketInputActive();
    }

    @Unique
    private KeyboardInput rustedfabricapi$keyboard(KeyboardAction action, int desktopKeyCode,
                                                    char character, boolean repeated) {
        OptionalInt translated = InputKeys.toGameKeyCode(desktopKeyCode);
        return new KeyboardInput(action, desktopKeyCode,
                translated.isPresent() ? translated.getAsInt() : -1,
                character, rustedfabricapi$modifiers(), repeated,
                rustedfabricapi$uiActiveBefore);
    }

    @Unique
    private PointerInput rustedfabricapi$pointer(PointerAction action, int button,
                                                  int rawX, int rawY, int wheelDelta) {
        GameEngine engine = GameEngine.getInstance();
        WorldPoint world = null;
        boolean insideWorld = false;
        if (engine != null && engine.hasLoadedLevel && engine.zoom > 0.0F
                && Float.isFinite(engine.zoom)) {
            world = new WorldPoint(engine.viewpointXSnapped + pointerX / engine.zoom,
                    engine.viewpointYSnapped + pointerY / engine.zoom);
            insideWorld = pointerX >= 0.0F && pointerY >= 0.0F
                    && pointerX <= engine.visibleWorldWidth * engine.zoom
                    && pointerY <= engine.visibleWorldHeight * engine.zoom;
        }
        return new PointerInput(action, MouseButton.fromDesktopCode(button), button,
                rawX, rawY, pointerX, pointerY,
                pointerX - rustedfabricapi$pointerBeforeX,
                pointerY - rustedfabricapi$pointerBeforeY,
                wheelDelta, rustedfabricapi$modifiers(), rustedfabricapi$uiActiveBefore,
                world, insideWorld);
    }

    @Unique
    private InputModifiers rustedfabricapi$modifiers() {
        GameEngine engine = GameEngine.getInstance();
        return engine != null ? InputModifiers.fromMask(engine.getModifierKeyMask())
                : InputModifiers.NONE;
    }

    @Unique
    private float rustedfabricapi$safeInputScale() {
        return inputScale > 0.0F && Float.isFinite(inputScale) ? inputScale : 1.0F;
    }
}
