package io.github.endx.rustedfabricapi.api.service;

import io.github.endx.rustedfabricapi.api.lifecycle.LifecycleScope;
import io.github.endx.rustedfabricapi.api.util.Identifier;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

public final class ServiceRegistryContractVerification {
    private ServiceRegistryContractVerification() {
    }

    public static void verify() {
        ServiceRegistry registry = new ServiceRegistry();
        ServiceKey<Greeting> key = ServiceKey.of("contract:greeting", Greeting.class);
        ServiceRegistration<Greeting> beta = registry.register(
                key, "beta:provider", 10, () -> "beta");
        ServiceRegistration<Greeting> alpha = registry.register(
                key, Identifier.parse("alpha:provider"), 10, () -> "alpha");
        ServiceRegistration<Greeting> preferred = registry.register(
                key, "contract:preferred", 20, () -> "preferred");

        require("preferred".equals(registry.require(key).message()),
                "highest-priority service was not selected");
        List<ServiceEntry<Greeting>> entries = registry.entries(key);
        require(entries.get(0).providerId().toString().equals("contract:preferred")
                        && entries.get(1).providerId().toString().equals("alpha:provider")
                        && entries.get(2).providerId().toString().equals("beta:provider"),
                "service providers were not ordered by priority then stable identifier");
        List<Greeting> values = registry.all(key);
        require(Arrays.asList(values.get(0).message(), values.get(1).message(),
                        values.get(2).message())
                        .equals(Arrays.asList("preferred", "alpha", "beta")),
                "service value order disagrees with entry order");
        require(registry.keys().equals(Arrays.asList(key)),
                "active service keys were not reported");

        boolean immutable = false;
        try {
            entries.clear();
        } catch (UnsupportedOperationException expected) {
            immutable = true;
        }
        require(immutable, "service entry snapshot was mutable");

        boolean duplicateRejected = false;
        try {
            registry.register(key, "alpha:provider", () -> "duplicate");
        } catch (IllegalStateException expected) {
            duplicateRejected = true;
        }
        require(duplicateRejected, "duplicate service provider id was accepted");

        ServiceKey<Runnable> conflicting = ServiceKey.of("contract:greeting", Runnable.class);
        boolean conflictingTypeRejected = false;
        try {
            registry.entries(conflicting);
        } catch (IllegalStateException expected) {
            conflictingTypeRejected = true;
        }
        require(conflictingTypeRejected, "one service id was allowed to change value type");

        preferred.close();
        require("alpha".equals(registry.find(key).orElseThrow().message()),
                "provider fallback was not deterministic after unregister");
        require(!preferred.unregister(), "service registration was not idempotent");
        alpha.close();
        beta.close();
        require(registry.providerCount(key) == 0 && registry.keys().isEmpty(),
                "closed service providers remained discoverable");

        boolean missingRejected = false;
        try {
            registry.require(key);
        } catch (NoSuchElementException expected) {
            missingRejected = true;
        }
        require(missingRejected, "required lookup accepted a missing service");

        ServiceKey<Greeting> globalKey = ServiceKey.of(
                "contract:global_greeting", Greeting.class);
        LifecycleScope owner = LifecycleScope.create("service-owner");
        owner.own("global service", ModServices.register(
                globalKey, "contract:global_provider", () -> "global"));
        require("global".equals(ModServices.require(globalKey).message()),
                "process-wide service facade did not expose its provider");
        owner.close();
        require(!ModServices.find(globalKey).isPresent(),
                "lifecycle-owned global service survived owner close");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private interface Greeting {
        String message();
    }
}
