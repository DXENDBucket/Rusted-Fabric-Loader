package io.github.endx.rustedfabric.android.inspector;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class InspectorContractVerification {
    private static final Map<String, String> ANCHORS = new LinkedHashMap<>();

    static {
        ANCHORS.put("gameEngine", "Lcom/corrodinggames/rts/gameFramework/l;");
        ANCHORS.put("frameworkGameObject", "Lcom/corrodinggames/rts/gameFramework/w;");
        ANCHORS.put("unit", "Lcom/corrodinggames/rts/game/units/am;");
        ANCHORS.put("orderableUnit", "Lcom/corrodinggames/rts/game/units/y;");
        ANCHORS.put("customUnit", "Lcom/corrodinggames/rts/game/units/custom/j;");
        ANCHORS.put("customUnitMetadata", "Lcom/corrodinggames/rts/game/units/custom/l;");
        ANCHORS.put("factoryQueueManager", "Lcom/corrodinggames/rts/game/units/d/k;");
        ANCHORS.put("effectManager", "Lcom/corrodinggames/rts/gameFramework/d/c;");
        ANCHORS.put("pathfindingEngine", "Lcom/corrodinggames/rts/gameFramework/k/l;");
        ANCHORS.put("settingsEngine", "Lcom/corrodinggames/rts/gameFramework/SettingsEngine;");
    }

    private InspectorContractVerification() {
    }

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("apk-inspector-contract-");
        try {
            Path apk = directory.resolve("private-user-copy.apk");
            createApk(apk);
            Path profiles = directory.resolve("profiles");
            createProfile(profiles);

            ApkInspector inspector = new ApkInspector();
            String first = inspector.inspect(apk, profiles).toJson();
            String second = inspector.inspect(apk, profiles).toJson();
            require(first.equals(second), "Reports must be byte-for-byte deterministic");
            require(first.contains("\"level\": \"STRUCTURAL\""), "Synthetic APK should match structurally");
            require(first.contains("\"status\": \"PENDING_MAPPING\""), "Missing mapping must remain pending");
            require(first.contains("\"matchPolicy\": \"structural\""),
                    "Legacy/synthetic profiles should default to structural matching");
            require(first.contains("\"launcherActivity\": \"com.example.game.MainActivity\""),
                    "Launcher activity was not parsed from binary XML");
            require(first.contains("\"classDefinitions\": 10"), "DEX class definitions were not counted");
            require(!first.contains(directory.toString()), "Report leaked a local filesystem path");
            for (String descriptor : ANCHORS.values()) {
                require(!first.contains(descriptor), "Report leaked a DEX class descriptor");
            }
            require(first.contains("\"reportContainsGamePayload\": false"), "Privacy contract is missing");

            Path exactProfiles = directory.resolve("exact-profiles");
            String apkSha256 = Hashing.sha256(apk);
            createExactProfile(exactProfiles, apkSha256);
            String exact = inspector.inspect(apk, exactProfiles).toJson();
            require(exact.contains("\"level\": \"VERIFIED\""),
                    "An exact APK and mapping checksum should be verified");
            require(exact.contains("\"status\": \"READY\""), "Exact mapping should be ready");
            require(exact.contains("\"matchPolicy\": \"exact\""), "Exact policy was not reported");

            Path exactMapping = exactProfiles.resolve("synthetic/mappings.tiny");
            Files.write(exactMapping, "corrupt".getBytes(StandardCharsets.UTF_8));
            boolean checksumRejected = false;
            try {
                inspector.inspect(apk, exactProfiles);
            } catch (IllegalArgumentException expected) {
                checksumRejected = expected.getMessage().contains("checksum mismatch");
            }
            require(checksumRejected, "A corrupted mapping file must be rejected");

            Path wrongHashProfiles = directory.resolve("wrong-hash-profiles");
            createExactProfile(wrongHashProfiles, repeat('0', 64));
            String wrongHash = inspector.inspect(apk, wrongHashProfiles).toJson();
            require(wrongHash.contains("\"level\": \"UNSUPPORTED\""),
                    "An exact profile must reject a modified APK hash");
            System.out.println("APK inspector parser, determinism, profile, and privacy contracts passed");
        } finally {
            deleteTree(directory);
        }
    }

    private static void createApk(Path apk) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(apk))) {
            add(zip, "AndroidManifest.xml", binaryManifest());
            add(zip, "classes.dex", syntheticDex(new ArrayList<>(ANCHORS.values())));
            add(zip, "assets/marker.txt", "synthetic-only".getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void createProfile(Path profiles) throws IOException {
        Path profile = profiles.resolve("synthetic/profile.properties");
        Files.createDirectories(profile.getParent());
        StringBuilder text = new StringBuilder();
        text.append("id=synthetic-pending\n")
                .append("packageName=com.example.game\n")
                .append("versionCode=42\n")
                .append("mappingFile=mappings.tiny\n");
        for (Map.Entry<String, String> anchor : ANCHORS.entrySet()) {
            text.append("anchor.").append(anchor.getKey()).append('=').append(anchor.getValue()).append('\n');
        }
        Files.write(profile, text.toString().getBytes(StandardCharsets.ISO_8859_1));
    }

    private static void createExactProfile(Path profiles, String apkSha256) throws IOException {
        Path directory = profiles.resolve("synthetic");
        Files.createDirectories(directory);
        Path mapping = directory.resolve("mappings.tiny");
        Files.write(mapping, "tiny\t2\t0\tofficial\tintermediary\tnamed\n"
                .getBytes(StandardCharsets.UTF_8));
        StringBuilder text = new StringBuilder();
        text.append("id=synthetic-exact\n")
                .append("matchPolicy=exact\n")
                .append("packageName=com.example.game\n")
                .append("versionName=1.0-test\n")
                .append("versionCode=42\n")
                .append("apkSha256=").append(apkSha256).append('\n')
                .append("mappingFile=mappings.tiny\n")
                .append("mappingFileSha256=").append(Hashing.sha256(mapping)).append('\n');
        for (Map.Entry<String, String> anchor : ANCHORS.entrySet()) {
            text.append("anchor.").append(anchor.getKey()).append('=').append(anchor.getValue()).append('\n');
        }
        Files.write(directory.resolve("profile.properties"),
                text.toString().getBytes(StandardCharsets.ISO_8859_1));
    }

    private static String repeat(char value, int count) {
        char[] values = new char[count];
        Arrays.fill(values, value);
        return new String(values);
    }

    private static byte[] binaryManifest() throws IOException {
        List<String> strings = Arrays.asList(
                "manifest", "package", "versionName", "versionCode", "compileSdkVersion",
                "com.example.game", "1.0-test", "uses-sdk", "minSdkVersion", "targetSdkVersion",
                "application", "name", ".TestApplication", "activity", ".MainActivity",
                "intent-filter", "action", "android.intent.action.MAIN", "category",
                "android.intent.category.LAUNCHER");
        Map<String, Integer> indexes = new LinkedHashMap<>();
        for (int i = 0; i < strings.size(); i++) {
            indexes.put(strings.get(i), i);
        }
        List<byte[]> chunks = new ArrayList<>();
        chunks.add(stringPool(strings));
        chunks.add(start(indexes, "manifest",
                text(indexes, "package", "com.example.game"), text(indexes, "versionName", "1.0-test"),
                number(indexes, "versionCode", 42), number(indexes, "compileSdkVersion", 34)));
        chunks.add(start(indexes, "uses-sdk", number(indexes, "minSdkVersion", 21),
                number(indexes, "targetSdkVersion", 34)));
        chunks.add(end(indexes, "uses-sdk"));
        chunks.add(start(indexes, "application", text(indexes, "name", ".TestApplication")));
        chunks.add(start(indexes, "activity", text(indexes, "name", ".MainActivity")));
        chunks.add(start(indexes, "intent-filter"));
        chunks.add(start(indexes, "action", text(indexes, "name", "android.intent.action.MAIN")));
        chunks.add(end(indexes, "action"));
        chunks.add(start(indexes, "category", text(indexes, "name", "android.intent.category.LAUNCHER")));
        chunks.add(end(indexes, "category"));
        chunks.add(end(indexes, "intent-filter"));
        chunks.add(end(indexes, "activity"));
        chunks.add(end(indexes, "application"));
        chunks.add(end(indexes, "manifest"));
        int total = 8;
        for (byte[] chunk : chunks) total += chunk.length;
        ByteArrayOutputStream output = new ByteArrayOutputStream(total);
        u16(output, 0x0003);
        u16(output, 8);
        u32(output, total);
        for (byte[] chunk : chunks) output.write(chunk);
        return output.toByteArray();
    }

    private static byte[] stringPool(List<String> strings) throws IOException {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();
        for (String string : strings) {
            offsets.add(data.size());
            byte[] encoded = string.getBytes(StandardCharsets.UTF_8);
            length8(data, string.length());
            length8(data, encoded.length);
            data.write(encoded);
            data.write(0);
        }
        while ((data.size() & 3) != 0) data.write(0);
        int headerSize = 28;
        int stringsStart = headerSize + offsets.size() * 4;
        int size = stringsStart + data.size();
        ByteArrayOutputStream output = new ByteArrayOutputStream(size);
        u16(output, 0x0001);
        u16(output, headerSize);
        u32(output, size);
        u32(output, strings.size());
        u32(output, 0);
        u32(output, 0x100);
        u32(output, stringsStart);
        u32(output, 0);
        for (Integer offset : offsets) u32(output, offset);
        output.write(data.toByteArray());
        return output.toByteArray();
    }

    private static byte[] start(Map<String, Integer> indexes, String element, Attr... attributes)
            throws IOException {
        int size = 36 + attributes.length * 20;
        ByteArrayOutputStream output = new ByteArrayOutputStream(size);
        u16(output, 0x0102); u16(output, 16); u32(output, size);
        u32(output, 1); u32(output, 0xffffffffL);
        u32(output, 0xffffffffL); u32(output, indexes.get(element));
        u16(output, 20); u16(output, 20); u16(output, attributes.length);
        u16(output, 0); u16(output, 0); u16(output, 0);
        for (Attr attribute : attributes) {
            u32(output, 0xffffffffL); u32(output, indexes.get(attribute.name));
            u32(output, attribute.rawString == null ? 0xffffffffL : indexes.get(attribute.rawString));
            u16(output, 8); output.write(0); output.write(attribute.type);
            u32(output, attribute.rawString == null ? attribute.number : indexes.get(attribute.rawString));
        }
        return output.toByteArray();
    }

    private static byte[] end(Map<String, Integer> indexes, String element) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(24);
        u16(output, 0x0103); u16(output, 16); u32(output, 24);
        u32(output, 1); u32(output, 0xffffffffL);
        u32(output, 0xffffffffL); u32(output, indexes.get(element));
        return output.toByteArray();
    }

    private static Attr text(Map<String, Integer> ignored, String name, String value) {
        return new Attr(name, value, 0, 0x03);
    }

    private static Attr number(Map<String, Integer> ignored, String name, long value) {
        return new Attr(name, null, value, 0x10);
    }

    private static byte[] syntheticDex(List<String> descriptors) throws IOException {
        int stringIds = 112;
        int typeIds = stringIds + descriptors.size() * 4;
        int classDefs = typeIds + descriptors.size() * 4;
        int dataOffset = classDefs + descriptors.size() * 32;
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        List<Integer> dataOffsets = new ArrayList<>();
        for (String descriptor : descriptors) {
            dataOffsets.add(dataOffset + data.size());
            lengthUleb(data, descriptor.length());
            data.write(descriptor.getBytes(StandardCharsets.US_ASCII));
            data.write(0);
        }
        int fileSize = dataOffset + data.size();
        byte[] dex = new byte[fileSize];
        byte[] magic = new byte[] {'d', 'e', 'x', '\n', '0', '3', '5', 0};
        System.arraycopy(magic, 0, dex, 0, magic.length);
        put32(dex, 32, fileSize); put32(dex, 36, 112); put32(dex, 40, 0x12345678L);
        put32(dex, 56, descriptors.size()); put32(dex, 60, stringIds);
        put32(dex, 64, descriptors.size()); put32(dex, 68, typeIds);
        put32(dex, 96, descriptors.size()); put32(dex, 100, classDefs);
        put32(dex, 104, data.size()); put32(dex, 108, dataOffset);
        for (int i = 0; i < descriptors.size(); i++) {
            put32(dex, stringIds + i * 4, dataOffsets.get(i));
            put32(dex, typeIds + i * 4, i);
            put32(dex, classDefs + i * 32, i);
            put32(dex, classDefs + i * 32 + 8, 0xffffffffL);
        }
        System.arraycopy(data.toByteArray(), 0, dex, dataOffset, data.size());
        return dex;
    }

    private static void add(ZipOutputStream zip, String name, byte[] bytes) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        entry.setTime(0);
        zip.putNextEntry(entry);
        zip.write(bytes);
        zip.closeEntry();
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (java.util.stream.Stream<Path> paths = Files.walk(root)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try { Files.delete(path); } catch (IOException error) { throw new RuntimeException(error); }
            });
        } catch (RuntimeException error) {
            if (error.getCause() instanceof IOException) throw (IOException) error.getCause();
            throw error;
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void length8(ByteArrayOutputStream output, int value) {
        if (value < 0x80) output.write(value);
        else { output.write(0x80 | (value >> 8)); output.write(value & 0xff); }
    }

    private static void lengthUleb(ByteArrayOutputStream output, int value) {
        do {
            int current = value & 0x7f;
            value >>>= 7;
            output.write(value == 0 ? current : current | 0x80);
        } while (value != 0);
    }

    private static void u16(ByteArrayOutputStream output, int value) {
        output.write(value & 0xff); output.write((value >>> 8) & 0xff);
    }

    private static void u32(ByteArrayOutputStream output, long value) {
        output.write((int) value & 0xff); output.write((int) (value >>> 8) & 0xff);
        output.write((int) (value >>> 16) & 0xff); output.write((int) (value >>> 24) & 0xff);
    }

    private static void put32(byte[] output, int offset, long value) {
        output[offset] = (byte) value; output[offset + 1] = (byte) (value >>> 8);
        output[offset + 2] = (byte) (value >>> 16); output[offset + 3] = (byte) (value >>> 24);
    }

    private static final class Attr {
        final String name;
        final String rawString;
        final long number;
        final int type;

        Attr(String name, String rawString, long number, int type) {
            this.name = name; this.rawString = rawString; this.number = number; this.type = type;
        }
    }
}
