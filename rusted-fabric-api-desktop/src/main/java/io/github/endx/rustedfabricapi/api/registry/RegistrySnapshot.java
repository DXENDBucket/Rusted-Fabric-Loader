package io.github.endx.rustedfabricapi.api.registry;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import io.github.endx.rustedfabricapi.api.util.Identifier;

/** Immutable registry layout suitable for diagnostics, handshakes, and compatibility checks. */
public final class RegistrySnapshot {
    public static final int MAX_ENTRIES = 65_535;
    public static final int MAX_VALUE_TYPE_CHARS = 512;

    private final Identifier registryId;
    private final String valueTypeName;
    private final List<Identifier> ids;
    private final boolean frozen;
    private final String contentFingerprint;
    private final String layoutFingerprint;

    private RegistrySnapshot(Identifier registryId, String valueTypeName,
            List<Identifier> ids, boolean frozen) {
        this.registryId = Objects.requireNonNull(registryId, "registryId");
        String checkedType = Objects.requireNonNull(valueTypeName, "valueTypeName").trim();
        if (checkedType.isEmpty() || checkedType.length() > MAX_VALUE_TYPE_CHARS) {
            throw new IllegalArgumentException("Invalid registry value type name");
        }
        List<Identifier> copy = new ArrayList<Identifier>(Objects.requireNonNull(ids, "ids"));
        if (copy.size() > MAX_ENTRIES) throw new IllegalArgumentException("Registry is too large");
        HashSet<Identifier> unique = new HashSet<Identifier>();
        for (Identifier id : copy) {
            if (!unique.add(Objects.requireNonNull(id, "entry id"))) {
                throw new IllegalArgumentException("Duplicate registry snapshot ID: " + id);
            }
        }
        this.valueTypeName = checkedType;
        this.ids = Collections.unmodifiableList(copy);
        this.frozen = frozen;
        this.layoutFingerprint = fingerprint(copy);
        ArrayList<Identifier> sorted = new ArrayList<Identifier>(copy);
        Collections.sort(sorted);
        this.contentFingerprint = fingerprint(sorted);
    }

    public static RegistrySnapshot of(Identifier registryId, String valueTypeName,
            List<Identifier> ids, boolean frozen) {
        return new RegistrySnapshot(registryId, valueTypeName, ids, frozen);
    }

    public Identifier registryId() { return registryId; }

    public String valueTypeName() { return valueTypeName; }

    public List<Identifier> ids() { return ids; }

    public boolean frozen() { return frozen; }

    public int size() { return ids.size(); }

    /** Same value means both sides contain the same IDs, regardless of raw-ID order. */
    public String contentFingerprint() { return contentFingerprint; }

    /** Same value means both sides assign the same raw ID to every entry. */
    public String layoutFingerprint() { return layoutFingerprint; }

    public RegistryComparison compare(RegistrySnapshot remote) {
        return RegistryComparison.compare(this, remote);
    }

    private String fingerprint(List<Identifier> orderedIds) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            update(digest, "RFR1");
            update(digest, registryId.toString());
            update(digest, valueTypeName);
            for (Identifier id : orderedIds) update(digest, id.toString());
            byte[] bytes = digest.digest();
            char[] hex = new char[bytes.length * 2];
            char[] alphabet = "0123456789abcdef".toCharArray();
            for (int i = 0; i < bytes.length; i++) {
                int value = bytes[i] & 0xff;
                hex[i * 2] = alphabet[value >>> 4];
                hex[i * 2 + 1] = alphabet[value & 0x0f];
            }
            return new String(hex);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private static void update(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update((byte) (bytes.length >>> 24));
        digest.update((byte) (bytes.length >>> 16));
        digest.update((byte) (bytes.length >>> 8));
        digest.update((byte) bytes.length);
        digest.update(bytes);
    }

    @Override public String toString() {
        return "RegistrySnapshot{" + registryId + ", entries=" + ids.size()
                + ", frozen=" + frozen + '}';
    }
}
