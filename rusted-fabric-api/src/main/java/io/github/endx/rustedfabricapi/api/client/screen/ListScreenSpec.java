package io.github.endx.rustedfabricapi.api.client.screen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable content for a native, touch-scrollable list page. */
public final class ListScreenSpec {
    public static final int MAX_ENTRIES = 1024;

    private final String title;
    private final String summary;
    private final String emptyMessage;
    private final String backButton;
    private final String filterLabel;
    private final List<ListScreenEntry> entries;

    private ListScreenSpec(Builder builder) {
        title = ListScreenEntry.checked(builder.title, "title", 512, false);
        summary = ListScreenEntry.checked(builder.summary, "summary", 4096, true);
        emptyMessage = ListScreenEntry.checked(builder.emptyMessage, "emptyMessage", 4096, false);
        backButton = ListScreenEntry.checked(builder.backButton, "backButton", 128, false);
        filterLabel = ListScreenEntry.checked(builder.filterLabel, "filterLabel", 128, true);
        entries = Collections.unmodifiableList(new ArrayList<ListScreenEntry>(builder.entries));
    }

    public static Builder builder(String title) { return new Builder(title); }

    public String title() { return title; }
    public String summary() { return summary; }
    public String emptyMessage() { return emptyMessage; }
    public String backButton() { return backButton; }
    /** Empty when this page has no native filter field. */
    public String filterLabel() { return filterLabel; }
    public boolean filterEnabled() { return !filterLabel.isEmpty(); }
    public List<ListScreenEntry> entries() { return entries; }

    public static final class Builder {
        private final String title;
        private String summary = "";
        private String emptyMessage = "No entries";
        private String backButton = "Back";
        private String filterLabel = "";
        private final List<ListScreenEntry> entries = new ArrayList<ListScreenEntry>();

        private Builder(String title) { this.title = title; }

        public Builder summary(String value) {
            summary = value;
            return this;
        }

        public Builder emptyMessage(String value) {
            emptyMessage = value;
            return this;
        }

        public Builder backButton(String value) {
            backButton = value;
            return this;
        }

        /** Adds a top-right text filter using the native Mods-page styling. */
        public Builder filter(String label) {
            filterLabel = label;
            return this;
        }

        public Builder add(ListScreenEntry entry) {
            if (entries.size() >= MAX_ENTRIES) {
                throw new IllegalStateException("A list screen supports at most " + MAX_ENTRIES + " entries");
            }
            entries.add(java.util.Objects.requireNonNull(entry, "entry"));
            return this;
        }

        public Builder add(String title, String details, String description) {
            return add(ListScreenEntry.of(title, details, description));
        }

        public ListScreenSpec build() { return new ListScreenSpec(this); }
    }
}
