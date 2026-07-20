package io.github.endx.rustedfabricapi.api.service;

import io.github.endx.rustedfabricapi.api.util.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

/**
 * Thread-safe typed registry for optional inter-mod integration services.
 *
 * <p>Provider selection is deterministic: higher priority wins, then the lexicographically
 * smaller provider identifier. Registration/load order is never used as a tie-breaker.</p>
 */
public final class ServiceRegistry {
    private static final Comparator<ServiceEntry<?>> ENTRY_ORDER =
            Comparator.<ServiceEntry<?>, Integer>comparing(ServiceEntry::priority)
                    .reversed()
                    .thenComparing(ServiceEntry::providerId);

    private final Object lock = new Object();
    private final Map<Identifier, Class<?>> declaredTypes =
            new LinkedHashMap<Identifier, Class<?>>();
    private final Map<ServiceKey<?>, Map<Identifier, RegisteredEntry<?>>> services =
            new LinkedHashMap<ServiceKey<?>, Map<Identifier, RegisteredEntry<?>>>();

    public <T> ServiceRegistration<T> register(ServiceKey<T> key, Identifier providerId,
            T value) {
        return register(key, providerId, 0, value);
    }

    public <T> ServiceRegistration<T> register(ServiceKey<T> key, String providerId,
            T value) {
        return register(key, Identifier.parse(providerId), 0, value);
    }

    public <T> ServiceRegistration<T> register(ServiceKey<T> key, String providerId,
            int priority, T value) {
        return register(key, Identifier.parse(providerId), priority, value);
    }

    public <T> ServiceRegistration<T> register(ServiceKey<T> key, Identifier providerId,
            int priority, T value) {
        ServiceKey<T> checkedKey = Objects.requireNonNull(key, "key");
        Identifier checkedProvider = Objects.requireNonNull(providerId, "providerId");
        T checkedValue = Objects.requireNonNull(value, "value");
        if (!checkedKey.valueType().isInstance(checkedValue)) {
            throw new IllegalArgumentException("Service value for " + checkedKey.id()
                    + " is not a " + checkedKey.valueType().getName());
        }

        RegisteredEntry<T> registered;
        synchronized (lock) {
            Class<?> declared = declaredTypes.get(checkedKey.id());
            if (declared != null && !declared.equals(checkedKey.valueType())) {
                throw new IllegalStateException("Service " + checkedKey.id()
                        + " was already declared as " + declared.getName()
                        + ", not " + checkedKey.valueType().getName());
            }
            declaredTypes.put(checkedKey.id(), checkedKey.valueType());
            Map<Identifier, RegisteredEntry<?>> providers = services.get(checkedKey);
            if (providers == null) {
                providers = new LinkedHashMap<Identifier, RegisteredEntry<?>>();
                services.put(checkedKey, providers);
            }
            if (providers.containsKey(checkedProvider)) {
                throw new IllegalStateException("Provider " + checkedProvider
                        + " already registered service " + checkedKey.id());
            }
            registered = new RegisteredEntry<T>(this,
                    new ServiceEntry<T>(checkedKey, checkedProvider, priority, checkedValue));
            providers.put(checkedProvider, registered);
        }
        return registered;
    }

    public <T> Optional<T> find(ServiceKey<T> key) {
        List<ServiceEntry<T>> entries = entries(key);
        return entries.isEmpty() ? Optional.empty() : Optional.of(entries.get(0).value());
    }

    public <T> T require(ServiceKey<T> key) {
        return find(key).orElseThrow(() -> new NoSuchElementException(
                "No provider registered for service " + key.id()));
    }

    public <T> List<T> all(ServiceKey<T> key) {
        List<ServiceEntry<T>> entries = entries(key);
        ArrayList<T> values = new ArrayList<T>(entries.size());
        for (ServiceEntry<T> entry : entries) values.add(entry.value());
        return Collections.unmodifiableList(values);
    }

    public <T> List<ServiceEntry<T>> entries(ServiceKey<T> key) {
        ServiceKey<T> checkedKey = Objects.requireNonNull(key, "key");
        ArrayList<ServiceEntry<T>> result = new ArrayList<ServiceEntry<T>>();
        synchronized (lock) {
            checkDeclaredTypeLocked(checkedKey);
            Map<Identifier, RegisteredEntry<?>> providers = services.get(checkedKey);
            if (providers != null) {
                for (RegisteredEntry<?> provider : providers.values()) {
                    @SuppressWarnings("unchecked")
                    ServiceEntry<T> entry = (ServiceEntry<T>) provider.entry;
                    result.add(entry);
                }
            }
        }
        result.sort((left, right) -> ENTRY_ORDER.compare(left, right));
        return Collections.unmodifiableList(result);
    }

    public List<ServiceKey<?>> keys() {
        ArrayList<ServiceKey<?>> result;
        synchronized (lock) {
            result = new ArrayList<ServiceKey<?>>(services.keySet());
        }
        Collections.sort(result);
        return Collections.unmodifiableList(result);
    }

    public <T> int providerCount(ServiceKey<T> key) {
        return entries(key).size();
    }

    private void checkDeclaredTypeLocked(ServiceKey<?> key) {
        Class<?> declared = declaredTypes.get(key.id());
        if (declared != null && !declared.equals(key.valueType())) {
            throw new IllegalStateException("Service " + key.id()
                    + " was declared as " + declared.getName()
                    + ", not " + key.valueType().getName());
        }
    }

    private boolean unregister(RegisteredEntry<?> registration) {
        synchronized (lock) {
            Map<Identifier, RegisteredEntry<?>> providers = services.get(registration.entry.key());
            if (providers == null
                    || providers.get(registration.entry.providerId()) != registration) return false;
            providers.remove(registration.entry.providerId());
            if (providers.isEmpty()) services.remove(registration.entry.key());
            return true;
        }
    }

    private static final class RegisteredEntry<T> implements ServiceRegistration<T> {
        private final ServiceRegistry registry;
        private final ServiceEntry<T> entry;
        private boolean active = true;

        RegisteredEntry(ServiceRegistry registry, ServiceEntry<T> entry) {
            this.registry = registry;
            this.entry = entry;
        }

        @Override public ServiceKey<T> key() { return entry.key(); }
        @Override public Identifier providerId() { return entry.providerId(); }
        @Override public int priority() { return entry.priority(); }
        @Override public T value() { return entry.value(); }

        @Override
        public synchronized boolean isActive() { return active; }

        @Override
        public synchronized boolean unregister() {
            if (!active) return false;
            active = false;
            return registry.unregister(this);
        }

        @Override
        public void close() { unregister(); }
    }
}
