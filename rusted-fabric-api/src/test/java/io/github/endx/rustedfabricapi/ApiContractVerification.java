package io.github.endx.rustedfabricapi;

import io.github.endx.rustedfabricapi.api.RustedFabricAPIContext;
import io.github.endx.rustedfabricapi.api.RustedFabricAPIEntrypoint;
import io.github.endx.rustedfabricapi.api.RustedFabricAPIKeys;
import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ApiContractVerification {
    private ApiContractVerification() {
    }

    public static void main(String[] args) {
        verifiesListenerOrderAndSnapshotRefresh();
        verifiesContextDefensiveCopies();
        verifiesEntrypointAdapter();
        System.out.println("Rusted Fabric API contract verification passed");
    }

    private static void verifiesListenerOrderAndSnapshotRefresh() {
        List<String> calls = new ArrayList<String>();
        RustedFabricEvent<Probe> event = RustedFabricEvent.create(listeners -> value -> {
            for (Probe listener : listeners) {
                listener.accept(value);
            }
        });

        event.register(value -> calls.add("first:" + value));
        event.register(value -> calls.add("second:" + value));
        event.invoker().accept("ready");

        require(calls.size() == 2, "expected two listener calls");
        require("first:ready".equals(calls.get(0)), "listeners must retain registration order");
        require("second:ready".equals(calls.get(1)), "second listener was not called");
    }

    private static void verifiesContextDefensiveCopies() {
        String[] sourceArgs = new String[] { "one", "two" };
        Map<String, Object> source = new HashMap<String, Object>();
        source.put(RustedFabricAPIKeys.K_CONTEXT_VERSION, Integer.valueOf(1));
        source.put(RustedFabricAPIKeys.K_GAME_DIR, Paths.get("game"));
        source.put(RustedFabricAPIKeys.K_GAME_ARGS, sourceArgs);
        source.put(RustedFabricAPIKeys.K_RUNTIME_NAMESPACE, "named");
        source.put(RustedFabricAPIKeys.K_ANDROID, Boolean.FALSE);

        RustedFabricAPIContext context = new RustedFabricAPIContext(source);
        sourceArgs[0] = "changed-at-source";
        String[] returnedArgs = context.gameArgs();
        returnedArgs[1] = "changed-by-caller";

        require(context.contextVersion() == 1, "context version missing");
        require("one".equals(context.gameArgs()[0]), "constructor must copy launch arguments");
        require("two".equals(context.gameArgs()[1]), "gameArgs must return a defensive copy");
        require("named".equals(context.runtimeNamespace()), "runtime namespace missing");
        require(!context.androidRuntime(), "unexpected Android runtime flag");

        boolean immutable = false;
        try {
            context.asMap().put("unexpected", "value");
        } catch (UnsupportedOperationException expected) {
            immutable = true;
        }
        require(immutable, "raw context view must be immutable");
    }

    private static void verifiesEntrypointAdapter() {
        final int[] observedVersion = new int[1];
        RustedFabricAPIEntrypoint entrypoint = new RustedFabricAPIEntrypoint() {
            @Override
            protected void onRustedFabricAPI(RustedFabricAPIContext context) {
                observedVersion[0] = context.contextVersion();
            }
        };
        Map<String, Object> raw = new HashMap<String, Object>();
        raw.put(RustedFabricAPIKeys.K_CONTEXT_VERSION, Integer.valueOf(1));
        entrypoint.accept(raw);
        require(observedVersion[0] == 1, "entrypoint adapter did not expose the typed context");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private interface Probe {
        void accept(String value);
    }
}
