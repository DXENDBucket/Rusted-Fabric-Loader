package io.github.endx.rustedfabricapi.api.asset;

import io.github.endx.rustedfabricapi.api.RustedCustomUnitRegistry;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public final class JavaUnitAssetLoader {
    private static final String[] CUSTOM_UNIT_LOADER_CLASSES = {
            "rustedwarfare.custom.CustomUnitLoader",
            "com.corrodinggames.rts.game.units.custom.ag"
    };
    private static final String[] CUSTOM_UNIT_METADATA_CLASSES = {
            "rustedwarfare.custom.CustomUnitMetadata",
            "com.corrodinggames.rts.game.units.custom.l"
    };
    private static final String[] SOUND_EFFECT_LIST_CLASSES = {
            "rustedwarfare.custom.SoundEffectList",
            "com.corrodinggames.rts.game.units.custom.bl"
    };
    private static final String[] EFFECT_LIST_CLASSES = {
            "rustedwarfare.custom.EffectList",
            "com.corrodinggames.rts.game.units.custom.z"
    };

    private JavaUnitAssetLoader() {
    }

    public static Object loadImage(Object metadata, String basePath, String imagePath, boolean smoothing) {
        requireMetadata(metadata);
        return invokeStatic(findClass(CUSTOM_UNIT_LOADER_CLASSES),
                new String[]{"loadImageInConfigTimed", "a"},
                basePath, imagePath, smoothing, metadata);
    }

    public static Object loadImageWithContext(Object metadata, String basePath, String imagePath, boolean smoothing,
                                              String section, String key) {
        requireMetadata(metadata);
        return invokeStatic(findClass(CUSTOM_UNIT_LOADER_CLASSES),
                new String[]{"loadImageInConfigWithContext", "a"},
                basePath, imagePath, smoothing, metadata, section, key);
    }

    public static Object loadSound(Object metadata, String basePath, String soundPath) {
        requireMetadata(metadata);
        Object sound = invokeStatic(findClass(CUSTOM_UNIT_LOADER_CLASSES),
                new String[]{"loadSoundInConfigTimed", "a"},
                basePath, soundPath, metadata);
        markSoundUsedByCurrentMod(sound);
        return sound;
    }

    public static Object parseSoundList(Object metadata, String soundList) {
        requireMetadata(metadata);
        return invokeStatic(findClass(SOUND_EFFECT_LIST_CLASSES),
                new String[]{"parseSoundList", "a"},
                metadata, soundList);
    }

    public static Object createEffectList(Object metadata, String rawEffectList, boolean resolve) {
        requireMetadata(metadata);
        Object effectList = newEffectList(metadata, rawEffectList);
        if (resolve) {
            invokeInstance(effectList, new String[]{"resolveEffectReferences", "c"});
        }
        return effectList;
    }

    public static void applyMainImage(Object metadata, Object image, int frameWidth, int frameHeight,
                                      int totalFrames, int defaultFrame) {
        requireMetadata(metadata);
        requireImage(image, "image");
        setFieldValue(metadata, new String[]{"image", "ad"}, image);
        applyFrameLayout(metadata, frameWidth, frameHeight, totalFrames, defaultFrame);
    }

    public static void applyFrameLayout(Object metadata, int frameWidth, int frameHeight,
                                        int totalFrames, int defaultFrame) {
        requireMetadata(metadata);
        Object image = getFieldValue(metadata, new String[]{"image", "ad"});
        requireImage(image, "metadata.image");

        int imageWidth = getImageWidth(image);
        int imageHeight = getImageHeight(image);
        int resolvedFrameWidth = frameWidth > 0 ? frameWidth : imageWidth;
        int resolvedFrameHeight = frameHeight > 0 ? frameHeight : imageHeight;
        if (resolvedFrameWidth <= 0 || resolvedFrameHeight <= 0) {
            throw new IllegalArgumentException("Frame dimensions must be positive");
        }

        int framesPerRow = Math.max(1, imageWidth / resolvedFrameWidth);
        int rows = Math.max(1, imageHeight / resolvedFrameHeight);
        int resolvedTotalFrames = totalFrames > 0 ? totalFrames : Math.max(1, framesPerRow * rows);
        int resolvedDefaultFrame = clamp(defaultFrame, 0, resolvedTotalFrames - 1);

        setIntField(metadata, new String[]{"frameWidth", "W"}, resolvedFrameWidth);
        setIntField(metadata, new String[]{"frameHeight", "X"}, resolvedFrameHeight);
        setIntField(metadata, new String[]{"imageFrameWidth", "af"}, resolvedFrameWidth);
        setIntField(metadata, new String[]{"imageFrameHeight", "ag"}, resolvedFrameHeight);
        setIntField(metadata, new String[]{"framesPerRow", "V"}, framesPerRow);
        setIntField(metadata, new String[]{"totalFrames", "U"}, resolvedTotalFrames);
        setIntField(metadata, new String[]{"defaultFrame", "Y"}, resolvedDefaultFrame);
    }

    public static Object applyTeamColorImages(Object metadata, Object image, Object teamColoringMode) {
        requireMetadata(metadata);
        Object sourceImage = image != null ? image : getFieldValue(metadata, new String[]{"image", "ad"});
        requireImage(sourceImage, "team color source image");
        Object mode = teamColoringMode != null
                ? teamColoringMode
                : getFieldValue(metadata, new String[]{"teamColoringMode", "ac"});
        if (mode == null) {
            return null;
        }

        if (teamColoringMode != null) {
            setFieldValue(metadata, new String[]{"teamColoringMode", "ac"}, teamColoringMode);
        }

        Object images = invokeInstance(metadata, new String[]{"createTeamColorImages", "a"}, sourceImage, mode);
        setFieldValue(metadata, new String[]{"teamColorImages", "ar"}, images);
        return images;
    }

    public static Object applyTeamColorTurretImages(Object metadata, Object image, Object teamColoringMode) {
        requireMetadata(metadata);
        Object sourceImage = image != null ? image : getFieldValue(metadata, new String[]{"imageTurret", "ao"});
        if (sourceImage == null) {
            return null;
        }
        Object mode = teamColoringMode != null
                ? teamColoringMode
                : getFieldValue(metadata, new String[]{"teamColoringMode", "ac"});
        if (mode == null) {
            return null;
        }

        Object images = invokeInstance(metadata, new String[]{"createTeamColorImages", "a"}, sourceImage, mode);
        setFieldValue(metadata, new String[]{"teamColorTurretImages", "at"}, images);
        return images;
    }

    public static void applyShadowImage(Object metadata, Object shadowImage, boolean shadowImageHasFrames) {
        requireMetadata(metadata);
        if (shadowImage != null) {
            requireImage(shadowImage, "shadowImage");
        }
        setFieldValue(metadata, new String[]{"imageShadow", "ap"}, shadowImage);
        setBooleanField(metadata, new String[]{"shadowImageHasFrames", "aq"}, shadowImageHasFrames);
    }

    public static Object applyIcons(Object metadata, Object iconBuild, Object iconZoomedOut,
                                    Object teamColoringMode, boolean neverShowZoomedOut) {
        requireMetadata(metadata);
        if (iconBuild != null) {
            requireImage(iconBuild, "iconBuild");
        }
        setFieldValue(metadata, new String[]{"iconBuild", "aw"}, iconBuild);

        if (neverShowZoomedOut || iconZoomedOut == null) {
            setFieldValue(metadata, new String[]{"iconZoomedOutTeamImages", "as"}, null);
            return null;
        }

        requireImage(iconZoomedOut, "iconZoomedOut");
        Object mode = teamColoringMode != null
                ? teamColoringMode
                : getFieldValue(metadata, new String[]{"teamColoringMode", "ac"});
        if (mode == null) {
            setFieldValue(metadata, new String[]{"iconZoomedOutTeamImages", "as"}, null);
            return null;
        }

        Object images = invokeInstance(metadata, new String[]{"createTeamColorImages", "a"}, iconZoomedOut, mode);
        setFieldValue(metadata, new String[]{"iconZoomedOutTeamImages", "as"}, images);
        return images;
    }

    public static void markImageUsedByCurrentMod(Object image) {
        if (image != null) {
            invokeStatic(findClass(CUSTOM_UNIT_LOADER_CLASSES),
                    new String[]{"markImageUsedByCurrentMod", "a"},
                    image);
        }
    }

    public static void markImageArrayUsedByCurrentMod(Object images) {
        if (images != null) {
            invokeStatic(findClass(CUSTOM_UNIT_LOADER_CLASSES),
                    new String[]{"markImageArrayUsedByCurrentMod", "a"},
                    images);
        }
    }

    public static void markSoundUsedByCurrentMod(Object sound) {
        if (sound != null) {
            invokeStatic(findClass(CUSTOM_UNIT_LOADER_CLASSES),
                    new String[]{"markSoundUsedByCurrentMod", "a"},
                    sound);
        }
    }

    public static void registerPendingCustomUnitType(Object metadata) {
        RustedCustomUnitRegistry.registerPendingCustomUnitType(metadata);
    }

    public static String enableAllLoadedCustomUnitTypes(boolean includeDisabledMods) {
        return RustedCustomUnitRegistry.commitPendingCustomUnits(includeDisabledMods);
    }

    public static void rebuildCustomUnitLookupAndActionLinks() {
        RustedCustomUnitRegistry.rebuildCustomUnitLookupAndActionLinks();
    }

    public static List<String> validateAssetContract(Object metadata) {
        requireMetadata(metadata);
        List<String> issues = new ArrayList<>();

        Object image = getFieldValue(metadata, new String[]{"image", "ad"});
        if (!isImageValid(image)) {
            issues.add("metadata.image must be a non-null GameImage with positive dimensions");
        }

        int frameWidth = getIntField(metadata, new String[]{"imageFrameWidth", "af"});
        int frameHeight = getIntField(metadata, new String[]{"imageFrameHeight", "ag"});
        int framesPerRow = getIntField(metadata, new String[]{"framesPerRow", "V"});
        int totalFrames = getIntField(metadata, new String[]{"totalFrames", "U"});
        int defaultFrame = getIntField(metadata, new String[]{"defaultFrame", "Y"});
        if (frameWidth <= 0 || frameHeight <= 0) {
            issues.add("imageFrameWidth/imageFrameHeight must be positive");
        }
        if (framesPerRow < 1) {
            issues.add("framesPerRow must be at least 1");
        }
        if (totalFrames < 1) {
            issues.add("totalFrames must be at least 1");
        }
        if (defaultFrame < 0 || defaultFrame >= Math.max(1, totalFrames)) {
            issues.add("defaultFrame must be inside totalFrames");
        }

        Object teamColorImages = getFieldValue(metadata, new String[]{"teamColorImages", "ar"});
        if (teamColorImages != null && !teamColorImages.getClass().isArray()) {
            issues.add("teamColorImages must be null or an array");
        }

        Object iconZoomedOutTeamImages = getFieldValue(metadata, new String[]{"iconZoomedOutTeamImages", "as"});
        if (iconZoomedOutTeamImages != null && !iconZoomedOutTeamImages.getClass().isArray()) {
            issues.add("iconZoomedOutTeamImages must be null or an array");
        }

        Object shadow = getFieldValue(metadata, new String[]{"imageShadow", "ap"});
        if (shadow != null && !isImageValid(shadow)) {
            issues.add("imageShadow must be null or a valid GameImage");
        }

        return issues;
    }

    public static boolean isImageValid(Object image) {
        return image != null && getImageWidth(image) > 0 && getImageHeight(image) > 0;
    }

    public static int getImageWidth(Object image) {
        if (image == null) {
            return 0;
        }
        Object width = invokeInstanceIfPresent(image, new String[]{"getWidth"});
        if (width instanceof Number) {
            return ((Number) width).intValue();
        }
        return getIntField(image, new String[]{"width", "p"});
    }

    public static int getImageHeight(Object image) {
        if (image == null) {
            return 0;
        }
        Object height = invokeInstanceIfPresent(image, new String[]{"getHeight"});
        if (height instanceof Number) {
            return ((Number) height).intValue();
        }
        return getIntField(image, new String[]{"height", "q"});
    }

    private static void requireMetadata(Object metadata) {
        if (metadata == null) {
            throw new IllegalArgumentException("metadata must not be null");
        }
        if (!isAnyClass(metadata.getClass(), CUSTOM_UNIT_METADATA_CLASSES)) {
            throw new IllegalArgumentException("Expected CustomUnitMetadata, got " + metadata.getClass().getName());
        }
    }

    private static void requireImage(Object image, String label) {
        if (!isImageValid(image)) {
            throw new IllegalArgumentException(label + " must be a valid GameImage");
        }
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
            return Class.forName(className, false, JavaUnitAssetLoader.class.getClassLoader());
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    private static Object newEffectList(Object metadata, String rawEffectList) {
        Class<?> type = findClass(EFFECT_LIST_CLASSES);
        try {
            for (Constructor<?> constructor : type.getDeclaredConstructors()) {
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                if (parameterTypes.length == 2
                        && parameterTypes[0].isAssignableFrom(metadata.getClass())
                        && parameterTypes[1] == String.class) {
                    constructor.setAccessible(true);
                    return constructor.newInstance(metadata, rawEffectList);
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not create EffectList", e);
        }
        throw new IllegalStateException("Could not find EffectList(CustomUnitMetadata, String) constructor");
    }

    private static Object invokeStatic(Class<?> type, String[] names, Object... args) {
        Method method = findMethod(type, names, true, args);
        try {
            return method.invoke(null, args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not invoke static method " + method.getName(), e);
        }
    }

    private static Object invokeInstance(Object owner, String[] names, Object... args) {
        Method method = findMethod(owner.getClass(), names, false, args);
        try {
            return method.invoke(owner, args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not invoke method " + method.getName(), e);
        }
    }

    private static Object invokeInstanceIfPresent(Object owner, String[] names, Object... args) {
        try {
            return invokeInstance(owner, names, args);
        } catch (IllegalStateException e) {
            return null;
        }
    }

    private static Method findMethod(Class<?> type, String[] names, boolean staticMethod, Object[] args) {
        Class<?> current = type;
        while (current != null) {
            Method[] methods = current.getDeclaredMethods();
            for (Method method : methods) {
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

    private static Object getFieldValue(Object owner, String[] names) {
        Field field = findField(owner.getClass(), names);
        try {
            return field.get(owner);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Could not read field " + field.getName(), e);
        }
    }

    private static void setFieldValue(Object owner, String[] names, Object value) {
        Field field = findField(owner.getClass(), names);
        try {
            field.set(owner, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Could not set field " + field.getName(), e);
        }
    }

    private static int getIntField(Object owner, String[] names) {
        Object value = getFieldValue(owner, names);
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    private static void setIntField(Object owner, String[] names, int value) {
        setFieldValue(owner, names, value);
    }

    private static void setBooleanField(Object owner, String[] names, boolean value) {
        setFieldValue(owner, names, value);
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

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
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
