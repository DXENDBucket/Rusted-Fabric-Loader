package org.lwjgl.system;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Transfers Android pointer frames into the touch state retained by the desktop game build. */
public final class RustedFabricTouch {
    private static final float[] XS = new float[10];
    private static final float[] YS = new float[10];
    private static final int[] IDS = new int[10];
    private static long appliedSequence;
    private static Access access;
    private static boolean unavailable;

    private RustedFabricTouch() {
    }

    /** Called by the Android-only game patch at the start of each Slick update. */
    public static void apply(Object slickGame) {
        if (slickGame == null || unavailable) return;
        long packed = nativePoll(XS, YS, IDS);
        long sequence = packed >>> 8;
        if (sequence == 0 || sequence == appliedSequence) return;
        int count = (int) (packed & 0x7fL);
        boolean down = (packed & 0x80L) != 0;
        try {
            if (access == null) access = new Access(slickGame.getClass());
            access.apply(slickGame, count, down);
            appliedSequence = sequence;
        } catch (ReflectiveOperationException | RuntimeException failure) {
            unavailable = true;
            System.err.println("Rusted Fabric touch bridge disabled: " + failure);
        }
    }

    private static final class Access {
        private final Field appFramework;
        private final Field inputScale;
        private final Field inputHandler;
        private final Field pointerCount;
        private final Field pointerXs;
        private final Field pointerYs;
        private final Field pointerPressures;
        private final Field pointerIds;
        private final Field currentDown;
        private final Field frameDown;
        private final Field lastPointerCount;
        private final Field buttonState;
        private final Field multiTouch;
        private final Field pressureCached;
        private final Field distanceCached;
        private final Field diameterCached;
        private final Method getPointerIds;

        Access(Class<?> slickType) throws ReflectiveOperationException {
            appFramework = field(slickType, "f");
            inputScale = field(slickType, "P");
            Class<?> frameworkType = appFramework.getType();
            inputHandler = field(frameworkType, "d");
            Class<?> stateType = inputHandler.getType();
            pointerCount = field(stateType, "a");
            pointerXs = field(stateType, "b");
            pointerYs = field(stateType, "c");
            pointerPressures = field(stateType, "d");
            pointerIds = field(stateType, "e");
            currentDown = field(stateType, "k");
            multiTouch = field(stateType, "l");
            frameDown = field(stateType, "m");
            lastPointerCount = field(stateType, "n");
            pressureCached = field(stateType, "o");
            distanceCached = field(stateType, "p");
            diameterCached = field(stateType, "q");
            buttonState = field(stateType, "r");
            getPointerIds = stateType.getDeclaredMethod("e");
            getPointerIds.setAccessible(true);
        }

        void apply(Object slickGame, int count, boolean down)
                throws ReflectiveOperationException {
            Object framework = appFramework.get(slickGame);
            if (framework == null) return;
            Object state = inputHandler.get(framework);
            if (state == null) return;
            float scale = inputScale.getFloat(slickGame);
            if (!(scale > 0.0f)) scale = 1.0f;
            float[] stateXs = (float[]) pointerXs.get(state);
            float[] stateYs = (float[]) pointerYs.get(state);
            float[] pressures = (float[]) pointerPressures.get(state);
            int[] stateIds = (int[]) pointerIds.get(state);
            int[] reflectedIds = (int[]) getPointerIds.invoke(state);
            for (int index = 0; index < count; ++index) {
                stateXs[index] = XS[index] / scale;
                stateYs[index] = YS[index] / scale;
                pressures[index] = 1.0f;
                stateIds[index] = IDS[index];
                reflectedIds[index] = IDS[index];
            }
            pointerCount.setInt(state, count);
            currentDown.setBoolean(state, down);
            multiTouch.setBoolean(state, count >= 2);
            if (down) frameDown.setBoolean(state, true);
            if (count > 0) lastPointerCount.setInt(state, count);
            pressureCached.setBoolean(state, false);
            distanceCached.setBoolean(state, false);
            diameterCached.setBoolean(state, false);
            buttonState.setInt(state, 0);
        }

        private static Field field(Class<?> type, String name) throws NoSuchFieldException {
            Field value = type.getDeclaredField(name);
            value.setAccessible(true);
            return value;
        }
    }

    private static native long nativePoll(float[] xs, float[] ys, int[] ids);
}
