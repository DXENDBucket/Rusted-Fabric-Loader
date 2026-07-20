package io.github.endx.rustedfabricapi.api.service;

import io.github.endx.rustedfabricapi.api.util.Identifier;

/** Idempotent handle for a service provider registration. */
public interface ServiceRegistration<T> extends AutoCloseable {
    ServiceKey<T> key();

    Identifier providerId();

    int priority();

    T value();

    boolean isActive();

    boolean unregister();

    @Override
    void close();
}
