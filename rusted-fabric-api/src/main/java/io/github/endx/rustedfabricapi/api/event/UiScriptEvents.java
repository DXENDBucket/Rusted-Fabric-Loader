package io.github.endx.rustedfabricapi.api.event;

public final class UiScriptEvents {
    public static final RustedFabricEvent<BeforePasswordPromptPopup> BEFORE_PASSWORD_PROMPT_POPUP =
            RustedFabricEvent.create(listeners -> (controller, passwordPrompt) -> {
                for (BeforePasswordPromptPopup listener : listeners) {
                    listener.beforePasswordPromptPopup(controller, passwordPrompt);
                }
            });

    public static final RustedFabricEvent<AfterPasswordPromptPopupQueued> AFTER_PASSWORD_PROMPT_POPUP_QUEUED =
            RustedFabricEvent.create(listeners -> (controller, passwordPrompt) -> {
                for (AfterPasswordPromptPopupQueued listener : listeners) {
                    listener.afterPasswordPromptPopupQueued(controller, passwordPrompt);
                }
            });

    public static final RustedFabricEvent<BeforeUiEventHandled> BEFORE_UI_EVENT_HANDLED =
            RustedFabricEvent.create(listeners -> (uiEngine, event) -> {
                for (BeforeUiEventHandled listener : listeners) {
                    listener.beforeUiEventHandled(uiEngine, event);
                }
            });

    public static final RustedFabricEvent<AfterUiDocumentLoaded> AFTER_UI_DOCUMENT_LOADED =
            RustedFabricEvent.create(listeners -> (uiEngine, document) -> {
                for (AfterUiDocumentLoaded listener : listeners) {
                    listener.afterUiDocumentLoaded(uiEngine, document);
                }
            });

    public static final RustedFabricEvent<AfterUiDocumentShown> AFTER_UI_DOCUMENT_SHOWN =
            RustedFabricEvent.create(listeners -> (uiEngine, document) -> {
                for (AfterUiDocumentShown listener : listeners) {
                    listener.afterUiDocumentShown(uiEngine, document);
                }
            });

    private UiScriptEvents() {
    }

    public interface BeforePasswordPromptPopup {
        void beforePasswordPromptPopup(Object controller, Object passwordPrompt);
    }

    public interface AfterPasswordPromptPopupQueued {
        void afterPasswordPromptPopupQueued(Object controller, Object passwordPrompt);
    }

    public interface BeforeUiEventHandled {
        void beforeUiEventHandled(Object uiEngine, String event);
    }

    public interface AfterUiDocumentLoaded {
        void afterUiDocumentLoaded(Object uiEngine, Object document);
    }

    public interface AfterUiDocumentShown {
        void afterUiDocumentShown(Object uiEngine, Object document);
    }
}
