package io.github.endx.rustedfabric.android.patcher;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.builder.MutableMethodImplementation;
import org.jf.dexlib2.builder.instruction.BuilderInstruction10x;
import org.jf.dexlib2.builder.instruction.BuilderInstruction11n;
import org.jf.dexlib2.builder.instruction.BuilderInstruction11x;
import org.jf.dexlib2.builder.instruction.BuilderInstruction21c;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.ReferenceInstruction;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.immutable.ImmutableClassDef;
import org.jf.dexlib2.immutable.ImmutableDexFile;
import org.jf.dexlib2.immutable.ImmutableMethod;
import org.jf.dexlib2.immutable.ImmutableMethodParameter;
import org.jf.dexlib2.immutable.reference.ImmutableStringReference;
import org.jf.dexlib2.writer.io.MemoryDataStore;
import org.jf.dexlib2.writer.pool.DexPool;

public final class LocalPatcherContractVerification {
    private static final String SOURCE_PACKAGE = "com.corrodinggames.rts";
    private static final String CLONE_PACKAGE = "io.github.endx.rwpatch";
    private static final String SOURCE_APPLICATION =
            "com.corrodinggames.rts.appFramework.RWApplication";
    private static final String SOURCE_AUTHORITY = SOURCE_PACKAGE + ".fileProvider";

    private LocalPatcherContractVerification() {
    }

    public static void main(String[] args) throws Exception {
        Path temporary = Files.createTempDirectory("rusted-fabric-local-patcher-");
        try {
            verifyBinaryXmlReplacement();
            verifyDexReplacement();
            verifyLifecycleWeave();
            verifyApkRebuild(temporary);
            verifyProfileMismatch(temporary);
            System.out.println("Local APK patcher contracts passed");
        } finally {
            deleteTree(temporary);
        }
    }

    private static void verifyLifecycleWeave() throws Exception {
        byte[] woven = DexLifecycleWeaver.weaveEngineInitialization(weavableDex());
        Method target = targetMethod(woven);
        java.util.List<Instruction> instructions = new java.util.ArrayList<>();
        target.getImplementation().getInstructions().forEach(instructions::add);
        require(callbackName(instructions.get(0)).equals(DexLifecycleWeaver.BEFORE_METHOD),
                "before engine callback was not inserted at method entry");
        require(instructions.get(instructions.size() - 1).getOpcode() == Opcode.RETURN_VOID
                        && callbackName(instructions.get(instructions.size() - 2))
                        .equals(DexLifecycleWeaver.AFTER_METHOD),
                "after engine callback was not inserted before normal return");
        verifyNetworkCallbacks(woven);
        try {
            DexLifecycleWeaver.weaveEngineInitialization(woven);
            throw new AssertionError("already woven DEX was accepted");
        } catch (PatchException expected) {
            require(expected.getReason() == PatchException.Reason.DEX_WEAVE_FAILED,
                    "wrong repeated weave reason: " + expected.getReason());
        }
    }

    private static void verifyNetworkCallbacks(byte[] woven) {
        DexBackedDexFile dex = new DexBackedDexFile(null, woven);
        int found = 0;
        for (ClassDef classDef : dex.getClasses()) {
            if (!DexLifecycleWeaver.NETWORK_CLASS.equals(classDef.getType())) continue;
            for (Method method : classDef.getMethods()) {
                String expected = null;
                if (DexLifecycleWeaver.REGISTER_METHOD.equals(method.getName())) {
                    expected = DexLifecycleWeaver.AFTER_REGISTER_CALLBACK;
                } else if (DexLifecycleWeaver.SERVER_INFO_METHOD.equals(method.getName())) {
                    expected = DexLifecycleWeaver.AFTER_SERVER_INFO_CALLBACK;
                } else if (DexLifecycleWeaver.SYSTEM_PACKET_METHOD.equals(method.getName())) {
                    expected = "Z".equals(method.getReturnType())
                            ? DexLifecycleWeaver.START_GAME_CALLBACK
                            : DexLifecycleWeaver.PACKET_TYPE.equals(
                            method.getParameterTypes().get(0).toString())
                            ? DexLifecycleWeaver.SYSTEM_PACKET_CALLBACK
                            : DexLifecycleWeaver.NETWORK_RESET_CALLBACK;
                }
                if (expected == null) continue;
                found++;
                boolean callback = false;
                for (Instruction instruction : method.getImplementation().getInstructions()) {
                    callback |= expected.equals(callbackName(instruction));
                }
                require(callback, "missing RFH1 callback: " + expected);
            }
        }
        require(found == 5, "expected five woven RFH1/session methods");
    }

