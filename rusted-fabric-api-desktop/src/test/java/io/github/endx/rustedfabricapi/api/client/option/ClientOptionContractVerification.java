package io.github.endx.rustedfabricapi.api.client.option;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.endx.rustedfabricapi.api.client.option.event.ClientOptionEvents;
import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;

public final class ClientOptionContractVerification {
    private ClientOptionContractVerification() {
    }

    public static void verify() {
        require(ClientOptions.all().size() == 38,
                "supported client option catalog is incomplete");
        Set<Object> ids = new HashSet<Object>();
        Set<String> nativeNames = new HashSet<String>();
        for (ClientOption<?> option : ClientOptions.all()) {
            require(ids.add(option.id()), "duplicate client option id: " + option.id());
            require(nativeNames.add(option.nativeName()),
                    "duplicate native client option name: " + option.nativeName());
            require(ClientOptions.findByNativeName(option.nativeName()).orElse(null) == option,
                    "native option lookup lost catalog identity: " + option);
        }
        require(ClientOptions.MASTER_VOLUME.isValid(Float.valueOf(0.0F))
                        && ClientOptions.MASTER_VOLUME.isValid(Float.valueOf(1.0F))
                        && !ClientOptions.MASTER_VOLUME.isValid(Float.valueOf(1.01F))
                        && !ClientOptions.MASTER_VOLUME.isValid(Float.valueOf(Float.NaN)),
                "volume validation drifted");
        require(!ClientOptions.UI_RENDER_SCALE.isValid(Float.valueOf(0.0F))
                        && ClientOptions.UI_RENDER_SCALE.isValid(Float.valueOf(1.0F)),
                "UI render scale validation drifted");

        ClientOptionChange<Boolean> showFps = ClientOptionChange.of(
                ClientOptions.SHOW_FPS, Boolean.FALSE, Boolean.TRUE);
        ClientOptionChange<Boolean> fullscreen = ClientOptionChange.of(
                ClientOptions.FULLSCREEN, Boolean.FALSE, Boolean.TRUE);
        ClientOptionChangeSet changeSet = new ClientOptionChangeSet(
                Arrays.asList(showFps, fullscreen));
        require(changeSet.size() == 2 && changeSet.contains(ClientOptions.SHOW_FPS)
                        && changeSet.restartRequired()
                        && Boolean.TRUE.equals(changeSet.change(ClientOptions.SHOW_FPS)
                                .orElseThrow(AssertionError::new).requestedValue()),
                "client option change-set value contract failed");

        AtomicInteger calls = new AtomicInteger();
        RustedFabricEvent.Registration first = ClientOptionEvents.BEFORE_UPDATE.subscribe(
                (settings, changes) -> {
                    calls.incrementAndGet();
                    return false;
                });
        RustedFabricEvent.Registration second = ClientOptionEvents.BEFORE_UPDATE.subscribe(
                (settings, changes) -> {
                    calls.incrementAndGet();
                    return true;
                });
        require(ClientOptionEvents.BEFORE_UPDATE.invoker().beforeUpdate(null, changeSet),
                "client option transaction cancellation was not aggregated");
        require(calls.get() == 2,
                "client option transaction cancellation skipped a listener");
        first.close();
        second.close();

        calls.set(0);
        ClientOptionUpdateResult updateResult = new ClientOptionUpdateResult(
                changeSet, true, false, true, true);
        RustedFabricEvent.Registration after = ClientOptionEvents.AFTER_UPDATE.subscribe(
                (settings, result) -> {
                    if (result.applied() && result.persistenceSuccessful()) calls.incrementAndGet();
                });
        RustedFabricEvent.Registration dynamic =
                ClientOptionEvents.AFTER_NATIVE_DYNAMIC_CHANGE.subscribe((settings, change) -> {
                    if (change.option() == ClientOptions.SHOW_FPS) calls.addAndGet(10);
                });
        RustedFabricEvent.Registration saved = ClientOptionEvents.AFTER_NATIVE_SAVE.subscribe(
                (settings, successful) -> calls.addAndGet(successful ? 100 : 1000));
        ClientOptionEvents.AFTER_UPDATE.invoker().afterUpdate(null, updateResult);
        ClientOptionEvents.AFTER_NATIVE_DYNAMIC_CHANGE.invoker().afterChange(null, showFps);
        ClientOptionEvents.AFTER_NATIVE_SAVE.invoker().afterSave(null, true);
        require(calls.get() == 111,
                "client option lifecycle events were not dispatched");
        after.close();
        dynamic.close();
        saved.close();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
