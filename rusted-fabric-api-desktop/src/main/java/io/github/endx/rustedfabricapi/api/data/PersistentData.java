package io.github.endx.rustedfabricapi.api.data;

import io.github.endx.rustedfabricapi.api.data.event.PersistentDataEvents;
import io.github.endx.rustedfabricapi.api.networking.PacketBuffer;
import io.github.endx.rustedfabricapi.api.networking.PacketPayload;
import io.github.endx.rustedfabricapi.api.util.Identifier;
import rustedwarfare.framework.GameObject;
import rustedwarfare.io.GameInputStream;
import rustedwarfare.io.GameOutputStream;
import rustedwarfare.unit.Unit;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.WeakHashMap;

/**
 * Fabric-style namespaced persistent components for the current world and individual units.
 *
 * <p>The data is stored in a length-delimited Loader extension after the native save terminator.
 * Vanilla ignores the trailing block, while Loader saves, replay snapshots and multiplayer resync
 * streams restore it. Unknown keys are retained as raw bytes when their mod is absent.</p>
 */
public final class PersistentData {
    public static final int FORMAT_VERSION = 1;
    public static final int MAX_EXTENSION_BYTES = 16 * 1024 * 1024;
    public static final int MAX_ENTRIES = 65_536;

    private static final int MAGIC = 0x52464A44; // RFJD
    private static final Object LOCK = new Object();
    private static final Map<Identifier, PersistentDataKey<?>> KEYS =
            new LinkedHashMap<Identifier, PersistentDataKey<?>>();
    private static final Map<Identifier, StoredValue> GLOBAL =
            new LinkedHashMap<Identifier, StoredValue>();
    private static final Map<Unit, Map<Identifier, StoredValue>> UNITS =
            new WeakHashMap<Unit, Map<Identifier, StoredValue>>();

    private PersistentData() {
    }

    public static <T> PersistentDataKey<T> register(
            Identifier id, int version, PersistentDataCodec<T> codec) {
        PersistentDataKey<T> key = new PersistentDataKey<T>(id, version, codec);
        synchronized (LOCK) {
            if (KEYS.containsKey(id)) {
                throw new IllegalStateException("Persistent data key is already registered: " + id);
            }
            KEYS.put(id, key);
        }
        return key;
    }

    public static <T> PersistentDataKey<T> register(
            String namespace, String path, int version, PersistentDataCodec<T> codec) {
        return register(Identifier.of(namespace, path), version, codec);
    }

    public static Set<Identifier> registeredIds() {
        synchronized (LOCK) {
            return Collections.unmodifiableSet(new TreeSet<Identifier>(KEYS.keySet()));
        }
    }

    public static <T> void setGlobal(PersistentDataKey<T> key, T value) {
        synchronized (LOCK) {
            requireRegistered(key);
            GLOBAL.put(key.id(), StoredValue.decoded(key, Objects.requireNonNull(value, "value")));
        }
    }

    public static <T> Optional<T> getGlobal(PersistentDataKey<T> key) {
        synchronized (LOCK) {
            requireRegistered(key);
            return decode(key, GLOBAL.get(key.id()), null);
        }
    }

    public static boolean removeGlobal(PersistentDataKey<?> key) {
        synchronized (LOCK) {
            requireRegistered(key);
            return GLOBAL.remove(key.id()) != null;
        }
    }

    public static Set<Identifier> globalIds() {
        synchronized (LOCK) {
            return Collections.unmodifiableSet(new TreeSet<Identifier>(GLOBAL.keySet()));
        }
    }

    public static <T> void set(Unit unit, PersistentDataKey<T> key, T value) {
        Objects.requireNonNull(unit, "unit");
        synchronized (LOCK) {
            requireRegistered(key);
            Map<Identifier, StoredValue> values = UNITS.get(unit);
            if (values == null) {
                values = new LinkedHashMap<Identifier, StoredValue>();
                UNITS.put(unit, values);
            }
            values.put(key.id(), StoredValue.decoded(key, Objects.requireNonNull(value, "value")));
        }
    }

    public static <T> Optional<T> get(Unit unit, PersistentDataKey<T> key) {
        Objects.requireNonNull(unit, "unit");
        synchronized (LOCK) {
            requireRegistered(key);
            Map<Identifier, StoredValue> values = UNITS.get(unit);
            return decode(key, values != null ? values.get(key.id()) : null, unit);
        }
    }

