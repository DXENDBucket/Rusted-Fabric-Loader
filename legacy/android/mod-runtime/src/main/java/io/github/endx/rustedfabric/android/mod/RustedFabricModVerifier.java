package io.github.endx.rustedfabric.android.mod;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import io.github.endx.rustedfabricapi.api.multiplayer.MultiplayerMod;

/** Strict verifier for a code-only Android Rusted Fabric mod archive. */
public final class RustedFabricModVerifier {
    public static final String METADATA_PATH = "META-INF/rusted-fabric.mod.properties";
    public static final String DEX_PATH = "classes.dex";
    public static final long MAX_ARCHIVE_BYTES = 64L * 1024L * 1024L;
    public static final long MAX_DEX_BYTES = 32L * 1024L * 1024L;
    public static final long MAX_TOTAL_UNCOMPRESSED_BYTES = 128L * 1024L * 1024L;
    public static final int MAX_ENTRIES = 256;

    private static final int MAX_METADATA_BYTES = 64 * 1024;
    private static final Pattern MOD_ID = Pattern.compile("[a-z][a-z0-9_-]{0,63}");
    private static final Pattern PROFILE_ID = Pattern.compile("[a-z][a-z0-9._-]{0,95}");
    private static final Pattern VERSION = Pattern.compile("[0-9A-Za-z][0-9A-Za-z._+\\-]{0,63}");
    private static final Pattern JAVA_CLASS = Pattern.compile(
            "[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)+");
    private static final Pattern CAPABILITY = Pattern.compile("[a-z][a-z0-9._-]{0,95}");

    public VerifiedModArchive verify(Path archivePath) throws ModVerificationException {
        if (archivePath == null || !Files.isRegularFile(archivePath)) {
            throw error(ModVerificationException.Reason.INVALID_ARCHIVE,
                    "Mod archive is missing");
        }
        try {
            long archiveSize = Files.size(archivePath);
            if (archiveSize <= 0 || archiveSize > MAX_ARCHIVE_BYTES) {
                throw error(ModVerificationException.Reason.LIMIT_EXCEEDED,
                        "Mod archive exceeds the size limit");
            }
            String archiveSha256 = sha256(Files.newInputStream(archivePath));
            try (ZipFile zip = new ZipFile(archivePath.toFile())) {
                Map<String, ZipEntry> entries = validateEntries(zip);
                ZipEntry metadataEntry = entries.get(METADATA_PATH);
                if (metadataEntry == null) {
                    throw error(ModVerificationException.Reason.INVALID_METADATA,
                            "Mod metadata is missing");
                }
                ZipEntry dexEntry = entries.get(DEX_PATH);
                if (dexEntry == null) {
                    throw error(ModVerificationException.Reason.MISSING_DEX,
                            "classes.dex is missing");
                }
                byte[] metadataBytes = read(zip, metadataEntry, MAX_METADATA_BYTES,
                        ModVerificationException.Reason.INVALID_METADATA);
                RustedFabricModMetadata metadata = parseMetadata(metadataBytes);
                byte[] dex = read(zip, dexEntry, MAX_DEX_BYTES,
                        ModVerificationException.Reason.LIMIT_EXCEEDED);
                Set<String> definedClasses = DexClassDefinitions.readBinaryNames(dex);
                validateDefinitions(definedClasses, metadata.getEntrypoint());
                return new VerifiedModArchive(archivePath.toAbsolutePath().normalize(), metadata,
                        archiveSha256, sha256(dex), definedClasses);
            }
        } catch (ModVerificationException expected) {
            throw expected;
        } catch (IOException malformed) {
            throw new ModVerificationException(ModVerificationException.Reason.INVALID_ARCHIVE,
                    "Mod archive cannot be read", malformed);
        }
    }

