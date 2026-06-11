package io.github.endx.rustedfabricapi.api.diagnostic;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

public final class OrderRuntimeDiagnostics {
    private static final String[] UNIT_CLASSES = {
            "rustedwarfare.unit.Unit",
            "com.corrodinggames.rts.game.units.am"
    };
    private static final String[] ORDERABLE_UNIT_CLASSES = {
            "rustedwarfare.unit.OrderableUnit",
            "com.corrodinggames.rts.game.units.y"
    };
    private static final String[] UNIT_ATTACK_MODE_CLASSES = {
            "rustedwarfare.unit.UnitAttackMode",
            "com.corrodinggames.rts.game.units.a"
    };
    private static final String[] TURRET_RUNTIME_STATE_CLASSES = {
            "rustedwarfare.unit.combat.TurretRuntimeState",
            "com.corrodinggames.rts.game.units.ap"
    };

    private static final EnumAlias[] ATTACK_MODE_ALIASES = {
            new EnumAlias("outOfRange", new String[]{"outOfRange", "a"}),
            new EnumAlias("onlyInRange", new String[]{"onlyInRange", "b"}),
            new EnumAlias("returnFire", new String[]{"returnFire", "c"}),
            new EnumAlias("holdFire", new String[]{"holdFire", "d"}),
            new EnumAlias("guardArea", new String[]{"guardArea", "e"}),
            new EnumAlias("aggressive", new String[]{"aggressive", "f"}),
            new EnumAlias("mixed", new String[]{"mixed", "g"})
    };

    private OrderRuntimeDiagnostics() {
    }

