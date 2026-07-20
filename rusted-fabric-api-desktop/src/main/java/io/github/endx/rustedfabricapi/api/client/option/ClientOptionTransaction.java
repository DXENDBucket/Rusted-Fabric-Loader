package io.github.endx.rustedfabricapi.api.client.option;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import rustedwarfare.core.SettingsEngine;

/** Mutable transaction builder; changes are not visible until the enclosing update commits. */
public final class ClientOptionTransaction {
    private final SettingsEngine settings;
    private final Map<ClientOption<?>, Object> requested =
            new LinkedHashMap<ClientOption<?>, Object>();

    ClientOptionTransaction(SettingsEngine settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    public <T> ClientOptionTransaction set(ClientOption<T> option, T value) {
        ClientOption<T> checked = Objects.requireNonNull(option, "option");
        requested.put(checked, checked.requireValid(value));
        return this;
    }

    /** Returns the pending value when set in this transaction, otherwise the current native value. */
    @SuppressWarnings("unchecked")
    public <T> T get(ClientOption<T> option) {
        ClientOption<T> checked = Objects.requireNonNull(option, "option");
        if (requested.containsKey(checked)) return (T) requested.get(checked);
        return checked.get(settings);
    }

    public ClientOptionTransaction reset(ClientOption<?> option) {
        requested.remove(Objects.requireNonNull(option, "option"));
        return this;
    }

    ClientOptionChangeSet changes() {
        ArrayList<ClientOptionChange<?>> result = new ArrayList<ClientOptionChange<?>>();
        for (Map.Entry<ClientOption<?>, Object> entry : requested.entrySet()) {
            addChange(result, entry.getKey(), entry.getValue());
        }
        return result.isEmpty() ? ClientOptionChangeSet.empty()
                : new ClientOptionChangeSet(result);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void addChange(List<ClientOptionChange<?>> result, ClientOption option, Object value) {
        Object previous = option.get(settings);
        if (!Objects.equals(previous, value)) {
            result.add(new ClientOptionChange(option, previous, value));
        }
    }
}
