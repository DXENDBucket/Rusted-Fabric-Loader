package io.github.endx.rustedfabricapi.api.client.screen.dialog;

import java.util.Objects;
import java.util.Optional;

/** Immutable description of a native two-button client dialog. */
public final class DialogSpec {
    private static final int MAX_TITLE_LENGTH = 256;
    private static final int MAX_MESSAGE_LENGTH = 16 * 1024;
    private static final int MAX_INPUT_LENGTH = 4 * 1024;
    private static final int MAX_BUTTON_LENGTH = 128;

    private final String title;
    private final String message;
    private final String inputDefaultValue;
    private final String primaryButton;
    private final String secondaryButton;
    private final boolean dismissible;
    private final boolean submitOnEnter;

    private DialogSpec(Builder builder) {
        this.title = requireVisible(builder.title, "title", MAX_TITLE_LENGTH);
        this.message = requireLength(builder.message, "message", MAX_MESSAGE_LENGTH);
        this.inputDefaultValue = builder.inputDefaultValue != null
                ? requireLength(builder.inputDefaultValue, "inputDefaultValue", MAX_INPUT_LENGTH)
                : null;
        this.primaryButton = requireVisible(builder.primaryButton, "primaryButton",
                MAX_BUTTON_LENGTH);
        this.secondaryButton = builder.secondaryButton != null
                ? requireVisible(builder.secondaryButton, "secondaryButton", MAX_BUTTON_LENGTH)
                : null;
        this.dismissible = builder.dismissible;
        this.submitOnEnter = builder.submitOnEnter;
    }

    public static Builder builder(String title, String message) {
        return new Builder(title, message);
    }

    public String title() { return title; }
    public String message() { return message; }
    public boolean hasTextInput() { return inputDefaultValue != null; }
    public Optional<String> inputDefaultValue() {
        return Optional.ofNullable(inputDefaultValue);
    }
    public String primaryButton() { return primaryButton; }
    public Optional<String> secondaryButton() { return Optional.ofNullable(secondaryButton); }
    public boolean dismissible() { return dismissible; }
    public boolean submitOnEnter() { return submitOnEnter; }

    private static String requireVisible(String value, String name, int maximum) {
        String checked = requireLength(value, name, maximum).trim();
        if (checked.isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        return checked;
    }

    private static String requireLength(String value, String name, int maximum) {
        Objects.requireNonNull(value, name);
        if (value.length() > maximum) {
            throw new IllegalArgumentException(name + " exceeds " + maximum + " characters");
        }
        return value;
    }

    public static final class Builder {
        private final String title;
        private final String message;
        private String inputDefaultValue;
        private String primaryButton = "OK";
        private String secondaryButton;
        private boolean dismissible = true;
        private boolean submitOnEnter = true;

        private Builder(String title, String message) {
            this.title = title;
            this.message = message;
        }

        /** Adds a text input; an empty default still creates and focuses the input control. */
        public Builder textInput(String defaultValue) {
            this.inputDefaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
            return this;
        }

        public Builder primaryButton(String label) {
            this.primaryButton = label;
            return this;
        }

        public Builder secondaryButton(String label) {
            this.secondaryButton = label;
            return this;
        }

        public Builder dismissible(boolean dismissible) {
            this.dismissible = dismissible;
            return this;
        }

        /** Controls whether Enter in the text field invokes the primary button. */
        public Builder submitOnEnter(boolean submitOnEnter) {
            this.submitOnEnter = submitOnEnter;
            return this;
        }

        public DialogSpec build() { return new DialogSpec(this); }
    }
}
