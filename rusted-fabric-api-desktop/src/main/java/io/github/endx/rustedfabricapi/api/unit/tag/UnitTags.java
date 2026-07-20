package io.github.endx.rustedfabricapi.api.unit.tag;

import rustedwarfare.custom.CustomTagList;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.custom.MutableTagListBuilder;
import rustedwarfare.custom.UnitTag;
import rustedwarfare.unit.Unit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Parsing, matching and runtime mutation helpers for mapped unit tags. */
public final class UnitTags {
    private UnitTags() {
    }

    public static CustomTagList empty() {
        return UnitTag.emptyTagList;
    }

    public static UnitTag tag(String name) {
        Objects.requireNonNull(name, "name");
        UnitTag result = UnitTag.parseSingleTag(name);
        if (result == null) throw new IllegalArgumentException("invalid unit tag: " + name);
        return result;
    }

    public static CustomTagList parse(String value) {
        Objects.requireNonNull(value, "value");
        CustomTagList result = UnitTag.parseTagList(value);
        return result != null ? result : empty();
    }

    public static CustomTagList of(String... names) {
        Objects.requireNonNull(names, "names");
        MutableTagListBuilder builder = new MutableTagListBuilder();
        for (String name : names) {
            builder.addTag(tag(Objects.requireNonNull(name, "names contains null")));
        }
        CustomTagList result = builder.toTagList();
        return result != null ? result : empty();
    }

    public static List<String> names(CustomTagList tags) {
        if (tags == null || tags.tags == null || tags.tags.length == 0) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<String>(tags.tags.length);
        for (UnitTag tag : tags.tags) {
            if (tag != null) result.add(tag.toString());
        }
        return Collections.unmodifiableList(result);
    }

    public static CustomTagList runtime(Unit unit) {
        Objects.requireNonNull(unit, "unit");
        CustomTagList result = unit.getRuntimeTags();
        return result != null ? result : empty();
    }

    public static boolean contains(CustomTagList tags, UnitTag tag) {
        Objects.requireNonNull(tag, "tag");
        return tags != null && UnitTag.tagListContains(tag, tags);
    }

    public static boolean contains(Unit unit, String tagName) {
        return contains(runtime(unit), tag(tagName));
    }

    public static boolean anyMatches(CustomTagList first, CustomTagList second) {
        if (first == null || second == null) return false;
        return UnitTag.anyTagMatches(first, second);
    }

    /** Returns whether every tag in {@code required} exists in {@code available}. */
    public static boolean containsAll(CustomTagList available, CustomTagList required) {
        if (required == null || required.isEmpty()) return true;
        return available != null && UnitTag.allTagsPresent(required, available);
    }

    /** Replaces runtime tags and refreshes the game's team tag index. */
    public static void set(CustomUnit unit, CustomTagList tags) {
        Objects.requireNonNull(unit, "unit");
        unit.setRuntimeTags(normalize(tags), false);
    }

    public static void add(CustomUnit unit, CustomTagList tags) {
        Objects.requireNonNull(unit, "unit");
        unit.addRuntimeTags(normalize(tags));
    }

    public static void remove(CustomUnit unit, CustomTagList tags) {
        Objects.requireNonNull(unit, "unit");
        unit.removeRuntimeTags(normalize(tags));
    }

    /** Restores metadata-defined tags and refreshes the game's team tag index. */
    public static void reset(CustomUnit unit) {
        Objects.requireNonNull(unit, "unit");
        unit.resetRuntimeTagsFromMetadata(false);
    }

    private static CustomTagList normalize(CustomTagList tags) {
        return tags != null ? tags : empty();
    }
}
