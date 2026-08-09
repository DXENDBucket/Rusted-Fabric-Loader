package io.github.endx.rustedfabricapi.internal.client.screen;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.Element;
import com.ElementDocument;
import io.github.endx.rustedfabricapi.api.client.screen.ScreenEvents;
import io.github.endx.rustedfabricapi.api.client.screen.ListScreenEntry;
import io.github.endx.rustedfabricapi.api.client.screen.ListScreenSpec;
import io.github.endx.rustedfabricapi.api.client.screen.UiDocumentChange;
import io.github.endx.rustedfabricapi.api.client.screen.UiDocumentKind;
import io.github.endx.rustedfabricapi.api.client.screen.UiDocumentSnapshot;
import io.github.endx.rustedfabricapi.mixin.accessor.PopupDocumentDataAccessor;
import rustedwarfare.ui.LibRocketUiController;
import rustedwarfare.ui.LibRocketUiEngine;
import rustedwarfare.ui.PopupDocumentData;

/** Internal bridge that is the only layer retaining native LibRocket document identities. */
public final class ScreenRuntime {
    private static final String LIST_SPEC_KEY = "rustedfabricapi:listSpec";
    private static final String LIST_PAGE_KEY = "rustedfabricapi:listPage";
    private static final String LIST_FILTER_EVENT = "__rustedfabricapi_list_filter__";
    private static final String LIST_FILTER_ID = "rustedfabricapi-list-filter";
    private static final String LIST_ENTRY_ID_PREFIX = "rustedfabricapi-list-entry-";
    private static final Object LOCK = new Object();
    private static final IdentityHashMap<ElementDocument, Long> IDS =
            new IdentityHashMap<ElementDocument, Long>();
    private static long nextId = 1L;
    private static LibRocketUiEngine owner;
    private static ElementDocument activeRef;
    private static ElementDocument popupRef;
    private static ElementDocument alertRef;
    private static UiDocumentSnapshot active;
    private static UiDocumentSnapshot popup;
    private static UiDocumentSnapshot alert;

    private ScreenRuntime() {
    }

    public static void onPageLoaded(LibRocketUiEngine engine, ElementDocument document) {
        if (engine == null || document == null) return;
        MainMenuRuntime.decorate(document);
        decorateListPage(engine, document);
        UiDocumentSnapshot snapshot;
        synchronized (LOCK) {
            ensureOwner(engine);
            snapshot = snapshot(document, UiDocumentKind.PAGE, null);
        }
        ScreenEvents.LOADED.invoker().onDocument(snapshot);
    }

    public static void onPageShown(LibRocketUiEngine engine, ElementDocument document) {
        if (engine == null || document == null) return;
        UiDocumentSnapshot implicitlyClosed = null;
        UiDocumentSnapshot oldActive;
        UiDocumentSnapshot oldTop;
        UiDocumentSnapshot opened;
        UiDocumentSnapshot newTop;
        synchronized (LOCK) {
            ensureOwner(engine);
            oldActive = active;
            oldTop = topTracked();
            if (activeRef != null && activeRef != document) {
                implicitlyClosed = active;
                IDS.remove(activeRef);
            }
            activeRef = document;
            active = snapshot(document, UiDocumentKind.PAGE, null);
            opened = active;
            newTop = topTracked();
        }
        if (implicitlyClosed != null) ScreenEvents.CLOSED.invoker().onDocument(implicitlyClosed);
        ScreenEvents.OPENED.invoker().onDocument(opened);
        fireChange(ScreenEvents.ACTIVE_PAGE_CHANGED, oldActive, opened);
        fireChange(ScreenEvents.TOPMOST_CHANGED, oldTop, newTop);
    }

