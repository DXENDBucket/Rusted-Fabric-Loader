package io.github.endx.rustedfabric.android.inspector;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class MappingProfileRepository {
    MappingProfileMatch find(Path root, AndroidManifestInfo manifest, DexInventory dex, String apkSha256)
            throws IOException {
        if (root == null || !Files.isDirectory(root)) {
            return MappingProfileMatch.unmatched();
        }
        List<Path> files;
        try (Stream<Path> stream = Files.walk(root)) {
            files = stream.filter(path -> path.getFileName().toString().equals("profile.properties"))
                    .sorted(Comparator.comparing(path -> root.relativize(path).toString()))
                    .collect(Collectors.toList());
        }
        List<Candidate> candidates = new ArrayList<>();
        for (Path file : files) {
            candidates.add(load(file, manifest, dex, apkSha256));
        }
        return candidates.stream()
                .filter(candidate -> candidate.eligible)
                .max(Comparator.comparingInt((Candidate candidate) -> candidate.score)
                        .thenComparing(candidate -> candidate.result.id, Comparator.reverseOrder()))
                .map(candidate -> candidate.result)
                .orElseGet(MappingProfileMatch::unmatched);
    }

    private Candidate load(Path file, AndroidManifestInfo manifest, DexInventory dex, String apkSha256)
            throws IOException {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
        }
        MappingProfileMatch result = new MappingProfileMatch();
        result.id = required(properties, "id", file);
        String expectedPackage = required(properties, "packageName", file);
        String expectedVersionCode = properties.getProperty("versionCode", "").trim();
        String expectedVersionName = properties.getProperty("versionName", "").trim();
        String expectedApkSha = properties.getProperty("apkSha256", "").trim().toLowerCase(java.util.Locale.ROOT);
        result.matchPolicy = properties.getProperty("matchPolicy", "structural")
                .trim().toLowerCase(java.util.Locale.ROOT);
        if (!"exact".equals(result.matchPolicy) && !"structural".equals(result.matchPolicy)) {
            throw new IllegalArgumentException("Unsupported matchPolicy in " + file.getFileName()
                    + ": " + result.matchPolicy);
        }
        result.packageMatches = expectedPackage.equals(manifest.packageName);
        boolean versionCodeMatches = expectedVersionCode.isEmpty()
                || (manifest.versionCode != null
                && expectedVersionCode.equals(Long.toString(manifest.versionCode)));
        boolean versionNameMatches = expectedVersionName.isEmpty()
                || expectedVersionName.equals(manifest.versionName);
        result.versionMatches = versionCodeMatches && versionNameMatches;

        List<String> anchorNames = new ArrayList<>();
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith("anchor.")) {
                anchorNames.add(key.substring("anchor.".length()));
            }
        }
        Collections.sort(anchorNames);
        for (String name : anchorNames) {
            String descriptor = properties.getProperty("anchor." + name).trim();
            result.anchors.put(name, dex.classDescriptors.contains(descriptor));
        }
        boolean allAnchors = !result.anchors.isEmpty()
                && result.anchors.values().stream().allMatch(Boolean.TRUE::equals);
        boolean hashMatches = !expectedApkSha.isEmpty() && expectedApkSha.equals(apkSha256);
        result.match = hashMatches ? "EXACT" : "STRUCTURAL";

        String mappingName = properties.getProperty("mappingFile", "").trim();
        String expectedMappingSha = properties.getProperty("mappingFileSha256", "")
                .trim().toLowerCase(java.util.Locale.ROOT);
        Path profileDirectory = file.getParent().toAbsolutePath().normalize();
        Path mappingFile = mappingName.isEmpty() ? null
                : profileDirectory.resolve(mappingName).toAbsolutePath().normalize();
        if (mappingFile != null && !mappingFile.startsWith(profileDirectory)) {
            throw new IllegalArgumentException("Mapping file must stay inside its profile directory");
        }
        if (mappingFile != null && Files.isRegularFile(mappingFile)) {
            result.mappingSha256 = Hashing.sha256(mappingFile);
            if (!expectedMappingSha.isEmpty() && !expectedMappingSha.equals(result.mappingSha256)) {
                throw new IllegalArgumentException("Mapping checksum mismatch for " + mappingFile.getFileName());
            }
            result.status = "READY";
        } else {
            result.status = "PENDING_MAPPING";
        }

        int score = (hashMatches ? 1000 : 0) + (result.packageMatches ? 100 : 0)
                + (result.versionMatches ? 50 : 0) + (allAnchors ? 25 : 0);
        boolean identityMatches = result.packageMatches && result.versionMatches;
        // A finalized exact profile must never be applied to a modified APK. Community variants get
        // their own structural profile only after their mapping anchors have been verified.
        boolean eligible = "exact".equals(result.matchPolicy)
                ? identityMatches && hashMatches
                : identityMatches && (hashMatches || allAnchors);
        return new Candidate(result, score, eligible);
    }

    private static String required(Properties properties, String key, Path file) {
        String value = properties.getProperty(key, "").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Missing " + key + " in mapping profile " + file.getFileName());
        }
        return value;
    }

    private static final class Candidate {
        final MappingProfileMatch result;
        final int score;
        final boolean eligible;

        Candidate(MappingProfileMatch result, int score, boolean eligible) {
            this.result = result;
            this.score = score;
            this.eligible = eligible;
        }
    }
}
