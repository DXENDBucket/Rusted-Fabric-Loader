package io.github.endx.rustedfabric.android.patcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.MutableMethodImplementation;
import org.jf.dexlib2.builder.instruction.BuilderInstruction35c;
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

/** Inserts fail-safe Loader lifecycle callbacks into the exact mapped engine init method. */
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

    private static final ImmutableMethodReference BEFORE = callback(BEFORE_METHOD);
    private static final ImmutableMethodReference AFTER = callback(AFTER_METHOD);

    private DexLifecycleWeaver() {
    }

    static byte[] weaveEngineInitialization(byte[] source) throws PatchException {
        try {
            DexBackedDexFile dex = new DexBackedDexFile(null, source);
            List<ClassDef> classes = new ArrayList<>();
            int targetClasses = 0;
            int targetMethods = 0;

            for (ClassDef classDef : dex.getClasses()) {
                if (!TARGET_CLASS.equals(classDef.getType())) {
                    classes.add(classDef);
                    continue;
                }
                targetClasses++;
                List<Method> methods = new ArrayList<>();
                for (Method method : classDef.getMethods()) {
                    if (isTarget(method)) {
                        targetMethods++;
                        methods.add(weave(method));
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
    }

    private static boolean isTarget(Method method) {
        return TARGET_CLASS.equals(method.getDefiningClass())
                && TARGET_METHOD.equals(method.getName())
                && "V".equals(method.getReturnType())
                && method.getParameterTypes().equals(Collections.singletonList(TARGET_PARAMETER));
    }

    private static boolean isCallback(Instruction instruction, String name) {
        if (instruction.getOpcode() != Opcode.INVOKE_STATIC
                || !(instruction instanceof ReferenceInstruction)) return false;
        Object reference = ((ReferenceInstruction) instruction).getReference();
        if (!(reference instanceof MethodReference)) return false;
        MethodReference method = (MethodReference) reference;
        return BRIDGE_CLASS.equals(method.getDefiningClass())
                && name.equals(method.getName())
                && method.getParameterTypes().isEmpty()
                && "V".equals(method.getReturnType());
    }

    private static BuilderInstruction35c invoke(MethodReference callback) {
        return new BuilderInstruction35c(Opcode.INVOKE_STATIC,
                0, 0, 0, 0, 0, 0, callback);
    }

    private static ImmutableMethodReference callback(String name) {
        return new ImmutableMethodReference(BRIDGE_CLASS, name,
                Collections.emptyList(), "V");
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
