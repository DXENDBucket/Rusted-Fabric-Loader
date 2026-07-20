package io.github.endx.rustedfabricapi.api.client.message;

import io.github.endx.rustedfabricapi.api.client.RustedWarfareClient;
import rustedwarfare.core.GameEngine;
import rustedwarfare.ui.MessageInterface;
import rustedwarfare.ui.MessageLine;

import java.util.Objects;
import java.util.Optional;

/** In-game message history, transient alerts, and modal dialogs. */
public final class ClientMessages {
    private ClientMessages() {
    }

    public static MessageInterface history() {
        GameEngine engine = RustedWarfareClient.requireEngine();
        if (engine.gameUI == null || engine.gameUI.messageInterface == null) {
            throw new IllegalStateException("The in-game message interface is not initialized");
        }
        return engine.gameUI.messageInterface;
    }

    public static Optional<MessageLine> postSystem(String message) {
        return post(null, message);
    }

    public static Optional<MessageLine> post(String sender, String message) {
        String checkedMessage = requireText(message, "message");
        String checkedSender = sender != null ? sender.trim() : null;
        if (checkedSender != null && checkedSender.isEmpty()) checkedSender = null;
        return Optional.ofNullable(history().addMessage(checkedSender, checkedMessage));
    }

    public static void clearHistory() {
        history().clearMessages();
    }

    public static void alert(String message) {
        RustedWarfareClient.requireEngine().showAlert(requireText(message, "message"));
    }

    public static void dialog(String title, String message) {
        RustedWarfareClient.requireEngine().showMessageBox(
                requireText(title, "title"), requireText(message, "message"));
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        String checked = value.trim();
        if (checked.isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        return checked;
    }
}
