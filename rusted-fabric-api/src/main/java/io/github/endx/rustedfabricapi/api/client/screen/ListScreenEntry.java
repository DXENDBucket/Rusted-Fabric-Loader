package io.github.endx.rustedfabricapi.api.client.screen;

import java.util.Objects;

/** One immutable row in a native scrolling list screen. */
public final class ListScreenEntry {
    private final String title;
    private final String details;
    private final String description;

    public ListScreenEntry(String title, String details, String description) {
        this.title = checked(title, "title", 512, false);
        this.details = checked(details, "details", 4096, true);
        this.description = checked(description, "description", 16_384, true);
    }

    public static ListScreenEntry of(String title, String details, String description) {
        return new ListScreenEntry(title, details, description);
    }

    public String title() { return title; }
    public String details() { return details; }
    public String description() { return description; }

    static String checked(String value, String name, int maximum, boolean blankAllowed) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (!blankAllowed && checked.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        if (checked.length() > maximum) {
            throw new IllegalArgumentException(name + " is too long");
        }
        return checked;
    }
}