    private static void verifyBinaryXmlReplacement() throws Exception {
        byte[] manifest = manifest();
        Map<String, String> replacements = manifestReplacements();
        byte[] patched = BinaryXmlStringRewriter.replace(manifest, replacements);
        Map<String, String> reverse = new LinkedHashMap<>();
        replacements.forEach((before, after) -> reverse.put(after, before));
        byte[] restored = BinaryXmlStringRewriter.replace(patched, reverse);
        require(Arrays.equals(manifest, restored),
                "binary XML string pool replacement is not reversible");
    }

    private static void verifyDexReplacement() throws Exception {
        byte[] original = dex(SOURCE_PACKAGE, SOURCE_AUTHORITY, "unrelated.value");
        Map<String, String> replacements = dexReplacements();
        byte[] patched = DexStringRewriter.replaceEqualWidth(original, replacements);
        require(!Arrays.equals(original, patched), "DEX replacement made no change");
        Map<String, String> reverse = new LinkedHashMap<>();
        replacements.forEach((before, after) -> reverse.put(after, before));
        byte[] restored = DexStringRewriter.replaceEqualWidth(patched, reverse);
        require(strings(restored).contains(SOURCE_PACKAGE)
                        && strings(restored).contains(SOURCE_AUTHORITY),
                "DEX replacement did not preserve the string table");
    }

    private static void verifyApkRebuild(Path temporary) throws Exception {
        Path source = temporary.resolve("source.apk");
        createApk(source);
        Path bootstrap = temporary.resolve("bootstrap.dex");
        Files.write(bootstrap, bootstrapDex());
        Path output = temporary.resolve("patched-unsigned.apk");
        PatchProfile profile = profileFor(source);
        PatchReport report = new LocalApkPatcher().patchUnsigned(new PatchRequest(
                source, bootstrap, output, profile, CLONE_PACKAGE));
        require(Files.isRegularFile(output), "patched APK was not created");
        require(!report.isSigned(), "unsigned milestone must not claim a signature");
        require(report.toJson().contains("bootstrap-secondary-dex-injection"),
                "code-free patch report is incomplete");
        require(report.toJson().contains("engine-init-lifecycle-weave"),
                "lifecycle weave is missing from the patch report");
        require(report.toJson().contains("rfh1-network-handshake-weave"),
                "RFH1 network weave is missing from the patch report");
        require(!report.toJson().contains(temporary.toString()),
                "patch report leaked a local path");

        try (ZipFile zip = new ZipFile(output.toFile())) {
            require(zip.getEntry("classes2.dex") != null, "bootstrap DEX was not injected");
            require(zip.getEntry("META-INF/CERT.RSA") == null
                            && zip.getEntry("META-INF/CERT.SF") == null
                            && zip.getEntry("META-INF/MANIFEST.MF") == null,
                    "source signatures were retained");
            byte[] patchedManifest = zip.getInputStream(zip.getEntry("AndroidManifest.xml"))
                    .readAllBytes();
            String manifestText = new String(patchedManifest, StandardCharsets.ISO_8859_1);
            require(manifestText.contains("queries")
                            && manifestText.contains(
                            "io.github.endx.rustedfabric.android.xposed.mods"),
                    "Loader provider visibility query was not inserted");
            Map<String, String> reverseManifest = new LinkedHashMap<>();
            manifestReplacements().forEach((before, after) -> reverseManifest.put(after, before));
            BinaryXmlStringRewriter.replace(patchedManifest, reverseManifest);
            byte[] patchedDex = zip.getInputStream(zip.getEntry("classes.dex")).readAllBytes();
            require(strings(patchedDex).contains(CLONE_PACKAGE)
                            && strings(patchedDex).contains(CLONE_PACKAGE + ".fileProvider"),
                    "primary DEX package-sensitive strings were not rewritten");
        }
        verifyStoredAlignment(Files.readAllBytes(output));
    }

