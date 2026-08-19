package io.github.endx.rustedfabric.android.jvm;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable input for the future native libjli/JNI host. */
public final class JvmLaunchPlan {
    private final Path workingDirectory;
    private final Path runtimeHome;
    private final Path nativeLibraryDirectory;
    private final List<Path> classpath;
    private final List<String> virtualMachineArguments;
    private final String mainClass;
    private final List<String> gameArguments;

    JvmLaunchPlan(Path workingDirectory, Path runtimeHome, Path nativeLibraryDirectory,
                  List<Path> classpath, List<String> virtualMachineArguments,
                  String mainClass, List<String> gameArguments) {
        this.workingDirectory = workingDirectory;
        this.runtimeHome = runtimeHome;
        this.nativeLibraryDirectory = nativeLibraryDirectory;
        this.classpath = immutable(classpath);
        this.virtualMachineArguments = immutableStrings(virtualMachineArguments);
        this.mainClass = mainClass;
        this.gameArguments = immutableStrings(gameArguments);
    }

    public Path workingDirectory() { return workingDirectory; }
    public Path runtimeHome() { return runtimeHome; }
    public Path nativeLibraryDirectory() { return nativeLibraryDirectory; }
    public List<Path> classpath() { return classpath; }
    public List<String> virtualMachineArguments() { return virtualMachineArguments; }
    public String mainClass() { return mainClass; }
    public List<String> gameArguments() { return gameArguments; }

    /** Returns a copy with launcher-selected VM properties appended after built-in defaults. */
    public JvmLaunchPlan withAdditionalVirtualMachineArguments(List<String> additional) {
        List<String> merged = new ArrayList<>(virtualMachineArguments);
        if (additional != null) merged.addAll(additional);
        return new JvmLaunchPlan(workingDirectory, runtimeHome, nativeLibraryDirectory,
                classpath, merged, mainClass, gameArguments);
    }

    private static List<Path> immutable(List<Path> values) {
        return Collections.unmodifiableList(new ArrayList<>(values));
    }

    private static List<String> immutableStrings(List<String> values) {
        return Collections.unmodifiableList(new ArrayList<>(values));
    }
}
