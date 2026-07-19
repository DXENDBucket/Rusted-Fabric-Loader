package io.github.endx.rustedfabric.android.xposed.jvm;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.github.endx.rustedfabric.android.jvm.JvmLaunchPlan;

/** JNI boundary that hosts one external OpenJDK VM in a dedicated future game process. */
public final class NativeJvmHost {
    private static final boolean PACKAGED;

    static {
        boolean loaded;
        try {
            System.loadLibrary("rustedfabric_jvmhost");
            loaded = true;
        } catch (LinkageError unavailable) {
            loaded = false;
        }
        PACKAGED = loaded;
    }

    private NativeJvmHost() {
    }

    public static boolean isPackaged() {
        return PACKAGED;
    }

    public static Result launch(JvmLaunchPlan plan) {
        if (plan == null) throw new IllegalArgumentException("plan must not be null");
        if (!PACKAGED) return new Result(1, "Native JVM host is not packaged for this ABI");
        List<String> options = new ArrayList<>(plan.virtualMachineArguments());
        StringBuilder classpath = new StringBuilder("-Djava.class.path=");
        for (int index = 0; index < plan.classpath().size(); ++index) {
            if (index > 0) classpath.append(File.pathSeparatorChar);
            classpath.append(plan.classpath().get(index).toAbsolutePath().normalize());
        }
        options.add(classpath.toString());
        int code = nativeLaunch(plan.runtimeHome().toString(), plan.workingDirectory().toString(),
                plan.nativeLibraryDirectory().toString(), plan.mainClass(),
                options.toArray(new String[0]), plan.gameArguments().toArray(new String[0]));
        return new Result(code, code == 0 ? "" : nativeLastError());
    }

    private static native int nativeLaunch(String runtimeHome, String workingDirectory,
                                           String nativeLibraryDirectory, String mainClass,
                                           String[] vmOptions, String[] gameArguments);

    private static native String nativeLastError();

    public static final class Result {
        private final int code;
        private final String detail;

        Result(int code, String detail) {
            this.code = code;
            this.detail = detail == null ? "" : detail;
        }

        public int code() { return code; }
        public String detail() { return detail; }
        public boolean succeeded() { return code == 0; }
    }
}
