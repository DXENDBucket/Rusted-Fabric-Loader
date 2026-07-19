package io.github.endx.rustedfabric.android.jvm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Runtime pieces supplied by the Loader, never by the imported game directory. */
public final class JvmBackendCapabilities {
    private final boolean java13;
    private final boolean lwjgl2;
    private final boolean openAl;
    private final boolean jinput;
    private final boolean rocketConnector;

    public JvmBackendCapabilities(boolean java13, boolean lwjgl2, boolean openAl,
                                  boolean jinput, boolean rocketConnector) {
        this.java13 = java13;
        this.lwjgl2 = lwjgl2;
        this.openAl = openAl;
        this.jinput = jinput;
        this.rocketConnector = rocketConnector;
    }

    public static JvmBackendCapabilities unavailable() {
        return new JvmBackendCapabilities(false, false, false, false, false);
    }

    public boolean isLaunchReady() {
        return missing().isEmpty();
    }

    public List<String> missing() {
        List<String> missing = new ArrayList<>();
        if (!java13) missing.add("arm64-java-13");
        if (!lwjgl2) missing.add("lwjgl2-renderer-bridge");
        if (!openAl) missing.add("openal-arm64");
        if (!jinput) missing.add("android-input-bridge");
        if (!rocketConnector) missing.add("rocket-connector-arm64");
        return Collections.unmodifiableList(missing);
    }
}
