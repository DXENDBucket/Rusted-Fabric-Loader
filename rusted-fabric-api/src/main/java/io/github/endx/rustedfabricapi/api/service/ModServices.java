package io.github.endx.rustedfabricapi.api.service;

import io.github.endx.rustedfabricapi.api.util.Identifier;

import java.util.List;
import java.util.Optional;

/** Process-wide service registry used by ordinary mod entrypoints. */
public final class ModServices {
    private static final ServiceRegistry GLOBAL = new ServiceRegistry();

    private ModServices() {
    }

    public static ServiceRegistry global() { return GLOBAL; }

    public static <T> ServiceRegistration<T> register(ServiceKey<T> key,
            Identifier providerId, T value) {
        return GLOBAL.register(key, providerId, value);
    }

    public static <T> ServiceRegistration<T> register(ServiceKey<T> key,
            String providerId, T value) {
        return GLOBAL.register(key, providerId, value);
    }

    public static <T> ServiceRegistration<T> register(ServiceKey<T> key,
            Identifier providerId, int priority, T value) {
        return GLOBAL.register(key, providerId, priority, value);
    }

    public static <T> ServiceRegistration<T> register(ServiceKey<T> key,
            String providerId, int priority, T value) {
        return GLOBAL.register(key, providerId, priority, value);
    }

    public static <T> Optional<T> find(ServiceKey<T> key) { return GLOBAL.find(key); }

    public static <T> T require(ServiceKey<T> key) { return GLOBAL.require(key); }

    public static <T> List<T> all(ServiceKey<T> key) { return GLOBAL.all(key); }

    public static <T> List<ServiceEntry<T>> entries(ServiceKey<T> key) {
        return GLOBAL.entries(key);
    }
}
