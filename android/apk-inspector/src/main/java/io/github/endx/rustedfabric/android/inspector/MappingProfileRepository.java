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
        String expectedVersion = properties.getProperty("versionCode", "").trim();
        String expectedApkSha = properties.getProperty("apkSha256", "").trim().toLowerCase(java.util.Locale.ROOT);
        result.packageMatches = expectedPackage.equals(manifest.packageName);
        result.versionMatches = expectedVersion.isEmpty()
                || (manifest.versionCode != null && expectedVersion.equals(Long.toString(manifest.versionCode)));

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
        Path profileDirectory = file.getParent().toAbsolutePath().normalize();
        Path mappingFile = mappingName.isEmpty() ? null
                : profileDirectory.resolve(mappingName).toAbsolutePath().normalize();
        if (mappingFile != null && !mappingFile.startsWith(profileDirectory)) {
            throw new IllegalArgumentException("Mapping file must stay inside its profile directory");
        }
        if (mappingFile != null && Files.isRegularFile(mappingFile)) {
            result.status = "READY";
            result.mappingSha256 = Hashing.sha256(mappingFile);
        } else {
            result.status = "PENDING_MAPPING";
        }

        int score = (hashMatches ? 1000 : 0) + (result.packageMatches ? 100 : 0)
                + (result.versionMatches ? 50 : 0) + (allAnchors ? 25 : 0);
        // Exact hashes accept a known community variant. Otherwise package, version and all anchors must agree.
        boolean eligible = hashMatches || (result.packageMatches && result.versionMatches && allAnchors);
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
