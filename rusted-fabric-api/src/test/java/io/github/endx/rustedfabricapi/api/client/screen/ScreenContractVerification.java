package io.github.endx.rustedfabricapi.api.client.screen;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import io.github.endx.rustedfabricapi.api.util.Identifier;

public final class ScreenContractVerification {
    private ScreenContractVerification() {
    }

    public static void verify() {
        Map<String, String> metadata = new LinkedHashMap<String, String>();
        metadata.put("mode", "sandbox");
        UiDocumentSnapshot popup = new UiDocumentSnapshot(7L, UiDocumentKind.POPUP,
                "gui/test.rml", "Test", "Message", "", true, metadata);
        metadata.put("mode", "changed");

        require(popup.id() == 7L && popup.kind() == UiDocumentKind.POPUP
                        && popup.modal() && popup.hasTextInput() && popup.showBackButton()
                        && popup.inputDefaultValue().isEmpty()
                        && popup.metadata("mode").orElse("").equals("sandbox"),
                "screen snapshot lost a value or failed to copy metadata");
        try {
            popup.metadata().put("other", "value");
            throw new AssertionError("screen metadata was mutable");
        } catch (UnsupportedOperationException expected) {
            // Expected.
        }

        UiDocumentSnapshot sameIdentity = new UiDocumentSnapshot(7L, UiDocumentKind.ALERT,
                "other.rml", null, null, null, false, null);
        UiDocumentSnapshot page = new UiDocumentSnapshot(8L, UiDocumentKind.PAGE,
                "gui/main.rml", null, null, null, false, null);
        require(popup.equals(sameIdentity) && popup.hashCode() == sameIdentity.hashCode()
                        && !page.modal() && !page.hasTextInput(),
                "document identity or kind semantics drifted");

        UiDocumentChange opened = new UiDocumentChange(null, page);
        UiDocumentChange closed = new UiDocumentChange(page, null);
        UiDocumentChange replaced = new UiDocumentChange(page, popup);
        require(opened.opened() && !opened.closed() && closed.closed()
                        && replaced.replaced()
                        && replaced.previous().orElseThrow().equals(page)
                        && replaced.next().orElseThrow().equals(popup),
                "document change classification drifted");

        AtomicInteger calls = new AtomicInteger();
        RustedFabricEvent.Registration loaded = ScreenEvents.LOADED.subscribe(
                document -> calls.incrementAndGet());
        RustedFabricEvent.Registration openedEvent = ScreenEvents.OPENED.subscribe(
                document -> calls.addAndGet(10));
        RustedFabricEvent.Registration closedEvent = ScreenEvents.CLOSED.subscribe(
                document -> calls.addAndGet(100));
        RustedFabricEvent.Registration activeChanged = ScreenEvents.ACTIVE_PAGE_CHANGED.subscribe(
                change -> calls.addAndGet(1_000));
        RustedFabricEvent.Registration topmostChanged = ScreenEvents.TOPMOST_CHANGED.subscribe(
                change -> calls.addAndGet(10_000));
        ScreenEvents.LOADED.invoker().onDocument(page);
        ScreenEvents.OPENED.invoker().onDocument(page);
        ScreenEvents.CLOSED.invoker().onDocument(page);
        ScreenEvents.ACTIVE_PAGE_CHANGED.invoker().onChange(opened);
        ScreenEvents.TOPMOST_CHANGED.invoker().onChange(replaced);
        require(calls.get() == 11_111, "screen lifecycle events were not dispatched");
        loaded.close();
        openedEvent.close();
        closedEvent.close();
        activeChanged.close();
        topmostChanged.close();

        ListScreenSpec list = ListScreenSpec.builder("Java Mods")
                .summary("2 loaded")
                .emptyMessage("None")
                .backButton("Return")
                .filter("Filter:")
                .action("Reload", calls::incrementAndGet)
                .add("Alpha", "1.0", "First")
                .add(ListScreenEntry.of("Beta", "2.0", "Second"))
                .build();
        require(list.entries().size() == 2 && list.title().equals("Java Mods")
                        && list.filterEnabled() && list.filterLabel().equals("Filter:")
                        && list.actions().size() == 1
                        && list.entries().get(1).description().equals("Second"),
                "list screen spec lost content");
        list.actions().get(0).invoke();
        require(calls.get() == 11_112, "list screen action was not invoked");
        try {
            list.entries().add(ListScreenEntry.of("Other", "", ""));
            throw new AssertionError("list screen entries were mutable");
        } catch (UnsupportedOperationException expected) {
            // Expected.
        }

        MainMenuButton button = MainMenuButton.dynamic(Identifier.of("contract", "menu"),
                () -> "Contract", calls::incrementAndGet);
        RustedFabricEvent.Registration menu = MainMenuButtons.register(button);
        require(MainMenuButtons.registered().contains(button) && button.label().equals("Contract"),
                "main-menu button was not registered");
        require(menu.unregister() && !MainMenuButtons.registered().contains(button),
                "main-menu button was not unregistered");

        try {
            new UiDocumentChange(null, null);
            throw new AssertionError("empty document change was accepted");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }

        expectIllegal(() -> ListScreenSpec.builder(" ").build());
        expectIllegal(() -> ListScreenEntry.of(" ", "", ""));
    }

    private static void expectIllegal(Runnable action) {
        try {
            action.run();
            throw new AssertionError("invalid screen content was accepted");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
