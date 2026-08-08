package io.github.endx.rustedfabricapi.api.asset.condition;

import java.util.Objects;

import com.google.gson.JsonObject;
import io.github.endx.rustedfabricapi.api.util.Identifier;

/** Stable identity and decoder for a registered resource condition. */
public final class ResourceConditionType {
    private final Identifier id;
    private final ResourceConditionDecoder decoder;

    ResourceConditionType(Identifier id, ResourceConditionDecoder decoder) {
        this.id = Objects.requireNonNull(id, "id");
        this.decoder = Objects.requireNonNull(decoder, "decoder");
    }

    public Identifier id() { return id; }

    ResourceCondition decode(JsonObject object) {
        return Objects.requireNonNull(decoder.decode(object),
                "Resource condition decoder returned null for " + id);
    }

    @Override public String toString() { return "ResourceConditionType{" + id + '}'; }
}