    public static boolean has(Unit unit, PersistentDataKey<?> key) {
        Objects.requireNonNull(unit, "unit");
        synchronized (LOCK) {
            requireRegistered(key);
            Map<Identifier, StoredValue> values = UNITS.get(unit);
            return values != null && values.containsKey(key.id());
        }
    }

    public static boolean remove(Unit unit, PersistentDataKey<?> key) {
        Objects.requireNonNull(unit, "unit");
        synchronized (LOCK) {
            requireRegistered(key);
            Map<Identifier, StoredValue> values = UNITS.get(unit);
            if (values == null || values.remove(key.id()) == null) return false;
            if (values.isEmpty()) UNITS.remove(unit);
            return true;
        }
    }

    public static Set<Identifier> ids(Unit unit) {
        Objects.requireNonNull(unit, "unit");
        synchronized (LOCK) {
            Map<Identifier, StoredValue> values = UNITS.get(unit);
            return values == null ? Collections.emptySet()
                    : Collections.unmodifiableSet(new TreeSet<Identifier>(values.keySet()));
        }
    }

    public static void copy(Unit source, Unit target) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        synchronized (LOCK) {
            Map<Identifier, StoredValue> values = UNITS.get(source);
            if (values == null || values.isEmpty()) {
                UNITS.remove(target);
                return;
            }
            Map<Identifier, StoredValue> copy = new LinkedHashMap<Identifier, StoredValue>();
            for (Map.Entry<Identifier, StoredValue> entry : values.entrySet()) {
                try {
                    EncodedEntry encoded = entry.getValue().encode(entry.getKey());
                    copy.put(entry.getKey(), StoredValue.raw(encoded.version, encoded.bytes));
                } catch (RuntimeException failure) {
                    PersistentDataEvents.CODEC_FAILURE.invoker()
                            .onCodecFailure(entry.getKey(), source, true, failure);
                }
            }
            if (copy.isEmpty()) UNITS.remove(target);
            else UNITS.put(target, copy);
        }
    }

    /** Clears world and unit values but retains key registrations. */
    public static void clearRuntime() {
        synchronized (LOCK) {
            GLOBAL.clear();
            UNITS.clear();
        }
    }

    /** Loader runtime hook; mods should normally use the typed get/set methods. */
    public static void writeSaveExtension(GameOutputStream output) {
        Objects.requireNonNull(output, "output");
        PersistentDataEvents.BEFORE_WRITE.invoker().beforeWrite();
        byte[] body;
        try {
            body = encodeBody();
        } catch (RuntimeException failure) {
            PersistentDataEvents.MALFORMED_BLOCK.invoker().onMalformedBlock(failure);
            return;
        }
        output.writeInt(MAGIC);
        output.writeInt(FORMAT_VERSION);
        output.writeInt(body.length);
        output.writeRawBytes(body);
    }

    /** Loader runtime hook; returns the number of restored global and unit entries. */
    public static int readSaveExtension(GameInputStream input) {
        Objects.requireNonNull(input, "input");
        InputStream raw = input.getCurrentInputStream();
        if (raw == null) return 0;
        try {
            if (raw.available() < 12) return 0;
            boolean canReset = raw.markSupported();
            if (canReset) raw.mark(12);
            int magic = input.readInt();
            if (magic != MAGIC) {
                if (canReset) raw.reset();
                return 0;
            }
            int formatVersion = input.readInt();
            int length = input.readInt();
            if (length < 0 || length > MAX_EXTENSION_BYTES || length > raw.available()) {
                throw new IllegalArgumentException("Invalid persistent extension length: " + length);
            }
            byte[] body = new byte[length];
            new DataInputStream(raw).readFully(body);
            int entries = decodeBody(formatVersion, body);
            PersistentDataEvents.AFTER_READ.invoker().afterRead(formatVersion, entries);
            return entries;
        } catch (IOException failure) {
            RuntimeException wrapped = new IllegalArgumentException(
                    "Could not read persistent extension", failure);
            PersistentDataEvents.MALFORMED_BLOCK.invoker().onMalformedBlock(wrapped);
            return 0;
        } catch (RuntimeException failure) {
            PersistentDataEvents.MALFORMED_BLOCK.invoker().onMalformedBlock(failure);
            return 0;
        }
    }

    private static byte[] encodeBody() {
        List<EncodedEntry> globals;
        List<EncodedUnit> units;
        synchronized (LOCK) {
            globals = encodeEntries(GLOBAL, null);
            units = new ArrayList<EncodedUnit>();
            List<Map.Entry<Unit, Map<Identifier, StoredValue>>> owners =
                    new ArrayList<Map.Entry<Unit, Map<Identifier, StoredValue>>>(UNITS.entrySet());
            owners.sort(Comparator.comparingLong(entry -> entry.getKey().id));
            for (Map.Entry<Unit, Map<Identifier, StoredValue>> owner : owners) {
                Unit unit = owner.getKey();
                if (unit == null || unit.id == 0L || !GameObject.allGameObjects.contains(unit)) continue;
                List<EncodedEntry> entries = encodeEntries(owner.getValue(), unit);
                if (!entries.isEmpty()) units.add(new EncodedUnit(unit.id, entries));
            }
        }
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(bytes);
            output.writeInt(globals.size());
            for (EncodedEntry entry : globals) writeEntry(output, entry);
            output.writeInt(units.size());
            for (EncodedUnit unit : units) {
                output.writeLong(unit.id);
                output.writeInt(unit.entries.size());
                for (EncodedEntry entry : unit.entries) writeEntry(output, entry);
            }
            output.flush();
            byte[] result = bytes.toByteArray();
            if (result.length > MAX_EXTENSION_BYTES) {
                throw new IllegalStateException("Persistent extension exceeds "
                        + MAX_EXTENSION_BYTES + " bytes");
            }
            return result;
        } catch (IOException impossible) {
            throw new IllegalStateException("Could not encode persistent extension", impossible);
        }
    }

    private static int decodeBody(int formatVersion, byte[] body) {
        if (formatVersion != FORMAT_VERSION) {
            throw new IllegalArgumentException("Unsupported persistent format version: " + formatVersion);
        }
        try {
            DataInputStream input = new DataInputStream(new ByteArrayInputStream(body));
            Map<Identifier, StoredValue> globals =
                    new LinkedHashMap<Identifier, StoredValue>();
            Map<Unit, Map<Identifier, StoredValue>> units =
                    new WeakHashMap<Unit, Map<Identifier, StoredValue>>();
            int globalCount = readCount(input, "global entry");
            for (int i = 0; i < globalCount; i++) {
                DecodedEntry entry = readEntry(input);
                globals.put(entry.id, StoredValue.raw(entry.version, entry.bytes));
            }
            int unitCount = readCount(input, "unit");
            for (int i = 0; i < unitCount; i++) {
                long unitId = input.readLong();
                int entryCount = readCount(input, "unit entry");
                Unit unit = GameObject.getUnitById(unitId, false);
                Map<Identifier, StoredValue> values =
                        new LinkedHashMap<Identifier, StoredValue>();
                for (int j = 0; j < entryCount; j++) {
                    DecodedEntry entry = readEntry(input);
                    values.put(entry.id, StoredValue.raw(entry.version, entry.bytes));
                }
                if (unit != null && !values.isEmpty()) units.put(unit, values);
            }
            if (input.available() != 0) {
                throw new IllegalArgumentException("Persistent extension has "
                        + input.available() + " trailing bytes");
            }
            synchronized (LOCK) {
                GLOBAL.clear();
                GLOBAL.putAll(globals);
                UNITS.clear();
                UNITS.putAll(units);
            }
            int restored = globals.size();
            for (Map<Identifier, StoredValue> values : units.values()) restored += values.size();
            return restored;
        } catch (EOFException failure) {
            throw new IllegalArgumentException("Persistent extension ended unexpectedly", failure);
        } catch (IOException failure) {
            throw new IllegalArgumentException("Could not decode persistent extension", failure);
        }
    }

    private static List<EncodedEntry> encodeEntries(
            Map<Identifier, StoredValue> values, Unit unit) {
        List<Identifier> ids = new ArrayList<Identifier>(values.keySet());
        Collections.sort(ids);
        List<EncodedEntry> result = new ArrayList<EncodedEntry>(ids.size());
        for (Identifier id : ids) {
            StoredValue value = values.get(id);
            try {
                result.add(value.encode(id));
            } catch (RuntimeException failure) {
                PersistentDataEvents.CODEC_FAILURE.invoker()
                        .onCodecFailure(id, unit, true, failure);
            }
        }
        return result;
    }

    private static void writeEntry(DataOutputStream output, EncodedEntry entry) throws IOException {
        byte[] id = entry.id.toString().getBytes(StandardCharsets.UTF_8);
        output.writeInt(id.length);
        output.write(id);
        output.writeInt(entry.version);
        output.writeInt(entry.bytes.length);
        output.write(entry.bytes);
    }

    private static DecodedEntry readEntry(DataInputStream input) throws IOException {
        int idLength = input.readInt();
        if (idLength <= 0 || idLength > Identifier.MAX_ENCODED_BYTES || idLength > input.available()) {
            throw new IllegalArgumentException("Invalid persistent identifier length: " + idLength);
        }
        byte[] idBytes = new byte[idLength];
        input.readFully(idBytes);
        Identifier id = Identifier.parse(new String(idBytes, StandardCharsets.UTF_8));
        int version = input.readInt();
        if (version < 0) throw new IllegalArgumentException("Negative data version for " + id);
        int length = input.readInt();
        if (length < 0 || length > PacketPayload.MAX_BYTES || length > input.available()) {
            throw new IllegalArgumentException("Invalid payload length for " + id + ": " + length);
        }
        byte[] bytes = new byte[length];
        input.readFully(bytes);
        return new DecodedEntry(id, version, bytes);
    }

    private static int readCount(DataInputStream input, String label) throws IOException {
        int count = input.readInt();
        if (count < 0 || count > MAX_ENTRIES) {
            throw new IllegalArgumentException("Invalid " + label + " count: " + count);
        }
        return count;
    }

    @SuppressWarnings("unchecked")
    private static <T> Optional<T> decode(
            PersistentDataKey<T> key, StoredValue stored, Unit unit) {
        if (stored == null) return Optional.empty();
        if (stored.decoded) return Optional.of((T) stored.value);
        try {
            PacketBuffer buffer = PacketBuffer.reader(PacketPayload.of(stored.bytes));
            T value = Objects.requireNonNull(key.codec().decode(buffer, stored.version),
                    "persistent codec returned null");
            buffer.requireFullyRead();
            stored.value = value;
            stored.key = key;
            stored.decoded = true;
            stored.bytes = null;
            return Optional.of(value);
        } catch (RuntimeException failure) {
            PersistentDataEvents.CODEC_FAILURE.invoker()
                    .onCodecFailure(key.id(), unit, false, failure);
            return Optional.empty();
        }
    }

    private static void requireRegistered(PersistentDataKey<?> key) {
        Objects.requireNonNull(key, "key");
        if (KEYS.get(key.id()) != key) {
            throw new IllegalArgumentException("Persistent data key is not registered here: " + key);
        }
    }

    private static final class StoredValue {
        private int version;
        private byte[] bytes;
        private Object value;
        private PersistentDataKey<?> key;
        private boolean decoded;

        private static StoredValue raw(int version, byte[] bytes) {
            StoredValue result = new StoredValue();
            result.version = version;
            result.bytes = bytes.clone();
            return result;
        }

        private static StoredValue decoded(PersistentDataKey<?> key, Object value) {
            StoredValue result = new StoredValue();
            result.version = key.version();
            result.key = key;
            result.value = value;
            result.decoded = true;
            return result;
        }

        @SuppressWarnings("unchecked")
        private EncodedEntry encode(Identifier id) {
            if (!decoded) return new EncodedEntry(id, version, bytes.clone());
            PacketBuffer buffer = PacketBuffer.writer();
            ((PersistentDataCodec<Object>) key.codec()).encode(buffer, value);
            return new EncodedEntry(id, key.version(), buffer.toPayload().copyBytes());
        }

    }

    private static final class EncodedEntry {
        private final Identifier id;
        private final int version;
        private final byte[] bytes;

        private EncodedEntry(Identifier id, int version, byte[] bytes) {
            this.id = id;
            this.version = version;
            this.bytes = bytes;
        }
    }

    private static final class EncodedUnit {
        private final long id;
        private final List<EncodedEntry> entries;

        private EncodedUnit(long id, List<EncodedEntry> entries) {
            this.id = id;
            this.entries = entries;
        }
    }

    private static final class DecodedEntry {
        private final Identifier id;
        private final int version;
        private final byte[] bytes;

        private DecodedEntry(Identifier id, int version, byte[] bytes) {
            this.id = id;
            this.version = version;
            this.bytes = bytes;
        }
    }
}
