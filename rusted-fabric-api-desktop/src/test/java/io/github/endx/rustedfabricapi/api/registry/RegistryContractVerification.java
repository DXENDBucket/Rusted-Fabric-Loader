package io.github.endx.rustedfabricapi.api.registry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import io.github.endx.rustedfabricapi.api.networking.PacketBuffer;
import io.github.endx.rustedfabricapi.api.networking.PacketCodec;
import io.github.endx.rustedfabricapi.api.util.Identifier;

/** Stable ID, raw-ID, identity, event, type, and freeze checks for mod registries. */
public final class RegistryContractVerification {
    private RegistryContractVerification() {
    }

    public static void verify() {
        RegistryKey<Widget> key = RegistryKey.of("contract:widgets", Widget.class);
        ModRegistry<Widget> registry = ModRegistries.create(key);
        List<String> events = new ArrayList<String>();
        RustedFabricEvent.Registration added = registry.events().AFTER_ENTRY_ADDED.subscribe(
                (source, entry) -> events.add(entry.rawId() + ":" + entry.id()));
        RustedFabricEvent.Registration beforeFreeze = registry.events().BEFORE_FREEZE.subscribe(
                source -> events.add("before:" + source.size()));
        RustedFabricEvent.Registration afterFreeze = registry.events().AFTER_FREEZE.subscribe(
                source -> events.add("after:" + source.size()));

        Widget alpha = new Widget("alpha");
        Widget beta = new Widget("beta");
        RegistryEntry<Widget> first = registry.register("contract:alpha", alpha);
        RegistryEntry<Widget> second = registry.register("contract:beta", beta);
        require(first.rawId() == 0 && second.rawId() == 1,
                "registry raw IDs did not follow insertion order");
        require(registry.values().equals(Arrays.asList(alpha, beta))
                        && registry.ids().equals(Arrays.asList(
                                Identifier.parse("contract:alpha"),
                                Identifier.parse("contract:beta"))),
                "registry snapshots lost insertion order");
        require(registry.entry(1).orElseThrow(AssertionError::new) == second
                        && registry.entry(beta).orElseThrow(AssertionError::new) == second
                        && registry.getOrThrow(Identifier.parse("contract:alpha")) == alpha,
                "registry lookup did not preserve entry or value identity");
        require(ModRegistries.find(RegistryKey.of("contract:widgets", Widget.class))
                        .orElseThrow(AssertionError::new) == registry,
                "root registry lookup did not resolve an equivalent typed key");

        expectFailure(() -> registry.register("contract:alpha", new Widget("duplicate")),
                "duplicate registry ID was accepted");
        expectFailure(() -> registry.register("contract:duplicate_value", alpha),
                "duplicate value identity was accepted");
        expectFailure(() -> ModRegistries.find(
                        RegistryKey.of("contract:widgets", String.class)),
                "registry root accepted an incompatible value type");

        require(registry.freeze() && !registry.freeze() && registry.isFrozen(),
                "registry freeze was not idempotent");
        expectFailure(() -> registry.register("contract:late", new Widget("late")),
                "frozen registry accepted a late entry");
        require(events.equals(Arrays.asList(
                        "0:contract:alpha", "1:contract:beta", "before:2", "after:2")),
                "registry events were missing or out of order: " + events);
        require(ModRegistries.snapshot().contains(registry),
                "root registry snapshot omitted the custom registry");

        RegistrySnapshot local = registry.snapshot();
        RegistrySnapshot roundTrip = RegistryCodecs.SNAPSHOT.decodePayload(
                RegistryCodecs.SNAPSHOT.encodePayload(local));
        RegistryComparison exact = local.compare(roundTrip);
        require(exact.status() == RegistryComparison.Status.EXACT_LAYOUT
                        && exact.stableIdsCompatible() && exact.rawIdsCompatible()
                        && local.contentFingerprint().equals(roundTrip.contentFingerprint())
                        && local.layoutFingerprint().equals(roundTrip.layoutFingerprint()),
                "frozen registry snapshot did not survive a binary round trip");

        RegistrySnapshot reordered = RegistrySnapshot.of(key.id(), key.valueType().getName(),
                Arrays.asList(Identifier.parse("contract:beta"),
                        Identifier.parse("contract:alpha")), true);
        RegistryComparison orderMismatch = local.compare(reordered);
        require(orderMismatch.status()
                        == RegistryComparison.Status.SAME_ENTRIES_DIFFERENT_ORDER
                        && orderMismatch.stableIdsCompatible()
                        && !orderMismatch.rawIdsCompatible()
                        && local.contentFingerprint().equals(reordered.contentFingerprint())
                        && !local.layoutFingerprint().equals(reordered.layoutFingerprint()),
                "registry comparison treated a raw-ID order mismatch as safe");

        RegistrySnapshot different = RegistrySnapshot.of(key.id(), key.valueType().getName(),
                Arrays.asList(Identifier.parse("contract:alpha"),
                        Identifier.parse("contract:gamma")), true);
        RegistryComparison contentMismatch = local.compare(different);
        require(contentMismatch.status() == RegistryComparison.Status.DIFFERENT_ENTRIES
                        && contentMismatch.missingLocally().equals(Arrays.asList(
                                Identifier.parse("contract:gamma")))
                        && contentMismatch.missingRemotely().equals(Arrays.asList(
                                Identifier.parse("contract:beta")))
                        && !contentMismatch.stableIdsCompatible(),
                "registry comparison lost its directional missing-entry sets");

        PacketCodec<Widget> valueCodec = RegistryCodecs.value(registry);
        PacketCodec<RegistryEntry<Widget>> entryCodec = RegistryCodecs.entry(registry);
        require(valueCodec.decodePayload(valueCodec.encodePayload(alpha)) == alpha
                        && entryCodec.decodePayload(entryCodec.encodePayload(second)) == second,
                "stable registry codecs did not preserve local value/entry identity");
        PacketBuffer unknown = PacketBuffer.writer();
        unknown.writeString("contract:missing");
        expectFailure(() -> valueCodec.decodePayload(unknown.toPayload()),
                "stable registry codec accepted an unknown remote ID");

        added.close();
        beforeFreeze.close();
        afterFreeze.close();
    }

    private static void expectFailure(Runnable action, String message) {
        try {
            action.run();
            throw new AssertionError(message);
        } catch (IllegalArgumentException | IllegalStateException expected) {
            // Expected.
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class Widget {
        final String name;

        Widget(String name) { this.name = name; }
    }
}
