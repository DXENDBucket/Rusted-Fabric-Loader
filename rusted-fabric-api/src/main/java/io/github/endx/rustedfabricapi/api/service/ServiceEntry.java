package io.github.endx.rustedfabricapi.api.service;

import io.github.endx.rustedfabricapi.api.util.Identifier;

import java.util.Objects;

/** Immutable description of one currently registered service provider. */
public final class ServiceEntry<T> {
    private final ServiceKey<T> key;
    private final Identifier providerId;
    private final int priority;
    private final T value;

    ServiceEntry(ServiceKey<T> key, Identifier providerId, int priority, T value) {
        this.key = Objects.requireNonNull(key, "key");
        this.providerId = Objects.requireNonNull(providerId, "providerId");
        this.priority = priority;
        this.value = Objects.requireNonNull(value, "value");
    }

    public ServiceKey<T> key() { return key; }

    public Identifier providerId() { return providerId; }

    public int priority() { return priority; }

    public T value() { return value; }

    @Override
    public String toString() {
        return "ServiceEntry{" + key.id() + " from " + providerId
                + ", priority=" + priority + '}';
    }
}
