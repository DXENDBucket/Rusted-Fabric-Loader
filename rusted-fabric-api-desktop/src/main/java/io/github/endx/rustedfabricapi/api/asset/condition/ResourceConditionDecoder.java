package io.github.endx.rustedfabricapi.api.asset.condition;

import com.google.gson.JsonObject;

/** Decodes one JSON condition object during a resource reloader's prepare phase. */
@FunctionalInterface
public interface ResourceConditionDecoder {
    ResourceCondition decode(JsonObject object);
}
