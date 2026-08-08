package io.github.endx.rustedfabricapi.api.client.input;

import rustedwarfare.input.InputAction;
import rustedwarfare.input.InputBindingRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/** Fabric-style registration and discovery of desktop key bindings. */
public final class KeyBindings {
    private static final Pattern ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Object LOCK = new Object();
    private static final Map<String, ModKeyBinding> CUSTOM =
            new LinkedHashMap<String, ModKeyBinding>();
    private static final Map<String, String> INSTALLED_CATEGORIES =
            new LinkedHashMap<String, String>();
    private static volatile InputBindingRegistry activeRegistry;

    private KeyBindings() {
    }

    public static ModKeyBinding register(String id, String displayName, String defaultBinding) {
        return register(id, displayName, "Rusted Fabric Mods", defaultBinding);
    }

    /**
     * Registers a binding immediately when possible, or defers it until the game's input registry
     * is constructed. Register during the mod entrypoint so saved bindings can be loaded normally.
     */
    public static ModKeyBinding register(String id, String displayName, String category,
            String defaultBinding) {
        String checkedId = normalizeId(id);
        String checkedName = requireText(displayName, "displayName");
        String checkedCategory = requireText(category, "category");
        String checkedDefault = requireText(defaultBinding, "defaultBinding");
        synchronized (LOCK) {
            ModKeyBinding existing = CUSTOM.get(checkedId);
            if (existing != null) {
                if (!existing.displayName().equals(checkedName)
                        || !existing.category().equals(checkedCategory)
                        || !existing.defaultBinding().equals(checkedDefault)) {
                    throw new IllegalStateException("Conflicting key binding registration: " + checkedId);
                }
                return existing;
            }
            String nativeName = checkedName + " [" + checkedId.replace(':', '.') + "]";
            ModKeyBinding created = new ModKeyBinding(checkedId, checkedName, checkedCategory,
                    checkedDefault, nativeName);
            CUSTOM.put(checkedId, created);
            InputBindingRegistry registry = activeRegistry;
            if (registry != null) {
                if (registry != activeRegistry) onRegistryCreated(registry);
                else install(registry, created);
            }
            return created;
        }
    }

    public static Optional<ModKeyBinding> find(String id) {
        String checkedId = normalizeId(id);
        synchronized (LOCK) {
            return Optional.ofNullable(CUSTOM.get(checkedId));
        }
    }

    public static List<ModKeyBinding> customBindings() {
        synchronized (LOCK) {
            return Collections.unmodifiableList(new ArrayList<ModKeyBinding>(CUSTOM.values()));
        }
    }

    public static List<InputAction> allNativeBindings() {
        InputBindingRegistry registry = findRegistry();
        if (registry == null || registry.actions == null) return Collections.emptyList();
        List<InputAction> result = new ArrayList<InputAction>();
        for (Object value : registry.actions) {
            if (value instanceof InputAction) result.add((InputAction) value);
        }
        return Collections.unmodifiableList(result);
    }

    public static Optional<InputAction> findNativeByConfigKey(String configKey) {
        String checked = requireText(configKey, "configKey").toLowerCase(Locale.ROOT);
        for (InputAction action : allNativeBindings()) {
            if (checked.equals(action.getConfigKey())) return Optional.of(action);
        }
        return Optional.empty();
    }

    /** Internal Loader hook; installs registrations early enough for the settings loader. */
    public static void onRegistryCreated(InputBindingRegistry registry) {
        Objects.requireNonNull(registry, "registry");
        synchronized (LOCK) {
            activeRegistry = registry;
            INSTALLED_CATEGORIES.clear();
            for (ModKeyBinding binding : CUSTOM.values()) install(registry, binding);
        }
    }

    /** Internal Loader hook; emits edge events before normal client tick listeners. */
    public static void pollRegisteredBindings() {
        InputBindingRegistry registry = findRegistry();
        if (registry != null && registry != activeRegistry) onRegistryCreated(registry);
        for (ModKeyBinding binding : customBindings()) binding.poll();
    }

    static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        String checked = value.trim();
        if (checked.isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        return checked;
    }

    private static String normalizeId(String value) {
        String checked = requireText(value, "id").toLowerCase(Locale.ROOT);
        if (!ID.matcher(checked).matches()) {
            throw new IllegalArgumentException(
                    "id must use lowercase namespace:path syntax: " + checked);
        }
        return checked;
    }

    private static InputBindingRegistry findRegistry() {
        return activeRegistry;
    }

    private static void install(InputBindingRegistry registry, ModKeyBinding binding) {
        String categoryKey = binding.category().toLowerCase(Locale.ROOT);
        if (!INSTALLED_CATEGORIES.containsKey(categoryKey)) {
            registry.createActionCategory(binding.category());
            INSTALLED_CATEGORIES.put(categoryKey, binding.category());
        }
        InputAction action = registry.createVisibleAction(binding.nativeDisplayName());
        action.setKeyBinding(binding.defaultBinding(), 0);
        binding.install(action);
    }
}
