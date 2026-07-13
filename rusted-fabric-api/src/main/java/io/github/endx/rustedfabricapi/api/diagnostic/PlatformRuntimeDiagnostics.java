package io.github.endx.rustedfabricapi.api.diagnostic;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Namespace-neutral access to the platform and file-change infrastructure named in mappings v1.0-RC1.
 */
public final class PlatformRuntimeDiagnostics {
    private static final String[] OPERATING_SYSTEM_DETECTOR_CLASSES = {
            "rustedwarfare.platform.OperatingSystemDetector",
            "com.corrodinggames.rts.gameFramework.g"
    };
    private static final String[] OPERATING_SYSTEM_CLASSES = {
            "rustedwarfare.platform.OperatingSystem",
            "com.corrodinggames.rts.gameFramework.h"
    };
    private static final String[] PLATFORM_EXTENSION_CLASSES = {
            "rustedwarfare.platform.PlatformExtension",
            "com.corrodinggames.rts.gameFramework.l.a"
    };
    private static final String[] FILE_CHANGE_ENGINE_CLASSES = {
            "rustedwarfare.io.FileChangeEngine",
            "com.corrodinggames.rts.gameFramework.j"
    };

    private PlatformRuntimeDiagnostics() {
    }

    public static Object operatingSystem() {
        return RustedReflection.invokeStatic(OPERATING_SYSTEM_DETECTOR_CLASSES,
                new String[]{"getOperatingSystem", "a"});
    }

    public static String operatingSystemName() {
        Object operatingSystem = operatingSystem();
        String[][] aliases = {
                {"WINDOWS", "a"},
                {"MACOS", "b"},
                {"LINUX", "c"},
                {"OTHER", "d"}
        };
        for (String[] alias : aliases) {
            Object candidate = RustedReflection.getStaticFieldValue(OPERATING_SYSTEM_CLASSES,
                    new String[]{alias[0], alias[1]});
            if (candidate == operatingSystem) {
                return alias[0];
            }
        }
        return operatingSystem != null ? operatingSystem.toString() : "";
    }

    public static String platformDescription() {
        Object value = RustedReflection.invokeStatic(PLATFORM_EXTENSION_CLASSES,
                new String[]{"getPlatformDescription", "a"});
        return value != null ? value.toString() : "";
    }

    public static boolean supportsFilePicker() {
        return Boolean.TRUE.equals(RustedReflection.invokeStatic(PLATFORM_EXTENSION_CLASSES,
                new String[]{"supportsFilePicker", "b"}));
    }

    public static float safeAreaInset() {
        Object value = RustedReflection.invokeStatic(PLATFORM_EXTENSION_CLASSES,
                new String[]{"getSafeAreaInset", "c"});
        return value instanceof Number ? ((Number) value).floatValue() : 0.0f;
    }

    public static Map<String, Object> describePlatform() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("operatingSystem", operatingSystem());
        result.put("operatingSystemName", operatingSystemName());
        result.put("description", platformDescription());
        result.put("supportsFilePicker", Boolean.valueOf(supportsFilePicker()));
        result.put("safeAreaInset", Float.valueOf(safeAreaInset()));
        result.put("backend", RustedReflection.getStaticFieldValue(PLATFORM_EXTENSION_CLASSES,
                new String[]{"backend", "a"}));
        return Collections.unmodifiableMap(result);
    }

    public static long getLastModified(String path, boolean useCache) {
        Object value = RustedReflection.invokeStatic(FILE_CHANGE_ENGINE_CLASSES,
                new String[]{"getLastModified", "a"}, path, Boolean.valueOf(useCache));
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    public static void setFileChangePollingEnabled(boolean enabled) {
        RustedReflection.invokeStatic(FILE_CHANGE_ENGINE_CLASSES,
                new String[]{"setEnabled", "a"}, Boolean.valueOf(enabled));
    }

    public static void scanForFileChanges() {
        RustedReflection.invokeStatic(FILE_CHANGE_ENGINE_CLASSES,
                new String[]{"scanForChanges", "b"});
    }

    public static Map<String, Object> fileChangeCacheSnapshot() {
        Object value = RustedReflection.getStaticFieldValue(FILE_CHANGE_ENGINE_CLASSES,
                new String[]{"lastModifiedCache", "a"});
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        if (value instanceof Map<?, ?>) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return Collections.unmodifiableMap(result);
    }
}
