package io.github.endx.rustedfabric.android.bootstrap;

/** Process-selection rules shared by the Xposed entrypoint and future backends. */
public final class BootstrapPolicy {
    public static final String OFFICIAL_PACKAGE = "com.corrodinggames.rts";

    private BootstrapPolicy() {
    }

    /**
     * Only the first package load for the explicitly scoped official package may install hooks.
     * Package-renamed community variants require an explicit profile and are not guessed here.
     */
    public static boolean shouldInstall(String packageName, boolean firstPackage) {
        return firstPackage && OFFICIAL_PACKAGE.equals(packageName);
    }
}
