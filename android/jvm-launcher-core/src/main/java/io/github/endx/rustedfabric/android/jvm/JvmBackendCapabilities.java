package io.github.endx.rustedfabric.android.jvm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Runtime pieces supplied by the Loader, never by the imported game directory. */
public final class JvmBackendCapabilities {
    private final boolean java17;
    private final boolean jvmHost;
    private final boolean lwjgl2;
    private final boolean openAl;
    private final boolean jinput;
    private final boolean rocketConnector;

    public JvmBackendCapabilities(boolean java17, boolean lwjgl2, boolean openAl,
                                  boolean jinput, boolean rocketConnector) {
        this(java17, true, lwjgl2, openAl, jinput, rocketConnector);
    }

    public JvmBackendCapabilities(boolean java17, boolean jvmHost, boolean lwjgl2,
                                  boolean openAl, boolean jinput, boolean rocketConnector) {
        this.java17 = java17;
        this.jvmHost = jvmHost;
        this.lwjgl2 = lwjgl2;
        this.openAl = openAl;
        this.jinput = jinput;
        this.rocketConnector = rocketConnector;
    }

    public static JvmBackendCapabilities unavailable() {
        return new JvmBackendCapabilities(false, false, false, false, false, false);
    }

    public boolean isLaunchReady() {
        return missing().isEmpty();
    }

    public boolean hasJava17() { return java17; }
    public boolean hasJvmHost() { return jvmHost; }
    public boolean hasLwjgl2() { return lwjgl2; }
    public boolean hasOpenAl() { return openAl; }
    public boolean hasJinput() { return jinput; }
    public boolean hasRocketConnector() { return rocketConnector; }

    public List<String> missing() {
        List<String> missing = new ArrayList<>();
        if (!java17) missing.add("arm64-java-17");
        if (!jvmHost) missing.add("native-jvm-host");
        if (!lwjgl2) missing.add("lwjgl2-renderer-bridge");
        if (!openAl) missing.add("openal-arm64");
        if (!jinput) missing.add("android-input-bridge");
        if (!rocketConnector) missing.add("rocket-connector-arm64");
        return Collections.unmodifiableList(missing);
    }
}
