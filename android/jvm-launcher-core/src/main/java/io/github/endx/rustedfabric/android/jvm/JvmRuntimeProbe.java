package io.github.endx.rustedfabric.android.jvm;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Detects Loader-owned JVM and native adapter files without loading executable code. */
public final class JvmRuntimeProbe {
    private JvmRuntimeProbe() {
    }

    public static JvmBackendCapabilities inspect(Path runtimeHome, Path packagedNativeDirectory) {
        boolean java17 = isJava17Runtime(runtimeHome);
        boolean host = nativeFile(packagedNativeDirectory, "librustedfabric_jvmhost.so");
        boolean lwjgl2 = nativeFile(packagedNativeDirectory, "liblwjgl.so");
        boolean openAl = nativeFile(packagedNativeDirectory, "libopenal.so");
        boolean jinput = nativeFile(packagedNativeDirectory, "librustedfabric_input.so");
        boolean rocket = nativeFile(packagedNativeDirectory, "librocketconnector.so");
        return new JvmBackendCapabilities(java17, host, lwjgl2, openAl, jinput, rocket);
    }

    public static Path serverVm(Path runtimeHome) {
        return runtimeHome.resolve("lib").resolve("server").resolve("libjvm.so");
    }

    public static String runtimeIssue(Path runtimeHome) {
        if (runtimeHome == null || !Files.isDirectory(runtimeHome)) return "not installed";
        Path release = runtimeHome.resolve("release");
        if (!Files.isRegularFile(release)) return "release metadata is missing";
        Map<String, String> metadata;
        try {
            metadata = releaseMetadata(release);
        } catch (IOException unreadable) {
            return "release metadata is unreadable";
        }
        String version = metadata.get("JAVA_VERSION");
        if (version == null || !(version.equals("17") || version.startsWith("17."))) {
            return "Java 17 is required";
        }
        String osName = metadata.get("OS_NAME");
        if (osName == null || !"Linux".equalsIgnoreCase(osName)) {
            return "runtime OS is " + osName + ", not Linux/Android";
        }
        String architecture = metadata.get("OS_ARCH");
        if (architecture == null || !("aarch64".equalsIgnoreCase(architecture)
                || "arm64".equalsIgnoreCase(architecture))) {
            return "runtime architecture is " + architecture + ", not ARM64";
        }
        if (!isAarch64Elf(serverVm(runtimeHome))) {
            return "lib/server/libjvm.so is not an AArch64 ELF library";
        }
        if (!isAarch64Elf(runtimeHome.resolve("lib").resolve("libjava.so"))) {
            return "lib/libjava.so is not an AArch64 ELF library";
        }
        Path modules = runtimeHome.resolve("lib").resolve("modules");
        try {
            if (!Files.isRegularFile(modules) || Files.size(modules) == 0L) {
                return "lib/modules is missing or empty";
            }
        } catch (IOException unreadable) {
            return "lib/modules is unreadable";
        }
        return "";
    }

    private static boolean isJava17Runtime(Path runtimeHome) {
        return runtimeIssue(runtimeHome).isEmpty();
    }

    private static Map<String, String> releaseMetadata(Path release) throws IOException {
        Map<String, String> values = new HashMap<>();
        String text = new String(Files.readAllBytes(release), StandardCharsets.UTF_8);
        for (String line : text.split("\\R")) {
            int separator = line.indexOf('=');
            if (separator <= 0) continue;
            String key = line.substring(0, separator).trim();
            String value = line.substring(separator + 1).trim();
            if (value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\""))
                    || (value.startsWith("'") && value.endsWith("'")))) {
                value = value.substring(1, value.length() - 1);
            }
            values.put(key, value);
        }
        return values;
    }

    private static boolean isAarch64Elf(Path library) {
        if (!Files.isRegularFile(library)) return false;
        try (java.io.InputStream input = Files.newInputStream(library)) {
            byte[] header = new byte[20];
            int offset = 0;
            while (offset < header.length) {
                int read = input.read(header, offset, header.length - offset);
                if (read < 0) return false;
                offset += read;
            }
            return (header[0] & 0xff) == 0x7f && header[1] == 'E' && header[2] == 'L'
                    && header[3] == 'F' && header[4] == 2 && header[5] == 1
                    && (header[18] & 0xff) == 0xb7 && header[19] == 0;
        } catch (IOException unreadable) {
            return false;
        }
    }

    private static boolean nativeFile(Path directory, String name) {
        return directory != null && Files.isRegularFile(directory.resolve(name));
    }
}
