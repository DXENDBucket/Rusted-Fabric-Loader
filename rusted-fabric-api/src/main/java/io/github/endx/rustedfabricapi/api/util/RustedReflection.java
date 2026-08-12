package io.github.endx.rustedfabricapi.api.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class RustedReflection {
    private static final ClassValue<ConcurrentMap<String, DeclaredFieldLookup>> DECLARED_FIELDS =
            new ClassValue<ConcurrentMap<String, DeclaredFieldLookup>>() {
                @Override
                protected ConcurrentMap<String, DeclaredFieldLookup> computeValue(Class<?> type) {
                    return new ConcurrentHashMap<String, DeclaredFieldLookup>();
                }
            };

    private RustedReflection() {
    }

    public static Object newInstance(String[] classNames, Object... args) {
        Class<?> type = findClass(classNames);
        Constructor<?> constructor = findConstructor(type, args);
        try {
            return constructor.newInstance(args);
        } catch (InvocationTargetException e) {
            throwUnchecked("Could not create " + type.getName(), e);
            return null;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not create " + type.getName(), e);
        }
    }

    public static Object invokeStatic(String[] classNames, String[] methodNames, Object... args) {
        return invokeStatic(findClass(classNames), methodNames, args);
    }

    public static Object invokeStatic(Class<?> type, String[] methodNames, Object... args) {
        Method method = findMethod(type, methodNames, true, args);
        return invoke(method, null, args);
    }

    public static Object invokeInstance(Object owner, String[] methodNames, Object... args) {
        requireNonNull(owner, "owner");
        Method method = findMethod(owner.getClass(), methodNames, false, args);
        return invoke(method, owner, args);
    }

    public static Object getStaticFieldValue(String[] classNames, String[] fieldNames) {
        Field field = findField(findClass(classNames), fieldNames);
        try {
            return field.get(null);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Could not read field " + field.getName(), e);
        }
    }

    public static Object getFieldValue(Object owner, String[] fieldNames) {
        requireNonNull(owner, "owner");
        Field field = findField(owner.getClass(), fieldNames);
        try {
            return field.get(owner);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Could not read field " + field.getName(), e);
        }
    }

    public static void setFieldValue(Object owner, String[] fieldNames, Object value) {
        requireNonNull(owner, "owner");
        Field field = findField(owner.getClass(), fieldNames);
        try {
            field.set(owner, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Could not set field " + field.getName(), e);
        }
    }

    public static String getStringField(Object owner, String[] fieldNames) {
        Object value = getFieldValue(owner, fieldNames);
        return value != null ? value.toString() : null;
    }

    public static int getIntField(Object owner, String[] fieldNames) {
        Object value = getFieldValue(owner, fieldNames);
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    public static float getFloatField(Object owner, String[] fieldNames) {
        Object value = getFieldValue(owner, fieldNames);
        return value instanceof Number ? ((Number) value).floatValue() : 0.0F;
    }

    public static boolean getBooleanField(Object owner, String[] fieldNames) {
        Object value = getFieldValue(owner, fieldNames);
        return Boolean.TRUE.equals(value);
    }

    public static List<Object> snapshotIterable(Object value) {
        if (value == null) {
            return Collections.emptyList();
        }
        if (value instanceof Iterable) {
            List<Object> result = new ArrayList<Object>();
            for (Object item : (Iterable<?>) value) {
                result.add(item);
            }
            return result;
        }
        Class<?> type = value.getClass();
        if (type.isArray()) {
            int length = java.lang.reflect.Array.getLength(value);
            List<Object> result = new ArrayList<Object>(length);
            for (int i = 0; i < length; i++) {
                result.add(java.lang.reflect.Array.get(value, i));
            }
            return result;
        }
        return Collections.singletonList(value);
    }

    public static boolean isAnyClass(Class<?> type, String[] classNames) {
        for (String className : classNames) {
            Class<?> expected = tryFindClass(className);
            if (expected != null && expected.isAssignableFrom(type)) {
                return true;
            }
        }
        return false;
    }

    public static Class<?> findClass(String[] classNames) {
        for (String className : classNames) {
            Class<?> type = tryFindClass(className);
            if (type != null) {
                return type;
            }
        }
        throw new IllegalStateException("Could not find any class: " + join(classNames));
    }

    public static Class<?> tryFindClass(String className) {
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        if (contextLoader != null) {
            try {
                return Class.forName(className, false, contextLoader);
            } catch (ClassNotFoundException ignored) {
            }
        }

        try {
            return Class.forName(className, false, RustedReflection.class.getClassLoader());
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    private static Object invoke(Method method, Object owner, Object[] args) {
        try {
            return method.invoke(owner, args);
        } catch (InvocationTargetException e) {
            throwUnchecked("Could not invoke method " + method.getName(), e);
            return null;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not invoke method " + method.getName(), e);
        }
    }

    private static void throwUnchecked(String message, InvocationTargetException e) {
        Throwable cause = e.getCause();
        if (cause instanceof RuntimeException) {
            throw (RuntimeException) cause;
        }
        if (cause instanceof Error) {
            throw (Error) cause;
        }
        throw new IllegalStateException(message + ": " + describeThrowable(cause), cause);
    }

    private static String describeThrowable(Throwable throwable) {
        if (throwable == null) {
            return "<no cause>";
        }
        String message = throwable.getMessage();
        return message == null || message.trim().isEmpty()
                ? throwable.getClass().getName()
                : throwable.getClass().getName() + ": " + message;
    }

    private static Constructor<?> findConstructor(Class<?> type, Object[] args) {
        for (Constructor<?> constructor : type.getDeclaredConstructors()) {
            if (parametersMatch(constructor.getParameterTypes(), args)) {
                constructor.setAccessible(true);
                return constructor;
            }
        }
        throw new IllegalStateException("Could not find constructor on " + type.getName());
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

    private static Field findField(Class<?> type, String[] names) {
        Class<?> current = type;
        while (current != null) {
            for (String name : names) {
                DeclaredFieldLookup lookup = declaredField(current, name);
                if (lookup.field != null) return lookup.field;
            }
            current = current.getSuperclass();
        }
        throw new IllegalStateException("Could not find field " + join(names) + " on " + type.getName());
    }

    private static DeclaredFieldLookup declaredField(Class<?> owner, String name) {
        ConcurrentMap<String, DeclaredFieldLookup> fields = DECLARED_FIELDS.get(owner);
        DeclaredFieldLookup cached = fields.get(name);
        if (cached != null) return cached;

        DeclaredFieldLookup resolved;
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            resolved = new DeclaredFieldLookup(field);
        } catch (NoSuchFieldException ignored) {
            resolved = DeclaredFieldLookup.MISSING;
        }
        DeclaredFieldLookup raced = fields.putIfAbsent(name, resolved);
        return raced == null ? resolved : raced;
    }

    private static boolean parametersMatch(Class<?>[] parameterTypes, Object[] args) {
        if (parameterTypes.length != args.length) {
            return false;
        }
        for (int i = 0; i < parameterTypes.length; i++) {
            Object arg = args[i];
            if (arg == null) {
                if (parameterTypes[i].isPrimitive()) {
                    return false;
                }
                continue;
            }
            Class<?> parameterType = wrap(parameterTypes[i]);
            if (!parameterType.isAssignableFrom(arg.getClass())) {
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

    private static boolean contains(String[] values, String value) {
        for (String item : values) {
            if (item.equals(value)) {
                return true;
            }
        }
        return false;
    }

    private static void requireNonNull(Object value, String label) {
        if (value == null) {
            throw new IllegalArgumentException(label + " must not be null");
        }
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

    private static final class DeclaredFieldLookup {
        static final DeclaredFieldLookup MISSING = new DeclaredFieldLookup(null);

        final Field field;

        DeclaredFieldLookup(Field field) {
            this.field = field;
        }
    }
}
