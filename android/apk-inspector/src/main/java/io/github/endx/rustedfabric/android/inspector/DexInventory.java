package io.github.endx.rustedfabric.android.inspector;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class DexInventory {
    int dexFiles;
    long strings;
    long types;
    long prototypes;
    long fields;
    long methods;
    long classDefinitions;
    final Set<String> classDescriptors = new LinkedHashSet<>();
    final List<String> dexSha256 = new ArrayList<>();

    String classSetSha256() {
        MessageDigest digest = Hashing.digest();
        List<String> sorted = new ArrayList<>(classDescriptors);
        Collections.sort(sorted);
        for (String descriptor : sorted) {
            digest.update(descriptor.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            digest.update((byte) '\n');
        }
        return Hashing.hex(digest.digest());
    }

    void finish() {
        Collections.sort(dexSha256);
    }
}
