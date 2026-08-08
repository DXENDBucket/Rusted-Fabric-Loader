package io.github.endx.rustedfabricapi.api.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

/** A path-safe file scoped to one mod beneath Fabric Loader's external config directory. */
public final class ModConfigFile {
    private final String modId;
    private final Path relativePath;

    ModConfigFile(String modId, Path relativePath) {
        this.modId = modId;
        this.relativePath = relativePath;
    }

    public String modId() { return modId; }

    public Path relativePath() { return relativePath; }

    public Path path() {
        Path directory = ModConfigFiles.directory(modId);
        Path result = directory.resolve(relativePath).normalize();
        if (!result.startsWith(directory)) {
            throw new IllegalStateException("Configuration path escaped the mod directory");
        }
        return result;
    }

    public boolean exists() { return Files.isRegularFile(path()); }

    public Optional<byte[]> readBytes() throws IOException {
        Path path = path();
        if (!Files.isRegularFile(path)) {
            ConfigEvents.AFTER_READ.invoker().afterRead(this, false, 0);
            return Optional.empty();
        }
        long size = Files.size(path);
        if (size > ModConfigFiles.MAX_READ_BYTES) {
            throw new IOException("Configuration file exceeds " + ModConfigFiles.MAX_READ_BYTES + " bytes");
        }
        byte[] bytes = Files.readAllBytes(path);
        if (bytes.length > ModConfigFiles.MAX_READ_BYTES) {
            throw new IOException("Configuration file grew beyond the read limit");
        }
        ConfigEvents.AFTER_READ.invoker().afterRead(this, true, bytes.length);
        return Optional.of(bytes);
    }

    public Optional<String> readUtf8() throws IOException {
        Optional<byte[]> bytes = readBytes();
        return bytes.isPresent()
                ? Optional.of(new String(bytes.get(), StandardCharsets.UTF_8))
                : Optional.empty();
    }

    public Properties readProperties() throws IOException {
        Properties properties = new Properties();
        Optional<byte[]> bytes = readBytes();
        if (bytes.isPresent()) {
            try (InputStream input = new ByteArrayInputStream(bytes.get())) {
                properties.load(input);
            }
        }
        return properties;
    }

    /** Writes with a same-directory temporary file and atomic replacement where supported. */
    public boolean writeBytes(byte[] content) throws IOException {
        byte[] copy = Arrays.copyOf(Objects.requireNonNull(content, "content"), content.length);
        if (copy.length > ModConfigFiles.MAX_READ_BYTES) {
            throw new IllegalArgumentException("Configuration content exceeds "
                    + ModConfigFiles.MAX_READ_BYTES + " bytes");
        }
        if (ConfigEvents.BEFORE_WRITE.invoker().beforeMutation(this)) return false;
        Path target = path();
        Path temporary = null;
        boolean success = false;
        try {
            Files.createDirectories(target.getParent());
            String prefix = target.getFileName().toString();
            if (prefix.length() < 3) prefix = (prefix + "___").substring(0, 3);
            temporary = Files.createTempFile(target.getParent(), prefix, ".tmp");
            Files.write(temporary, copy, StandardOpenOption.TRUNCATE_EXISTING);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
            success = true;
            return true;
        } finally {
            if (temporary != null) Files.deleteIfExists(temporary);
            ConfigEvents.AFTER_WRITE.invoker().afterMutation(this, success);
        }
    }

    public boolean writeUtf8(String content) throws IOException {
        return writeBytes(Objects.requireNonNull(content, "content").getBytes(StandardCharsets.UTF_8));
    }

    public boolean writeProperties(Properties properties, String comment) throws IOException {
        Objects.requireNonNull(properties, "properties");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (OutputStream output = bytes) {
            properties.store(output, comment);
        }
        return writeBytes(bytes.toByteArray());
    }

    public boolean delete() throws IOException {
        if (ConfigEvents.BEFORE_DELETE.invoker().beforeMutation(this)) return false;
        boolean success = false;
        try {
            success = Files.deleteIfExists(path());
            return success;
        } finally {
            ConfigEvents.AFTER_DELETE.invoker().afterMutation(this, success);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ModConfigFile)) return false;
        ModConfigFile file = (ModConfigFile) other;
        return modId.equals(file.modId) && relativePath.equals(file.relativePath);
    }

    @Override
    public int hashCode() { return 31 * modId.hashCode() + relativePath.hashCode(); }

    @Override
    public String toString() { return modId + ':' + relativePath.toString().replace('\\', '/'); }
}
