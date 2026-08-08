package io.github.endx.rustedfabric.android.inspector;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

final class ApkInspector {
    private static final int MAX_MANIFEST_SIZE = 4 * 1024 * 1024;
    private static final int MAX_DEX_SIZE = 256 * 1024 * 1024;
    private static final int MAX_DEX_FILES = 32;
    private static final Pattern DEX_NAME = Pattern.compile("classes(?:[2-9]|[1-9][0-9]+)?\\.dex");

    ApkInspectionResult inspect(Path apk, Path profiles) throws IOException {
        if (!Files.isRegularFile(apk)) {
            throw new IllegalArgumentException("APK input is not a regular file");
        }
        ApkInspectionResult result = new ApkInspectionResult();
        result.apkSize = Files.size(apk);
        result.apkSha256 = Hashing.sha256(apk);
        DexInventory dex = new DexInventory();
        Set<String> abis = new LinkedHashSet<>();
        byte[] manifestBytes = null;
        DexParser dexParser = new DexParser();

        try (ZipFile zip = new ZipFile(apk.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                result.entryCount++;
                String name = entry.getName();
                if ("AndroidManifest.xml".equals(name)) {
                    manifestBytes = readLimited(zip, entry, MAX_MANIFEST_SIZE);
                } else if (DEX_NAME.matcher(name).matches()) {
                    if (dex.dexFiles >= MAX_DEX_FILES) {
                        throw new IllegalArgumentException("APK contains more than " + MAX_DEX_FILES + " DEX files");
                    }
                    dexParser.add(readLimited(zip, entry, MAX_DEX_SIZE), dex);
                } else if (name.startsWith("lib/") && name.endsWith(".so")) {
                    String remainder = name.substring(4);
                    int separator = remainder.indexOf('/');
                    if (separator > 0) {
                        abis.add(remainder.substring(0, separator));
                        result.nativeLibraryCount++;
                    }
                }
            }
        }
        if (manifestBytes == null) {
            throw new IllegalArgumentException("APK has no AndroidManifest.xml");
        }
        dex.finish();
        result.dex = dex;
        result.manifest = new BinaryAndroidManifestParser().parse(manifestBytes);
        result.signing = new ApkSigningInspector().inspect(apk);
        result.nativeAbis.addAll(abis);
        Collections.sort(result.nativeAbis);
        result.profile = new MappingProfileRepository().find(profiles, result.manifest, dex, result.apkSha256);
        return result;
    }

    private static byte[] readLimited(ZipFile zip, ZipEntry entry, int maximum) throws IOException {
        long declaredSize = entry.getSize();
        if (declaredSize > maximum) {
            throw new IllegalArgumentException(entry.getName() + " exceeds the inspection size limit");
        }
        try (InputStream input = zip.getInputStream(entry);
             ByteArrayOutputStream output = new ByteArrayOutputStream(
                     declaredSize > 0 ? (int) Math.min(declaredSize, maximum) : 8192)) {
            byte[] buffer = new byte[32 * 1024];
            int total = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > maximum) {
                    throw new IllegalArgumentException(entry.getName() + " exceeds the inspection size limit");
                }
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }
}
