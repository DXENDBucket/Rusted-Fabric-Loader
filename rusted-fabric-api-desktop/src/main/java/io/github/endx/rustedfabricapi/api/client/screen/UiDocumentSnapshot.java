package io.github.endx.rustedfabricapi.api.client.screen;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Immutable, LibRocket-free view of an open or loading UI document. */
public final class UiDocumentSnapshot {
    private final long id;
    private final UiDocumentKind kind;
    private final String path;
    private final String title;
    private final String message;
    private final String inputDefaultValue;
    private final boolean hasTextInput;
    private final boolean showBackButton;
    private final Map<String, String> metadata;

    public UiDocumentSnapshot(long id, UiDocumentKind kind, String path, String title,
            String message, String inputDefaultValue, boolean showBackButton,
            Map<String, String> metadata) {
        if (id <= 0L) throw new IllegalArgumentException("id must be positive");
        this.id = id;
        this.kind = Objects.requireNonNull(kind, "kind");
        this.path = clean(path);
        this.title = clean(title);
        this.message = clean(message);
        this.hasTextInput = inputDefaultValue != null;
        this.inputDefaultValue = clean(inputDefaultValue);
        this.showBackButton = showBackButton;
        Map<String, String> copy = new LinkedHashMap<String, String>();
        if (metadata != null) {
            for (Map.Entry<String, String> entry : metadata.entrySet()) {
                String key = clean(entry.getKey());
                if (!key.isEmpty()) copy.put(key, clean(entry.getValue()));
            }
        }
        this.metadata = Collections.unmodifiableMap(copy);
    }

    public long id() { return id; }
    public UiDocumentKind kind() { return kind; }
    public String path() { return path; }
    public Optional<String> title() { return optional(title); }
    public Optional<String> message() { return optional(message); }
    public Optional<String> inputDefaultValue() { return optional(inputDefaultValue); }
    public boolean hasTextInput() { return hasTextInput; }
    public boolean showBackButton() { return showBackButton; }
    public boolean modal() { return kind != UiDocumentKind.PAGE; }
    public Map<String, String> metadata() { return metadata; }
    public Optional<String> metadata(String key) {
        return Optional.ofNullable(metadata.get(Objects.requireNonNull(key, "key")));
    }

    private static Optional<String> optional(String value) {
        return value.isEmpty() ? Optional.empty() : Optional.of(value);
    }

    private static String clean(String value) {
        return value != null ? value : "";
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof UiDocumentSnapshot
                && id == ((UiDocumentSnapshot) other).id;
    }

    @Override
    public int hashCode() { return Long.hashCode(id); }

    @Override
    public String toString() {
        return "UiDocumentSnapshot{" + id + ", " + kind + ", path='" + path + '\''
                + title().map(value -> ", title='" + value + '\'').orElse("") + '}';
    }
}
