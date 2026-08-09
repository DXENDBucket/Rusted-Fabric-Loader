package io.github.endx.rustedfabricapi.api.ini;

import java.util.Objects;

/** A serializable, documentation-friendly selector for INI section names. */
public final class IniSectionSelector {
    public enum Kind { EXACT, PREFIX, ANY }

    private static final IniSectionSelector ANY = new IniSectionSelector(Kind.ANY, "*");
    private final Kind kind;
    private final String value;

    private IniSectionSelector(Kind kind, String value) {
        this.kind = kind;
        this.value = value;
    }

    public static IniSectionSelector exact(String section) {
        return new IniSectionSelector(Kind.EXACT, requireName(section, "section"));
    }

    public static IniSectionSelector prefix(String prefix) {
        return new IniSectionSelector(Kind.PREFIX, requireName(prefix, "prefix"));
    }

    public static IniSectionSelector any() {
        return ANY;
    }

    public Kind kind() {
        return kind;
    }

    public String value() {
        return value;
    }

    public boolean matches(String section) {
        String candidate = Objects.requireNonNull(section, "section");
        String expected = value;
        return kind == Kind.ANY
                || (kind == Kind.EXACT && candidate.equals(expected))
                || (kind == Kind.PREFIX && candidate.startsWith(expected));
    }

    public String displayName() {
        if (kind == Kind.ANY) return "[*]";
        return "[" + value + (kind == Kind.PREFIX ? "*" : "") + "]";
    }

    private static String requireName(String value, String label) {
        String result = Objects.requireNonNull(value, label).trim();
        if (result.isEmpty()) throw new IllegalArgumentException(label + " must not be empty");
        return result;
    }
}
