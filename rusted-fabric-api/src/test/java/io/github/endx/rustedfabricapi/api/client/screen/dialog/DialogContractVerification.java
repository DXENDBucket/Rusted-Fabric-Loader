package io.github.endx.rustedfabricapi.api.client.screen.dialog;

import java.util.concurrent.atomic.AtomicInteger;

public final class DialogContractVerification {
    private DialogContractVerification() {
    }

    public static void verify() {
        DialogSpec spec = DialogSpec.builder("Rename", "Enter a new name")
                .textInput("")
                .primaryButton("Save")
                .secondaryButton("Cancel")
                .dismissible(false)
                .submitOnEnter(false)
                .build();
        require(spec.title().equals("Rename") && spec.message().equals("Enter a new name")
                        && spec.hasTextInput() && spec.inputDefaultValue().isPresent()
                        && spec.inputDefaultValue().orElse("missing").isEmpty()
                        && spec.primaryButton().equals("Save")
                        && spec.secondaryButton().orElse("").equals("Cancel")
                        && !spec.dismissible() && !spec.submitOnEnter(),
                "dialog specification lost a value");

        DialogResult submitted = new DialogResult(DialogChoice.PRIMARY, "");
        DialogResult dismissed = new DialogResult(DialogChoice.DISMISSED, null);
        require(submitted.submitted() && submitted.inputCaptured()
                        && submitted.input().isPresent() && submitted.input().orElse("x").isEmpty()
                        && !dismissed.submitted() && !dismissed.inputCaptured()
                        && dismissed.input().isEmpty(),
                "dialog result did not preserve empty input or dismissal");

        AtomicInteger calls = new AtomicInteger();
        DialogCallback callback = result -> calls.addAndGet(
                result.choice() == DialogChoice.SECONDARY ? 10 : 1);
        callback.onComplete(new DialogResult(DialogChoice.SECONDARY, null));
        require(calls.get() == 10, "dialog callback did not receive the result");

        DialogHandle first = new DialogHandle(4L);
        DialogHandle same = new DialogHandle(4L);
        DialogHandle other = new DialogHandle(5L);
        require(first.equals(same) && first.hashCode() == same.hashCode()
                        && !first.equals(other) && first.id() == 4L,
                "dialog handle identity drifted");

        expectIllegal(() -> new DialogResult(DialogChoice.DISMISSED, "value"),
                "dismissed result captured input");
        expectIllegal(() -> DialogSpec.builder(" ", "message").build(),
                "blank dialog title was accepted");
        expectIllegal(() -> DialogSpec.builder("Title", "message")
                        .primaryButton(" ").build(),
                "blank primary button was accepted");
        expectIllegal(() -> DialogSpec.builder("Title", "message")
                        .textInput("x".repeat(4097)).build(),
                "oversized input default was accepted");
    }

    private static void expectIllegal(Runnable action, String message) {
        try {
            action.run();
            throw new AssertionError(message);
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
