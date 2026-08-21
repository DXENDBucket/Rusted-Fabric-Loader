package io.github.endx.rustedfabric.android.launcher.ui;

import android.content.Context;
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

/**
 * Invisible Android text editor used while a libRocket text control owns focus.
 *
 * <p>The desktop game has no Android {@code EditText}, so an IME will not appear merely because
 * libRocket focused an {@code <input>}. This view owns the platform {@link InputConnection} and
 * forwards only committed text to the game. Composition remains inside the IME until the user
 * selects a candidate, which prevents Chinese pinyin updates from being inserted repeatedly.</p>
 */
final class GameTextInputView extends View {
    interface Sink {
        void commitText(String text);

        void backspace(int count);

        void enter();
    }

    private final Sink sink;
    private final Editable composing = new SpannableStringBuilder();

    GameTextInputView(Context context, Sink sink) {
        super(context);
        this.sink = sink;
        setFocusable(true);
        setFocusableInTouchMode(true);
        setVisibility(VISIBLE);
        setAlpha(0.01f);
    }

    @Override
    public boolean onCheckIsTextEditor() {
        return true;
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo attributes) {
        attributes.inputType = InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;
        attributes.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI
                | EditorInfo.IME_FLAG_NO_FULLSCREEN
                | EditorInfo.IME_ACTION_DONE;
        attributes.initialSelStart = 0;
        attributes.initialSelEnd = 0;
        return new BaseInputConnection(this, true) {
            @Override
            public Editable getEditable() {
                return composing;
            }

            @Override
            public boolean setComposingText(CharSequence text, int newCursorPosition) {
                composing.clear();
                if (text != null) composing.append(text);
                BaseInputConnection.setComposingSpans(composing);
                return true;
            }

            @Override
            public boolean commitText(CharSequence text, int newCursorPosition) {
                if (text != null && text.length() > 0) sink.commitText(text.toString());
                clearComposition();
                return true;
            }

            @Override
            public boolean finishComposingText() {
                // A few Android IMEs finish a candidate without a separate commitText call.
                if (composing.length() > 0) sink.commitText(composing.toString());
                clearComposition();
                return true;
            }

            @Override
            public boolean deleteSurroundingText(int beforeLength, int afterLength) {
                if (composing.length() > 0) {
                    int remove = Math.min(Math.max(beforeLength, 1), composing.length());
                    composing.delete(composing.length() - remove, composing.length());
                } else if (beforeLength > 0) {
                    sink.backspace(beforeLength);
                }
                return true;
            }

            @Override
            public boolean sendKeyEvent(KeyEvent event) {
                if (event.getAction() != KeyEvent.ACTION_DOWN) return true;
                if (event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
                    return deleteSurroundingText(1, 0);
                }
                if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                        || event.getKeyCode() == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                    sink.enter();
                    return true;
                }
                int character = event.getUnicodeChar(event.getMetaState());
                if (character != 0 && !Character.isISOControl(character)) {
                    sink.commitText(new String(Character.toChars(character)));
                }
                return true;
            }

            @Override
            public boolean performEditorAction(int actionCode) {
                sink.enter();
                return true;
            }

            private void clearComposition() {
                BaseInputConnection.removeComposingSpans(composing);
                composing.clear();
            }
        };
    }

    void showKeyboard() {
        if (!requestFocus()) return;
        InputMethodManager manager = (InputMethodManager)
                getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager != null) {
            post(() -> manager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT));
        }
    }

    void hideKeyboard() {
        InputMethodManager manager = (InputMethodManager)
                getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager != null) manager.hideSoftInputFromWindow(getWindowToken(), 0);
        composing.clear();
        clearFocus();
    }
}
