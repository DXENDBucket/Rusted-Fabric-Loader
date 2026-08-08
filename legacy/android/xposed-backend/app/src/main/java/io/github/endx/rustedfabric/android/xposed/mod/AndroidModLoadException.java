package io.github.endx.rustedfabric.android.xposed.mod;

public final class AndroidModLoadException extends Exception {
    public enum Reason {
        VERIFICATION_FAILED,
        API_VERSION_MISMATCH,
        MAPPING_PROFILE_MISMATCH,
        CAPABILITY_MISSING,
        CACHE_DIRECTORY_INVALID,
        ENTRYPOINT_INVALID,
        ENTRYPOINT_FAILED
    }

    private final Reason reason;

    AndroidModLoadException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    AndroidModLoadException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
