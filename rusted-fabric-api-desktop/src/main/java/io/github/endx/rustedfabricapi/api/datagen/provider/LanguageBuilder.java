package io.github.endx.rustedfabricapi.api.datagen.provider;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import io.github.endx.rustedfabricapi.api.util.Identifier;

/** Duplicate-safe translation builder used by {@link LanguageDataProvider}. */
public final class LanguageBuilder {
    private final String namespace;
    private final LinkedHashMap<String, String> translations =
            new LinkedHashMap<String, String>();

    LanguageBuilder(String namespace) { this.namespace = namespace; }

    public LanguageBuilder add(Identifier key, String translation) {
        Identifier checked = Objects.requireNonNull(key, "key");
        if (!namespace.equals(checked.namespace())) {
            throw new IllegalArgumentException("Translation key belongs to namespace "
                    + checked.namespace() + ", expected " + namespace);
        }
        return addPath(checked.path(), translation);
    }

    /** Accepts either a local path or a full identifier in this provider's namespace. */
    public LanguageBuilder add(String key, String translation) {
        Objects.requireNonNull(key, "key");
        return key.indexOf(':') >= 0
                ? add(Identifier.parse(key), translation)
                : addPath(Identifier.of(namespace, key).path(), translation);
    }

    public int size() { return translations.size(); }

    public Map<String, String> entries() {
        return Collections.unmodifiableMap(new TreeMap<String, String>(translations));
    }

    String propertiesText() {
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, String> entry : new TreeMap<String, String>(translations).entrySet()) {
            result.append(escape(entry.getKey(), true)).append('=')
                    .append(escape(entry.getValue(), false)).append('\n');
        }
        return result.toString();
    }

    private LanguageBuilder addPath(String path, String translation) {
        String checked = Objects.requireNonNull(translation, "translation");
        if (translations.putIfAbsent(path, checked) != null) {
            throw new IllegalArgumentException("Duplicate translation key: "
                    + namespace + ':' + path);
        }
        return this;
    }

    private static String escape(String value, boolean key) {
        StringBuilder result = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\': result.append("\\\\"); break;
                case '\t': result.append("\\t"); break;
                case '\n': result.append("\\n"); break;
                case '\r': result.append("\\r"); break;
                case '\f': result.append("\\f"); break;
                case ' ':
                    if (key || i == 0) result.append('\\');
                    result.append(' ');
                    break;
                case '=':
                case ':':
                case '#':
                case '!':
                    result.append('\\').append(c);
                    break;
                default: result.append(c);
            }
        }
        return result.toString();
    }
}