    private static void verifyProfileMismatch(Path temporary) throws Exception {
        Path source = temporary.resolve("mismatch-source.apk");
        createApk(source);
        Path bootstrap = temporary.resolve("mismatch-bootstrap.dex");
        Files.write(bootstrap, dex("bootstrap"));
        PatchProfile wrong = new PatchProfile("wrong", repeat('0', 64), SOURCE_PACKAGE,
                SOURCE_APPLICATION, SOURCE_AUTHORITY);
        try {
            new LocalApkPatcher().patchUnsigned(new PatchRequest(source, bootstrap,
                    temporary.resolve("must-not-exist.apk"), wrong, CLONE_PACKAGE));
            throw new AssertionError("mismatched APK profile was accepted");
        } catch (PatchException expected) {
            require(expected.getReason() == PatchException.Reason.PROFILE_MISMATCH,
                    "wrong mismatch reason: " + expected.getReason());
        }
    }

    private static PatchProfile profileFor(Path source) throws IOException {
        return new PatchProfile("synthetic", PatcherSha256.digest(source), SOURCE_PACKAGE,
                SOURCE_APPLICATION, SOURCE_AUTHORITY);
    }

    private static void createApk(Path target) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(target))) {
            putStored(zip, "AndroidManifest.xml", manifest());
            putStored(zip, "classes.dex", weavableDex());
            putStored(zip, "resources.arsc", new byte[]{1, 2, 3, 4, 5});
            putStored(zip, "assets/example.txt", "asset".getBytes(StandardCharsets.UTF_8));
            putStored(zip, "META-INF/MANIFEST.MF", new byte[]{1});
            putStored(zip, "META-INF/CERT.SF", new byte[]{2});
            putStored(zip, "META-INF/CERT.RSA", new byte[]{3});
        }
    }

    private static byte[] weavableDex() throws IOException {
        MutableMethodImplementation body = new MutableMethodImplementation(2);
        body.addInstruction(new BuilderInstruction21c(Opcode.CONST_STRING, 0,
                new ImmutableStringReference(SOURCE_PACKAGE)));
        body.addInstruction(new BuilderInstruction21c(Opcode.CONST_STRING, 0,
                new ImmutableStringReference(SOURCE_AUTHORITY)));
        body.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));
        ImmutableMethod method = new ImmutableMethod(DexLifecycleWeaver.TARGET_CLASS,
                DexLifecycleWeaver.TARGET_METHOD,
                Collections.singletonList(new ImmutableMethodParameter(
                        DexLifecycleWeaver.TARGET_PARAMETER, Collections.emptySet(), null)),
                "V", AccessFlags.PUBLIC.getValue() | AccessFlags.FINAL.getValue(),
                Collections.emptySet(), Collections.emptySet(), body);
        ImmutableClassDef classDef = new ImmutableClassDef(DexLifecycleWeaver.TARGET_CLASS,
                AccessFlags.PUBLIC.getValue(), "Ljava/lang/Object;", Collections.emptyList(),
                "SyntheticEngine.java", Collections.emptySet(), Collections.emptyList(),
                Collections.singletonList(method));
        MemoryDataStore output = new MemoryDataStore();
        ImmutableClassDef network = networkClass();
        DexPool.writeTo(output, new ImmutableDexFile(Opcodes.getDefault(),
                Arrays.asList(classDef, network)));
        return output.getData();
    }

    private static ImmutableClassDef networkClass() {
        java.util.List<ImmutableMethod> methods = new java.util.ArrayList<>();
        methods.add(networkMethod(DexLifecycleWeaver.REGISTER_METHOD,
                DexLifecycleWeaver.CONNECTION_TYPE, AccessFlags.PRIVATE.getValue()));
        methods.add(networkMethod(DexLifecycleWeaver.SERVER_INFO_METHOD,
                DexLifecycleWeaver.CONNECTION_TYPE, AccessFlags.PRIVATE.getValue()));
        methods.add(networkMethod(DexLifecycleWeaver.SYSTEM_PACKET_METHOD,
                DexLifecycleWeaver.PACKET_TYPE,
                AccessFlags.PUBLIC.getValue() | AccessFlags.FINAL.getValue()));
        methods.add(networkMethod(DexLifecycleWeaver.SYSTEM_PACKET_METHOD,
                "Z", AccessFlags.PUBLIC.getValue() | AccessFlags.FINAL.getValue()));
        methods.add(startMethod());
        return new ImmutableClassDef(DexLifecycleWeaver.NETWORK_CLASS,
                AccessFlags.PUBLIC.getValue(), "Ljava/lang/Object;", Collections.emptyList(),
                "SyntheticNetwork.java", Collections.emptySet(), Collections.emptyList(), methods);
    }

    private static ImmutableMethod networkMethod(String name, String parameter, int flags) {
        MutableMethodImplementation body = new MutableMethodImplementation(2);
        body.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));
        return new ImmutableMethod(DexLifecycleWeaver.NETWORK_CLASS, name,
                Collections.singletonList(new ImmutableMethodParameter(
                        parameter, Collections.emptySet(), null)),
                "V", flags, Collections.emptySet(), Collections.emptySet(), body);
    }

    private static ImmutableMethod startMethod() {
        MutableMethodImplementation body = new MutableMethodImplementation(4);
        body.addInstruction(new BuilderInstruction11n(Opcode.CONST_4, 0, 1));
        body.addInstruction(new BuilderInstruction11x(Opcode.RETURN, 0));
        return new ImmutableMethod(DexLifecycleWeaver.NETWORK_CLASS,
                DexLifecycleWeaver.SYSTEM_PACKET_METHOD,
                Arrays.asList(
                        new ImmutableMethodParameter(DexLifecycleWeaver.CONNECTION_TYPE,
                                Collections.emptySet(), null),
                        new ImmutableMethodParameter("Z", Collections.emptySet(), null)),
                "Z", AccessFlags.PUBLIC.getValue() | AccessFlags.FINAL.getValue(),
                Collections.emptySet(), Collections.emptySet(), body);
    }

    private static Method targetMethod(byte[] dexBytes) {
        DexBackedDexFile dex = new DexBackedDexFile(null, dexBytes);
        for (ClassDef classDef : dex.getClasses()) {
            for (Method method : classDef.getMethods()) {
                if (DexLifecycleWeaver.TARGET_CLASS.equals(method.getDefiningClass())
                        && DexLifecycleWeaver.TARGET_METHOD.equals(method.getName())) return method;
            }
        }
        throw new AssertionError("woven target method is missing");
    }

    private static String callbackName(Instruction instruction) {
        if (!(instruction instanceof ReferenceInstruction)) return "";
        Object reference = ((ReferenceInstruction) instruction).getReference();
        if (!(reference instanceof MethodReference)) return "";
        MethodReference method = (MethodReference) reference;
        return DexLifecycleWeaver.BRIDGE_CLASS.equals(method.getDefiningClass())
                ? method.getName() : "";
    }

    private static void putStored(ZipOutputStream zip, String name, byte[] data) throws IOException {
        CRC32 crc = new CRC32();
        crc.update(data);
        ZipEntry entry = new ZipEntry(name);
        entry.setMethod(ZipEntry.STORED);
        entry.setSize(data.length);
        entry.setCompressedSize(data.length);
        entry.setCrc(crc.getValue());
        entry.setTime(315532800000L);
        zip.putNextEntry(entry);
        zip.write(data);
        zip.closeEntry();
    }

    private static Map<String, String> manifestReplacements() {
        Map<String, String> replacements = new LinkedHashMap<>();
        replacements.put(SOURCE_PACKAGE, CLONE_PACKAGE);
        replacements.put(SOURCE_APPLICATION, PatchProfile.PATCHED_APPLICATION);
        replacements.put(SOURCE_AUTHORITY, CLONE_PACKAGE + ".fileProvider");
        return replacements;
    }

    private static Map<String, String> dexReplacements() {
        Map<String, String> replacements = new LinkedHashMap<>();
        replacements.put(SOURCE_PACKAGE, CLONE_PACKAGE);
        replacements.put(SOURCE_AUTHORITY, CLONE_PACKAGE + ".fileProvider");
        return replacements;
    }

    private static byte[] manifest() {
        String[] values = {SOURCE_PACKAGE, SOURCE_APPLICATION, SOURCE_AUTHORITY, "manifest",
                "application", "provider", "http://schemas.android.com/apk/res/android",
                "authorities"};
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        int[] offsets = new int[values.length];
        for (int index = 0; index < values.length; index++) {
            offsets[index] = data.size();
            byte[] utf8 = values[index].getBytes(StandardCharsets.UTF_8);
            writeLength8(data, values[index].length());
            writeLength8(data, utf8.length);
            data.write(utf8, 0, utf8.length);
            data.write(0);
        }
        while ((data.size() & 3) != 0) data.write(0);
        int poolSize = 28 + values.length * 4 + data.size();
        int resourceMapSize = 8 + values.length * 4;
        int nodesSize = 36 + 36 + 36 + 24 + 24 + 24;
        byte[] xml = new byte[8 + poolSize + resourceMapSize + nodesSize];
        putU16(xml, 0, 0x0003);
        putU16(xml, 2, 8);
        putI32(xml, 4, xml.length);
        int pool = 8;
        putU16(xml, pool, 0x0001);
        putU16(xml, pool + 2, 28);
        putI32(xml, pool + 4, poolSize);
        putI32(xml, pool + 8, values.length);
        putI32(xml, pool + 12, 0);
        putI32(xml, pool + 16, 0x100);
        putI32(xml, pool + 20, 28 + values.length * 4);
        putI32(xml, pool + 24, 0);
        for (int index = 0; index < offsets.length; index++) {
            putI32(xml, pool + 28 + index * 4, offsets[index]);
        }
        byte[] bytes = data.toByteArray();
        System.arraycopy(bytes, 0, xml, pool + 28 + values.length * 4, bytes.length);
        int resourceMap = pool + poolSize;
        putU16(xml, resourceMap, 0x0180);
        putU16(xml, resourceMap + 2, 8);
        putI32(xml, resourceMap + 4, resourceMapSize);
        putI32(xml, resourceMap + 8 + 7 * 4, 0x01010018);
        int cursor = resourceMap + resourceMapSize;
        cursor = putStartElement(xml, cursor, 3);
        cursor = putStartElement(xml, cursor, 4);
        cursor = putStartElement(xml, cursor, 5);
        cursor = putEndElement(xml, cursor, 5);
        cursor = putEndElement(xml, cursor, 4);
        putEndElement(xml, cursor, 3);
        return xml;
    }

    private static int putStartElement(byte[] xml, int offset, int nameIndex) {
        putU16(xml, offset, 0x0102);
        putU16(xml, offset + 2, 16);
        putI32(xml, offset + 4, 36);
        putI32(xml, offset + 12, -1);
        putI32(xml, offset + 16, -1);
        putI32(xml, offset + 20, nameIndex);
        putU16(xml, offset + 24, 20);
        putU16(xml, offset + 26, 20);
        return offset + 36;
    }

    private static int putEndElement(byte[] xml, int offset, int nameIndex) {
        putU16(xml, offset, 0x0103);
        putU16(xml, offset + 2, 16);
        putI32(xml, offset + 4, 24);
        putI32(xml, offset + 12, -1);
        putI32(xml, offset + 16, -1);
        putI32(xml, offset + 20, nameIndex);
        return offset + 24;
    }

    private static byte[] dex(String... values) {
        int dataSize = 0;
        for (String value : values) dataSize += 1 + value.getBytes(StandardCharsets.UTF_8).length + 1;
        int stringIdsOffset = 112;
        int dataOffset = stringIdsOffset + values.length * 4;
        int fileSize = dataOffset + dataSize;
        ByteBuffer buffer = ByteBuffer.allocate(fileSize).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(new byte[]{'d', 'e', 'x', '\n', '0', '3', '5', 0});
        buffer.position(32);
        buffer.putInt(fileSize).putInt(112).putInt(0x12345678);
        buffer.position(56);
        buffer.putInt(values.length).putInt(stringIdsOffset);
        int cursor = dataOffset;
        for (int index = 0; index < values.length; index++) {
            buffer.putInt(stringIdsOffset + index * 4, cursor);
            byte[] bytes = values[index].getBytes(StandardCharsets.UTF_8);
            buffer.put(cursor++, (byte) values[index].length());
            buffer.position(cursor);
            buffer.put(bytes);
            cursor += bytes.length;
            buffer.put(cursor++, (byte) 0);
        }
        return buffer.array();
    }

    private static byte[] bootstrapDex() {
        String descriptor = "Lio/github/endx/rustedfabric/android/patched/PatchedApplication;";
        byte[] value = descriptor.getBytes(StandardCharsets.UTF_8);
        int stringIdsOffset = 112;
        int typeIdsOffset = 116;
        int classDefsOffset = 120;
        int dataOffset = 152;
        int fileSize = dataOffset + 1 + value.length + 1;
        ByteBuffer buffer = ByteBuffer.allocate(fileSize).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(new byte[]{'d', 'e', 'x', '\n', '0', '3', '5', 0});
        buffer.position(32);
        buffer.putInt(fileSize).putInt(112).putInt(0x12345678);
        buffer.position(56);
        buffer.putInt(1).putInt(stringIdsOffset);
        buffer.putInt(1).putInt(typeIdsOffset);
        buffer.position(96);
        buffer.putInt(1).putInt(classDefsOffset);
        buffer.putInt(stringIdsOffset, dataOffset);
        buffer.putInt(typeIdsOffset, 0);
        buffer.putInt(classDefsOffset, 0);
        buffer.put(dataOffset, (byte) descriptor.length());
        buffer.position(dataOffset + 1);
        buffer.put(value).put((byte) 0);
        return buffer.array();
    }

    private static java.util.List<String> strings(byte[] dex) {
        int count = getI32(dex, 56);
        int ids = getI32(dex, 60);
        java.util.List<String> result = new java.util.ArrayList<>();
        for (int index = 0; index < count; index++) {
            int cursor = getI32(dex, ids + index * 4);
            while ((dex[cursor++] & 0x80) != 0) { }
            int end = cursor;
            while (dex[end] != 0) end++;
            result.add(new String(dex, cursor, end - cursor, StandardCharsets.UTF_8));
        }
        return result;
    }

    private static void verifyStoredAlignment(byte[] zip) {
        int cursor = 0;
        while (cursor + 30 <= zip.length && getI32(zip, cursor) == 0x04034b50) {
            int method = getU16(zip, cursor + 8);
            int compressedSize = getI32(zip, cursor + 18);
            int nameLength = getU16(zip, cursor + 26);
            int extraLength = getU16(zip, cursor + 28);
            String name = new String(zip, cursor + 30, nameLength, StandardCharsets.UTF_8);
            int dataOffset = cursor + 30 + nameLength + extraLength;
            if (method == ZipEntry.STORED) {
                require((dataOffset & 3) == 0, "stored APK entry is not aligned: " + name);
            }
            cursor = dataOffset + compressedSize;
        }
    }

    private static void writeLength8(ByteArrayOutputStream output, int value) {
        if (value > 127) output.write(((value >>> 8) & 127) | 128);
        output.write(value & 255);
    }

    private static void putU16(byte[] data, int offset, int value) {
        data[offset] = (byte) value;
        data[offset + 1] = (byte) (value >>> 8);
    }

    private static void putI32(byte[] data, int offset, int value) {
        putU16(data, offset, value);
        putU16(data, offset + 2, value >>> 16);
    }

    private static int getU16(byte[] data, int offset) {
        return (data[offset] & 255) | ((data[offset + 1] & 255) << 8);
    }

    private static int getI32(byte[] data, int offset) {
        return getU16(data, offset) | (getU16(data, offset + 2) << 16);
    }

    private static String repeat(char value, int count) {
        char[] chars = new char[count];
        Arrays.fill(chars, value);
        return new String(chars);
    }

    private static void deleteTree(Path root) throws IOException {
        try (java.util.stream.Stream<Path> paths = Files.walk(root)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException failure) {
                    throw new RuntimeException(failure);
                }
            });
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