    public static Map<String, Object> describeOrderRuntime(Object unit) {
        requireOrderableUnit(unit);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putFloatField(result, unit, "activeOrderElapsedTime", new String[]{"activeOrderElapsedTime", "V"});
        putBooleanField(result, unit, "pathRepathRequested", new String[]{"pathRepathRequested", "j"});
        putBooleanField(result, unit, "hasMoveTarget", new String[]{"hasMoveTarget", "k"});
        putFloatField(result, unit, "moveTargetX", new String[]{"moveTargetX", "l"});
        putFloatField(result, unit, "moveTargetY", new String[]{"moveTargetY", "m"});
        putFloatField(result, unit, "repathDelayTimer", new String[]{"repathDelayTimer", "s"});
        putFloatField(result, unit, "standoffFacingAngle", new String[]{"standoffFacingAngle", "aq"});
        putBooleanField(result, unit, "standoffRetreatActive", new String[]{"standoffRetreatActive", "ar"});
        putBooleanField(result, unit, "attackMoveSearchFailed", new String[]{"attackMoveSearchFailed", "as"});
        putField(result, unit, "orderUpdateStateScratch", new String[]{"orderUpdateStateScratch", "aP"});
        putField(result, unit, "actionExecutionResultScratch", new String[]{"actionExecutionResultScratch", "bn"});
        putOptional(result, "orderUpdateStateScratchDescription", new Supplier<Object>() {
            @Override
            public Object get() {
                Object state = RustedReflection.getFieldValue(unit, new String[]{"orderUpdateStateScratch", "aP"});
                return state != null ? WaypointDiagnostics.describeWaypointUpdateState(state) : null;
            }
        });
        putOptional(result, "actionExecutionResultScratchDescription", new Supplier<Object>() {
            @Override
            public Object get() {
                Object actionResult = RustedReflection.getFieldValue(unit,
                        new String[]{"actionExecutionResultScratch", "bn"});
                return actionResult != null ? WaypointDiagnostics.describeActionExecutionResult(actionResult) : null;
            }
        });
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeCombatRuntime(Object unit) {
        requireOrderableUnit(unit);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        Object attackMode = RustedReflection.getFieldValue(unit, new String[]{"attackMode", "P"});
        result.put("attackMode", attackMode);
        result.put("attackModeName", canonicalAliasName(UNIT_ATTACK_MODE_CLASSES, ATTACK_MODE_ALIASES, attackMode));
        putField(result, unit, "attackTarget", new String[]{"attackTarget", "R"});
        putFloatField(result, unit, "autoTargetSearchTimer", new String[]{"autoTargetSearchTimer", "S"});
        putFloatField(result, unit, "turretTargetRefreshTimer", new String[]{"turretTargetRefreshTimer", "T"});
        putFloatField(result, unit, "attackEffectTimer", new String[]{"attackEffectTimer", "U"});
        putArrayLengthField(result, unit, "turretStatesLength", new String[]{"turretStates", "cL"});
        putOptional(result, "hasAttackTarget", new Supplier<Object>() {
            @Override
            public Object get() {
                return Boolean.valueOf(hasAttackTarget(unit));
            }
        });
        putOptional(result, "hasFiringTurretTarget", new Supplier<Object>() {
            @Override
            public Object get() {
                return Boolean.valueOf(hasFiringTurretTarget(unit));
            }
        });
        putOptional(result, "currentTargetUnit", new Supplier<Object>() {
            @Override
            public Object get() {
                return getCurrentTargetUnit(unit);
            }
        });
        putOptional(result, "canUpdateAutoTargeting", new Supplier<Object>() {
            @Override
            public Object get() {
                return Boolean.valueOf(canUpdateAutoTargeting(unit));
            }
        });
        putOptional(result, "autoAttackSearchRange", new Supplier<Object>() {
            @Override
            public Object get() {
                return Float.valueOf(getAutoAttackSearchRange(unit, false));
            }
        });
        putOptional(result, "autoAttackSearchRangeWithExtra", new Supplier<Object>() {
            @Override
            public Object get() {
                return Float.valueOf(getAutoAttackSearchRange(unit, true));
            }
        });
        putOptional(result, "turretCount", new Supplier<Object>() {
            @Override
            public Object get() {
                return Integer.valueOf(getTurretCount(unit));
            }
        });
        return Collections.unmodifiableMap(result);
    }

    public static List<Object> turretStatesSnapshot(Object unit) {
        requireUnit(unit);
        return Collections.unmodifiableList(arraySnapshot(
                RustedReflection.getFieldValue(unit, new String[]{"turretStates", "cL"})));
    }

    public static List<Map<String, Object>> describeTurretStatesSnapshot(Object unit) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Object turretState : turretStatesSnapshot(unit)) {
            if (turretState != null) {
                result.add(describeTurretRuntimeState(turretState));
            }
        }
        return Collections.unmodifiableList(result);
    }

    public static Map<String, Object> describeTurretRuntimeState(Object turretState) {
        requireTurretRuntimeState(turretState);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putFloatField(result, turretState, "aimAngle", new String[]{"aimAngle", "a"});
        putFloatField(result, turretState, "previousAimAngle", new String[]{"previousAimAngle", "b"});
        putFloatField(result, turretState, "aimAngularVelocity", new String[]{"aimAngularVelocity", "c"});
        putFloatField(result, turretState, "aimLockTimer", new String[]{"aimLockTimer", "d"});
        putFloatField(result, turretState, "reloadTimer", new String[]{"reloadTimer", "e"});
        putFloatField(result, turretState, "warmupTimer", new String[]{"warmupTimer", "f"});
        putBooleanField(result, turretState, "aimReady", new String[]{"aimReady", "g"});
        putFloatField(result, turretState, "shotSpreadOffsetX", new String[]{"shotSpreadOffsetX", "h"});
        putFloatField(result, turretState, "shotSpreadOffsetY", new String[]{"shotSpreadOffsetY", "i"});
        putField(result, turretState, "target", new String[]{"target", "j"});
        putBooleanField(result, turretState, "alternateFireSide", new String[]{"alternateFireSide", "m"});
        putOptional(result, "aimLockClear", new Supplier<Object>() {
            @Override
            public Object get() {
                return Boolean.valueOf(isAimLockClear(turretState));
            }
        });
        putOptional(result, "negativeAimLock", new Supplier<Object>() {
            @Override
            public Object get() {
                return Boolean.valueOf(hasNegativeAimLock(turretState));
            }
        });
        return Collections.unmodifiableMap(result);
    }

    public static boolean hasAttackTarget(Object unit) {
        requireOrderableUnit(unit);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(unit, new String[]{"hasAttackTarget", "Z"}));
    }

    public static boolean hasFiringTurretTarget(Object unit) {
        requireOrderableUnit(unit);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(unit,
                new String[]{"hasFiringTurretTarget", "aa"}));
    }

    public static Object getCurrentTargetUnit(Object unit) {
        requireOrderableUnit(unit);
        return RustedReflection.invokeInstance(unit, new String[]{"getCurrentTargetUnit", "ab"});
    }

    public static boolean canUpdateAutoTargeting(Object unit) {
        requireOrderableUnit(unit);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(unit,
                new String[]{"canUpdateAutoTargeting", "ad"}));
    }

    public static float getAutoAttackSearchRange(Object unit, boolean includeExtraRange) {
        requireOrderableUnit(unit);
        Object value = RustedReflection.invokeInstance(unit, new String[]{"getAutoAttackSearchRange", "b"},
                Boolean.valueOf(includeExtraRange));
        return value instanceof Number ? ((Number) value).floatValue() : 0.0F;
    }

    public static int getTurretCount(Object unit) {
        requireOrderableUnit(unit);
        Object value = RustedReflection.invokeInstance(unit, new String[]{"getTurretCount", "bl"});
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    public static boolean isTurretAimedAtTarget(Object unit, int turretIndex) {
        requireOrderableUnit(unit);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(unit,
                new String[]{"isTurretAimedAtTarget", "u"}, Integer.valueOf(turretIndex)));
    }

    public static Map<String, Object> describeTurretFireAccessors(final Object unit, final int turretIndex) {
        requireOrderableUnit(unit);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("turretIndex", Integer.valueOf(turretIndex));
        putOptional(result, "fireDelay", new Supplier<Object>() {
            @Override
            public Object get() {
                return Float.valueOf(getTurretFireDelay(unit, turretIndex));
            }
        });
        putOptional(result, "warmupTime", new Supplier<Object>() {
            @Override
            public Object get() {
                return Float.valueOf(getTurretWarmupTime(unit, turretIndex));
            }
        });
        putOptional(result, "warmupNoReset", new Supplier<Object>() {
            @Override
            public Object get() {
                return Boolean.valueOf(isTurretWarmupNoReset(unit, turretIndex));
            }
        });
        putOptional(result, "warmupShootDelayTransfer", new Supplier<Object>() {
            @Override
            public Object get() {
                return Float.valueOf(getTurretWarmupShootDelayTransfer(unit, turretIndex));
            }
        });
        putOptional(result, "canFire", new Supplier<Object>() {
            @Override
            public Object get() {
                return Boolean.valueOf(canTurretFire(unit, turretIndex));
            }
        });
        putOptional(result, "linkedDelayTurretIndex", new Supplier<Object>() {
            @Override
            public Object get() {
                return Integer.valueOf(getLinkedDelayTurretIndex(unit, turretIndex));
            }
        });
        putOptional(result, "projectileDirectDamage", new Supplier<Object>() {
            @Override
            public Object get() {
                return Float.valueOf(getTurretProjectileDirectDamage(unit, turretIndex));
            }
        });
        putOptional(result, "barrelForwardOffset", new Supplier<Object>() {
            @Override
            public Object get() {
                return Float.valueOf(getTurretBarrelForwardOffset(unit, turretIndex));
            }
        });
        putOptional(result, "muzzlePoint3D", new Supplier<Object>() {
            @Override
            public Object get() {
                return getTurretMuzzlePoint3D(unit, turretIndex);
            }
        });
        putOptional(result, "muzzlePoint", new Supplier<Object>() {
            @Override
            public Object get() {
                return getTurretMuzzlePoint(unit, turretIndex);
            }
        });
        putOptional(result, "basePoint3D", new Supplier<Object>() {
            @Override
            public Object get() {
                return getTurretBasePoint3D(unit, turretIndex);
            }
        });
        putOptional(result, "worldPoint", new Supplier<Object>() {
            @Override
            public Object get() {
                return getTurretWorldPoint(unit, turretIndex);
            }
        });
        putOptional(result, "recoilOffset", new Supplier<Object>() {
            @Override
            public Object get() {
                return Float.valueOf(getTurretRecoilOffset(unit, turretIndex));
            }
        });
        putOptional(result, "recoilOutTime", new Supplier<Object>() {
            @Override
            public Object get() {
                return Float.valueOf(getTurretRecoilOutTime(unit, turretIndex));
            }
        });
        putOptional(result, "recoilReturnTime", new Supplier<Object>() {
            @Override
            public Object get() {
                return Float.valueOf(getTurretRecoilReturnTime(unit, turretIndex));
            }
        });
        putOptional(result, "shotSpreadOffset", new Supplier<Object>() {
            @Override
            public Object get() {
                return getTurretShotSpreadOffset(unit, turretIndex);
            }
        });
        putOptional(result, "aimOffsetSpread", new Supplier<Object>() {
            @Override
            public Object get() {
                return Float.valueOf(getTurretAimOffsetSpread(unit, turretIndex));
            }
        });
        return Collections.unmodifiableMap(result);
    }

    public static float getTurretFireDelay(Object unit, int turretIndex) {
        return invokeFloat(unit, new String[]{"getTurretFireDelay", "b"}, turretIndex);
    }

    public static float getTurretWarmupTime(Object unit, int turretIndex) {
        return invokeFloat(unit, new String[]{"getTurretWarmupTime", "e"}, turretIndex);
    }

    public static boolean isTurretWarmupNoReset(Object unit, int turretIndex) {
        requireOrderableUnit(unit);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(unit,
                new String[]{"isTurretWarmupNoReset", "s"}, Integer.valueOf(turretIndex)));
    }

    public static float getTurretWarmupShootDelayTransfer(Object unit, int turretIndex) {
        return invokeFloat(unit, new String[]{"getTurretWarmupShootDelayTransfer", "t"}, turretIndex);
    }

    public static boolean canTurretFire(Object unit, int turretIndex) {
        requireOrderableUnit(unit);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(unit,
                new String[]{"canTurretFire", "r"}, Integer.valueOf(turretIndex)));
    }

    public static int getLinkedDelayTurretIndex(Object unit, int turretIndex) {
        requireOrderableUnit(unit);
        Object value = RustedReflection.invokeInstance(unit, new String[]{"getLinkedDelayTurretIndex", "v"},
                Integer.valueOf(turretIndex));
        return value instanceof Number ? ((Number) value).intValue() : -1;
    }

    public static float getTurretProjectileDirectDamage(Object unit, int turretIndex) {
        return invokeFloat(unit, new String[]{"getTurretProjectileDirectDamage", "q"}, turretIndex);
    }

    public static float getTurretBarrelForwardOffset(Object unit, int turretIndex) {
        return invokeFloat(unit, new String[]{"getTurretBarrelForwardOffset", "g"}, turretIndex);
    }

    public static Object getTurretMuzzlePoint3D(Object unit, int turretIndex) {
        return invokeTurretObject(unit, new String[]{"getTurretMuzzlePoint3D", "D"}, turretIndex);
    }

    public static Object getTurretMuzzlePoint(Object unit, int turretIndex) {
        return invokeTurretObject(unit, new String[]{"getTurretMuzzlePoint", "E"}, turretIndex);
    }

    public static Object getTurretBasePoint3D(Object unit, int turretIndex) {
        return invokeTurretObject(unit, new String[]{"getTurretBasePoint3D", "F"}, turretIndex);
    }

    public static Object getTurretWorldPoint(Object unit, int turretIndex) {
        return invokeTurretObject(unit, new String[]{"getTurretWorldPoint", "G"}, turretIndex);
    }

    public static float getTurretRecoilOffset(Object unit, int turretIndex) {
        return invokeFloat(unit, new String[]{"getTurretRecoilOffset", "H"}, turretIndex);
    }

    public static float getTurretRecoilOutTime(Object unit, int turretIndex) {
        return invokeFloat(unit, new String[]{"getTurretRecoilOutTime", "I"}, turretIndex);
    }

    public static float getTurretRecoilReturnTime(Object unit, int turretIndex) {
        return invokeFloat(unit, new String[]{"getTurretRecoilReturnTime", "J"}, turretIndex);
    }

    public static Object getTurretShotSpreadOffset(Object unit, int turretIndex) {
        return invokeTurretObject(unit, new String[]{"getTurretShotSpreadOffset", "K"}, turretIndex);
    }

    public static float getTurretAimOffsetSpread(Object unit, int turretIndex) {
        return invokeFloat(unit, new String[]{"getTurretAimOffsetSpread", "L"}, turretIndex);
    }

    public static boolean isTargetWithinAttackRange(Object unit, Object target) {
        requireOrderableUnit(unit);
        requireUnit(target);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(unit,
                new String[]{"isTargetWithinAttackRange", "h"}, target));
    }

    public static boolean canAutoAttackTarget(Object unit, Object target, boolean checkSearchRange) {
        requireOrderableUnit(unit);
        requireUnit(target);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(unit,
                new String[]{"canAutoAttackTarget", "a"}, target, Boolean.valueOf(checkSearchRange)));
    }

    public static boolean canAutoAttackVisibleTarget(Object unit, Object target, boolean checkSearchRange) {
        requireOrderableUnit(unit);
        requireUnit(target);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(unit,
                new String[]{"canAutoAttackVisibleTarget", "b"}, target, Boolean.valueOf(checkSearchRange)));
    }

    public static boolean isAimLockClear(Object turretState) {
        requireTurretRuntimeState(turretState);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(turretState,
                new String[]{"isAimLockClear", "a"}));
    }

    public static boolean hasNegativeAimLock(Object turretState) {
        requireTurretRuntimeState(turretState);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(turretState,
                new String[]{"hasNegativeAimLock", "b"}));
    }

    public static Map<String, Object> describeOrderRuntimeStatics() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putStaticField(result, "primaryPassiveTargetSearchCallback",
                new String[]{"primaryPassiveTargetSearchCallback", "aQ"});
        putStaticField(result, "fallbackPassiveTargetSearchCallback",
                new String[]{"fallbackPassiveTargetSearchCallback", "aR"});
        putStaticField(result, "primaryTurretTargetSearchCallback",
                new String[]{"primaryTurretTargetSearchCallback", "aS"});
        putStaticField(result, "fallbackTurretTargetSearchCallback",
                new String[]{"fallbackTurretTargetSearchCallback", "aT"});
        putStaticField(result, "nearestUnitSearchCallback", new String[]{"nearestUnitSearchCallback", "bo"});
        return Collections.unmodifiableMap(result);
    }

    private static void requireUnit(Object unit) {
        requireAny(unit, UNIT_CLASSES, "Unit");
    }

    private static void requireOrderableUnit(Object unit) {
        requireAny(unit, ORDERABLE_UNIT_CLASSES, "OrderableUnit");
    }

    private static void requireTurretRuntimeState(Object turretState) {
        requireAny(turretState, TURRET_RUNTIME_STATE_CLASSES, "TurretRuntimeState");
    }

    private static void requireAny(Object value, String[] classNames, String label) {
        if (value == null) {
            throw new IllegalArgumentException(label + " must not be null");
        }
        if (!RustedReflection.isAnyClass(value.getClass(), classNames)) {
            throw new IllegalArgumentException("Expected " + label + ", got " + value.getClass().getName());
        }
    }

    private static void putField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        result.put(key, RustedReflection.getFieldValue(owner, fieldNames));
    }

    private static void putStaticField(Map<String, Object> result, String key, String[] fieldNames) {
        result.put(key, RustedReflection.getStaticFieldValue(ORDERABLE_UNIT_CLASSES, fieldNames));
    }

    private static void putIntField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        result.put(key, Integer.valueOf(RustedReflection.getIntField(owner, fieldNames)));
    }

    private static void putFloatField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        result.put(key, Float.valueOf(RustedReflection.getFloatField(owner, fieldNames)));
    }

    private static void putBooleanField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        result.put(key, Boolean.valueOf(RustedReflection.getBooleanField(owner, fieldNames)));
    }

    private static void putArrayLengthField(Map<String, Object> result, Object owner, String key,
                                            String[] fieldNames) {
        Object value = RustedReflection.getFieldValue(owner, fieldNames);
        result.put(key, Integer.valueOf(value != null && value.getClass().isArray() ? Array.getLength(value) : 0));
    }

    private static void putOptional(Map<String, Object> result, String key, Supplier<Object> valueSupplier) {
        try {
            result.put(key, valueSupplier.get());
        } catch (RuntimeException e) {
            result.put(key + "Error", e.getClass().getName() + ": " + e.getMessage());
        }
    }

    private static List<Object> arraySnapshot(Object value) {
        if (value == null || !value.getClass().isArray()) {
            return Collections.emptyList();
        }
        int length = Array.getLength(value);
        List<Object> result = new ArrayList<Object>(length);
        for (int i = 0; i < length; i++) {
            result.add(Array.get(value, i));
        }
        return result;
    }

    private static float invokeFloat(Object unit, String[] methodNames, int turretIndex) {
        requireOrderableUnit(unit);
        Object value = RustedReflection.invokeInstance(unit, methodNames, Integer.valueOf(turretIndex));
        return value instanceof Number ? ((Number) value).floatValue() : 0.0F;
    }

    private static Object invokeTurretObject(Object unit, String[] methodNames, int turretIndex) {
        requireOrderableUnit(unit);
        return RustedReflection.invokeInstance(unit, methodNames, Integer.valueOf(turretIndex));
    }

    private static String canonicalAliasName(String[] classNames, EnumAlias[] aliases, Object value) {
        if (value == null) {
            return null;
        }
        String normalized = normalize(value instanceof Enum ? ((Enum<?>) value).name() : value.toString());
        for (EnumAlias alias : aliases) {
            Object candidate = RustedReflection.getStaticFieldValue(classNames, alias.fieldNames);
            if (candidate == value || candidate.equals(value) || normalize(alias.name).equals(normalized)) {
                return alias.name;
            }
        }
        return value.toString();
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        String lower = value.trim().toLowerCase(Locale.ROOT);
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (c != '_' && c != '-' && c != ' ') {
                result.append(c);
            }
        }
        return result.toString();
    }

    private static final class EnumAlias {
        private final String name;
        private final String[] fieldNames;

        private EnumAlias(String name, String[] fieldNames) {
            this.name = name;
            this.fieldNames = fieldNames;
        }
    }
}
