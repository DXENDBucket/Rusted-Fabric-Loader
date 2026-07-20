package io.github.endx.rustedfabricapi.api.service;

import io.github.endx.rustedfabricapi.api.util.Identifier;

import java.util.Objects;

/** Typed identity shared by providers and consumers of an optional mod service. */
public final class ServiceKey<T> implements Comparable<ServiceKey<?>> {
    private final Identifier id;
    private final Class<T> valueType;

    private ServiceKey(Identifier id, Class<T> valueType) {
        this.id = id;
        this.valueType = valueType;
    }

    public static <T> ServiceKey<T> of(Identifier id, Class<T> valueType) {
        return new ServiceKey<T>(Objects.requireNonNull(id, "id"),
                Objects.requireNonNull(valueType, "valueType"));
    }

    public static <T> ServiceKey<T> of(String id, Class<T> valueType) {
        return of(Identifier.parse(id), valueType);
    }

    public Identifier id() { return id; }

    public Class<T> valueType() { return valueType; }

    @Override
    public int compareTo(ServiceKey<?> other) {
        int byId = id.compareTo(Objects.requireNonNull(other, "other").id);
        return byId != 0 ? byId : valueType.getName().compareTo(other.valueType.getName());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ServiceKey<?>)) return false;
        ServiceKey<?> key = (ServiceKey<?>) other;
        return id.equals(key.id) && valueType.equals(key.valueType);
    }

    @Override
    public int hashCode() { return 31 * id.hashCode() + valueType.hashCode(); }

    @Override
    public String toString() {
        return "ServiceKey{" + id + " -> " + valueType.getName() + '}';
    }
}
