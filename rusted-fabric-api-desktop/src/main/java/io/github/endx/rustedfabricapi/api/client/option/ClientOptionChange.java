package io.github.endx.rustedfabricapi.api.client.option;

import java.util.Objects;

/** Immutable old/new value pair for one client option. */
public final class ClientOptionChange<T> {
    private final ClientOption<T> option;
    private final T previousValue;
    private final T requestedValue;

    ClientOptionChange(ClientOption<T> option, T previousValue, T requestedValue) {
        this.option = Objects.requireNonNull(option, "option");
        this.previousValue = previousValue;
        this.requestedValue = option.requireValid(requestedValue);
    }

    public static <T> ClientOptionChange<T> of(ClientOption<T> option,
            T previousValue, T requestedValue) {
        return new ClientOptionChange<T>(option, previousValue, requestedValue);
    }

    public ClientOption<T> option() { return option; }
    public T previousValue() { return previousValue; }
    public T requestedValue() { return requestedValue; }
    public boolean restartRequired() { return option.restartRequired(); }

    @Override
    public String toString() {
        return option + ": " + previousValue + " -> " + requestedValue;
    }
}
