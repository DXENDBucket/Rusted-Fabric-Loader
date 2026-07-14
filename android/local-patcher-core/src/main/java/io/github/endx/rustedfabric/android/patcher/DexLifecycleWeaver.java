package io.github.endx.rustedfabric.android.patcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.MutableMethodImplementation;
import org.jf.dexlib2.builder.instruction.BuilderInstruction35c;
import org.jf.dexlib2.builder.instruction.BuilderInstruction11n;
import org.jf.dexlib2.builder.instruction.BuilderInstruction11x;
import org.jf.dexlib2.builder.instruction.BuilderInstruction21t;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.ReferenceInstruction;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.immutable.ImmutableClassDef;
import org.jf.dexlib2.immutable.ImmutableDexFile;
import org.jf.dexlib2.immutable.ImmutableMethod;
import org.jf.dexlib2.immutable.reference.ImmutableMethodReference;
import org.jf.dexlib2.writer.io.MemoryDataStore;
import org.jf.dexlib2.writer.pool.DexPool;

/** Inserts fail-safe Loader lifecycle and RFH1 callbacks into the exact mapped methods. */
final class DexLifecycleWeaver {
    // Build the descriptor at runtime so the distributed Loader does not contain a game-class
    // descriptor that could be mistaken for a bundled class definition by payload audits.
    static final String TARGET_CLASS = descriptor("com.corrodinggames.rts.game.i");
    static final String TARGET_METHOD = "a";
    static final String TARGET_PARAMETER = "Landroid/content/Context;";
    static final String BRIDGE_CLASS =
            "Lio/github/endx/rustedfabric/android/patched/EngineLifecycleBridge;";
    static final String BEFORE_METHOD = "beforeEngineInitialization";
    static final String AFTER_METHOD = "afterEngineInitialization";
    static final String NETWORK_CLASS = descriptor("com.corrodinggames.rts.gameFramework.j.ae");
    static final String CONNECTION_TYPE =
            descriptor("com.corrodinggames.rts.gameFramework.j.c");
    static final String PACKET_TYPE =
            descriptor("com.corrodinggames.rts.gameFramework.j.bi");
    static final String REGISTER_METHOD = "f";
    static final String SERVER_INFO_METHOD = "d";
    static final String SYSTEM_PACKET_METHOD = "a";
    static final String AFTER_REGISTER_CALLBACK = "afterClientRegistration";
    static final String AFTER_SERVER_INFO_CALLBACK = "afterServerInfo";
    static final String SYSTEM_PACKET_CALLBACK = "onSystemPacket";
    static final String NETWORK_RESET_CALLBACK = "afterNetworkReset";
    static final String START_GAME_CALLBACK = "allowGameStart";

    private static final ImmutableMethodReference BEFORE = callback(BEFORE_METHOD);
    private static final ImmutableMethodReference AFTER = callback(AFTER_METHOD);
    private static final ImmutableMethodReference AFTER_REGISTER = callbackWithObjects(
            AFTER_REGISTER_CALLBACK);
    private static final ImmutableMethodReference AFTER_SERVER_INFO = callbackWithObjects(
            AFTER_SERVER_INFO_CALLBACK);
    private static final ImmutableMethodReference SYSTEM_PACKET = callbackWithObjects(
            SYSTEM_PACKET_CALLBACK);
    private static final ImmutableMethodReference NETWORK_RESET = new ImmutableMethodReference(
            BRIDGE_CLASS, NETWORK_RESET_CALLBACK,
            java.util.Arrays.asList("Ljava/lang/Object;", "Z"), "V");
    private static final ImmutableMethodReference ALLOW_GAME_START = new ImmutableMethodReference(
            BRIDGE_CLASS, START_GAME_CALLBACK,
            java.util.Arrays.asList("Ljava/lang/Object;", "Ljava/lang/Object;"), "Z");

    private DexLifecycleWeaver() {
    }

