package io.github.endx.rustedfabricapi.internal.client.screen;

import com.Element;
import com.ElementDocument;
import io.github.endx.rustedfabricapi.api.client.screen.dialog.DialogCallback;
import io.github.endx.rustedfabricapi.api.client.screen.dialog.DialogChoice;
import io.github.endx.rustedfabricapi.api.client.screen.dialog.DialogResult;
import io.github.endx.rustedfabricapi.api.client.screen.dialog.DialogSpec;
import rustedwarfare.ui.LibRocketUiController;
import rustedwarfare.ui.LibRocketUiEngine;
import rustedwarfare.ui.PopupButton;
import rustedwarfare.ui.PopupDocumentData;

/** Internal owner for native popup documents and exactly-once dialog completion. */
public final class DialogRuntime {
    private static final Object LOCK = new Object();
    private static long nextId = 1L;
    private static ActiveDialog active;

    private DialogRuntime() {
    }

    public static long show(DialogSpec spec, DialogCallback callback) {
        LibRocketUiEngine engine = currentEngine();
        if (engine == null) throw new IllegalStateException("LibRocket UI is not initialized");

        ActiveDialog dialog;
        synchronized (LOCK) {
            if (active != null || engine.getPopupDocument() != null) return -1L;
            dialog = new ActiveDialog(nextId++, engine, spec, callback);
            active = dialog;
        }

        ElementDocument document = null;
        try {
            PopupDocumentData data = new PopupDocumentData();
            data.title = spec.title();
            data.message = spec.message();
            data.inputDefaultValue = spec.inputDefaultValue().orElse(null);
            PopupButton primary = new PopupButton(spec.primaryButton(),
                    () -> submit(dialog, DialogChoice.PRIMARY));
            primary.bindEnterKey = spec.hasTextInput() && spec.submitOnEnter();
            data.button1 = primary;
            data.button2 = spec.secondaryButton().map(label -> new PopupButton(label,
                    () -> submit(dialog, DialogChoice.SECONDARY))).orElse(null);
            data.showImmediately = false;
            data.showBackButton = spec.dismissible();
            data.onClose = () -> complete(dialog, new DialogResult(DialogChoice.DISMISSED, null));

            document = engine.createPopupDocumentFromData(data);
            if (document == null) {
                discard(dialog);
                return -1L;
            }
            dialog.document = document;
            if (!engine.showPopupDocumentData(data)) {
                discard(dialog);
                engine.closeDocument(document);
                return -1L;
            }
            return dialog.id;
        } catch (RuntimeException | Error failure) {
            discard(dialog);
            if (document != null && engine.getPopupDocument() != document) {
                engine.closeDocument(document);
            }
            throw failure;
        }
    }

    public static boolean isOpen(long id) {
        synchronized (LOCK) {
            return active != null && active.id == id && !active.completed;
        }
    }

    public static boolean dismiss(long id) {
        ActiveDialog dialog;
        synchronized (LOCK) {
            dialog = active;
            if (dialog == null || dialog.id != id || dialog.completed) return false;
        }
        if (dialog.engine.getPopupDocument() == dialog.document) {
            return dialog.engine.closePopup();
        }
        complete(dialog, new DialogResult(DialogChoice.DISMISSED, null));
        return false;
    }

    private static void submit(ActiveDialog dialog, DialogChoice choice) {
        String input = dialog.spec.hasTextInput() ? readInput(dialog.document) : null;
        DialogResult result = new DialogResult(choice, input);
        if (!markCompleted(dialog)) return;
        if (dialog.engine.getPopupDocument() == dialog.document) {
            dialog.engine.closePopup();
        }
        dialog.callback.onComplete(result);
    }

    private static void complete(ActiveDialog dialog, DialogResult result) {
        if (!markCompleted(dialog)) return;
        dialog.callback.onComplete(result);
    }

    private static boolean markCompleted(ActiveDialog dialog) {
        synchronized (LOCK) {
            if (dialog.completed) return false;
            dialog.completed = true;
            if (active == dialog) active = null;
            return true;
        }
    }

    private static void discard(ActiveDialog dialog) {
        synchronized (LOCK) {
            dialog.completed = true;
            if (active == dialog) active = null;
        }
    }

    private static String readInput(ElementDocument document) {
        if (document == null) return "";
        Element input = document.getElementById("textInput");
        if (input == null) return "";
        String value = input.getAttribute("value");
        return value != null ? value : "";
    }

    private static LibRocketUiEngine currentEngine() {
        LibRocketUiController controller = LibRocketUiController.getInstance();
        return controller != null ? controller.libRocket : null;
    }

    private static final class ActiveDialog {
        final long id;
        final LibRocketUiEngine engine;
        final DialogSpec spec;
        final DialogCallback callback;
        ElementDocument document;
        boolean completed;

        ActiveDialog(long id, LibRocketUiEngine engine, DialogSpec spec, DialogCallback callback) {
            this.id = id;
            this.engine = engine;
            this.spec = spec;
            this.callback = callback;
        }
    }
}
