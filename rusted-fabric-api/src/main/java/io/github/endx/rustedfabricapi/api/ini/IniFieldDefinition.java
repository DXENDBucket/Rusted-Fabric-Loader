package io.github.endx.rustedfabricapi.api.ini;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Immutable definition of one opt-in INI extension. */
public final class IniFieldDefinition<T> {
    private static final Pattern OWNER_ID = Pattern.compile("[a-z][a-z0-9_-]*");
    private static final Pattern FIELD_ID = Pattern.compile("[a-z][a-z0-9_]*");

    @FunctionalInterface
    public interface Activation { boolean activates(IniFieldContext context); }
    @FunctionalInterface
    public interface Decoder<T> { T decode(IniFieldContext context) throws Exception; }
    @FunctionalInterface
    public interface Validator<T> { void validate(IniFieldContext context, T value) throws Exception; }
    @FunctionalInterface
    public interface NativeFallback<T> { String fallback(IniFieldContext context, T value); }
    @FunctionalInterface
    public interface Applier<T> { void apply(IniAppliedField<T> field) throws Exception; }

    private final String ownerId;
    private final String fieldId;
    private final IniSectionSelector section;
    private final String key;
    private final boolean keyPrefix;
    private final IniExtensionKind kind;
    private final IniApplicationPhase applicationPhase;
    private final Activation activation;
    private final Decoder<T> decoder;
    private final Validator<T> validator;
    private final NativeFallback<T> nativeFallback;
    private final Applier<T> applier;
    private final IniFieldDocumentation documentation;

    private IniFieldDefinition(Builder<T> builder) {
        ownerId = requireId(builder.ownerId, "ownerId", OWNER_ID);
        fieldId = requireId(builder.fieldId, "fieldId", FIELD_ID);
        section = Objects.requireNonNull(builder.section, "section");
        key = requireText(builder.key, "key");
        keyPrefix = builder.keyPrefix;
        kind = Objects.requireNonNull(builder.kind, "kind");
        applicationPhase = Objects.requireNonNull(builder.applicationPhase, "applicationPhase");
        decoder = Objects.requireNonNull(builder.decoder, "decoder");
        if (kind != IniExtensionKind.NEW_KEY && builder.activation == null) {
            throw new IllegalStateException(kind + " fields must declare activatesWhen(...); native values must remain untouched");
        }
        activation = builder.activation != null ? builder.activation : context -> true;
        validator = builder.validator != null ? builder.validator : (context, value) -> { };
        nativeFallback = builder.nativeFallback;
        applier = builder.applier != null ? builder.applier : field -> { };
        documentation = builder.documentation != null ? builder.documentation
                : new IniFieldDocumentation("", "", "", "", IniMultiplayerImpact.GAMEPLAY_SYNCED);
    }

    public static <T> Builder<T> builder(String ownerId, String fieldId,
                                          IniSectionSelector section, String key) {
        return new Builder<T>(ownerId, fieldId, section, key);
    }

    public String ownerId() { return ownerId; }
    public String fieldId() { return fieldId; }
    public String qualifiedId() { return ownerId + ":" + fieldId; }
    public IniSectionSelector section() { return section; }
    public String key() { return key; }
    public boolean matchesKeyPrefix() { return keyPrefix; }
    public IniExtensionKind kind() { return kind; }
    public IniApplicationPhase applicationPhase() { return applicationPhase; }
    public IniFieldDocumentation documentation() { return documentation; }

    public boolean matches(String candidateSection, String candidateKey) {
        return section.matches(candidateSection)
                && (keyPrefix ? candidateKey.startsWith(key) : key.equals(candidateKey));
    }

    public boolean activates(IniFieldContext context) { return activation.activates(context); }
    public T decode(IniFieldContext context) throws Exception { return decoder.decode(context); }
    public void validate(IniFieldContext context, T value) throws Exception { validator.validate(context, value); }
    public String nativeFallback(IniFieldContext context, T value) {
        if (nativeFallback == null) return null;
        String fallback = nativeFallback.fallback(context, value);
        if (fallback == null) throw new IllegalStateException("Native fallback returned null for " + qualifiedId());
        return fallback;
    }
    public boolean hasNativeFallback() { return nativeFallback != null; }
    public void apply(IniAppliedField<T> field) throws Exception { applier.apply(field); }

    public static final class Builder<T> {
        private final String ownerId;
        private final String fieldId;
        private final IniSectionSelector section;
        private final String key;
        private IniExtensionKind kind = IniExtensionKind.NEW_KEY;
        private IniApplicationPhase applicationPhase = IniApplicationPhase.AFTER_STATIC_VARIABLES;
        private Activation activation;
        private Decoder<T> decoder;
        private Validator<T> validator;
        private NativeFallback<T> nativeFallback;
        private Applier<T> applier;
        private IniFieldDocumentation documentation;
        private boolean keyPrefix;

        private Builder(String ownerId, String fieldId, IniSectionSelector section, String key) {
            this.ownerId = ownerId;
            this.fieldId = fieldId;
            this.section = section;
            this.key = key;
        }

        public Builder<T> kind(IniExtensionKind value) { kind = value; return this; }
        public Builder<T> applicationPhase(IniApplicationPhase value) { applicationPhase = value; return this; }
        public Builder<T> activatesWhen(Activation value) { activation = value; return this; }
        public Builder<T> decoder(Decoder<T> value) { decoder = value; return this; }
        public Builder<T> validator(Validator<T> value) { validator = value; return this; }
        public Builder<T> nativeFallback(NativeFallback<T> value) { nativeFallback = value; return this; }
        public Builder<T> applier(Applier<T> value) { applier = value; return this; }
        public Builder<T> documentation(IniFieldDocumentation value) { documentation = value; return this; }
        /** Matches all keys beginning with the builder key; activation can narrow suffixes. */
        public Builder<T> matchKeyPrefix() { keyPrefix = true; return this; }
        public IniFieldDefinition<T> build() { return new IniFieldDefinition<T>(this); }
    }

    private static String requireId(String value, String label, Pattern pattern) {
        String result = requireText(value, label).toLowerCase(Locale.ROOT);
        if (!pattern.matcher(result).matches()) {
            throw new IllegalArgumentException("Invalid lowercase " + label + ": " + value);
        }
        return result;
    }

    private static String requireText(String value, String label) {
        String result = Objects.requireNonNull(value, label).trim();
        if (result.isEmpty()) throw new IllegalArgumentException(label + " must not be empty");
        return result;
    }
}
