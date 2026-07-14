package io.github.endx.rustedfabric.android.patcher;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Rebuilds an unsigned, aligned, side-by-side APK entirely from user-local inputs. */
public final class LocalApkPatcher {
    public static final long MAX_SOURCE_APK_BYTES = 512L * 1024L * 1024L;
    public static final long MAX_BOOTSTRAP_DEX_BYTES = 16L * 1024L * 1024L;
    private static final Pattern DEX_ENTRY = Pattern.compile("classes([2-9][0-9]*)?\\.dex");

    public PatchReport patchUnsigned(PatchRequest request) throws PatchException {
        validateInputFile(request.getSourceApk(), MAX_SOURCE_APK_BYTES, "Source APK");
        validateInputFile(request.getBootstrapDex(), MAX_BOOTSTRAP_DEX_BYTES, "Bootstrap DEX");
        try {
            String sourceSha256 = PatcherSha256.digest(request.getSourceApk());
            if (!request.getProfile().getSourceSha256().equals(sourceSha256)) {
                throw new PatchException(PatchException.Reason.PROFILE_MISMATCH,
                        "Source APK does not match patch profile " + request.getProfile().getId());
            }
            byte[] bootstrapDex = Files.readAllBytes(request.getBootstrapDex());
            validateBootstrapDex(bootstrapDex, request);

            Path output = request.getOutputApk().toAbsolutePath().normalize();
            Path parent = output.getParent();
            if (parent == null) {
                throw new PatchException(PatchException.Reason.OUTPUT_FAILED,
                        "Output APK must have a parent directory");
            }
            Files.createDirectories(parent);
            Path temporary = parent.resolve("." + output.getFileName() + "."
                    + UUID.randomUUID() + ".tmp");
            String bootstrapEntry;
            try {
                bootstrapEntry = rebuild(request, bootstrapDex, temporary);
                atomicMove(temporary, output);
            } finally {
                Files.deleteIfExists(temporary);
            }
            String outputSha256 = PatcherSha256.digest(output);
            return new PatchReport(request.getProfile().getId(), sourceSha256, outputSha256,
                    request.getClonePackage(), bootstrapEntry, false, Arrays.asList(
                    "manifest-package-rewrite", "manifest-application-rewrite",
                    "provider-authority-rewrite", "loader-provider-query-declaration",
                    "equal-width-dex-string-rewrite",
                    "legacy-signature-removal", "bootstrap-secondary-dex-injection",
                    "stored-entry-alignment"));
        } catch (PatchException expected) {
            throw expected;
        } catch (IOException failure) {
            throw new PatchException(PatchException.Reason.OUTPUT_FAILED,
                    "Local APK patch failed", failure);
        }
    }

