package io.github.endx.rustedfabric.android.inspector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class AndroidManifestInfo {
    String packageName;
    String versionName;
    Long versionCode;
    Long compileSdk;
    Long minSdk;
    Long targetSdk;
    String applicationClass;
    String launcherActivity;
    final List<String> permissions = new ArrayList<>();

    void finish() {
        Collections.sort(permissions);
    }
}
