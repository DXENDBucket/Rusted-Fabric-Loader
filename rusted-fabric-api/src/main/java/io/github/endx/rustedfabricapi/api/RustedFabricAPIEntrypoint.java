package io.github.endx.rustedfabricapi.api;

import java.util.Map;
import java.util.function.Consumer;

public abstract class RustedFabricAPIEntrypoint implements Consumer<Map<String, Object>> {
    @Override
    public final void accept(Map<String, Object> raw) {
        onRustedFabricAPI(new RustedFabricAPIContext(raw));
    }

    protected abstract void onRustedFabricAPI(RustedFabricAPIContext ctx);
}