    static byte[] weaveEngineInitialization(byte[] source) throws PatchException {
        try {
            DexBackedDexFile dex = new DexBackedDexFile(null, source);
            List<ClassDef> classes = new ArrayList<>();
            int targetClasses = 0;
            int targetMethods = 0;
            int networkClasses = 0;
            int networkMethods = 0;

            for (ClassDef classDef : dex.getClasses()) {
                if (!TARGET_CLASS.equals(classDef.getType())
                        && !NETWORK_CLASS.equals(classDef.getType())) {
                    classes.add(classDef);
                    continue;
                }
                if (TARGET_CLASS.equals(classDef.getType())) targetClasses++;
                else networkClasses++;
                List<Method> methods = new ArrayList<>();
                for (Method method : classDef.getMethods()) {
                    if (isTarget(method)) {
                        targetMethods++;
                        methods.add(weave(method));
                    } else if (isNetworkTarget(method)) {
                        networkMethods++;
                        methods.add(weaveNetwork(method));
                    } else if (isStartTarget(method)) {
                        networkMethods++;
                        methods.add(weaveStartGate(method));
                    } else {
                        methods.add(method);
                    }
                }
                classes.add(new ImmutableClassDef(classDef.getType(), classDef.getAccessFlags(),
                        classDef.getSuperclass(), classDef.getInterfaces(),
                        classDef.getSourceFile(), classDef.getAnnotations(),
                        classDef.getFields(), methods));
            }
            if (targetClasses != 1 || targetMethods != 1) {
                throw failure("Mapped engine init method was not found exactly once");
            }
            if (networkClasses != 1 || networkMethods != 5) {
                throw failure("Mapped RFH1 network methods were not found exactly once");
            }

            MemoryDataStore output = new MemoryDataStore(source.length + 1024);
            DexPool.writeTo(output, new ImmutableDexFile(dex.getOpcodes(), classes));
            byte[] result = output.getData();
            verify(result);
            return result;
        } catch (PatchException expected) {
            throw expected;
        } catch (IOException | RuntimeException invalidDex) {
            throw new PatchException(PatchException.Reason.DEX_WEAVE_FAILED,
                    "Could not weave the mapped engine lifecycle method", invalidDex);
        }
    }

    private static ImmutableMethod weaveStartGate(Method method) throws PatchException {
        MethodImplementation implementation = method.getImplementation();
        if (implementation == null || implementation.getRegisterCount() < 4) {
            throw failure("Mapped start-game method has no scratch register");
        }
        for (Instruction instruction : implementation.getInstructions()) {
            if (isCallback(instruction, START_GAME_CALLBACK)) {
                throw failure("Mapped start-game method is already woven");
            }
        }
        int p0 = implementation.getRegisterCount() - 3;
        int p1 = implementation.getRegisterCount() - 2;
        if (p0 < 1 || p1 > 15) {
            throw failure("Mapped start-game parameters cannot use the RFH1 gate");
        }
        MutableMethodImplementation mutable = new MutableMethodImplementation(implementation);
        org.jf.dexlib2.builder.Label proceed = mutable.newLabelForIndex(0);
        mutable.addInstruction(0, invoke(ALLOW_GAME_START, p0, p1));
        mutable.addInstruction(1, new BuilderInstruction11x(Opcode.MOVE_RESULT, 0));
        mutable.addInstruction(2, new BuilderInstruction21t(Opcode.IF_NEZ, 0, proceed));
        mutable.addInstruction(3, new BuilderInstruction11n(Opcode.CONST_4, 0, 0));
        mutable.addInstruction(4, new BuilderInstruction11x(Opcode.RETURN, 0));
        return copyWithImplementation(method, mutable);
    }

    private static ImmutableMethod weaveNetwork(Method method) throws PatchException {
        MethodImplementation implementation = method.getImplementation();
        if (implementation == null || AccessFlags.STATIC.isSet(method.getAccessFlags())) {
            throw failure("Mapped network method has an unexpected shape");
        }
        List<? extends Instruction> original = toList(implementation.getInstructions());
        String callbackName = networkCallback(method);
        for (Instruction instruction : original) {
            if (isCallback(instruction, callbackName)) {
                throw failure("Mapped network method is already woven");
            }
        }
        int p0 = implementation.getRegisterCount() - 2;
        int p1 = implementation.getRegisterCount() - 1;
        if (p0 < 0 || p1 > 15) {
            throw failure("Mapped network parameter registers cannot use invoke-35c");
        }
        MutableMethodImplementation mutable = new MutableMethodImplementation(implementation);
        if (SYSTEM_PACKET_CALLBACK.equals(callbackName)) {
            mutable.addInstruction(0, invoke(SYSTEM_PACKET, p0, p1));
        } else {
            List<Integer> returns = new ArrayList<>();
            for (int index = 0; index < original.size(); index++) {
                if (original.get(index).getOpcode() == Opcode.RETURN_VOID) returns.add(index);
            }
            if (returns.isEmpty()) throw failure("Mapped network sender has no normal return");
            ImmutableMethodReference callback = AFTER_REGISTER_CALLBACK.equals(callbackName)
                    ? AFTER_REGISTER : AFTER_SERVER_INFO_CALLBACK.equals(callbackName)
                    ? AFTER_SERVER_INFO : NETWORK_RESET;
            for (int index = returns.size() - 1; index >= 0; index--) {
                mutable.addInstruction(returns.get(index), invoke(callback, p0, p1));
            }
        }
        return copyWithImplementation(method, mutable);
    }

