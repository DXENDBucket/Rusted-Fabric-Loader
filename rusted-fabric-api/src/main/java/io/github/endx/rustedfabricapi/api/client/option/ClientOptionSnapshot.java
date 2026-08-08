package io.github.endx.rustedfabricapi.api.client.option;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import rustedwarfare.core.SettingsEngine;

/** Immutable values for the complete supported client-option catalog at one instant. */
public final class ClientOptionSnapshot {
    private final Map<ClientOption<?>, Object> values;

    private ClientOptionSnapshot(SettingsEngine settings) {
        LinkedHashMap<ClientOption<?>, Object> captured =
                new LinkedHashMap<ClientOption<?>, Object>();
        for (ClientOption<?> option : ClientOptions.all()) {
            captured.put(option, option.get(settings));
        }
        this.values = Collections.unmodifiableMap(captured);
    }

    public static ClientOptionSnapshot capture(SettingsEngine settings) {
        return new ClientOptionSnapshot(Objects.requireNonNull(settings, "settings"));
    }

    @SuppressWarnings("unchecked")
    public <T> T get(ClientOption<T> option) {
        ClientOption<T> checked = Objects.requireNonNull(option, "option");
        if (!values.containsKey(checked)) {
            throw new IllegalArgumentException("Option is not present in this snapshot: " + checked);
        }
        return (T) values.get(checked);
    }

    public Map<ClientOption<?>, Object> values() { return values; }
    public int size() { return values.size(); }

    @Override
    public String toString() {
        return values.toString();
    }
}
