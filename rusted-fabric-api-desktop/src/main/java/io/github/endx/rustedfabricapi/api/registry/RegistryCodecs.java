package io.github.endx.rustedfabricapi.api.registry;

import java.util.ArrayList;
import java.util.Objects;

import io.github.endx.rustedfabricapi.api.networking.PacketBuffer;
import io.github.endx.rustedfabricapi.api.networking.PacketCodec;
import io.github.endx.rustedfabricapi.api.util.Identifier;

/** Stable-identifier codecs for registry values, entries, and compatibility snapshots. */
public final class RegistryCodecs {
    public static final PacketCodec<RegistrySnapshot> SNAPSHOT = PacketCodec.of(
            RegistryCodecs::encodeSnapshot, RegistryCodecs::decodeSnapshot);

    private RegistryCodecs() {
    }

    public static <T> PacketCodec<T> value(ModRegistry<T> registry) {
        ModRegistry<T> checked = Objects.requireNonNull(registry, "registry");
        return PacketCodec.of((buffer, value) -> writeId(buffer,
                        checked.entry(value).orElseThrow(() -> new IllegalArgumentException(
                                "Value is not registered in " + checked.key().id())).id()),
                buffer -> checked.getOrThrow(readId(buffer)));
    }

    public static <T> PacketCodec<RegistryEntry<T>> entry(ModRegistry<T> registry) {
        ModRegistry<T> checked = Objects.requireNonNull(registry, "registry");
        return PacketCodec.of((buffer, value) -> {
                    RegistryEntry<T> entry = Objects.requireNonNull(value, "value");
                    RegistryEntry<T> current = checked.entry(entry.id())
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Entry is not registered in " + checked.key().id()));
                    if (current != entry) {
                        throw new IllegalArgumentException("Entry belongs to another registry snapshot");
                    }
                    writeId(buffer, entry.id());
                }, buffer -> checked.entry(readId(buffer)).orElseThrow(() ->
                        new IllegalArgumentException("Unknown registry entry in "
                                + checked.key().id())));
    }

    private static void encodeSnapshot(PacketBuffer buffer, RegistrySnapshot snapshot) {
        RegistrySnapshot checked = Objects.requireNonNull(snapshot, "snapshot");
        writeId(buffer, checked.registryId());
        buffer.writeString(checked.valueTypeName(), RegistrySnapshot.MAX_VALUE_TYPE_CHARS);
        buffer.writeBoolean(checked.frozen());
        buffer.writeVarInt(checked.size());
        for (Identifier id : checked.ids()) writeId(buffer, id);
    }

    private static RegistrySnapshot decodeSnapshot(PacketBuffer buffer) {
        Identifier registryId = readId(buffer);
        String valueType = buffer.readString(RegistrySnapshot.MAX_VALUE_TYPE_CHARS);
        boolean frozen = buffer.readBoolean();
        int size = buffer.readVarInt();
        if (size < 0 || size > RegistrySnapshot.MAX_ENTRIES) {
            throw new IllegalArgumentException("Invalid registry snapshot size: " + size);
        }
        ArrayList<Identifier> ids = new ArrayList<Identifier>(size);
        for (int i = 0; i < size; i++) ids.add(readId(buffer));
        return RegistrySnapshot.of(registryId, valueType, ids, frozen);
    }

    private static void writeId(PacketBuffer buffer, Identifier id) {
        buffer.writeIdentifier(Objects.requireNonNull(id, "id"));
    }

    private static Identifier readId(PacketBuffer buffer) {
        return buffer.readIdentifier();
    }
}