    private static ImmutableMethod weave(Method method) throws PatchException {
        int flags = method.getAccessFlags();
        if (!AccessFlags.PUBLIC.isSet(flags) || !AccessFlags.FINAL.isSet(flags)
                || AccessFlags.STATIC.isSet(flags)) {
            throw failure("Mapped engine init method has unexpected access flags");
        }
        MethodImplementation implementation = method.getImplementation();
        if (implementation == null) {
            throw failure("Mapped engine init method has no implementation");
        }

        List<? extends Instruction> original = toList(implementation.getInstructions());
        List<Integer> returns = new ArrayList<>();
        for (int index = 0; index < original.size(); index++) {
            Instruction instruction = original.get(index);
            if (instruction.getOpcode() == Opcode.RETURN_VOID) returns.add(index);
            if (isCallback(instruction, BEFORE_METHOD) || isCallback(instruction, AFTER_METHOD)) {
                throw failure("Mapped engine init method is already woven");
            }
        }
        if (returns.size() != 1) {
            throw failure("Mapped engine init method has an unexpected return shape");
        }

        MutableMethodImplementation mutable = new MutableMethodImplementation(implementation);
        for (int index = returns.size() - 1; index >= 0; index--) {
            mutable.addInstruction(returns.get(index), invoke(AFTER));
        }
        mutable.addInstruction(0, invoke(BEFORE));
        return new ImmutableMethod(method.getDefiningClass(), method.getName(),
                method.getParameters(), method.getReturnType(), method.getAccessFlags(),
                method.getAnnotations(), method.getHiddenApiRestrictions(), mutable);
    }

    private static void verify(byte[] dexBytes) throws PatchException {
        DexBackedDexFile dex = new DexBackedDexFile(null, dexBytes);
        int targets = 0;
        for (ClassDef classDef : dex.getClasses()) {
            for (Method method : classDef.getMethods()) {
                if (!isTarget(method)) continue;
                targets++;
                MethodImplementation implementation = method.getImplementation();
                if (implementation == null) throw failure("Woven method has no implementation");
                List<? extends Instruction> instructions = toList(implementation.getInstructions());
                int before = 0;
                int after = 0;
                int returns = 0;
                for (int index = 0; index < instructions.size(); index++) {
                    Instruction instruction = instructions.get(index);
                    if (isCallback(instruction, BEFORE_METHOD)) {
                        before++;
                        if (index != 0) throw failure("Before callback is not the first instruction");
                    }
                    if (isCallback(instruction, AFTER_METHOD)) after++;
                    if (instruction.getOpcode() == Opcode.RETURN_VOID) {
                        returns++;
                        if (index == 0 || !isCallback(instructions.get(index - 1), AFTER_METHOD)) {
                            throw failure("A normal return is missing its after callback");
                        }
                    }
                }
                if (before != 1 || after != 1 || returns != 1) {
                    throw failure("Woven callback counts do not match the exact profile");
                }
            }
        }
        if (targets != 1) throw failure("Woven target verification failed");
        verifyNetwork(dex);
    }

    private static void verifyNetwork(DexBackedDexFile dex) throws PatchException {
        int targets = 0;
        for (ClassDef classDef : dex.getClasses()) {
            for (Method method : classDef.getMethods()) {
                if (!isNetworkTarget(method) && !isStartTarget(method)) continue;
                targets++;
                String expected = networkCallback(method);
                List<? extends Instruction> instructions = toList(
                        method.getImplementation().getInstructions());
                int callbacks = 0;
                int returns = 0;
                for (int index = 0; index < instructions.size(); index++) {
                    if (isCallback(instructions.get(index), expected)) callbacks++;
                    if (instructions.get(index).getOpcode() == Opcode.RETURN_VOID) {
                        returns++;
                        if (!SYSTEM_PACKET_CALLBACK.equals(expected)
                                && (index == 0 || !isCallback(instructions.get(index - 1), expected))) {
                            throw failure("Mapped network return is missing its callback");
                        }
                    }
                }
                if (SYSTEM_PACKET_CALLBACK.equals(expected)
                        || START_GAME_CALLBACK.equals(expected)) {
                    if (callbacks != 1 || !isCallback(instructions.get(0), expected)) {
                        throw failure("System packet callback is not at method entry");
                    }
                } else if (callbacks != returns || returns == 0) {
                    throw failure("Network sender callback count does not match returns");
                }
            }
        }
        if (targets != 5) throw failure("Woven RFH1 network target verification failed");
    }

