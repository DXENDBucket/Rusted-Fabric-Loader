package io.github.endx.rustedfabricapi.api.asset.condition;

import io.github.endx.rustedfabricapi.api.util.Identifier;

/** IDs of conditions supplied by Rusted Fabric API. */
public final class BuiltinResourceConditions {
    public static final Identifier TRUE = id("true");
    public static final Identifier FALSE = id("false");
    public static final Identifier ALL_MODS_LOADED = id("all_mods_loaded");
    public static final Identifier ANY_MOD_LOADED = id("any_mod_loaded");
    public static final Identifier NOT = id("not");
    public static final Identifier ALL = id("all");
    public static final Identifier ANY = id("any");
    public static final Identifier REGISTRY_CONTAINS = id("registry_contains");
    public static final Identifier TAG_CONTAINS = id("tag_contains");

    private BuiltinResourceConditions() {
    }

    private static Identifier id(String path) { return Identifier.of("rusted_fabric", path); }
}
