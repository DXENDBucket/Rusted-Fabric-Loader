package io.github.endx.rustedfabricapi.api;

import java.util.Map;
import java.util.function.Consumer;

/** Typed adapter for the shared Loader's Rusted-specific Fabric map callback. */
public abstract class RustedFabricAPIEntrypoint implements Consumer<Map<String, Object>> {
    @Override
    public final void accept(Map<String, Object> raw) {
        RustedFabricAPIContext context = new RustedFabricAPIContext(raw);
        RustedFabricRuntime.installContext(context);
        onRustedFabricAPI(context);
    }

    protected abstract void onRustedFabricAPI(RustedFabricAPIContext context);
}
