package io.github.endx.rustedfabricapi.api.datagen;

/** One deterministic producer of resources for a normal mod Jar. */
@FunctionalInterface
public interface DataProvider {
    void generate(DataOutput output) throws Exception;
}
