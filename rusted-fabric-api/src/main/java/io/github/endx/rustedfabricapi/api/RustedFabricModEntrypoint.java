package io.github.endx.rustedfabricapi.api;

/** Platform-neutral mod entrypoint. Windows Jar and Android DEX builds may share this source. */
@FunctionalInterface
public interface RustedFabricModEntrypoint {
    void onInitialize(RustedFabricAPIContext context);
}
