package io.github.endx.rustedfabricapi.api.ini.action;

import io.github.endx.rustedfabricapi.api.ini.IniFieldDocumentation;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Definition of one opt-in field that becomes a native custom-action effect. */
public final class IniActionEffectDefinition<T> {
    private static final Pattern OWNER_ID = Pattern.compile("[a-z][a-z0-9_-]*");
    private static final Pattern FIELD_ID = Pattern.compile("[a-z][a-z0-9_]*");

    @FunctionalInterface
    public interface Decoder<T> { T decode(IniActionFieldContext context) throws Exception; }
    @FunctionalInterface
    public interface Validator<T> {
        void validate(IniActionFieldContext context, T value) throws Exception;
    }
    @FunctionalInterface
    public interface Handler<T> {
        void execute(IniActionExecutionContext context, T value) throws Exception;
    }

    private final String ownerId;
    private final String fieldId;
    private final String key;
    private final IniActionSectionScope scope;
    private final int priority;
    private final String exclusiveGroup;
    private final Decoder<T> decoder;
    private final Validator<T> validator;
    private final Handler<T> handler;
    private final IniFieldDocumentation documentation;

    private IniActionEffectDefinition(Builder<T> builder) {
        ownerId = requireId(builder.ownerId, "ownerId", OWNER_ID);
        fieldId = requireId(builder.fieldId, "fieldId", FIELD_ID);
        key = requireText(builder.key, "key");
        scope = Objects.requireNonNull(builder.scope, "scope");
        priority = builder.priority;
        exclusiveGroup = builder.exclusiveGroup == null ? null
                : requireId(builder.exclusiveGroup, "exclusiveGroup", FIELD_ID);
        decoder = Objects.requireNonNull(builder.decoder, "decoder");
        validator = builder.validator != null ? builder.validator : (context, value) -> { };
        handler = Objects.requireNonNull(builder.handler, "handler");
        documentation = builder.documentation != null ? builder.documentation
                : new IniFieldDocumentation("", "", "", "", null);
    }

    public static <T> Builder<T> builder(String ownerId, String fieldId, String key) {
        return new Builder<T>(ownerId, fieldId, key);
    }

    public String ownerId() { return ownerId; }
    public String fieldId() { return fieldId; }
    public String qualifiedId() { return ownerId + ":" + fieldId; }
    public String key() { return key; }
    public IniActionSectionScope scope() { return scope; }
    /** Higher-priority effects are attached and executed before lower-priority effects. */
    public int priority() { return priority; }
    public String exclusiveGroup() { return exclusiveGroup; }
    public IniFieldDocumentation documentation() { return documentation; }
    public T decode(IniActionFieldContext context) throws Exception { return decoder.decode(context); }
    public void validate(IniActionFieldContext context, T value) throws Exception {
        validator.validate(context, value);
    }
    public void execute(IniActionExecutionContext context, T value) throws Exception {
        handler.execute(context, value);
    }

    public static final class Builder<T> {
        private final String ownerId;
        private final String fieldId;
        private final String key;
        private IniActionSectionScope scope = IniActionSectionScope.ACTION_AND_HIDDEN;
        private int priority;
        private String exclusiveGroup;
        private Decoder<T> decoder;
        private Validator<T> validator;
        private Handler<T> handler;
        private IniFieldDocumentation documentation;

        private Builder(String ownerId, String fieldId, String key) {
            this.ownerId = ownerId;
            this.fieldId = fieldId;
            this.key = key;
        }

        public Builder<T> scope(IniActionSectionScope value) { scope = value; return this; }
        public Builder<T> priority(int value) { priority = value; return this; }
        public Builder<T> exclusiveGroup(String value) { exclusiveGroup = value; return this; }
        public Builder<T> decoder(Decoder<T> value) { decoder = value; return this; }
        public Builder<T> validator(Validator<T> value) { validator = value; return this; }
        public Builder<T> handler(Handler<T> value) { handler = value; return this; }
        public Builder<T> documentation(IniFieldDocumentation value) {
            documentation = value;
            return this;
        }
        public IniActionEffectDefinition<T> build() {
            return new IniActionEffectDefinition<T>(this);
        }
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