    public static void onOverlayShown(LibRocketUiEngine engine, PopupDocumentData data,
            UiDocumentKind kind) {
        if (engine == null || data == null
                || kind == UiDocumentKind.PAGE) return;
        PopupDocumentDataAccessor accessor = (PopupDocumentDataAccessor) data;
        ElementDocument overlayDocument = accessor.rustedfabricapi$getDocument();
        if (overlayDocument == null) return;
        UiDocumentSnapshot implicitlyClosed = null;
        UiDocumentSnapshot oldTop;
        UiDocumentSnapshot opened;
        UiDocumentSnapshot newTop;
        synchronized (LOCK) {
            ensureOwner(engine);
            oldTop = topTracked();
            if (kind == UiDocumentKind.ALERT) {
                if (alertRef != null && alertRef != overlayDocument) {
                    implicitlyClosed = alert;
                    IDS.remove(alertRef);
                }
                alertRef = overlayDocument;
                alert = snapshot(overlayDocument, kind, data);
                opened = alert;
            } else {
                if (popupRef != null && popupRef != overlayDocument) {
                    implicitlyClosed = popup;
                    IDS.remove(popupRef);
                }
                popupRef = overlayDocument;
                popup = snapshot(overlayDocument, kind, data);
                opened = popup;
            }
            newTop = topTracked();
        }
        if (implicitlyClosed != null) ScreenEvents.CLOSED.invoker().onDocument(implicitlyClosed);
        ScreenEvents.OPENED.invoker().onDocument(opened);
        fireChange(ScreenEvents.TOPMOST_CHANGED, oldTop, newTop);
    }

    public static void onDocumentClosed(Object engineObject, ElementDocument document) {
        if (!(engineObject instanceof LibRocketUiEngine) || document == null) return;
        LibRocketUiEngine engine = (LibRocketUiEngine) engineObject;
        UiDocumentSnapshot closed;
        UiDocumentSnapshot oldActive;
        UiDocumentSnapshot newActive;
        UiDocumentSnapshot oldTop;
        UiDocumentSnapshot newTop;
        synchronized (LOCK) {
            ensureOwner(engine);
            oldActive = active;
            oldTop = topTracked();
            if (document == alertRef
                    || (alertRef == null && engine.getAlertDocument() == document)) {
                closed = alert != null && document == alertRef
                        ? alert : snapshot(document, UiDocumentKind.ALERT, null);
                alertRef = null;
                alert = null;
            } else if (document == popupRef
                    || (popupRef == null && engine.getPopupDocument() == document)) {
                closed = popup != null && document == popupRef
                        ? popup : snapshot(document, UiDocumentKind.POPUP, null);
                popupRef = null;
                popup = null;
            } else if (document == activeRef
                    || (activeRef == null && engine.getActiveDocument() == document)) {
                closed = active != null && document == activeRef
                        ? active : snapshot(document, UiDocumentKind.PAGE, null);
                activeRef = null;
                active = null;
            } else {
                return;
            }
            IDS.remove(document);
            newActive = active;
            newTop = topTracked();
        }
        ScreenEvents.CLOSED.invoker().onDocument(closed);
        fireChange(ScreenEvents.ACTIVE_PAGE_CHANGED, oldActive, newActive);
        fireChange(ScreenEvents.TOPMOST_CHANGED, oldTop, newTop);
    }

    public static boolean isUiOpen() {
        LibRocketUiController controller = LibRocketUiController.getInstance();
        return controller != null && controller.isUiOpen();
    }

    public static Optional<UiDocumentSnapshot> activePage() {
        LibRocketUiEngine engine = currentEngine();
        return engine != null ? current(engine, engine.getActiveDocument(), UiDocumentKind.PAGE)
                : Optional.empty();
    }

    public static Optional<UiDocumentSnapshot> popup() {
        LibRocketUiEngine engine = currentEngine();
        return engine != null ? current(engine, engine.getPopupDocument(), UiDocumentKind.POPUP)
                : Optional.empty();
    }

    public static Optional<UiDocumentSnapshot> alert() {
        LibRocketUiEngine engine = currentEngine();
        return engine != null ? current(engine, engine.getAlertDocument(), UiDocumentKind.ALERT)
                : Optional.empty();
    }

    public static Optional<UiDocumentSnapshot> topmost() {
        LibRocketUiEngine engine = currentEngine();
        if (engine == null) return Optional.empty();
        ElementDocument document = engine.getTopmostDocument();
        if (document == null) return Optional.empty();
        UiDocumentKind kind = document == engine.getAlertDocument() ? UiDocumentKind.ALERT
                : document == engine.getPopupDocument() ? UiDocumentKind.POPUP
                : UiDocumentKind.PAGE;
        return current(engine, document, kind);
    }

    public static boolean closeTopmostOverlay() {
        LibRocketUiEngine engine = requireEngine();
        return engine.closeAlertOrPopup();
    }

    public static void back() { requireEngine().backToLastDocument(); }
    public static void reloadActivePage() { requireEngine().reloadDocument(); }
    public static void clearHistory() { requireEngine().clearHistory(); }

