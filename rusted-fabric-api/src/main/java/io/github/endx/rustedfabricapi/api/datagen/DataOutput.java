package io.github.endx.rustedfabricapi.api.datagen;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import io.github.endx.rustedfabricapi.api.util.Identifier;

/** Provider-local, in-memory output; files are published only after every provider succeeds. */
public final class DataOutput {
    public static final int MAX_RESOURCE_BYTES = 128 * 1024 * 1024;
    private static final Gson PRETTY_JSON = new GsonBuilder().setPrettyPrinting()
            .disableHtmlEscaping().create();

    private final String modId;
    private final Identifier providerId;
    private final LinkedHashMap<String, byte[]> resources =
            new LinkedHashMap<String, byte[]>();

    DataOutput(String modId, Identifier providerId) {
        this.modId = modId;
        this.providerId = providerId;
    }

    public String modId() { return modId; }

    public Identifier providerId() { return providerId; }

    public void write(String relativePath, byte[] content) {
        String path = validatePath(relativePath);
        byte[] checked = Objects.requireNonNull(content, "content");
        if (checked.length > MAX_RESOURCE_BYTES) {
            throw new IllegalArgumentException("Generated resource exceeds "
                    + MAX_RESOURCE_BYTES + " bytes: " + path);
        }
        if (resources.containsKey(path)) {
            throw new IllegalArgumentException("Provider " + providerId
                    + " generated the same path twice: " + path);
        }
        resources.put(path, checked.clone());
    }

    public void writeUtf8(String relativePath, String content) {
        write(relativePath, Objects.requireNonNull(content, "content")
                .getBytes(StandardCharsets.UTF_8));
    }

    /** Pretty-prints UTF-8 JSON with a stable trailing newline. */
    public void writeJson(String relativePath, JsonElement json) {
        writeUtf8(relativePath, PRETTY_JSON.toJson(
                Objects.requireNonNull(json, "json")) + "\n");
    }

    public List<String> paths() {
        return Collections.unmodifiableList(new ArrayList<String>(resources.keySet()));
    }

    Map<String, byte[]> resources() { return resources; }

    static String validateModId(String modId) {
        if (modId == null) throw new NullPointerException("modId");
        String value = modId.trim().toLowerCase(Locale.ROOT);
        if (value.length() < 2 || value.length() > 64) {
            throw new IllegalArgumentException("modId length must be between 2 and 64 characters");
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!(c >= 'a' && c <= 'z') && !(c >= '0' && c <= '9')
                    && c != '_' && c != '-') {
                throw new IllegalArgumentException("Invalid modId character: " + c);
            }
        }
        return value;
    }

    static String validatePath(String relativePath) {
        if (relativePath == null) throw new NullPointerException("relativePath");
        String value = relativePath.trim().replace('\\', '/');
        if (value.isEmpty() || value.length() > 1024 || value.startsWith("/")) {
            throw new IllegalArgumentException("Generated path must be a non-empty relative path");
        }
        String[] segments = value.split("/", -1);
        for (String segment : segments) {
            if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) {
                throw new IllegalArgumentException("Invalid generated path segment: " + value);
            }
            for (int i = 0; i < segment.length(); i++) {
                char c = segment.charAt(i);
                if (Character.isISOControl(c) || c == ':') {
                    throw new IllegalArgumentException("Unsupported generated path character");
                }
            }
        }
        Path parsed;
        try {
            parsed = Path.of(value).normalize();
        } catch (RuntimeException failure) {
            throw new IllegalArgumentException("Invalid generated path: " + value, failure);
        }
        if (parsed.isAbsolute() || parsed.startsWith("..")) {
            throw new IllegalArgumentException("Generated path escaped its output root");
        }
        return parsed.toString().replace('\\', '/');
    }
}
