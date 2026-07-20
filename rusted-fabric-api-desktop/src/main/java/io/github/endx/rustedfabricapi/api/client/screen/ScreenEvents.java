package io.github.endx.rustedfabricapi.api.client.screen;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;

/** Fabric-style UI document lifecycle without LibRocket document types. */
public final class ScreenEvents {
    public static final RustedFabricEvent<Document> LOADED = documentEvent();
    public static final RustedFabricEvent<Document> OPENED = documentEvent();
    public static final RustedFabricEvent<Document> CLOSED = documentEvent();
    public static final RustedFabricEvent<Change> ACTIVE_PAGE_CHANGED = changeEvent();
    public static final RustedFabricEvent<Change> TOPMOST_CHANGED = changeEvent();

    private ScreenEvents() {
    }

    private static RustedFabricEvent<Document> documentEvent() {
        return RustedFabricEvent.create(listeners -> document -> {
            for (Document listener : listeners) listener.onDocument(document);
        });
    }

    private static RustedFabricEvent<Change> changeEvent() {
        return RustedFabricEvent.create(listeners -> change -> {
            for (Change listener : listeners) listener.onChange(change);
        });
    }

    @FunctionalInterface
    public interface Document {
        void onDocument(UiDocumentSnapshot document);
    }

    @FunctionalInterface
    public interface Change {
        void onChange(UiDocumentChange change);
    }
}
