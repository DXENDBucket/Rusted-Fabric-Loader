package io.github.endx.rustedfabricapi.api;

import io.github.endx.rustedfabricapi.api.event.RustedCustomUnitRegistryEvents;
import io.github.endx.rustedfabricapi.api.event.RustedIniEvents;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RustedCustomUnitRegistry {
    private static final String[] CUSTOM_UNIT_LOADER_CLASSES = {
            "rustedwarfare.custom.CustomUnitLoader",
            "com.corrodinggames.rts.game.units.custom.ag"
    };
    private static final String[] CUSTOM_UNIT_METADATA_CLASSES = {
            "rustedwarfare.custom.CustomUnitMetadata",
            "com.corrodinggames.rts.game.units.custom.l"
    };
    private static final String[] MOD_INFO_CLASSES = {
            "rustedwarfare.mod.ModInfo",
            "com.corrodinggames.rts.gameFramework.i.b"
    };
    private static final String[] NAMED_INPUT_STREAM_CLASSES = {
            "rustedwarfare.io.NamedInputStream",
            "com.corrodinggames.rts.gameFramework.utility.j"
    };
    private static final ThreadLocal<Boolean> JAVA_REGISTRATION_PARSE_ACTIVE =
            new ThreadLocal<Boolean>();
    private static final ThreadLocal<Boolean> JAVA_REGISTRATION_COMMIT_ACTIVE =
            new ThreadLocal<Boolean>();

    private RustedCustomUnitRegistry() {
    }

    public interface AssetProvider {
        InputStream open(String path) throws IOException;
    }

    public static Object registerIniUnit(String unitId, String iniText) {
        if (iniText == null) {
            throw new IllegalArgumentException("iniText must not be null");
        }
        return registerIniUnit(unitId,
                new ByteArrayInputStream(iniText.getBytes(StandardCharsets.UTF_8)),
                System.currentTimeMillis(), null, "", "");
    }

    public static Object registerIniUnit(String unitId, InputStream iniStream) {
        return registerIniUnit(unitId, iniStream, System.currentTimeMillis(), null, "", "");
    }

    public static Object registerIniUnit(String unitId, InputStream iniStream,
                                         Object modInfo, String resourceRoot, String templateRoot) {
        return registerIniUnit(unitId, iniStream, System.currentTimeMillis(), modInfo, resourceRoot, templateRoot);
    }

    public static Object registerIniUnit(String virtualPath, byte[] iniBytes, Object modInfo, AssetProvider assetProvider) {
        requireText(virtualPath, "virtualPath");
        byte[] bytes = iniBytes;
        if (bytes == null) {
            if (assetProvider == null) {
                throw new IllegalArgumentException("iniBytes or assetProvider must be provided");
            }
            bytes = readAsset(assetProvider, virtualPath);
        }
        String resourceRoot = directoryOf(virtualPath);
        return registerIniUnit(virtualPath,
                new ByteArrayInputStream(bytes),
                System.currentTimeMillis(),
                modInfo,
                resourceRoot,
                resourceRoot,
                assetProvider);
    }

    public static Object registerIniUnit(String unitId, InputStream iniStream, long sourceTimestamp,
                                         Object modInfo, String resourceRoot, String templateRoot) {
        return registerIniUnit(unitId, iniStream, sourceTimestamp, modInfo, resourceRoot, templateRoot, null);
    }

    private static Object registerIniUnit(String unitId, InputStream iniStream, long sourceTimestamp,
                                          Object modInfo, String resourceRoot, String templateRoot,
                                          AssetProvider assetProvider) {
        requireText(unitId, "unitId");
        if (iniStream == null) {
            throw new IllegalArgumentException("iniStream must not be null");
        }
        if (modInfo != null && !isAnyClass(modInfo.getClass(), MOD_INFO_CLASSES)) {
            throw new IllegalArgumentException("Expected ModInfo, got " + modInfo.getClass().getName());
        }

        RustedIniEvents.ParseStreamContext context = new RustedIniEvents.ParseStreamContext(
                unitId, iniStream, sourceTimestamp, modInfo, null, nullToEmpty(resourceRoot), nullToEmpty(templateRoot),
                assetProvider);
        RustedIniEvents.BEFORE_PARSE_STREAM.invoker().beforeParseStream(context);
        if (context.cancelled()) {
            Object override = context.metadataOverride();
            if (override != null) {
                registerPendingCustomUnitType(override);
            }
            return override;
        }

        Object namedInputStream = newNamedInputStream(context.inputStream(), context.unitId(), context.unitId());
        context.namedInputStream(namedInputStream);

        Object metadata;
        JAVA_REGISTRATION_PARSE_ACTIVE.set(Boolean.TRUE);
        try {
            metadata = invokeStatic(findClass(CUSTOM_UNIT_LOADER_CLASSES),
                    new String[]{"parseCustomUnitMetadata", "a"},
                    context.unitId(),
                    new BufferedInputStream((InputStream) namedInputStream),
                    Long.valueOf(context.sourceTimestamp()),
                    context.modInfo(),
                    context.namedInputStream(),
                    nullToEmpty(context.resourceRoot()),
                    nullToEmpty(context.templateRoot()));
        } finally {
            JAVA_REGISTRATION_PARSE_ACTIVE.remove();
        }

        Object replacement = RustedCustomUnitRegistryEvents.AFTER_METADATA_PARSED.invoker()
                .afterMetadataParsed(context, metadata);
        if (replacement != metadata) {
            replacePendingCustomUnitType(metadata, replacement);
            metadata = replacement;
        }
        return metadata;
    }

    public static String commitPendingCustomUnits() {
        return commitPendingCustomUnits(false);
    }

    public static String commitPendingCustomUnits(boolean includeDisabledMods) {
        if (RustedCustomUnitRegistryEvents.BEFORE_COMMIT.invoker()
                .beforeCommit(getPendingCustomUnitTypesSnapshot(), includeDisabledMods)) {
            return getLastCustomUnitLoadError();
        }

        Object result;
        JAVA_REGISTRATION_COMMIT_ACTIVE.set(Boolean.TRUE);
        try {
            result = invokeStatic(findClass(CUSTOM_UNIT_LOADER_CLASSES),
                    new String[]{"enableAllLoadedCustomUnitTypes", "b"},
                    Boolean.valueOf(includeDisabledMods));
        } finally {
            JAVA_REGISTRATION_COMMIT_ACTIVE.remove();
        }
        String text = result != null ? result.toString() : null;
        RustedCustomUnitRegistryEvents.AFTER_COMMIT.invoker()
                .afterCommit(getActiveCustomUnitTypesSnapshot(), text, getUnitTypeReplacementMapSnapshot());
        return text;
    }

    public static void registerPendingCustomUnitType(Object metadata) {
        requireMetadata(metadata);
        Object pending = getStaticFieldValue(findClass(CUSTOM_UNIT_METADATA_CLASSES),
                new String[]{"pendingCustomUnitTypes", "c"});
        if (!(pending instanceof List)) {
            throw new IllegalStateException("CustomUnitMetadata.pendingCustomUnitTypes is not a List");
        }

        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) pending;
        Object selected = RustedCustomUnitRegistryEvents.BEFORE_PENDING_REGISTER.invoker()
                .beforePendingRegister(metadata, new ArrayList<Object>(list));
        if (selected == null) {
            return;
        }
        requireMetadata(selected);

        synchronized (list) {
            if (!list.contains(selected)) {
                list.add(selected);
            }
        }
        RustedCustomUnitRegistryEvents.AFTER_PENDING_REGISTER.invoker()
                .afterPendingRegister(selected, list.size());
    }

    public static void replacePendingCustomUnitType(Object previousMetadata, Object replacementMetadata) {
        Object pending = getStaticFieldValue(findClass(CUSTOM_UNIT_METADATA_CLASSES),
                new String[]{"pendingCustomUnitTypes", "c"});
        if (!(pending instanceof List)) {
            throw new IllegalStateException("CustomUnitMetadata.pendingCustomUnitTypes is not a List");
        }

        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) pending;
        synchronized (list) {
            if (previousMetadata != null) {
                list.remove(previousMetadata);
            }
        }
        if (replacementMetadata != null) {
            registerPendingCustomUnitType(replacementMetadata);
        }
    }

    public static boolean validateCustomUnitLookupAndActionLinks(boolean strict) {
        Object result = invokeStatic(findClass(CUSTOM_UNIT_LOADER_CLASSES),
                new String[]{"validateCustomUnitLookupAndActionLinks", "c"},
                Boolean.valueOf(strict));
        return Boolean.TRUE.equals(result);
    }

    public static void rebuildCustomUnitLookupAndActionLinks() {
        invokeStatic(findClass(CUSTOM_UNIT_LOADER_CLASSES),
                new String[]{"rebuildCustomUnitLookupAndActionLinks", "g"});
    }

    public static String getLastCustomUnitLoadError() {
        Object value = getStaticFieldValue(findClass(CUSTOM_UNIT_LOADER_CLASSES),
                new String[]{"lastCustomUnitLoadError", "s"});
        return value != null ? value.toString() : null;
    }

    public static List<Object> getPendingCustomUnitTypesSnapshot() {
        return snapshotStaticList(new String[]{"pendingCustomUnitTypes", "c"});
    }

    public static List<Object> getActiveCustomUnitTypesSnapshot() {
        return snapshotStaticList(new String[]{"activeCustomUnitTypes", "d"});
    }

    public static Map<Object, Object> getUnitTypeReplacementMapSnapshot() {
        Object value = getStaticFieldValue(findClass(CUSTOM_UNIT_METADATA_CLASSES),
                new String[]{"unitTypeReplacementMap", "f"});
        if (!(value instanceof Map)) {
            return Collections.emptyMap();
        }
        @SuppressWarnings("unchecked")
        Map<Object, Object> map = (Map<Object, Object>) value;
        return new HashMap<Object, Object>(map);
    }

    public static boolean isJavaRegistrationParseActive() {
        return Boolean.TRUE.equals(JAVA_REGISTRATION_PARSE_ACTIVE.get());
    }

    public static boolean isJavaRegistrationCommitActive() {
        return Boolean.TRUE.equals(JAVA_REGISTRATION_COMMIT_ACTIVE.get());
    }

    private static List<Object> snapshotStaticList(String[] fieldNames) {
        Object value = getStaticFieldValue(findClass(CUSTOM_UNIT_METADATA_CLASSES), fieldNames);
        if (!(value instanceof List)) {
            return Collections.emptyList();
        }
        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) value;
        synchronized (list) {
            return new ArrayList<Object>(list);
        }
    }

    private static Object newNamedInputStream(InputStream inputStream, String displayName, String sourcePath) {
        Class<?> type = findClass(NAMED_INPUT_STREAM_CLASSES);
        try {
            Constructor<?> constructor = type.getConstructor(InputStream.class, String.class, String.class);
            return constructor.newInstance(inputStream, displayName, sourcePath);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not create NamedInputStream", e);
        }
    }

    private static void requireMetadata(Object metadata) {
        if (metadata == null) {
            throw new IllegalArgumentException("metadata must not be null");
        }
        if (!isAnyClass(metadata.getClass(), CUSTOM_UNIT_METADATA_CLASSES)) {
            throw new IllegalArgumentException("Expected CustomUnitMetadata, got " + metadata.getClass().getName());
        }
    }

    private static void requireText(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " must not be empty");
        }
    }

    private static byte[] readAsset(AssetProvider assetProvider, String path) {
        try {
            InputStream inputStream = assetProvider.open(path);
            if (inputStream == null) {
                throw new IllegalArgumentException("assetProvider returned null for " + path);
            }
            try {
                return inputStream.readAllBytes();
            } finally {
                inputStream.close();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not read INI asset " + path, e);
        }
    }

    private static String directoryOf(String path) {
        int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        if (slash < 0) {
            return "";
        }
        return path.substring(0, slash + 1).replace('\\', '/');
    }

    private static String nullToEmpty(String value) {
        return value != null ? value : "";
    }

    private static boolean isAnyClass(Class<?> type, String[] classNames) {
        for (String className : classNames) {
            Class<?> expected = tryFindClass(className);
            if (expected != null && expected.isAssignableFrom(type)) {
                return true;
            }
        }
        return false;
    }

    private static Class<?> findClass(String[] classNames) {
        for (String className : classNames) {
            Class<?> type = tryFindClass(className);
            if (type != null) {
                return type;
            }
        }
        throw new IllegalStateException("Could not find any class: " + join(classNames));
    }

    private static Class<?> tryFindClass(String className) {
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        if (contextLoader != null) {
            try {
                return Class.forName(className, false, contextLoader);
            } catch (ClassNotFoundException ignored) {
            }
        }

        try {
            return Class.forName(className, false, RustedCustomUnitRegistry.class.getClassLoader());
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    private static Object invokeStatic(Class<?> type, String[] names, Object... args) {
        Method method = findMethod(type, names, true, args);
        try {
            return method.invoke(null, args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new IllegalStateException("Could not invoke static method "
                    + method.getName() + ": " + describeThrowable(cause), cause);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not invoke static method "
                    + method.getName() + ": " + describeThrowable(e), e);
        }
    }

    private static String describeThrowable(Throwable throwable) {
        if (throwable == null) {
            return "<no cause>";
        }

        String message = throwable.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return throwable.getClass().getName();
        }
        return throwable.getClass().getName() + ": " + message;
    }

    private static Method findMethod(Class<?> type, String[] names, boolean staticMethod, Object[] args) {
        Class<?> current = type;
        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                if (Modifier.isStatic(method.getModifiers()) != staticMethod) {
                    continue;
                }
                if (!contains(names, method.getName())) {
                    continue;
                }
                if (!parametersMatch(method.getParameterTypes(), args)) {
                    continue;
                }
                method.setAccessible(true);
                return method;
            }
            current = current.getSuperclass();
        }
        throw new IllegalStateException("Could not find method " + join(names) + " on " + type.getName());
    }

    private static boolean parametersMatch(Class<?>[] parameterTypes, Object[] args) {
        if (parameterTypes.length != args.length) {
            return false;
        }
        for (int i = 0; i < parameterTypes.length; i++) {
            Object arg = args[i];
            Class<?> parameterType = wrap(parameterTypes[i]);
            if (arg != null && !parameterType.isAssignableFrom(arg.getClass())) {
                return false;
            }
            if (arg == null && parameterTypes[i].isPrimitive()) {
                return false;
            }
        }
        return true;
    }

    private static Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        if (type == char.class) {
            return Character.class;
        }
        return Void.class;
    }

    private static Object getStaticFieldValue(Class<?> type, String[] names) {
        Field field = findField(type, names);
        try {
            return field.get(null);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Could not read field " + field.getName(), e);
        }
    }

    private static Field findField(Class<?> type, String[] names) {
        Class<?> current = type;
        while (current != null) {
            for (String name : names) {
                try {
                    Field field = current.getDeclaredField(name);
                    field.setAccessible(true);
                    return field;
                } catch (NoSuchFieldException ignored) {
                }
            }
            current = current.getSuperclass();
        }
        throw new IllegalStateException("Could not find field " + join(names) + " on " + type.getName());
    }

    private static boolean contains(String[] values, String value) {
        for (String item : values) {
            if (item.equals(value)) {
                return true;
            }
        }
        return false;
    }

    private static String join(String[] values) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                result.append('/');
            }
            result.append(values[i]);
        }
        return result.toString();
    }
}