    private static boolean isTarget(Method method) {
        return TARGET_CLASS.equals(method.getDefiningClass())
                && TARGET_METHOD.equals(method.getName())
                && "V".equals(method.getReturnType())
                && method.getParameterTypes().equals(Collections.singletonList(TARGET_PARAMETER));
    }

    private static boolean isNetworkTarget(Method method) {
        if (!NETWORK_CLASS.equals(method.getDefiningClass())
                || !"V".equals(method.getReturnType())
                || method.getParameterTypes().size() != 1) return false;
        String parameter = method.getParameterTypes().get(0).toString();
        return (REGISTER_METHOD.equals(method.getName()) && CONNECTION_TYPE.equals(parameter))
                || (SERVER_INFO_METHOD.equals(method.getName()) && CONNECTION_TYPE.equals(parameter))
                || (SYSTEM_PACKET_METHOD.equals(method.getName()) && PACKET_TYPE.equals(parameter))
                || (SYSTEM_PACKET_METHOD.equals(method.getName()) && "Z".equals(parameter));
    }

    private static boolean isStartTarget(Method method) {
        return NETWORK_CLASS.equals(method.getDefiningClass())
                && SYSTEM_PACKET_METHOD.equals(method.getName())
                && "Z".equals(method.getReturnType())
                && method.getParameterTypes().equals(java.util.Arrays.asList(
                        CONNECTION_TYPE, "Z"));
    }

    private static String networkCallback(Method method) {
        if (isStartTarget(method)) return START_GAME_CALLBACK;
        if (REGISTER_METHOD.equals(method.getName())) return AFTER_REGISTER_CALLBACK;
        if (SERVER_INFO_METHOD.equals(method.getName())) return AFTER_SERVER_INFO_CALLBACK;
        return PACKET_TYPE.equals(method.getParameterTypes().get(0).toString())
                ? SYSTEM_PACKET_CALLBACK : NETWORK_RESET_CALLBACK;
    }

    private static boolean isCallback(Instruction instruction, String name) {
        if (instruction.getOpcode() != Opcode.INVOKE_STATIC
                || !(instruction instanceof ReferenceInstruction)) return false;
        Object reference = ((ReferenceInstruction) instruction).getReference();
        if (!(reference instanceof MethodReference)) return false;
        MethodReference method = (MethodReference) reference;
        return BRIDGE_CLASS.equals(method.getDefiningClass())
                && name.equals(method.getName())
                && ("V".equals(method.getReturnType()) || "Z".equals(method.getReturnType()));
    }

    private static BuilderInstruction35c invoke(MethodReference callback) {
        return new BuilderInstruction35c(Opcode.INVOKE_STATIC,
                0, 0, 0, 0, 0, 0, callback);
    }

    private static BuilderInstruction35c invoke(MethodReference callback, int first, int second) {
        return new BuilderInstruction35c(Opcode.INVOKE_STATIC,
                2, first, second, 0, 0, 0, callback);
    }

    private static ImmutableMethodReference callback(String name) {
        return new ImmutableMethodReference(BRIDGE_CLASS, name,
                Collections.emptyList(), "V");
    }

    private static ImmutableMethodReference callbackWithObjects(String name) {
        return new ImmutableMethodReference(BRIDGE_CLASS, name,
                java.util.Arrays.asList("Ljava/lang/Object;", "Ljava/lang/Object;"), "V");
    }

    private static ImmutableMethod copyWithImplementation(Method method,
            MethodImplementation implementation) {
        return new ImmutableMethod(method.getDefiningClass(), method.getName(),
                method.getParameters(), method.getReturnType(), method.getAccessFlags(),
                method.getAnnotations(), method.getHiddenApiRestrictions(), implementation);
    }

    private static String descriptor(String binaryName) {
        return "L" + binaryName.replace('.', '/') + ";";
    }

    private static <T> List<T> toList(Iterable<? extends T> values) {
        List<T> result = new ArrayList<>();
        for (T value : values) result.add(value);
        return result;
    }

    private static PatchException failure(String message) {
        return new PatchException(PatchException.Reason.DEX_WEAVE_FAILED, message);
    }
}
