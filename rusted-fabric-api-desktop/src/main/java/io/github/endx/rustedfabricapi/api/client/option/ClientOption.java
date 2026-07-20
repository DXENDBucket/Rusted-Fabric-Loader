package io.github.endx.rustedfabricapi.api.client.option;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import io.github.endx.rustedfabricapi.api.util.Identifier;
import rustedwarfare.core.SettingsEngine;

/** A typed, validated key for one supported native client setting. */
public final class ClientOption<T> {
    private final Identifier id;
    private final String nativeName;
    private final Class<T> type;
    private final boolean restartRequired;
    private final Function<SettingsEngine, T> reader;
    private final BiConsumer<SettingsEngine, T> writer;
    private final Predicate<T> validator;
    private final String validationDescription;

    ClientOption(Identifier id, String nativeName, Class<T> type, boolean restartRequired,
            Function<SettingsEngine, T> reader, BiConsumer<SettingsEngine, T> writer,
            Predicate<T> validator, String validationDescription) {
        this.id = Objects.requireNonNull(id, "id");
        this.nativeName = Objects.requireNonNull(nativeName, "nativeName");
        this.type = Objects.requireNonNull(type, "type");
        this.restartRequired = restartRequired;
        this.reader = Objects.requireNonNull(reader, "reader");
        this.writer = Objects.requireNonNull(writer, "writer");
        this.validator = Objects.requireNonNull(validator, "validator");
        this.validationDescription = Objects.requireNonNull(
                validationDescription, "validationDescription");
    }

    public Identifier id() { return id; }
    public String nativeName() { return nativeName; }
    public Class<T> type() { return type; }
    public boolean restartRequired() { return restartRequired; }

    /** Reads this option from an explicitly supplied native settings object. */
    public T get(SettingsEngine settings) {
        return reader.apply(Objects.requireNonNull(settings, "settings"));
    }

    public boolean isValid(T value) {
        return value != null && type.isInstance(value) && validator.test(value);
    }

    public T requireValid(T value) {
        if (!isValid(value)) {
            throw new IllegalArgumentException(id + " must be " + validationDescription);
        }
        return value;
    }

    void set(SettingsEngine settings, T value) {
        writer.accept(Objects.requireNonNull(settings, "settings"), requireValid(value));
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
