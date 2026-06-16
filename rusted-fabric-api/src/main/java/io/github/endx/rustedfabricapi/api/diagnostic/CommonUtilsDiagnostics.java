package io.github.endx.rustedfabricapi.api.diagnostic;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class CommonUtilsDiagnostics {
    private static final String[] COMMON_UTILS_CLASSES = {
            "rustedwarfare.util.CommonUtils",
            "com.corrodinggames.rts.gameFramework.f"
    };
    private static final String[] CPU_CORE_FILE_FILTER_CLASSES = {
            "rustedwarfare.util.CommonUtils$CpuCoreFileFilter",
            "com.corrodinggames.rts.gameFramework.f$a"
    };

    private CommonUtilsDiagnostics() {
    }

    public static boolean isCommonUtilsAvailable() {
        return RustedReflection.tryFindClass(COMMON_UTILS_CLASSES[0]) != null
                || RustedReflection.tryFindClass(COMMON_UTILS_CLASSES[1]) != null;
    }

    public static boolean isCpuCoreFileFilter(Object value) {
        return value != null && RustedReflection.isAnyClass(value.getClass(), CPU_CORE_FILE_FILTER_CLASSES);
    }

    public static Map<String, Object> describeCommonUtilsState() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("available", Boolean.valueOf(isCommonUtilsAvailable()));
        putStaticField(result, "random", new String[]{"random", "a"});
        putStaticField(result, "deterministicRandom", new String[]{"deterministicRandom", "b"});
        putStaticField(result, "scratchPoint", new String[]{"scratchPoint", "c"});
        putStaticField(result, "scratchPoint2", new String[]{"scratchPoint2", "d"});
        putStaticArrayLength(result, "sqrtIntLookupLength", new String[]{"sqrtIntLookup", "j"});
        putStaticArrayLength(result, "alphaNumericCharsLength", new String[]{"alphaNumericChars", "k"});
        putStaticArrayLength(result, "sinTableLength", new String[]{"sinTable", "t"});
        putStaticArrayLength(result, "cosTableLength", new String[]{"cosTable", "u"});
        putStaticArrayLength(result, "atan2Lookup0Length", new String[]{"atan2Lookup0", "l"});
        putStaticIntField(result, "atan2FallbackLogCount", new String[]{"atan2FallbackLogCount", "i"});
        return Collections.unmodifiableMap(result);
    }

    public static Object newCpuCoreFileFilter() {
        return RustedReflection.newInstance(CPU_CORE_FILE_FILTER_CLASSES);
    }

    public static boolean acceptCpuCoreFile(Object filter, File file) {
        requireCpuCoreFileFilter(filter);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(filter, new String[]{"accept"}, file));
    }

    public static void resetDeterministicRandom() {
        invoke(new String[]{"resetDeterministicRandom", "a"});
    }

    public static int deterministicRandomIntForUnit(Object unit, int min, int max) {
        return intValue(invoke(new String[]{"deterministicRandomIntForUnit", "a"},
                unit, Integer.valueOf(min), Integer.valueOf(max)));
    }

    public static float deterministicRandomFloatForUnit(Object unit, float min, float max, int salt) {
        return floatValue(invoke(new String[]{"deterministicRandomFloatForUnit", "a"},
                unit, Float.valueOf(min), Float.valueOf(max), Integer.valueOf(salt)));
    }

    public static float deterministicRandomFloatForUnitNoNullCheck(Object unit, float min, float max, int salt) {
        return floatValue(invoke(new String[]{"deterministicRandomFloatForUnitNoNullCheck", "b"},
                unit, Float.valueOf(min), Float.valueOf(max), Integer.valueOf(salt)));
    }

    public static int deterministicRandomIntForObject(Object gameObject, int min, int max, int salt) {
        return intValue(invoke(new String[]{"deterministicRandomIntForObject", "a"},
                gameObject, Integer.valueOf(min), Integer.valueOf(max), Integer.valueOf(salt)));
    }

    public static float deterministicRandomFloat2dp(float min, float max, int salt) {
        return floatValue(invoke(new String[]{"deterministicRandomFloat2dp", "a"},
                Float.valueOf(min), Float.valueOf(max), Integer.valueOf(salt)));
    }

    public static float deterministicRandomFloat3dp(float min, float max, int salt) {
        return floatValue(invoke(new String[]{"deterministicRandomFloat3dp", "b"},
                Float.valueOf(min), Float.valueOf(max), Integer.valueOf(salt)));
    }

    public static int deterministicRandomInt(int min, int max, int salt) {
        return intValue(invoke(new String[]{"deterministicRandomInt", "a"},
                Integer.valueOf(min), Integer.valueOf(max), Integer.valueOf(salt)));
    }

    public static float randomFloat(float min, float max) {
        return floatValue(invoke(new String[]{"randomFloat", "c"}, Float.valueOf(min), Float.valueOf(max)));
    }

    public static int randomInt(int maxExclusive) {
        return intValue(invoke(new String[]{"randomInt", "c"}, Integer.valueOf(maxExclusive)));
    }

    public static int randomIntBetweenInclusive(int min, int max) {
        return intValue(invoke(new String[]{"randomIntBetweenInclusive", "a"},
                Integer.valueOf(min), Integer.valueOf(max)));
    }

    public static String randomAlphaNumericString(int length) {
        return stringValue(invoke(new String[]{"randomAlphaNumericString", "e"}, Integer.valueOf(length)));
    }

    public static String randomUuid() {
        return stringValue(invoke(new String[]{"randomUuid", "b"}));
    }

    public static float sqrt(float value) {
        return floatValue(invoke(new String[]{"sqrt", "a"}, Float.valueOf(value)));
    }

    public static int sqrtIntCached(int value) {
        return intValue(invoke(new String[]{"sqrtIntCached", "a"}, Integer.valueOf(value)));
    }

    public static float approachZero(float value, float step) {
        return floatValue(invoke(new String[]{"approachZero", "a"}, Float.valueOf(value), Float.valueOf(step)));
    }

    public static float approach(float value, float target, float step) {
        return floatValue(invoke(new String[]{"approach", "a"},
                Float.valueOf(value), Float.valueOf(target), Float.valueOf(step)));
    }

    public static float clampMagnitude(float value, float maxMagnitude) {
        return floatValue(invoke(new String[]{"clampMagnitude", "b"},
                Float.valueOf(value), Float.valueOf(maxMagnitude)));
    }

    public static float clamp(float value, float min, float max) {
        return floatValue(invoke(new String[]{"clamp", "b"},
                Float.valueOf(value), Float.valueOf(min), Float.valueOf(max)));
    }

    public static int clampInt(int value, int min, int max) {
        return intValue(invoke(new String[]{"clampInt", "b"},
                Integer.valueOf(value), Integer.valueOf(min), Integer.valueOf(max)));
    }

    public static int clampByte(int value) {
        return intValue(invoke(new String[]{"clampByte", "b"}, Integer.valueOf(value)));
    }

    public static void rotatePointAround(float x, float y, float angleDegrees, Object pointF) {
        invoke(new String[]{"rotatePointAround", "a"},
                Float.valueOf(x), Float.valueOf(y), Float.valueOf(angleDegrees), pointF);
    }

    public static float distanceSquared(float x1, float y1, float x2, float y2) {
        return floatValue(invoke(new String[]{"distanceSquared", "a"},
                Float.valueOf(x1), Float.valueOf(y1), Float.valueOf(x2), Float.valueOf(y2)));
    }

    public static float distance(float x1, float y1, float x2, float y2) {
        return floatValue(invoke(new String[]{"distance", "b"},
                Float.valueOf(x1), Float.valueOf(y1), Float.valueOf(x2), Float.valueOf(y2)));
    }

    public static int distanceInt(float x1, float y1, float x2, float y2) {
        return intValue(invoke(new String[]{"distanceInt", "c"},
                Float.valueOf(x1), Float.valueOf(y1), Float.valueOf(x2), Float.valueOf(y2)));
    }

    public static int tileDistanceChebyshev(int x1, int y1, int x2, int y2) {
        return intValue(invoke(new String[]{"tileDistanceChebyshev", "a"},
                Integer.valueOf(x1), Integer.valueOf(y1), Integer.valueOf(x2), Integer.valueOf(y2)));
    }

    public static float normalizeAngle(float angleDegrees, boolean allowNegative) {
        return floatValue(invoke(new String[]{"normalizeAngle", "a"},
                Float.valueOf(angleDegrees), Boolean.valueOf(allowNegative)));
    }

    public static float clampAngleDelta(float angle, float target, float maxDelta) {
        return floatValue(invoke(new String[]{"clampAngleDelta", "c"},
                Float.valueOf(angle), Float.valueOf(target), Float.valueOf(maxDelta)));
    }

    public static float angleTo(float x1, float y1, float x2, float y2) {
        return floatValue(invoke(new String[]{"angleTo", "d"},
                Float.valueOf(x1), Float.valueOf(y1), Float.valueOf(x2), Float.valueOf(y2)));
    }

    public static boolean lineSegmentsIntersect(Object a1, Object a2, Object b1, Object b2) {
        return Boolean.TRUE.equals(invoke(new String[]{"lineSegmentsIntersect", "a"}, a1, a2, b1, b2));
    }

    public static void normalizeRect(Object rect) {
        invoke(new String[]{"normalizeRect", "a"}, rect);
    }

    public static void normalizeRectF(Object rectF) {
        invoke(new String[]{"normalizeRectF", "a"}, rectF);
    }

    public static float radiansToDegrees(float radians) {
        return floatValue(invoke(new String[]{"radiansToDegrees", "b"}, Float.valueOf(radians)));
    }

    public static float pow(float value, float exponent) {
        return floatValue(invoke(new String[]{"pow", "e"}, Float.valueOf(value), Float.valueOf(exponent)));
    }

    public static double abs(double value) {
        return doubleValue(invoke(new String[]{"abs", "a"}, Double.valueOf(value)));
    }

    public static float abs(float value) {
        return floatValue(invoke(new String[]{"abs", "c"}, Float.valueOf(value)));
    }

    public static int abs(int value) {
        return intValue(invoke(new String[]{"abs", "d"}, Integer.valueOf(value)));
    }

    public static int max(int a, int b) {
        return intValue(invoke(new String[]{"max", "b"}, Integer.valueOf(a), Integer.valueOf(b)));
    }

    public static int min(int a, int b) {
        return intValue(invoke(new String[]{"min", "c"}, Integer.valueOf(a), Integer.valueOf(b)));
    }

    public static float max(float a, float b) {
        return floatValue(invoke(new String[]{"max", "f"}, Float.valueOf(a), Float.valueOf(b)));
    }

    public static float min(float a, float b) {
        return floatValue(invoke(new String[]{"min", "g"}, Float.valueOf(a), Float.valueOf(b)));
    }

    public static double min(double a, double b) {
        return doubleValue(invoke(new String[]{"min", "a"}, Double.valueOf(a), Double.valueOf(b)));
    }

    public static boolean almostEqual005(float a, float b) {
        return Boolean.TRUE.equals(invoke(new String[]{"almostEqual005", "h"}, Float.valueOf(a), Float.valueOf(b)));
    }

    public static boolean absDifferenceWithin(float a, float b, float range) {
        return Boolean.TRUE.equals(invoke(new String[]{"absDifferenceWithin", "e"},
                Float.valueOf(a), Float.valueOf(b), Float.valueOf(range)));
    }

    public static float roundToFloat(float value) {
        return floatValue(invoke(new String[]{"roundToFloat", "d"}, Float.valueOf(value)));
    }

    public static float ceilToFloat(float value) {
        return floatValue(invoke(new String[]{"ceilToFloat", "e"}, Float.valueOf(value)));
    }

    public static int floorToInt(float value) {
        return intValue(invoke(new String[]{"floorToInt", "f"}, Float.valueOf(value)));
    }

    public static void expandRectF(Object rectF, float amount) {
        invoke(new String[]{"expandRectF", "a"}, rectF, Float.valueOf(amount));
    }

    public static void expandRect(Object rect, float amount) {
        invoke(new String[]{"expandRect", "a"}, rect, Float.valueOf(amount));
    }

    public static float lerp(float start, float end, float amount) {
        return floatValue(invoke(new String[]{"lerp", "f"},
                Float.valueOf(start), Float.valueOf(end), Float.valueOf(amount)));
    }

    public static float easeInOutQuad(float amount) {
        return floatValue(invoke(new String[]{"easeInOutQuad", "i"}, Float.valueOf(amount)));
    }

    public static int lerpColor(int startColor, int endColor, float amount) {
        return intValue(invoke(new String[]{"lerpColor", "a"},
                Integer.valueOf(startColor), Integer.valueOf(endColor), Float.valueOf(amount)));
    }

    public static boolean rectOverlapsRectF(Object rect, Object rectF) {
        return Boolean.TRUE.equals(invoke(new String[]{"rectOverlapsRectF", "a"}, rect, rectF));
    }

    public static boolean rectFOverlaps(Object firstRectF, Object secondRectF) {
        return Boolean.TRUE.equals(invoke(new String[]{"rectFOverlaps", "a"}, firstRectF, secondRectF));
    }

    public static int argb(int alpha, int red, int green, int blue) {
        return intValue(invoke(new String[]{"argb", "b"},
                Integer.valueOf(alpha), Integer.valueOf(red), Integer.valueOf(green), Integer.valueOf(blue)));
    }

    public static float fastAtan2(float y, float x) {
        return floatValue(invoke(new String[]{"fastAtan2", "i"}, Float.valueOf(y), Float.valueOf(x)));
    }

    public static float fastSin(float degrees) {
        return floatValue(invoke(new String[]{"fastSin", "j"}, Float.valueOf(degrees)));
    }

    public static float fastCos(float degrees) {
        return floatValue(invoke(new String[]{"fastCos", "k"}, Float.valueOf(degrees)));
    }

    public static boolean almostEqual(float a, float b) {
        return Boolean.TRUE.equals(invoke(new String[]{"almostEqual", "j"}, Float.valueOf(a), Float.valueOf(b)));
    }

    public static boolean almostEqualTiny(float a, float b) {
        return Boolean.TRUE.equals(invoke(new String[]{"almostEqualTiny", "k"}, Float.valueOf(a), Float.valueOf(b)));
    }

    public static boolean almostEqualDouble(double a, double b) {
        return Boolean.TRUE.equals(invoke(new String[]{"almostEqualDouble", "b"}, Double.valueOf(a), Double.valueOf(b)));
    }

    public static String formatCurrentDate(String pattern) {
        return stringValue(invoke(new String[]{"formatCurrentDate", "a"}, pattern));
    }

    public static String booleanToString(boolean value) {
        return stringValue(invoke(new String[]{"booleanToString", "a"}, Boolean.valueOf(value)));
    }

    public static String formatNumberOrInteger(double value) {
        return stringValue(invoke(new String[]{"formatNumberOrInteger", "b"}, Double.valueOf(value)));
    }

    public static String formatFloat2dp(float value) {
        return stringValue(invoke(new String[]{"formatFloat2dp", "g"}, Float.valueOf(value)));
    }

    public static String formatDouble2dp(double value) {
        return stringValue(invoke(new String[]{"formatDouble2dp", "c"}, Double.valueOf(value)));
    }

    public static String formatFloat(float value, int decimals) {
        return stringValue(invoke(new String[]{"formatFloat", "a"}, Float.valueOf(value), Integer.valueOf(decimals)));
    }

    public static String formatDouble(double value, int decimals) {
        return stringValue(invoke(new String[]{"formatDouble", "a"}, Double.valueOf(value), Integer.valueOf(decimals)));
    }

    public static String formatSeconds(float seconds) {
        return stringValue(invoke(new String[]{"formatSeconds", "h"}, Float.valueOf(seconds)));
    }

    public static String truncateDoubleDecimalString(double value, int decimals) {
        return stringValue(invoke(new String[]{"truncateDoubleDecimalString", "b"},
                Double.valueOf(value), Integer.valueOf(decimals)));
    }

    public static String limitStringLength(String value, int maxLength) {
        return stringValue(invoke(new String[]{"limitStringLength", "a"}, value, Integer.valueOf(maxLength)));
    }

    public static String ellipsizeString(String value, int maxLength) {
        return stringValue(invoke(new String[]{"ellipsizeString", "b"}, value, Integer.valueOf(maxLength)));
    }

    public static String md5Hex(String value) {
        return stringValue(invoke(new String[]{"md5Hex", "b"}, value));
    }

    public static String sha256HexShort14(String value) {
        return stringValue(invoke(new String[]{"sha256HexShort14", "c"}, value));
    }

    public static String sha256HexShort4(String value) {
        return stringValue(invoke(new String[]{"sha256HexShort4", "d"}, value));
    }

    public static String repeatedSha256Hex(String value, int count) {
        return stringValue(invoke(new String[]{"repeatedSha256Hex", "c"}, value, Integer.valueOf(count)));
    }

    public static String sha256Hex(String value) {
        return stringValue(invoke(new String[]{"sha256Hex", "e"}, value));
    }

    public static byte[] sha256BytesFromString(String value) {
        return byteArrayValue(invoke(new String[]{"sha256BytesFromString", "f"}, value));
    }

    public static String bytesToHexUppercase(byte[] bytes) {
        return stringValue(invoke(new String[]{"bytesToHexUppercase", "a"}, bytes));
    }

    public static String sha256HexForBytes(byte[] bytes) {
        return stringValue(invoke(new String[]{"sha256HexForBytes", "b"}, bytes));
    }

    public static byte[] sha256Bytes(byte[] bytes) {
        return byteArrayValue(invoke(new String[]{"sha256Bytes", "c"}, bytes));
    }

    public static String repeatStringInclusive(String value, int count) {
        return stringValue(invoke(new String[]{"repeatStringInclusive", "d"}, value, Integer.valueOf(count)));
    }

    public static String padRightWithSpaces(String value, int length) {
        return stringValue(invoke(new String[]{"padRightWithSpaces", "e"}, value, Integer.valueOf(length)));
    }

    public static String padLeftWithString(String value, int length, String padding) {
        return stringValue(invoke(new String[]{"padLeftWithString", "a"},
                value, Integer.valueOf(length), padding));
    }

    public static String leftAlignString(String value, int length) {
        return stringValue(invoke(new String[]{"leftAlignString", "f"}, value, Integer.valueOf(length)));
    }

    public static String getStaticFieldNameForValue(Class<?> type, int value) {
        return stringValue(invoke(new String[]{"getStaticFieldNameForValue", "a"}, type, Integer.valueOf(value)));
    }

    public static String resourceIdToPath(int resourceId) {
        return stringValue(invoke(new String[]{"resourceIdToPath", "f"}, Integer.valueOf(resourceId)));
    }

    public static String formatFileSize(int bytes) {
        return stringValue(invoke(new String[]{"formatFileSize", "g"}, Integer.valueOf(bytes)));
    }

    public static String colorToHex(int color) {
        return stringValue(invoke(new String[]{"colorToHex", "h"}, Integer.valueOf(color)));
    }

    public static String fileNameWithoutExtension(String path) {
        return stringValue(invoke(new String[]{"fileNameWithoutExtension", "g"}, path));
    }

    public static String parentPath(String path) {
        return stringValue(invoke(new String[]{"parentPath", "h"}, path));
    }

    public static int countChar(String value, char search) {
        return intValue(invoke(new String[]{"countChar", "a"}, value, Character.valueOf(search)));
    }

    public static String escapeXml(String value) {
        return stringValue(invoke(new String[]{"escapeXml", "i"}, value));
    }

    public static String unescapeXml(String value) {
        return stringValue(invoke(new String[]{"unescapeXml", "o"}, value));
    }

    public static String unquoteIfQuoted(String value) {
        return stringValue(invoke(new String[]{"unquoteIfQuoted", "p"}, value));
    }

    public static String removeEscapeSlashes(String value) {
        return stringValue(invoke(new String[]{"removeEscapeSlashes", "q"}, value));
    }

    public static String replaceIfContains(String value, String target, String replacement) {
        return stringValue(invoke(new String[]{"replaceIfContains", "a"}, value, target, replacement));
    }

    public static boolean contains(String value, String target) {
        return Boolean.TRUE.equals(invoke(new String[]{"contains", "c"}, value, target));
    }

    public static boolean containsChar(String value, char target) {
        return Boolean.TRUE.equals(invoke(new String[]{"containsChar", "b"}, value, Character.valueOf(target)));
    }

    public static String[] split(String value, char delimiter) {
        Object result = invoke(new String[]{"split", "c"}, value, Character.valueOf(delimiter));
        return result instanceof String[] ? (String[]) result : new String[0];
    }

    public static boolean isLooseNumberString(String value) {
        return Boolean.TRUE.equals(invoke(new String[]{"isLooseNumberString", "r"}, value));
    }

    public static boolean isStrictNumberString(String value) {
        return Boolean.TRUE.equals(invoke(new String[]{"isStrictNumberString", "s"}, value));
    }

    public static String removeTrailingNewline(String value) {
        return stringValue(invoke(new String[]{"removeTrailingNewline", "j"}, value));
    }

    public static String removeSuffix(String value, String suffix) {
        return stringValue(invoke(new String[]{"removeSuffix", "a"}, value, suffix));
    }

    public static String fileName(String path) {
        return stringValue(invoke(new String[]{"fileName", "k"}, path));
    }

    public static String joinPath(String first, String second) {
        return stringValue(invoke(new String[]{"joinPath", "b"}, first, second));
    }

    public static String join(CharSequence delimiter, Iterable<?> values) {
        return stringValue(invoke(new String[]{"join", "a"}, delimiter, values));
    }

    public static Integer parseIntegerOrNull(String value) {
        Object result = invoke(new String[]{"parseIntegerOrNull", "l"}, value);
        return result instanceof Integer ? (Integer) result : null;
    }

    public static Long parseLongOrNull(String value) {
        Object result = invoke(new String[]{"parseLongOrNull", "m"}, value);
        return result instanceof Long ? (Long) result : null;
    }

    public static boolean containsNonAscii(String value) {
        return Boolean.TRUE.equals(invoke(new String[]{"containsNonAscii", "n"}, value));
    }

    public static boolean equalsNullable(String first, String second) {
        return Boolean.TRUE.equals(invoke(new String[]{"equalsNullable", "d"}, first, second));
    }

    public static boolean equalsNullable(Integer first, Integer second) {
        return Boolean.TRUE.equals(invoke(new String[]{"equalsNullable", "a"}, first, second));
    }

    public static void copyStream(InputStream inputStream, OutputStream outputStream) {
        invoke(new String[]{"copyStream", "a"}, inputStream, outputStream);
    }

    public static String readStreamToStringAndClose(InputStream inputStream) {
        return stringValue(invoke(new String[]{"readStreamToStringAndClose", "a"}, inputStream));
    }

    public static String readFileUtf8(File file) {
        return stringValue(invoke(new String[]{"readFileUtf8", "a"}, file));
    }

    public static String readInputStreamUtf8AndClose(InputStream inputStream) {
        return stringValue(invoke(new String[]{"readInputStreamUtf8AndClose", "b"}, inputStream));
    }

    public static String stackTraceToString(Exception exception) {
        return stringValue(invoke(new String[]{"stackTraceToString", "a"}, exception));
    }

    public static String exceptionMessage(Exception exception) {
        return stringValue(invoke(new String[]{"exceptionMessage", "b"}, exception));
    }

    public static String exceptionMessage(Exception exception, boolean includeStackTrace) {
        return stringValue(invoke(new String[]{"exceptionMessage", "a"},
                exception, Boolean.valueOf(includeStackTrace)));
    }

    public static int getCpuCoreCount() {
        return intValue(invoke(new String[]{"getCpuCoreCount", "c"}));
    }

    public static void copyByteArray(byte[] source, byte[] target) {
        invoke(new String[]{"copyByteArray", "a"}, source, target);
    }

    public static long elapsedMillisFromNanos(long startNanos, long endNanos) {
        return longValue(invoke(new String[]{"elapsedMillisFromNanos", "a"},
                Long.valueOf(startNanos), Long.valueOf(endNanos)));
    }

    public static String formatDurationHms(long seconds) {
        return stringValue(invoke(new String[]{"formatDurationHms", "a"}, Long.valueOf(seconds)));
    }

    public static int[] secondsToHmsParts(long seconds) {
        Object result = invoke(new String[]{"secondsToHmsParts", "b"}, Long.valueOf(seconds));
        return result instanceof int[] ? (int[]) result : new int[0];
    }

    private static Object invoke(String[] methodNames, Object... args) {
        return RustedReflection.invokeStatic(COMMON_UTILS_CLASSES, methodNames, args);
    }

    private static void requireCpuCoreFileFilter(Object value) {
        if (!isCpuCoreFileFilter(value)) {
            throw new IllegalArgumentException("Expected CpuCoreFileFilter, got " + describe(value));
        }
    }

    private static void putStaticField(Map<String, Object> result, String key, String[] fieldNames) {
        try {
            result.put(key, RustedReflection.getStaticFieldValue(COMMON_UTILS_CLASSES, fieldNames));
        } catch (RuntimeException ignored) {
        }
    }

    private static void putStaticIntField(Map<String, Object> result, String key, String[] fieldNames) {
        try {
            Object value = RustedReflection.getStaticFieldValue(COMMON_UTILS_CLASSES, fieldNames);
            result.put(key, Integer.valueOf(intValue(value)));
        } catch (RuntimeException ignored) {
        }
    }

    private static void putStaticArrayLength(Map<String, Object> result, String key, String[] fieldNames) {
        try {
            Object value = RustedReflection.getStaticFieldValue(COMMON_UTILS_CLASSES, fieldNames);
            result.put(key, Integer.valueOf(arrayLength(value)));
        } catch (RuntimeException ignored) {
        }
    }

    private static int arrayLength(Object value) {
        return value != null && value.getClass().isArray() ? Array.getLength(value) : 0;
    }

    private static String stringValue(Object value) {
        return value != null ? value.toString() : null;
    }

    private static int intValue(Object value) {
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    private static long longValue(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    private static float floatValue(Object value) {
        return value instanceof Number ? ((Number) value).floatValue() : 0.0F;
    }

    private static double doubleValue(Object value) {
        return value instanceof Number ? ((Number) value).doubleValue() : 0.0D;
    }

    private static byte[] byteArrayValue(Object value) {
        return value instanceof byte[] ? (byte[]) value : new byte[0];
    }

    private static String describe(Object value) {
        return value == null ? "null" : value.getClass().getName();
    }
}
