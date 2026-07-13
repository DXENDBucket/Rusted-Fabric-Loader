package io.github.endx.rustedfabric.android.inspector;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

final class MappingProfileMatch {
    String id;
    String status = "UNMATCHED";
    String match = "NONE";
    String mappingSha256;
    boolean packageMatches;
    boolean versionMatches;
    final Map<String, Boolean> anchors = new LinkedHashMap<>();

    static MappingProfileMatch unmatched() {
        MappingProfileMatch result = new MappingProfileMatch();
        result.anchors.putAll(Collections.emptyMap());
        return result;
    }
}