    public static void openList(ListScreenSpec spec) {
        ListScreenSpec checked = java.util.Objects.requireNonNull(spec, "spec");
        Map<String, Object> metadata = new LinkedHashMap<String, Object>();
        metadata.put(LIST_PAGE_KEY, Boolean.TRUE);
        metadata.put(LIST_SPEC_KEY, checked);
        metadata.put("title", checked.title());
        requireEngine().setDocument("mods.rml", new java.util.HashMap<String, Object>(metadata));
    }

    /** Handles the small set of opaque events owned by API list pages. */
    public static boolean handleUiEvent(String event) {
        if (event == null) return false;
        LibRocketUiEngine engine = currentEngine();
        ElementDocument document = engine != null ? engine.getActiveDocument() : null;
        if (document == null || !Boolean.TRUE.equals(document.getMetadata(LIST_PAGE_KEY))) {
            return false;
        }
        String value = event.trim();
        if ("mods.loadMods()".equals(value)) return true;
        if (!LIST_FILTER_EVENT.equals(value)) return false;
        Object rawSpec = document.getMetadata(LIST_SPEC_KEY);
        if (rawSpec instanceof ListScreenSpec) {
            applyListFilter(document, (ListScreenSpec) rawSpec);
        }
        return true;
    }

    private static void decorateListPage(LibRocketUiEngine engine, ElementDocument document) {
        Object rawSpec = document.getMetadata(LIST_SPEC_KEY);
        if (!(rawSpec instanceof ListScreenSpec)) return;
        ListScreenSpec spec = (ListScreenSpec) rawSpec;
        com.Element body = document.getElementById("body");
        if (body == null) return;
        body.setInnerRML(listMarkup(spec));
        engine.loadCharsetIfNeededOnChildren(body, true);
    }

    private static String listMarkup(ListScreenSpec spec) {
        StringBuilder html = new StringBuilder(2048 + spec.entries().size() * 256);
        html.append("<div class=\"background panelMainNoAlpha whiteText\">")
                .append("<div class=\"panelCloseButton\" onclick=\"backOrClose()\" click_on_escape=\"\"/>")
                .append("<div id=\"scrollDivWrap\"><div id=\"scrollDiv\">");
        if (spec.filterEnabled()) {
            html.append("<div class=\"modFilter smallForms inputDiv textinputUnicodeWrap\"><label>")
                    .append(html(spec.filterLabel()))
                    .append("</label> <input id=\"").append(LIST_FILTER_ID)
                    .append("\" type=\"text\" value=\"\" onchange=\"")
                    .append(LIST_FILTER_EVENT).append("\" onkeyup=\"")
                    .append(LIST_FILTER_EVENT).append("\"/></div>");
        }
        html.append("<h2>").append(html(spec.title())).append("</h2>");
        if (!spec.summary().isEmpty()) {
            html.append("<p class=\"helpTextLarge\">").append(html(spec.summary())).append("</p>");
        }
        html.append("<div class=\"group\">");
        if (spec.entries().isEmpty()) {
            html.append("<p class=\"center\">").append(html(spec.emptyMessage())).append("</p>");
        } else {
            for (int i = 0; i < spec.entries().size(); i++) {
                ListScreenEntry entry = spec.entries().get(i);
                html.append("<div id=\"").append(LIST_ENTRY_ID_PREFIX).append(i)
                        .append("\" class=\"modItem\"><div><label>")
                        .append(html(entry.title())).append("</label></div>");
                if (!entry.details().isEmpty()) {
                    html.append("<p class=\"modInfo\">").append(html(entry.details())).append("</p>");
                }
                if (!entry.description().isEmpty()) {
                    html.append("<p class=\"modMessage\">").append(html(entry.description())).append("</p>");
                }
                html.append("</div>");
            }
        }
        return html.append("</div><br/></div><div class=\"mainButtons\"><button onclick=\"backOrClose()\">")
                .append(html(spec.backButton())).append("</button></div></div></div>").toString();
    }