    private Map<String, ZipEntry> validateEntries(ZipFile zip)
            throws ModVerificationException {
        Map<String, ZipEntry> entries = new LinkedHashMap<>();
        long totalSize = 0;
        int count = 0;
        Enumeration<? extends ZipEntry> enumeration = zip.entries();
        while (enumeration.hasMoreElements()) {
            ZipEntry entry = enumeration.nextElement();
            count++;
            if (count > MAX_ENTRIES) {
                throw error(ModVerificationException.Reason.LIMIT_EXCEEDED,
                        "Mod archive has too many entries");
            }
            String name = entry.getName();
            validateEntryName(name);
            if (entries.put(name, entry) != null) {
                throw error(ModVerificationException.Reason.INVALID_ARCHIVE,
                        "Mod archive has duplicate entries");
            }
            if (entry.isDirectory()) {
                continue;
            }
            long size = entry.getSize();
            if (size < 0) {
                throw error(ModVerificationException.Reason.INVALID_ARCHIVE,
                        "Mod archive has an entry with unknown size");
            }
            if (size > MAX_TOTAL_UNCOMPRESSED_BYTES - totalSize) {
                throw error(ModVerificationException.Reason.LIMIT_EXCEEDED,
                        "Mod archive expands beyond the size limit");
            }
            totalSize += size;
            if (!allowedEntry(name)) {
                throw error(ModVerificationException.Reason.FORBIDDEN_ENTRY,
                        "Mod archive contains a forbidden entry: " + safeName(name));
            }
        }
        return entries;
    }

    private static boolean allowedEntry(String name) {
        if (METADATA_PATH.equals(name) || DEX_PATH.equals(name)
                || "LICENSE".equals(name) || "NOTICE".equals(name)
                || "META-INF/LICENSE".equals(name) || "META-INF/NOTICE".equals(name)) {
            return true;
        }
        return name.startsWith("assets/") && name.length() > "assets/".length();
    }

    private static void validateEntryName(String name) throws ModVerificationException {
        if (name == null || name.isEmpty() || name.startsWith("/") || name.indexOf('\\') >= 0
                || name.indexOf('\0') >= 0) {
            throw error(ModVerificationException.Reason.INVALID_ARCHIVE,
                    "Mod archive has an unsafe entry name");
        }
        String[] segments = name.split("/", -1);
        for (int index = 0; index < segments.length; index++) {
            String segment = segments[index];
            boolean trailingDirectorySlash = index == segments.length - 1 && segment.isEmpty();
            if ((!trailingDirectorySlash && segment.isEmpty())
                    || "..".equals(segment) || ".".equals(segment)) {
                throw error(ModVerificationException.Reason.INVALID_ARCHIVE,
                        "Mod archive has a path traversal entry");
            }
        }
    }

    private RustedFabricModMetadata parseMetadata(byte[] bytes)
            throws ModVerificationException {
        String text = new String(bytes, StandardCharsets.UTF_8);
        Map<String, String> values = new LinkedHashMap<>();
        String[] lines = text.split("\\r?\\n", -1);
        for (int index = 0; index < lines.length; index++) {
            String line = lines[index].trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            int separator = line.indexOf('=');
            if (separator <= 0) {
                throw metadata("Malformed metadata line " + (index + 1));
            }
            String key = line.substring(0, separator).trim();
            String value = line.substring(separator + 1).trim();
            if (!key.matches("[A-Za-z][A-Za-z0-9.]*") || value.isEmpty()
                    || containsControl(value)) {
                throw metadata("Invalid metadata value at line " + (index + 1));
            }
            if (values.put(key, value) != null) {
                throw metadata("Duplicate metadata key: " + key);
            }
        }
        Set<String> allowed = new LinkedHashSet<>();
        Collections.addAll(allowed, "schemaVersion", "id", "version", "name", "entrypoint",
                "apiVersion", "mappingProfiles", "capabilities", "platform", "dex",
                "multiplayerMode", "multiplayerProtocol", "multiplayerSyncHash");
        for (String key : values.keySet()) {
            if (!allowed.contains(key)) {
                throw metadata("Unknown metadata key: " + key);
            }
        }

        int schemaVersion;
        try {
            schemaVersion = Integer.parseInt(required(values, "schemaVersion"));
        } catch (NumberFormatException invalid) {
            throw metadata("schemaVersion must be an integer");
        }
        if (schemaVersion != 1) {
            throw metadata("Unsupported schemaVersion");
        }
        String id = required(values, "id");
        String version = required(values, "version");
        String name = required(values, "name");
        String entrypoint = required(values, "entrypoint");
        String apiVersion = required(values, "apiVersion");
        if (!MOD_ID.matcher(id).matches() || !VERSION.matcher(version).matches()
                || !VERSION.matcher(apiVersion).matches()
                || name.length() > 128 || !JAVA_CLASS.matcher(entrypoint).matches()) {
            throw metadata("Mod identity or entrypoint is invalid");
        }
        if (!"android".equals(required(values, "platform"))
                || !DEX_PATH.equals(required(values, "dex"))) {
            throw metadata("The v1 archive must target Android classes.dex");
        }
        List<String> profiles = csv(required(values, "mappingProfiles"), PROFILE_ID, "mapping profile");
        List<String> capabilities = values.containsKey("capabilities")
                ? csv(values.get("capabilities"), CAPABILITY, "capability")
                : Collections.emptyList();
        MultiplayerMod multiplayer;
        try {
            MultiplayerMod.Mode mode = MultiplayerMod.Mode.parse(
                    values.getOrDefault("multiplayerMode", "unsafe"));
            multiplayer = new MultiplayerMod(id, version, mode,
                    values.getOrDefault("multiplayerProtocol", ""),
                    values.getOrDefault("multiplayerSyncHash", ""));
        } catch (IllegalArgumentException invalidMultiplayer) {
            throw metadata("Invalid multiplayer declaration: " + invalidMultiplayer.getMessage());
        }
        return new RustedFabricModMetadata(schemaVersion, id, version, name, entrypoint,
                apiVersion, profiles, capabilities, multiplayer);
    }

