package io.github.endx.rustedfabric.android.mod;

public final class ModVerificationException extends Exception {
    public enum Reason {
        INVALID_ARCHIVE,
        INVALID_METADATA,
        LIMIT_EXCEEDED,
        MISSING_DEX,
        FORBIDDEN_ENTRY,
        INVALID_DEX,
        FORBIDDEN_CLASS_DEFINITION,
        ENTRYPOINT_NOT_DEFINED
    }

    private final Reason reason;

    public ModVerificationException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public ModVerificationException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
