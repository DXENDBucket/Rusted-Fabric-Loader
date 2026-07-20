package io.github.endx.rustedfabricapi.api.client.input;

import java.util.concurrent.atomic.AtomicInteger;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import io.github.endx.rustedfabricapi.api.world.WorldPoint;

public final class ClientInputContractVerification {
    private ClientInputContractVerification() {
    }

    public static void verify() {
        InputModifiers modifiers = InputModifiers.fromMask(
                InputModifiers.CONTROL_MASK | InputModifiers.ALT_MASK | 0x40);
        require(modifiers.control() && modifiers.alt() && !modifiers.shift()
                        && modifiers.mask() == 5 && modifiers.contains(InputModifiers.NONE),
                "input modifier normalization drifted");

        KeyboardInput key = new KeyboardInput(KeyboardAction.PRESS, 30, 29, 'a',
                modifiers, true, false);
        require(key.action() == KeyboardAction.PRESS && key.desktopKeyCode() == 30
                        && key.gameKeyCode().orElse(-1) == 29 && key.character() == 'a'
                        && key.hasPrintableCharacter() && key.repeated()
                        && !key.userInterfaceActive(),
                "keyboard input snapshot lost a value");

        PointerInput pointer = new PointerInput(PointerAction.DRAG, MouseButton.LEFT, 0,
                200, 100, 100.0F, 50.0F, 4.0F, -2.0F, 0, modifiers, false,
                new WorldPoint(300.0F, 400.0F), true);
        require(pointer.action() == PointerAction.DRAG && pointer.button() == MouseButton.LEFT
                        && pointer.screenX() == 100.0F && pointer.screenY() == 50.0F
                        && pointer.deltaX() == 4.0F && pointer.deltaY() == -2.0F
                        && pointer.worldPosition().orElseThrow().equals(
                                new WorldPoint(300.0F, 400.0F))
                        && pointer.insideWorldViewport(),
                "pointer input snapshot lost a value");
        require(MouseButton.fromDesktopCode(0) == MouseButton.LEFT
                        && MouseButton.fromDesktopCode(1) == MouseButton.RIGHT
                        && MouseButton.fromDesktopCode(2) == MouseButton.MIDDLE
                        && MouseButton.fromDesktopCode(7) == MouseButton.OTHER
                        && MouseButton.fromDesktopCode(-1) == MouseButton.NONE,
                "desktop mouse button classification drifted");
        try {
            new PointerInput(PointerAction.MOVE, MouseButton.NONE, -1,
                    0, 0, 0.0F, 0.0F, 0.0F, 0.0F, 0, InputModifiers.NONE,
                    false, null, true);
            throw new AssertionError("inside-world pointer without a world position was accepted");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }

        AtomicInteger calls = new AtomicInteger();
        RustedFabricEvent.Registration keyRegistration =
                ClientInputEvents.KEY_PRESSED.subscribe(input -> calls.incrementAndGet());
        RustedFabricEvent.Registration pointerRegistration =
                ClientInputEvents.MOUSE_DRAGGED.subscribe(input -> calls.addAndGet(10));
        ClientInputEvents.KEY_PRESSED.invoker().onKeyboardInput(key);
        ClientInputEvents.MOUSE_DRAGGED.invoker().onPointerInput(pointer);
        require(calls.get() == 11, "client input events were not dispatched");
        keyRegistration.close();
        pointerRegistration.close();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
