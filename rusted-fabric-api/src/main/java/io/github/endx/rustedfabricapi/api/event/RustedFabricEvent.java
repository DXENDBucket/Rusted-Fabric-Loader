package io.github.endx.rustedfabricapi.api.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

public final class RustedFabricEvent<T> {
    private final CopyOnWriteArrayList<T> listeners = new CopyOnWriteArrayList<T>();
    private final Function<List<T>, T> invokerFactory;
    private volatile T invoker;

    private RustedFabricEvent(Function<List<T>, T> invokerFactory) {
        this.invokerFactory = Objects.requireNonNull(invokerFactory, "invokerFactory");
        updateInvoker();
    }

    public static <T> RustedFabricEvent<T> create(Function<List<T>, T> invokerFactory) {
        return new RustedFabricEvent<T>(invokerFactory);
    }

    public void register(T listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
        updateInvoker();
    }

    public T invoker() {
        return invoker;
    }

    private void updateInvoker() {
        List<T> snapshot = new ArrayList<T>(listeners);
        invoker = invokerFactory.apply(Collections.unmodifiableList(snapshot));
    }
}
