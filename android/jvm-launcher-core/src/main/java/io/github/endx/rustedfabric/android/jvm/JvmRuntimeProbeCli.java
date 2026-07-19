package io.github.endx.rustedfabric.android.jvm;

import java.nio.file.Path;
import java.nio.file.Paths;

/** Command-line validation for a locally extracted Android OpenJDK runtime. */
public final class JvmRuntimeProbeCli {
    private JvmRuntimeProbeCli() {
    }

    public static void main(String[] arguments) {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("Expected one extracted Java runtime directory");
        }
        Path runtimeHome = Paths.get(arguments[0]).toAbsolutePath().normalize();
        String issue = JvmRuntimeProbe.runtimeIssue(runtimeHome);
        if (!issue.isEmpty()) {
            throw new IllegalStateException("Rejected Android JVM runtime at " + runtimeHome
                    + ": " + issue);
        }
        System.out.println("Accepted Linux AArch64 Java 17 runtime: " + runtimeHome);
        System.out.println("libjvm: " + JvmRuntimeProbe.serverVm(runtimeHome));
    }
}
