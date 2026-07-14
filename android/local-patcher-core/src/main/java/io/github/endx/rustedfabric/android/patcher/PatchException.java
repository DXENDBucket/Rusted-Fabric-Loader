package io.github.endx.rustedfabric.android.patcher;

public final class PatchException extends Exception {
    public enum Reason {
        INPUT_MISSING,
        INPUT_TOO_LARGE,
        PROFILE_MISMATCH,
        INVALID_APK,
        MANIFEST_REWRITE_FAILED,
        DEX_REWRITE_FAILED,
        BOOTSTRAP_DEX_INVALID,
        SIGNING_FAILED,
        SIGNATURE_INVALID,
        OUTPUT_FAILED
    }

    private final Reason reason;

    public PatchException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public PatchException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
