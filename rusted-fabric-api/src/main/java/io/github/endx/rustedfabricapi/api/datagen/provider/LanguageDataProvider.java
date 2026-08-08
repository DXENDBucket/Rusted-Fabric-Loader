package io.github.endx.rustedfabricapi.api.datagen.provider;

import java.util.Locale;
import java.util.Objects;

import io.github.endx.rustedfabricapi.api.datagen.DataOutput;
import io.github.endx.rustedfabricapi.api.datagen.DataProvider;
import io.github.endx.rustedfabricapi.api.util.Identifier;

/** Base provider for deterministic UTF-8 translation properties. */
public abstract class LanguageDataProvider implements DataProvider {
    private final String namespace;
    private final String locale;

    protected LanguageDataProvider(String namespace, String locale) {
        this.namespace = Identifier.of(Objects.requireNonNull(namespace, "namespace"),
                "translation").namespace();
        this.locale = validateLocale(locale);
    }

    public String namespace() { return namespace; }

    public String locale() { return locale; }

    public String resourcePath() {
        return "assets/" + namespace + "/lang/" + locale + ".properties";
    }

    @Override public final void generate(DataOutput output) {
        LanguageBuilder builder = new LanguageBuilder(namespace);
        generateTranslations(builder);
        output.writeUtf8(resourcePath(), builder.propertiesText());
    }

    protected abstract void generateTranslations(LanguageBuilder translations);

    private static String validateLocale(String locale) {
        if (locale == null) throw new NullPointerException("locale");
        String value = locale.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        if (value.isEmpty() || value.length() > 32) {
            throw new IllegalArgumentException("Invalid language locale: " + locale);
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!(c >= 'a' && c <= 'z') && !(c >= '0' && c <= '9') && c != '_') {
                throw new IllegalArgumentException("Invalid language locale: " + locale);
            }
        }
        return value;
    }
}
