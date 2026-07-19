package io.github.endx.rustedfabric.android.jvm;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Tiny Loader-owned main class used to prove the external JVM can execute bytecode. */
public final class JvmHostSmokeMain {
    private JvmHostSmokeMain() {
    }

    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("Expected one result-file path");
        }
        Path result = Paths.get(arguments[0]).toAbsolutePath().normalize();
        String report = "rusted-fabric-jvm-smoke=ok\n"
                + "java.version=" + System.getProperty("java.version", "unknown") + "\n"
                + "os.name=" + System.getProperty("os.name", "unknown") + "\n"
                + "os.arch=" + System.getProperty("os.arch", "unknown") + "\n";
        Files.write(result, report.getBytes(StandardCharsets.UTF_8));
        System.out.print(report);
    }
}
