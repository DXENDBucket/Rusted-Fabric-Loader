package io.github.endx.rustedfabricapi.api;

import java.util.Map;
import java.util.function.Consumer;

/**
 * One entrypoint implementation for both Fabric's map callback and Android's DEX loader.
 * Subclasses can therefore be compiled from one source set into a Jar and a {@code .javamod}.
 */
public abstract class RustedFabricAPIEntrypoint
        implements Consumer<Map<String, Object>>, RustedFabricModEntrypoint {
    @Override
    public final void accept(Map<String, Object> raw) {
        RustedFabricAPIContext context = new RustedFabricAPIContext(raw);
        RustedFabricRuntime.installContext(context);
        onInitialize(context);
    }

    @Override
    public final void onInitialize(RustedFabricAPIContext context) {
        RustedFabricRuntime.installContext(context);
        onRustedFabricAPI(context);
    }

    protected abstract void onRustedFabricAPI(RustedFabricAPIContext context);
}