    private static List<String> csv(String value, Pattern pattern, String label)
            throws ModVerificationException {
        List<String> values = new ArrayList<>();
        Set<String> unique = new LinkedHashSet<>();
        for (String item : value.split(",", -1)) {
            String normalized = item.trim();
            if (!pattern.matcher(normalized).matches() || !unique.add(normalized)) {
                throw metadata("Invalid or duplicate " + label);
            }
            values.add(normalized);
        }
        if (values.isEmpty()) {
            throw metadata("At least one " + label + " is required");
        }
        return values;
    }

    private static void validateDefinitions(Set<String> classes, String entrypoint)
            throws ModVerificationException {
        if (classes.isEmpty()) {
            throw error(ModVerificationException.Reason.INVALID_DEX,
                    "Mod DEX has no class definitions");
        }
        for (String className : classes) {
            if (className.startsWith("com.corrodinggames.rts.")
                    || className.startsWith("io.github.endx.rustedfabricapi.")
                    || className.startsWith("io.github.endx.rustedfabric.android.")
                    || className.startsWith("io.github.libxposed.")
                    || className.startsWith("android.") || className.startsWith("java.")
                    || className.startsWith("javax.") || className.startsWith("dalvik.")) {
                throw error(ModVerificationException.Reason.FORBIDDEN_CLASS_DEFINITION,
                        "Mod DEX defines a reserved platform, API, loader, or game class");
            }
        }
        if (!classes.contains(entrypoint)) {
            throw error(ModVerificationException.Reason.ENTRYPOINT_NOT_DEFINED,
                    "Declared mod entrypoint is not defined by classes.dex");
        }
    }

    private static byte[] read(ZipFile zip, ZipEntry entry, long limit,
                               ModVerificationException.Reason reason)
            throws IOException, ModVerificationException {
        if (entry.getSize() > limit) {
            throw error(reason, "Mod entry exceeds its size limit");
        }
        try (InputStream input = zip.getInputStream(entry);
             ByteArrayOutputStream output = new ByteArrayOutputStream(
                     (int) Math.min(entry.getSize(), 64 * 1024))) {
            byte[] buffer = new byte[16 * 1024];
            long total = 0;
            for (int count = input.read(buffer); count >= 0; count = input.read(buffer)) {
                if (count == 0) {
                    continue;
                }
                total += count;
                if (total > limit) {
                    throw error(reason, "Mod entry expands beyond its size limit");
                }
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        }
    }

    private static String required(Map<String, String> values, String key)
            throws ModVerificationException {
        String value = values.get(key);
        if (value == null || value.isEmpty()) {
            throw metadata("Missing metadata key: " + key);
        }
        return value;
    }

    private static boolean containsControl(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isISOControl(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static String safeName(String value) {
        return value.length() <= 96 ? value : value.substring(0, 96);
    }

    private static String sha256(InputStream input) throws IOException {
        try (InputStream closeable = input) {
            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException impossible) {
                throw new IllegalStateException("SHA-256 is unavailable", impossible);
            }
            byte[] buffer = new byte[64 * 1024];
            for (int count = closeable.read(buffer); count >= 0; count = closeable.read(buffer)) {
                if (count > 0) {
                    digest.update(buffer, 0, count);
                }
            }
            return hex(digest.digest());
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            return hex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            result.append(String.format("%02x", value & 0xff));
        }
        return result.toString();
    }

    private static ModVerificationException metadata(String message) {
        return error(ModVerificationException.Reason.INVALID_METADATA, message);
    }

    private static ModVerificationException error(ModVerificationException.Reason reason,
                                                  String message) {
        return new ModVerificationException(reason, message);
    }
}
