package io.github.endx.rustedfabricapi.api.asset.reload;

import io.github.endx.rustedfabricapi.api.asset.ModResourcePack;

/** Two-stage reload listener: parse immutable input first, then publish it to runtime state. */
public interface ModResourceReloader<P> {
    P prepare(ModResourcePack resources) throws Exception;

    void apply(P prepared) throws Exception;
}
