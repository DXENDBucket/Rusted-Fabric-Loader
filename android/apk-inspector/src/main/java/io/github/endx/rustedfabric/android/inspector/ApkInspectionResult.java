package io.github.endx.rustedfabric.android.inspector;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ApkInspectionResult {
    String apkSha256;
    long apkSize;
    int entryCount;
    int nativeLibraryCount;
    final List<String> nativeAbis = new ArrayList<>();
    AndroidManifestInfo manifest;
    SigningInfo signing;
    DexInventory dex;
    MappingProfileMatch profile;

    String toJson() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schemaVersion", 1);

        Map<String, Object> apk = new LinkedHashMap<>();
        apk.put("sha256", apkSha256);
        apk.put("size", apkSize);
        apk.put("entryCount", entryCount);
        apk.put("dexCount", dex.dexFiles);
        apk.put("nativeLibraryCount", nativeLibraryCount);
        apk.put("nativeAbis", nativeAbis);
        root.put("apk", apk);

        Map<String, Object> manifestJson = new LinkedHashMap<>();
        manifestJson.put("packageName", manifest.packageName);
        manifestJson.put("versionName", manifest.versionName);
        manifestJson.put("versionCode", manifest.versionCode);
        manifestJson.put("compileSdk", manifest.compileSdk);
        manifestJson.put("minSdk", manifest.minSdk);
        manifestJson.put("targetSdk", manifest.targetSdk);
        manifestJson.put("applicationClass", manifest.applicationClass);
        manifestJson.put("launcherActivity", manifest.launcherActivity);
        manifestJson.put("permissions", manifest.permissions);
        root.put("manifest", manifestJson);

        Map<String, Object> signingJson = new LinkedHashMap<>();
        signingJson.put("v1CertificateSha256", signing.v1CertificateSha256);
        signingJson.put("apkSigningBlockPresent", signing.apkSigningBlockPresent);
        root.put("signing", signingJson);

        Map<String, Object> dexJson = new LinkedHashMap<>();
        dexJson.put("strings", dex.strings);
        dexJson.put("types", dex.types);
        dexJson.put("prototypes", dex.prototypes);
        dexJson.put("fields", dex.fields);
        dexJson.put("methods", dex.methods);
        dexJson.put("classDefinitions", dex.classDefinitions);
        dexJson.put("classSetSha256", dex.classSetSha256());
        dexJson.put("dexSha256", dex.dexSha256);
        root.put("dex", dexJson);

        root.put("anchors", new LinkedHashMap<>(profile.anchors));
        Map<String, Object> profileJson = new LinkedHashMap<>();
        profileJson.put("id", profile.id);
        profileJson.put("status", profile.status);
        profileJson.put("match", profile.match);
        profileJson.put("matchPolicy", profile.matchPolicy);
        profileJson.put("mappingSha256", profile.mappingSha256);
        root.put("mappingProfile", profileJson);

        List<String> reasons = new ArrayList<>();
        String level;
        boolean anchorsComplete = !profile.anchors.isEmpty()
                && profile.anchors.values().stream().allMatch(Boolean.TRUE::equals);
        if (dex.dexFiles == 0 || manifest.packageName == null || "UNMATCHED".equals(profile.status)) {
            level = "UNSUPPORTED";
            reasons.add("NO_COMPATIBLE_PROFILE");
        } else if (!anchorsComplete) {
            level = "PARTIAL";
            reasons.add("STRUCTURAL_ANCHORS_INCOMPLETE");
        } else if ("PENDING_MAPPING".equals(profile.status)) {
            level = "STRUCTURAL";
            reasons.add("STRUCTURAL_ANCHORS_MATCH");
            reasons.add("PROFILE_MAPPING_PENDING");
        } else if ("EXACT".equals(profile.match)) {
            level = "VERIFIED";
            reasons.add("EXACT_APK_AND_MAPPING_MATCH");
        } else {
            level = "MAPPED";
            reasons.add("STRUCTURAL_APK_AND_MAPPING_MATCH");
        }
        Map<String, Object> compatibility = new LinkedHashMap<>();
        compatibility.put("level", level);
        compatibility.put("targetBindingRequired", !profile.packageMatches);
        compatibility.put("reasons", reasons);
        root.put("compatibility", compatibility);

        Map<String, Object> privacy = new LinkedHashMap<>();
        privacy.put("reportContainsGamePayload", false);
        privacy.put("reportContainsLocalPath", false);
        root.put("privacy", privacy);
        return Json.write(root);
    }
}
