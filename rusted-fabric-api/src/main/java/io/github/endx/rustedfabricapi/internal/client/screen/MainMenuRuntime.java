package io.github.endx.rustedfabricapi.internal.client.screen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.Element;
import com.ElementDocument;
import io.github.endx.rustedfabricapi.api.client.screen.MainMenuButton;
import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import io.github.endx.rustedfabricapi.api.util.Identifier;

/** Owns native main-menu cloning and opaque Java action dispatch. */
public final class MainMenuRuntime {
    private static final String ACTION_PREFIX = "__rustedfabricapi_main_menu_action__";
    private static final Map<Identifier, MainMenuButton> BUTTONS =
            new LinkedHashMap<Identifier, MainMenuButton>();

    private MainMenuRuntime() {
    }

    public static RustedFabricEvent.Registration register(MainMenuButton button) {
        MainMenuButton checked = java.util.Objects.requireNonNull(button, "button");
        synchronized (BUTTONS) {
            if (BUTTONS.containsKey(checked.id())) {
                throw new IllegalStateException("Main-menu button is already registered: " + checked.id());
            }
            BUTTONS.put(checked.id(), checked);
        }
        return new Registration(checked);
    }

    public static List<MainMenuButton> registered() {
        synchronized (BUTTONS) {
            return Collections.unmodifiableList(new ArrayList<MainMenuButton>(BUTTONS.values()));
        }
    }

    public static boolean handleEvent(String event) {
        if (event == null) return false;
        String value = event.trim();
        if (!value.startsWith(ACTION_PREFIX)) return false;
        String rawId = value.substring(ACTION_PREFIX.length());
        MainMenuButton button = null;
        try {
            Identifier id = Identifier.parse(rawId);
            synchronized (BUTTONS) {
                button = BUTTONS.get(id);
            }
        } catch (IllegalArgumentException ignored) {
            return true;
        }
        if (button != null) button.action().run();
        return true;
    }

    public static void decorate(ElementDocument document) {
        if (document == null || !isMainMenu(document.documentPath)) return;
        Element originalButton = document.getElementById("modsButton");
        if (originalButton == null) return;
        Parent parent = findParent(document, originalButton);
        if (parent == null) return;

        Element before = parent.index + 2 < parent.element.getNumChildren()
                ? parent.element.getChild(parent.index + 2) : null;
        Element spacerTemplate = parent.index + 1 < parent.element.getNumChildren()
                ? parent.element.getChild(parent.index + 1) : null;
        for (MainMenuButton contribution : registered()) {
            if (document.getElementById(elementId(contribution.id())) != null) continue;
            Element button = originalButton.cloneAndFix();
            button.setAttribute("id", elementId(contribution.id()));
            button.setAttribute("onclick", ACTION_PREFIX + contribution.id());
            String label = contribution.label();
            button.setClassNames("");
            setButtonText(button, label);
            // Match RootScript.convertTextOnPage: the Unicode class belongs to the
            // button, not its cloned shadow paragraph. Putting it on both paragraphs
            // causes the shadow itself to inherit the hover colour.
            button.loadCharsetIfNeededWithCurrentText();
            insert(parent.element, button, before);
            if (spacerTemplate != null) {
                insert(parent.element, spacerTemplate.cloneAndFix(), before);
            }
        }
    }

    private static boolean isMainMenu(String path) {
        return path != null && path.replace('\\', '/').endsWith("mainMenu.rml");
    }

    private static String elementId(Identifier id) {
        return "rustedfabricapi-menu-" + id.toString().replace(':', '-').replace('/', '-');
    }

    private static void setButtonText(Element button, String label) {
        for (Object value : button.getAllNestedChildren()) {
            Element child = (Element) value;
            if ("p".equalsIgnoreCase(child.getTagName())) {
                child.setTextNoCharset(label, false);
            }
        }
    }

    private static void insert(Element parent, Element child, Element before) {
        if (before != null) parent.insertBefore(child, before);
        else parent.appendChild(child);
    }

    private static Parent findParent(Element current, Element target) {
        int count = current.getNumChildren();
        for (int i = 0; i < count; i++) {
            Element child = current.getChild(i);
            if (child == target || child.equals(target)) return new Parent(current, i);
            Parent nested = findParent(child, target);
            if (nested != null) return nested;
        }
        return null;
    }

    private static final class Parent {
        final Element element;
        final int index;

        Parent(Element element, int index) {
            this.element = element;
            this.index = index;
        }
    }

    private static final class Registration implements RustedFabricEvent.Registration {
        private final MainMenuButton button;
        private boolean active = true;

        Registration(MainMenuButton button) { this.button = button; }

        @Override
        public synchronized boolean unregister() {
            if (!active) return false;
            synchronized (BUTTONS) {
                BUTTONS.remove(button.id(), button);
            }
            active = false;
            return true;
        }

        @Override public void close() { unregister(); }
    }
}
