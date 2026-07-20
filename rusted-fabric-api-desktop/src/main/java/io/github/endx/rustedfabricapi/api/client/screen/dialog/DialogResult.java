package io.github.endx.rustedfabricapi.api.client.screen.dialog;

import java.util.Objects;
import java.util.Optional;

/** Immutable result delivered once after a dialog button or dismissal. */
public final class DialogResult {
    private final DialogChoice choice;
    private final String inputValue;

    public DialogResult(DialogChoice choice, String inputValue) {
        this.choice = Objects.requireNonNull(choice, "choice");
        if (choice == DialogChoice.DISMISSED && inputValue != null) {
            throw new IllegalArgumentException("dismissed dialogs cannot capture input");
        }
        this.inputValue = inputValue;
    }

    public DialogChoice choice() { return choice; }
    public boolean submitted() { return choice != DialogChoice.DISMISSED; }
    public boolean inputCaptured() { return inputValue != null; }

    /** Present for submitted text-input dialogs, including when the entered value is empty. */
    public Optional<String> input() { return Optional.ofNullable(inputValue); }

    @Override
    public String toString() {
        return "DialogResult{" + choice + (inputValue != null ? ", input='" + inputValue + '\'' : "")
                + '}';
    }
}
