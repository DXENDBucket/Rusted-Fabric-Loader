package io.github.endx.rustedfabricapi.api.ini;

/** Describes how an INI extension relates to Rusted Warfare's native syntax. */
public enum IniExtensionKind {
    /** A key that the native game does not define. */
    NEW_KEY,
    /** An additional legal value or range for a native key. */
    EXTENDED_VALUE,
    /** An additional textual format for a native key. */
    EXTENDED_FORMAT
}
