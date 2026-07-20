package io.github.endx.rustedfabricapi.api.datagen;

import java.util.Objects;
import java.util.Optional;

import io.github.endx.rustedfabricapi.api.util.Identifier;

/** Immutable provider result retained even when the transaction is not committed. */
public final class DataProviderResult {
    private final Identifier id;
    private final DataProviderStatus status;
    private final int resourceCount;
    private final String detail;
    private final Exception failure;

    DataProviderResult(Identifier id, DataProviderStatus status, int resourceCount,
            String detail, Exception failure) {
        this.id = Objects.requireNonNull(id, "id");
        this.status = Objects.requireNonNull(status, "status");
        this.resourceCount = resourceCount;
        this.detail = detail != null ? detail : "";
        this.failure = failure;
    }

    public Identifier id() { return id; }

    public DataProviderStatus status() { return status; }

    public int resourceCount() { return resourceCount; }

    public String detail() { return detail; }

    public Optional<Exception> failure() { return Optional.ofNullable(failure); }

    public boolean generated() { return status == DataProviderStatus.GENERATED; }

    @Override public String toString() {
        return "DataProviderResult{" + id + '=' + status + ", resources="
                + resourceCount + (detail.isEmpty() ? "" : ", " + detail) + '}';
    }
}
