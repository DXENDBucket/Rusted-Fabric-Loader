package io.github.endx.rustedfabricapi.api.ini;

/** A decoded extension value being applied to a unit's parsed metadata. */
public final class IniAppliedField<T> {
    private final IniFieldDefinition<T> definition;
    private final Object metadata;
    private final IniFieldContext source;
    private final T value;

    public IniAppliedField(IniFieldDefinition<T> definition, Object metadata,
                           IniFieldContext source, T value) {
        this.definition = definition;
        this.metadata = metadata;
        this.source = source;
        this.value = value;
    }

    public IniFieldDefinition<T> definition() { return definition; }
    public Object metadata() { return metadata; }
    public Object unitConfig() { return source.unitConfig(); }
    public IniFieldContext source() { return source; }
    public T value() { return value; }
}
