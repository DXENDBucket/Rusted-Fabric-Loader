package io.github.endx.rustedfabric.android.bootstrap;

import java.util.Locale;

/** Exact runtime identity and first hook anchor for the finalized Android 1.15 vc176 profile. */
public final class AndroidMappingProfile {
    public static final String ID = "rw-android-1.15-code176-v1.0";
    public static final String PACKAGE_NAME = BootstrapPolicy.OFFICIAL_PACKAGE;
    public static final String VERSION_NAME = "1.15";
    public static final long VERSION_CODE = 176L;
    public static final String APK_SHA256 =
            "328f37106985a2ba424efec9ac312ede0395f3bac56e3d5db5d642dd6aecc04c";
    public static final String MAPPING_SHA256 =
            "7df59d61092a7665f023242b0221baf3ba5a3e8a3f2415bfd85a247070676d07";

    public static final String GAME_ENGINE_INIT_OWNER = "com/corrodinggames/rts/game/i";
    public static final String GAME_ENGINE_INIT_NAME = "a";
    public static final String GAME_ENGINE_INIT_DESCRIPTOR = "(Landroid/content/Context;)V";
    public static final String GAME_ENGINE_INIT_NAMED = "init";

    private AndroidMappingProfile() {
    }

    public static Selection select(String packageName, String versionName, long versionCode,
                                   String apkSha256) {
        if (!PACKAGE_NAME.equals(packageName)) {
            return new Selection("PACKAGE_MISMATCH", false);
        }
        if (!VERSION_NAME.equals(versionName) || VERSION_CODE != versionCode) {
            return new Selection("VERSION_MISMATCH", false);
        }
        if (apkSha256 == null || apkSha256.trim().isEmpty()) {
            return new Selection("APK_HASH_UNAVAILABLE", false);
        }
        if (!APK_SHA256.equals(apkSha256.trim().toLowerCase(Locale.ROOT))) {
            return new Selection("APK_HASH_MISMATCH", false);
        }
        return new Selection("VERIFIED", true);
    }

    public static String gameEngineInitBinaryClassName() {
        return GAME_ENGINE_INIT_OWNER.replace('/', '.');
    }

    public static final class Selection {
        private final String status;
        private final boolean verified;

        private Selection(String status, boolean verified) {
            this.status = status;
            this.verified = verified;
        }

        public String getStatus() {
            return status;
        }

        public boolean isVerified() {
            return verified;
        }

        public String diagnosticStatus() {
            return status + ":" + ID;
        }
    }
}