    private static void applyListFilter(ElementDocument document, ListScreenSpec spec) {
        Element input = document.getElementById(LIST_FILTER_ID);
        if (input == null) return;
        String query = input.getValue();
        query = query != null ? query.trim().toLowerCase(Locale.ROOT) : "";
        for (int i = 0; i < spec.entries().size(); i++) {
            Element element = document.getElementById(LIST_ENTRY_ID_PREFIX + i);
            if (element == null) continue;
            ListScreenEntry entry = spec.entries().get(i);
            String searchable = (entry.title() + '\n' + entry.details() + '\n'
                    + entry.description()).toLowerCase(Locale.ROOT);
            if (query.isEmpty() || searchable.contains(query)) {
                element.removeClass("modItemFilteredOut");
            } else if (!element.hasClassName("modItemFilteredOut")) {
                element.addClass("modItemFilteredOut");
            }
        }
    }

    private static String html(String value) {
        StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&': escaped.append("&amp;"); break;
                case '<': escaped.append("&lt;"); break;
                case '>': escaped.append("&gt;"); break;
                case '"': escaped.append("&quot;"); break;
                case '\'': escaped.append("&#39;"); break;
                case '\n': escaped.append("<br/>"); break;
                case '\r': break;
                default: escaped.append(c);
            }
        }
        return escaped.toString();
    }

    private static Optional<UiDocumentSnapshot> current(LibRocketUiEngine engine,
            ElementDocument document, UiDocumentKind kind) {
        if (document == null) return Optional.empty();
        synchronized (LOCK) {
            ensureOwner(engine);
            UiDocumentSnapshot tracked = document == activeRef ? active
                    : document == popupRef ? popup : document == alertRef ? alert : null;
            return Optional.of(tracked != null ? tracked : snapshot(document, kind, null));
        }
    }

    private static UiDocumentSnapshot snapshot(ElementDocument document, UiDocumentKind kind,
            PopupDocumentData data) {
        Long id = IDS.get(document);
        if (id == null) {
            id = Long.valueOf(nextId++);
            IDS.put(document, id);
        }
        String path = document.documentPath != null
                ? document.documentPath.replace('\\', '/') : "";
        PopupDocumentDataAccessor accessor = data instanceof PopupDocumentDataAccessor
                ? (PopupDocumentDataAccessor) data : null;
        String title = accessor != null ? accessor.rustedfabricapi$getTitle() : "";
        String message = accessor != null ? accessor.rustedfabricapi$getMessage() : "";
        String input = accessor != null ? accessor.rustedfabricapi$getInputDefaultValue() : "";
        boolean back = accessor != null && accessor.rustedfabricapi$getShowBackButton();
        return new UiDocumentSnapshot(id.longValue(), kind, path, title, message, input, back,
                safeMetadata(document.metadata));
    }

    private static Map<String, String> safeMetadata(Map<?, ?> source) {
        if (source == null || source.isEmpty()) return Collections.emptyMap();
        Map<String, String> result = new LinkedHashMap<String, String>();
        int count = 0;
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (count >= 128) break;
            if (!(entry.getKey() instanceof String)) continue;
            Object value = entry.getValue();
            if (value instanceof CharSequence || value instanceof Number
                    || value instanceof Boolean || value instanceof Character
                    || value instanceof Enum<?>) {
                String text = String.valueOf(value);
                if (text.length() > 4096) text = text.substring(0, 4096);
                result.put((String) entry.getKey(), text);
                count++;
            }
        }
        return result;
    }

    private static UiDocumentSnapshot topTracked() {
        return alert != null ? alert : popup != null ? popup : active;
    }

    private static void fireChange(io.github.endx.rustedfabricapi.api.event.RustedFabricEvent<ScreenEvents.Change> event,
            UiDocumentSnapshot previous, UiDocumentSnapshot next) {
        if (same(previous, next)) return;
        event.invoker().onChange(new UiDocumentChange(previous, next));
    }

    private static boolean same(UiDocumentSnapshot first, UiDocumentSnapshot second) {
        return first == second || first != null && first.equals(second);
    }

    private static void ensureOwner(LibRocketUiEngine engine) {
        if (owner == engine) return;
        owner = engine;
        IDS.clear();
        activeRef = null;
        popupRef = null;
        alertRef = null;
        active = null;
        popup = null;
        alert = null;
    }

    private static LibRocketUiEngine currentEngine() {
        LibRocketUiController controller = LibRocketUiController.getInstance();
        return controller != null ? controller.libRocket : null;
    }

    private static LibRocketUiEngine requireEngine() {
        LibRocketUiEngine engine = currentEngine();
        if (engine == null) throw new IllegalStateException("LibRocket UI is not initialized");
        return engine;
    }
}
