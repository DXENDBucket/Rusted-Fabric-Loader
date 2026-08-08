package io.github.endx.rustedfabricapi.api.text;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.github.endx.rustedfabricapi.api.asset.ModResource;
import io.github.endx.rustedfabricapi.api.asset.ModResourcePack;
import io.github.endx.rustedfabricapi.api.asset.ModResources;
import io.github.endx.rustedfabricapi.api.util.Identifier;
import rustedwarfare.core.LanguageSettings;

/** Namespace-scoped UTF-8 translation bundles bundled in normal mod Jars. */
public final class Translations {
    private static final ConcurrentMap<String, Binding> BINDINGS =
            new ConcurrentHashMap<String, Binding>();
    private static final ConcurrentMap<CacheKey, Map<String, String>> CACHE =
            new ConcurrentHashMap<CacheKey, Map<String, String>>();

    private Translations() {
    }

    public static Registration registerMod(String modId) {
        return register(modId, ModResources.forMod(modId));
    }

    public static Registration register(String namespace, ModResourcePack resources) {
        String checked = Identifier.of(normalizeNamespace(namespace), "translation").namespace();
        Binding binding = new Binding(checked, Objects.requireNonNull(resources, "resources"));
        Binding previous = BINDINGS.putIfAbsent(checked, binding);
        if (previous != null) {
            throw new IllegalStateException("Translation namespace is already registered: " + checked);
        }
        invalidateCaches();
        return new Registration(binding);
    }

    public static String currentLanguage() {
        String language;
        try {
            language = LanguageSettings.c();
        } catch (RuntimeException exception) {
            language = Locale.getDefault().toString();
        }
        if (language == null) language = "en";
        String value = language.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        if (value.isEmpty()) return "en";
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!(c >= 'a' && c <= 'z') && !(c >= '0' && c <= '9') && c != '_') {
                return "en";
            }
        }
        return value;
    }

    public static boolean contains(Identifier key) {
        Identifier checked = Objects.requireNonNull(key, "key");
        return bundle(checked.namespace()).containsKey(checked.path());
    }

    public static String translate(Identifier key, Object... arguments) {
        return translateOr(key, Objects.requireNonNull(key, "key").toString(), arguments);
    }

    public static String translate(String key, Object... arguments) {
        return translate(Identifier.parse(key), arguments);
    }

    public static String translateOr(Identifier key, String fallback, Object... arguments) {
        Identifier checked = Objects.requireNonNull(key, "key");
        String pattern = bundle(checked.namespace()).get(checked.path());
        if (pattern == null) pattern = Objects.requireNonNull(fallback, "fallback");
        Object[] values = arguments != null ? arguments.clone() : new Object[0];
        if (values.length == 0) return pattern;
        Locale locale = Locale.forLanguageTag(currentLanguage().replace('_', '-'));
        return new MessageFormat(pattern, locale).format(values);
    }

    public static String translateOr(String key, String fallback, Object... arguments) {
        return translateOr(Identifier.parse(key), fallback, arguments);
    }

    public static List<String> registeredNamespaces() {
        ArrayList<String> result = new ArrayList<String>(BINDINGS.keySet());
        Collections.sort(result);
        return Collections.unmodifiableList(result);
    }

    public static void invalidateCaches() { CACHE.clear(); }

    private static Map<String, String> bundle(String namespace) {
        Binding binding = BINDINGS.get(namespace);
        if (binding == null) return Collections.emptyMap();
        CacheKey key = new CacheKey(namespace, currentLanguage(), LanguageSettings.c);
        Map<String, String> existing = CACHE.get(key);
        if (existing != null) return existing;
        Map<String, String> loaded = load(binding, key.language);
        Map<String, String> raced = CACHE.putIfAbsent(key, loaded);
        return raced != null ? raced : loaded;
    }

    private static Map<String, String> load(Binding binding, String language) {
        LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
        Set<String> locales = new LinkedHashSet<String>();
        locales.add("default");
        locales.add("en");
        int separator = language.indexOf('_');
        if (separator > 0) locales.add(language.substring(0, separator));
        locales.add(language);
        for (String locale : locales) {
            String path = "assets/" + binding.namespace + "/lang/" + locale + ".properties";
            ModResource resource = binding.resources.resource(path);
            try {
                if (!resource.exists()) continue;
                Properties properties = resource.readPropertiesUtf8();
                for (String name : properties.stringPropertyNames()) {
                    result.put(name, properties.getProperty(name));
                }
            } catch (IOException exception) {
                throw new IllegalStateException("Could not load translation resource " + resource, exception);
            }
        }
        return Collections.unmodifiableMap(result);
    }

    private static String normalizeNamespace(String namespace) {
        if (namespace == null) throw new NullPointerException("namespace");
        return namespace.trim().toLowerCase(Locale.ROOT);
    }

    private static final class Binding {
        final String namespace;
        final ModResourcePack resources;

        Binding(String namespace, ModResourcePack resources) {
            this.namespace = namespace;
            this.resources = resources;
        }
    }

    private static final class CacheKey {
        final String namespace;
        final String language;
        final int nativeVersion;

        CacheKey(String namespace, String language, int nativeVersion) {
            this.namespace = namespace;
            this.language = language;
            this.nativeVersion = nativeVersion;
        }

        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof CacheKey)) return false;
            CacheKey key = (CacheKey) other;
            return nativeVersion == key.nativeVersion && namespace.equals(key.namespace)
                    && language.equals(key.language);
        }

        @Override public int hashCode() {
            return 31 * (31 * namespace.hashCode() + language.hashCode()) + nativeVersion;
        }
    }

    public static final class Registration implements AutoCloseable {
        private final Binding binding;
        private boolean active = true;

        Registration(Binding binding) { this.binding = binding; }

        public String namespace() { return binding.namespace; }

        public synchronized boolean unregister() {
            if (!active) return false;
            active = false;
            boolean removed = BINDINGS.remove(binding.namespace, binding);
            if (removed) invalidateCaches();
            return removed;
        }

        @Override public void close() { unregister(); }
    }
}