    private String rebuild(PatchRequest request, byte[] bootstrapDex, Path temporary)
            throws IOException, PatchException {
        try (ZipFile source = new ZipFile(request.getSourceApk().toFile())) {
            Map<String, ZipEntry> entries = collectEntries(source);
            ZipEntry manifestEntry = required(entries, "AndroidManifest.xml");
            ZipEntry primaryDexEntry = required(entries, "classes.dex");
            byte[] manifest = readBounded(source, manifestEntry, 16 * 1024 * 1024);
            byte[] primaryDex = readBounded(source, primaryDexEntry, 128 * 1024 * 1024);

            Map<String, String> manifestReplacements = new LinkedHashMap<>();
            manifestReplacements.put(request.getProfile().getSourcePackage(),
                    request.getClonePackage());
            manifestReplacements.put(request.getProfile().getSourceApplication(),
                    PatchProfile.PATCHED_APPLICATION);
            manifestReplacements.put(request.getProfile().getSourceProviderAuthority(),
                    request.cloneProviderAuthority());
            byte[] patchedManifest = BinaryXmlStringRewriter.replace(
                    manifest, manifestReplacements);
            patchedManifest = BinaryXmlStringRewriter.addProviderQueries(patchedManifest,
                    Arrays.asList("io.github.endx.rustedfabric.android.xposed.mods",
                            "io.github.endx.rustedfabric.android.xposed.debug.mods"));

            Map<String, String> dexReplacements = new LinkedHashMap<>();
            dexReplacements.put(request.getProfile().getSourcePackage(),
                    request.getClonePackage());
            dexReplacements.put(request.getProfile().getSourceProviderAuthority(),
                    request.cloneProviderAuthority());
            byte[] patchedDex = DexStringRewriter.replaceEqualWidth(primaryDex, dexReplacements);
            String bootstrapEntry = nextDexEntry(entries.keySet());

            try (OutputStream fileOutput = Files.newOutputStream(temporary,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                 AlignedZipWriter output = new AlignedZipWriter(fileOutput)) {
                Enumeration<? extends ZipEntry> enumeration = source.entries();
                while (enumeration.hasMoreElements()) {
                    ZipEntry entry = enumeration.nextElement();
                    if (entry.isDirectory() || signatureEntry(entry.getName())) continue;
                    if ("AndroidManifest.xml".equals(entry.getName())) {
                        output.writeBytes(entry.getName(), patchedManifest, entry.getMethod());
                    } else if ("classes.dex".equals(entry.getName())) {
                        output.writeBytes(entry.getName(), patchedDex, entry.getMethod());
                    } else {
                        try (InputStream input = source.getInputStream(entry)) {
                            output.writeExisting(entry, input);
                        }
                    }
                }
                output.writeBytes(bootstrapEntry, bootstrapDex, ZipEntry.STORED);
            }
            verifyUnsignedOutput(temporary, bootstrapEntry);
            return bootstrapEntry;
        } catch (java.util.zip.ZipException invalid) {
            throw new PatchException(PatchException.Reason.INVALID_APK,
                    "Source APK is not a readable ZIP archive", invalid);
        }
    }

    private static Map<String, ZipEntry> collectEntries(ZipFile zip) throws PatchException {
        Map<String, ZipEntry> entries = new LinkedHashMap<>();
        Enumeration<? extends ZipEntry> enumeration = zip.entries();
        while (enumeration.hasMoreElements()) {
            ZipEntry entry = enumeration.nextElement();
            String name = entry.getName();
            if (name.startsWith("/") || name.contains("\\") || hasUnsafeSegment(name)
                    || entries.put(name, entry) != null) {
                throw new PatchException(PatchException.Reason.INVALID_APK,
                        "Source APK has unsafe or duplicate entries");
            }
        }
        return entries;
    }

    private static boolean hasUnsafeSegment(String name) {
        for (String segment : name.split("/", -1)) {
            if (segment.isEmpty() || "..".equals(segment)) return true;
        }
        return false;
    }

    private static void validateBootstrapDex(byte[] dex, PatchRequest request)
            throws PatchException {
        Set<String> definitions;
        try {
            definitions = DexStringRewriter.definedClasses(dex);
        } catch (PatchException invalid) {
            throw new PatchException(PatchException.Reason.BOOTSTRAP_DEX_INVALID,
                    "Bootstrap DEX is invalid", invalid);
        }
        String entrypoint = "L" + PatchProfile.PATCHED_APPLICATION.replace('.', '/') + ";";
        if (!definitions.contains(entrypoint)) {
            throw new PatchException(PatchException.Reason.BOOTSTRAP_DEX_INVALID,
                    "Bootstrap DEX does not define PatchedApplication");
        }
        String forbiddenPrefix = "L" + request.getProfile().getSourcePackage()
                .replace('.', '/') + "/";
        for (String definition : definitions) {
            if (definition.startsWith(forbiddenPrefix)) {
                throw new PatchException(PatchException.Reason.BOOTSTRAP_DEX_INVALID,
                        "Bootstrap DEX contains a game class definition");
            }
        }
    }

    private static ZipEntry required(Map<String, ZipEntry> entries, String name)
            throws PatchException {
        ZipEntry entry = entries.get(name);
        if (entry == null || entry.isDirectory()) {
            throw new PatchException(PatchException.Reason.INVALID_APK,
                    "Source APK is missing " + name);
        }
        return entry;
    }

    private static String nextDexEntry(Set<String> names) throws PatchException {
        int maximum = 1;
        for (String name : names) {
            Matcher matcher = DEX_ENTRY.matcher(name);
            if (matcher.matches()) {
                int index = matcher.group(1) == null ? 1 : Integer.parseInt(matcher.group(1));
                maximum = Math.max(maximum, index);
            }
        }
        if (maximum >= 99) {
            throw new PatchException(PatchException.Reason.INVALID_APK,
                    "Source APK has too many DEX files");
        }
        return "classes" + (maximum + 1) + ".dex";
    }

    private static boolean signatureEntry(String name) {
        String upper = name.toUpperCase(Locale.ROOT);
        if (!upper.startsWith("META-INF/")) return false;
        String file = upper.substring("META-INF/".length());
        return "MANIFEST.MF".equals(file) || file.endsWith(".SF") || file.endsWith(".RSA")
                || file.endsWith(".DSA") || file.endsWith(".EC");
    }

    private static byte[] readBounded(ZipFile zip, ZipEntry entry, int limit)
            throws IOException, PatchException {
        if (entry.getSize() < 0 || entry.getSize() > limit) {
            throw new PatchException(PatchException.Reason.INVALID_APK,
                    "APK entry exceeds patcher limits: " + entry.getName());
        }
        try (InputStream input = zip.getInputStream(entry);
             ByteArrayOutputStream output = new ByteArrayOutputStream(
                     (int) Math.min(entry.getSize(), 64 * 1024))) {
            byte[] buffer = new byte[32 * 1024];
            int total = 0;
            for (int count = input.read(buffer); count >= 0; count = input.read(buffer)) {
                if (count == 0) continue;
                total += count;
                if (total > limit) {
                    throw new PatchException(PatchException.Reason.INVALID_APK,
                            "APK entry expands beyond patcher limits: " + entry.getName());
                }
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        }
    }

    private static void verifyUnsignedOutput(Path output, String bootstrapEntry)
            throws IOException, PatchException {
        try (ZipFile zip = new ZipFile(output.toFile())) {
            if (zip.getEntry("AndroidManifest.xml") == null || zip.getEntry("classes.dex") == null
                    || zip.getEntry(bootstrapEntry) == null) {
                throw new PatchException(PatchException.Reason.OUTPUT_FAILED,
                        "Patched APK is missing required entries");
            }
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                if (signatureEntry(entries.nextElement().getName())) {
                    throw new PatchException(PatchException.Reason.OUTPUT_FAILED,
                            "Patched APK retains a legacy source signature");
                }
            }
        }
    }

    private static void validateInputFile(Path path, long limit, String label)
            throws PatchException {
        try {
            if (!Files.isRegularFile(path)) {
                throw new PatchException(PatchException.Reason.INPUT_MISSING,
                        label + " is missing");
            }
            long size = Files.size(path);
            if (size <= 0 || size > limit) {
                throw new PatchException(PatchException.Reason.INPUT_TOO_LARGE,
                        label + " exceeds supported limits");
            }
        } catch (IOException failure) {
            throw new PatchException(PatchException.Reason.INPUT_MISSING,
                    label + " cannot be inspected", failure);
        }
    }

    private static void atomicMove(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException unsupported) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
