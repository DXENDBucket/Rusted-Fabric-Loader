package io.github.endx.rustedfabricapi.api.client.screen;

import java.util.Objects;
import java.util.function.Supplier;

import io.github.endx.rustedfabricapi.api.util.Identifier;

/** A native-looking button contributed to the game's main menu. */
public final class MainMenuButton {
    private final Identifier id;
    private final Supplier<String> label;
    private final Runnable action;

    private MainMenuButton(Identifier id, Supplier<String> label, Runnable action) {
        this.id = Objects.requireNonNull(id, "id");
        this.label = Objects.requireNonNull(label, "label");
        this.action = Objects.requireNonNull(action, "action");
        checkedLabel(label.get());
    }

    public static MainMenuButton of(Identifier id, String label, Runnable action) {
        String checked = checkedLabel(label);
        return new MainMenuButton(id, () -> checked, action);
    }

    /** The supplier is evaluated whenever a new main-menu document is loaded. */
    public static MainMenuButton dynamic(Identifier id, Supplier<String> label, Runnable action) {
        return new MainMenuButton(id, label, action);
    }

    public Identifier id() { return id; }
    public String label() { return checkedLabel(label.get()); }
    public Runnable action() { return action; }

    private static String checkedLabel(String value) {
        if (value == null) throw new NullPointerException("label");
        String checked = value.trim();
        if (checked.isEmpty()) throw new IllegalArgumentException("label must not be blank");
        if (checked.length() > 128) throw new IllegalArgumentException("label is too long");
        return checked;
    }
}
