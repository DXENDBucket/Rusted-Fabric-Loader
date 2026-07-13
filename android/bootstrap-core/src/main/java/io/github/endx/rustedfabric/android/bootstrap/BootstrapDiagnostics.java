package io.github.endx.rustedfabric.android.bootstrap;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Stores only non-sensitive names from the first successful Application.attach call. It never
 * retains Android Context, Application, ClassLoader, APK path, or game objects.
 */
public final class BootstrapDiagnostics {
    public static final String MAPPING_PROFILE_STATUS = "PENDING_MAPPING";
    private static final AtomicReference<Snapshot> SNAPSHOT = new AtomicReference<>();

    private BootstrapDiagnostics() {
    }

    public static Snapshot captureOnce(String packageName, String processName,
                                       String applicationClassName, String classLoaderClassName) {
        Snapshot candidate = new Snapshot(packageName, processName, applicationClassName,
                classLoaderClassName, MAPPING_PROFILE_STATUS);
        SNAPSHOT.compareAndSet(null, candidate);
        return SNAPSHOT.get();
    }

    public static Snapshot current() {
        return SNAPSHOT.get();
    }

    static void resetForTests() {
        SNAPSHOT.set(null);
    }

    public static final class Snapshot {
        private final String packageName;
        private final String processName;
        private final String applicationClassName;
        private final String classLoaderClassName;
        private final String mappingProfileStatus;

        private Snapshot(String packageName, String processName, String applicationClassName,
                         String classLoaderClassName, String mappingProfileStatus) {
            this.packageName = safe(packageName);
            this.processName = safe(processName);
            this.applicationClassName = safe(applicationClassName);
            this.classLoaderClassName = safe(classLoaderClassName);
            this.mappingProfileStatus = safe(mappingProfileStatus);
        }

        public String getPackageName() {
            return packageName;
        }

        public String getProcessName() {
            return processName;
        }

        public String getApplicationClassName() {
            return applicationClassName;
        }

        public String getClassLoaderClassName() {
            return classLoaderClassName;
        }

        public String getMappingProfileStatus() {
            return mappingProfileStatus;
        }

        private static String safe(String value) {
            return value == null ? "unknown" : value;
        }
    }
}
