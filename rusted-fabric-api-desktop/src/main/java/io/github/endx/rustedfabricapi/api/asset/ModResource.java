package io.github.endx.rustedfabricapi.api.asset;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.Properties;

/** One safe relative resource in a {@link ModResourcePack}. */
public final class ModResource {
    private final ModResourcePack pack;
    private final Path relativePath;

    ModResource(ModResourcePack pack, Path relativePath) {
        this.pack = pack;
        this.relativePath = relativePath;
    }

    public ModResourcePack pack() { return pack; }

    public Path relativePath() { return relativePath; }

    public boolean exists() throws IOException {
        Optional<InputStream> stream = pack.open(relativePath);
        if (!stream.isPresent()) return false;
        try (InputStream ignored = stream.get()) {
            return true;
        }
    }

    public InputStream open() throws IOException {
        return pack.open(relativePath).orElseThrow(() ->
                new FileNotFoundException(pack.modId() + ':' + normalizedPath()));
    }

    public byte[] readBytes() throws IOException {
        try (InputStream input = open(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int total = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > ModResources.MAX_READ_BYTES) {
                    throw new IOException("Mod resource exceeds " + ModResources.MAX_READ_BYTES + " bytes");
                }
                output.write(buffer, 0, read);
            }
            byte[] bytes = output.toByteArray();
            ModResourceEvents.AFTER_READ.invoker().afterRead(this, bytes.length);
            return bytes;
        }
    }

    public String readUtf8() throws IOException {
        return new String(readBytes(), StandardCharsets.UTF_8);
    }

    public Properties readPropertiesUtf8() throws IOException {
        Properties properties = new Properties();
        try (Reader reader = new InputStreamReader(
                new ByteArrayInputStream(readBytes()), StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties;
    }

    /**
     * Produces a stable content-addressed real file for native APIs that cannot read Jar streams.
     * The cache is beneath the operating system's temporary directory, never the repository.
     */
    public Optional<Path> extractToCache() throws IOException {
        if (ModResourceEvents.BEFORE_EXTRACT.invoker().beforeExtract(this)) return Optional.empty();
        Path target = null;
        boolean success = false;
        try {
            byte[] bytes = readBytes();
            String hash = sha256(bytes);
            String leaf = safeLeaf(relativePath.getFileName().toString());
            target = ModResources.cacheRoot().resolve(pack.modId())
                    .resolve(hash.substring(0, 2)).resolve(hash + '-' + leaf).normalize();
            Path root = ModResources.cacheRoot();
            if (!target.startsWith(root)) throw new IOException("Resource cache path escaped its root");
            Files.createDirectories(target.getParent());
            boolean current = Files.isRegularFile(target) && Files.size(target) == bytes.length
                    && sha256(Files.readAllBytes(target)).equals(hash);
            if (!current) {
                Path temporary = Files.createTempFile(target.getParent(), "rfa", ".tmp");
                try {
                    Files.write(temporary, bytes, StandardOpenOption.TRUNCATE_EXISTING);
                    try {
                        Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE,
                                StandardCopyOption.REPLACE_EXISTING);
                    } catch (AtomicMoveNotSupportedException ignored) {
                        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                } finally {
                    Files.deleteIfExists(temporary);
                }
            }
            success = true;
            return Optional.of(target);
        } finally {
            ModResourceEvents.AFTER_EXTRACT.invoker().afterExtract(this, target, success);
        }
    }

    private String normalizedPath() { return relativePath.toString().replace('\\', '/'); }

    private static String safeLeaf(String name) {
        StringBuilder value = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            value.append(c < 32 || "<>:\"/\\|?*".indexOf(c) >= 0 ? '_' : c);
        }
        while (value.length() > 0
                && (value.charAt(value.length() - 1) == '.' || value.charAt(value.length() - 1) == ' ')) {
            value.setCharAt(value.length() - 1, '_');
        }
        if (value.length() == 0) value.append("resource");
        if (value.length() > 80) {
            String text = value.toString();
            int dot = text.lastIndexOf('.');
            String suffix = dot >= 0 && text.length() - dot <= 16 ? text.substring(dot) : "";
            return text.substring(0, 80 - suffix.length()) + suffix;
        }
        return value.toString();
    }

    private static String sha256(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte value : digest) result.append(String.format("%02x", value & 0xff));
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ModResource)) return false;
        ModResource resource = (ModResource) other;
        return pack == resource.pack && relativePath.equals(resource.relativePath);
    }

    @Override public int hashCode() { return 31 * System.identityHashCode(pack) + relativePath.hashCode(); }

    @Override public String toString() { return pack.modId() + ':' + normalizedPath(); }
}
