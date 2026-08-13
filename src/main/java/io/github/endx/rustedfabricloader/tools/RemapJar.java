package io.github.endx.rustedfabricloader.tools;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.RecordComponentVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AnnotationRemapper;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.FieldRemapper;
import org.objectweb.asm.commons.MethodRemapper;
import org.objectweb.asm.commons.RecordComponentRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public final class RemapJar {
    private static final String MIXIN_DESC = "Lorg/spongepowered/asm/mixin/Mixin;";
    private static final String SHADOW_DESC = "Lorg/spongepowered/asm/mixin/Shadow;";
    private static final String AT_DESC = "Lorg/spongepowered/asm/mixin/injection/At;";
    private static final String ACCESSOR_DESC = "Lorg/spongepowered/asm/mixin/gen/Accessor;";
    private static final String INVOKER_DESC = "Lorg/spongepowered/asm/mixin/gen/Invoker;";

    private RemapJar() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 5) {
            throw new IllegalArgumentException("Usage: RemapJar <input.jar> <output.jar> <mappings.tiny> <fromNs> <toNs> [classpath.jar...]");
        }

        Path input = Paths.get(args[0]).toAbsolutePath().normalize();
        Path output = Paths.get(args[1]).toAbsolutePath().normalize();
        Path mappings = Paths.get(args[2]).toAbsolutePath().normalize();
        String fromNamespace = args[3];
        String toNamespace = args[4];

        List<Path> classpath = new ArrayList<Path>();
        for (int i = 5; i < args.length; i++) {
            Path path = Paths.get(args[i]).toAbsolutePath().normalize();
            if (Files.isRegularFile(path)) {
                classpath.add(path);
            }
        }

        remap(input, output, mappings, fromNamespace, toNamespace, classpath);
    }

    public static void remap(Path input, Path output, Path mappings, String fromNamespace, String toNamespace, List<Path> classpath)
            throws IOException {
        if (!Files.isRegularFile(input)) {
            throw new IOException("Input jar does not exist: " + input);
        }
        if (!Files.isRegularFile(mappings)) {
            throw new IOException("Mappings file does not exist: " + mappings);
        }
        if (input.equals(output)) {
            throw new IOException("Input and output jar must be different: " + input);
        }

        Path parent = output.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.deleteIfExists(output);
        Path remappedClassesOutput = Files.createTempFile(parent != null ? parent : output.toAbsolutePath().getParent(),
                output.getFileName().toString(), ".classes.jar");
        Files.deleteIfExists(remappedClassesOutput);

        MixinMetadataRemapper metadataRemapper = MixinMetadataRemapper.read(mappings, fromNamespace, toNamespace);

        remapClassesExact(input, remappedClassesOutput, metadataRemapper, classpath);

        try {
            rewriteMixinMetadata(remappedClassesOutput, output, metadataRemapper);
            // The development namespace intentionally tolerates partially named hierarchies while
            // mappings are still being completed. Runtime artifacts cannot: they must satisfy the
            // actual official superclass/interface contracts before distribution.
            if ("official".equals(toNamespace)) {
                verifyConcreteClassesImplementAbstractMethods(output, classpath);
            }
        } finally {
            Files.deleteIfExists(remappedClassesOutput);
        }
    }

    private static void remapClassesExact(Path input, Path output, MixinMetadataRemapper mappings,
                                          List<Path> classpath) throws IOException {
        Map<String, ClassInfo> hierarchy = new HashMap<String, ClassInfo>();
        for (Path classpathEntry : classpath) {
            readClassHierarchy(classpathEntry, hierarchy, false);
        }
        readClassHierarchy(input, hierarchy, true);
        DefinitionRemapper definitions = new DefinitionRemapper(mappings);
        ReferenceRemapper references = new ReferenceRemapper(mappings, hierarchy);
        mappings.setHierarchy(hierarchy);
        Set<String> outputNames = new HashSet<String>();

        try (JarFile jarFile = new JarFile(input.toFile(), false);
             JarOutputStream jarOutput = new JarOutputStream(Files.newOutputStream(output))) {
            List<JarEntry> entries = new ArrayList<JarEntry>();
            Enumeration<JarEntry> enumeration = jarFile.entries();
            while (enumeration.hasMoreElements()) {
                JarEntry entry = enumeration.nextElement();
                if (!entry.isDirectory()) {
                    entries.add(entry);
                }
            }
            entries.sort(Comparator.comparing(JarEntry::getName));
            for (JarEntry entry : entries) {
                String name = entry.getName();
                byte[] bytes;
                try (InputStream stream = jarFile.getInputStream(entry)) {
                    bytes = readAllBytes(stream, new byte[8192]);
                }
                if (name.endsWith(".class")) {
                    ClassReader reader = new ClassReader(bytes);
                    ClassWriter writer = new ClassWriter(0);
                    reader.accept(new StrictClassRemapper(writer, references), 0);
                    bytes = writer.toByteArray();
                    name = definitions.map(reader.getClassName()) + ".class";
                }
                if (!outputNames.add(name)) {
                    throw new IOException("Remapped output entry collision: " + name);
                }
                JarEntry outputEntry = new JarEntry(name);
                outputEntry.setTime(315532800000L);
                jarOutput.putNextEntry(outputEntry);
                jarOutput.write(bytes);
                jarOutput.closeEntry();
            }
        }
        if (!references.ambiguous.isEmpty()) {
            throw new IOException("Ambiguous hierarchy mapping: " + references.ambiguous.iterator().next());
        }
    }

    private static void readClassHierarchy(Path input, Map<String, ClassInfo> result, boolean overwrite) throws IOException {
        try (JarFile jarFile = new JarFile(input.toFile(), false)) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                    continue;
                }
                try (InputStream stream = jarFile.getInputStream(entry)) {
                    ClassReader reader = new ClassReader(stream);
                    final ClassInfo info = new ClassInfo(reader.getClassName(), reader.getAccess(),
                            reader.getSuperName(), reader.getInterfaces());
                    reader.accept(new ClassVisitor(Opcodes.ASM9) {
                        @Override
                        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                            info.fields.put(ClassInfo.signature(name, descriptor), Integer.valueOf(access));
                            return null;
                        }

                        @Override
                        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                            info.methods.put(ClassInfo.signature(name, descriptor), Integer.valueOf(access));
                            return null;
                        }
                    }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                    if (overwrite || !result.containsKey(info.name)) {
                        result.put(info.name, info);
                    }
                }
            }
        }
    }

    /**
     * Rejects remapped classes which only appeared to implement a mapped abstract method in the
     * source namespace. The JVM permits such classes to load and reports AbstractMethodError only
     * when the missing method is invoked, so ordinary compilation and startup tests cannot catch
     * this class of remapping failure.
     */
    private static void verifyConcreteClassesImplementAbstractMethods(Path output,
                                                                       List<Path> classpath)
            throws IOException {
        Map<String, ClassInfo> hierarchy = new HashMap<String, ClassInfo>();
        for (Path classpathEntry : classpath) {
            readClassHierarchy(classpathEntry, hierarchy, false);
        }
        Map<String, ClassInfo> outputClasses = new HashMap<String, ClassInfo>();
        readClassHierarchy(output, outputClasses, true);
        hierarchy.putAll(outputClasses);

        for (ClassInfo concreteClass : outputClasses.values()) {
            if ((concreteClass.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE)) != 0) {
                continue;
            }
            Set<String> abstractMethods = collectInheritedAbstractMethods(concreteClass, hierarchy);
            for (String signature : abstractMethods) {
                if (!hasConcreteImplementation(concreteClass.name, signature, hierarchy)) {
                    throw new IOException("Remapped concrete class " + concreteClass.name
                            + " does not implement inherited abstract method "
                            + ClassInfo.displaySignature(signature));
                }
            }
        }
    }

    private static Set<String> collectInheritedAbstractMethods(ClassInfo concreteClass,
                                                                 Map<String, ClassInfo> hierarchy) {
        Set<String> result = new LinkedHashSet<String>();
        ArrayDeque<String> queue = new ArrayDeque<String>();
        if (concreteClass.superName != null) queue.add(concreteClass.superName);
        for (String interfaceName : concreteClass.interfaces) queue.add(interfaceName);
        Set<String> seen = new HashSet<String>();
        while (!queue.isEmpty()) {
            String className = queue.removeFirst();
            if (!seen.add(className)) continue;
            ClassInfo info = hierarchy.get(className);
            if (info == null) continue;
            for (Map.Entry<String, Integer> method : info.methods.entrySet()) {
                int access = method.getValue().intValue();
                if ((access & Opcodes.ACC_ABSTRACT) != 0
                        && (access & (Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE)) == 0) {
                    result.add(method.getKey());
                }
            }
            if (info.superName != null) queue.add(info.superName);
            for (String interfaceName : info.interfaces) queue.add(interfaceName);
        }
        return result;
    }

    private static boolean hasConcreteImplementation(String className, String signature,
                                                      Map<String, ClassInfo> hierarchy) {
        // Class declarations take precedence over interface defaults. An abstract redeclaration
        // therefore blocks a concrete method farther up the superclass chain.
        ClassInfo current = hierarchy.get(className);
        Set<String> interfaces = new LinkedHashSet<String>();
        while (current != null) {
            Integer access = findOverrideAccess(current, signature, hierarchy);
            if (access != null) {
                return (access.intValue() & Opcodes.ACC_ABSTRACT) == 0;
            }
            for (String interfaceName : current.interfaces) interfaces.add(interfaceName);
            current = current.superName != null ? hierarchy.get(current.superName) : null;
        }

        ArrayDeque<String> queue = new ArrayDeque<String>(interfaces);
        Set<String> seen = new HashSet<String>();
        while (!queue.isEmpty()) {
            String interfaceName = queue.removeFirst();
            if (!seen.add(interfaceName)) continue;
            ClassInfo info = hierarchy.get(interfaceName);
            if (info == null) continue;
            Integer access = findOverrideAccess(info, signature, hierarchy);
            if (access != null && (access.intValue() & Opcodes.ACC_ABSTRACT) == 0) {
                return true;
            }
            for (String parent : info.interfaces) queue.add(parent);
        }
        return false;
    }

    private static Integer findOverrideAccess(ClassInfo info, String inheritedSignature,
                                              Map<String, ClassInfo> hierarchy) {
        Integer exact = info.methods.get(inheritedSignature);
        if (exact != null && (exact.intValue() & (Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE)) == 0) {
            return exact;
        }
        for (Map.Entry<String, Integer> candidate : info.methods.entrySet()) {
            if ((candidate.getValue().intValue() & (Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE)) == 0
                    && isCovariantOverride(candidate.getKey(), inheritedSignature, hierarchy)) {
                return candidate.getValue();
            }
        }
        return null;
    }

    private static boolean isCovariantOverride(String candidateSignature, String inheritedSignature,
                                                Map<String, ClassInfo> hierarchy) {
        int candidateSeparator = candidateSignature.indexOf('\u0000');
        int inheritedSeparator = inheritedSignature.indexOf('\u0000');
        if (candidateSeparator < 0 || inheritedSeparator < 0
                || !candidateSignature.substring(0, candidateSeparator)
                .equals(inheritedSignature.substring(0, inheritedSeparator))) {
            return false;
        }
        Type candidateType = Type.getMethodType(candidateSignature.substring(candidateSeparator + 1));
        Type inheritedType = Type.getMethodType(inheritedSignature.substring(inheritedSeparator + 1));
        if (!java.util.Arrays.equals(candidateType.getArgumentTypes(), inheritedType.getArgumentTypes())) {
            return false;
        }
        return isAssignableReturnType(candidateType.getReturnType(), inheritedType.getReturnType(), hierarchy);
    }

    private static boolean isAssignableReturnType(Type candidate, Type inherited,
                                                  Map<String, ClassInfo> hierarchy) {
        if (candidate.equals(inherited)) return true;
        if (candidate.getSort() != Type.OBJECT && candidate.getSort() != Type.ARRAY) return false;
        if (inherited.getSort() == Type.ARRAY) {
            if (candidate.getSort() != Type.ARRAY
                    || candidate.getDimensions() != inherited.getDimensions()) return false;
            return isAssignableReturnType(candidate.getElementType(), inherited.getElementType(), hierarchy);
        }
        if (inherited.getSort() != Type.OBJECT) return false;
        if (candidate.getSort() == Type.ARRAY) {
            String expected = inherited.getInternalName();
            return "java/lang/Object".equals(expected) || "java/lang/Cloneable".equals(expected)
                    || "java/io/Serializable".equals(expected);
        }
        String expected = inherited.getInternalName();
        ArrayDeque<String> queue = new ArrayDeque<String>();
        queue.add(candidate.getInternalName());
        Set<String> seen = new HashSet<String>();
        while (!queue.isEmpty()) {
            String className = queue.removeFirst();
            if (!seen.add(className)) continue;
            if (expected.equals(className)) return true;
            ClassInfo info = hierarchy.get(className);
            if (info == null) continue;
            if (info.superName != null) queue.add(info.superName);
            for (String interfaceName : info.interfaces) queue.add(interfaceName);
        }
        return false;
    }

    private static class DefinitionRemapper extends Remapper {
        final MixinMetadataRemapper mappings;

        DefinitionRemapper(MixinMetadataRemapper mappings) {
            this.mappings = mappings;
        }

        @Override
        public String map(String internalName) {
            String mapped = mappings.anyClassToTo.get(internalName);
            return mapped != null ? mapped : internalName;
        }

        @Override
        public String mapFieldName(String owner, String name, String descriptor) {
            MemberMapping mapping = mappings.fields.get(new MemberKey(owner, name, descriptor));
            return mapping != null ? mapping.name : name;
        }

        @Override
        public String mapMethodName(String owner, String name, String descriptor) {
            if ("<init>".equals(name) || "<clinit>".equals(name)) {
                return name;
            }
            MemberMapping mapping = mappings.methods.get(new MemberKey(owner, name, descriptor));
            return mapping != null ? mapping.name : name;
        }

        @Override
        public String mapRecordComponentName(String owner, String name, String descriptor) {
            return mapFieldName(owner, name, descriptor);
        }
    }

    private static final class ReferenceRemapper extends DefinitionRemapper {
        private final Map<String, ClassInfo> hierarchy;
        final Set<String> ambiguous = new java.util.TreeSet<String>();

        ReferenceRemapper(MixinMetadataRemapper mappings, Map<String, ClassInfo> hierarchy) {
            super(mappings);
            this.hierarchy = hierarchy;
        }

        @Override
        public String mapFieldName(String owner, String name, String descriptor) {
            MemberMapping direct = mappings.fields.get(new MemberKey(owner, name, descriptor));
            return direct != null ? direct.name : resolve(false, owner, name, descriptor);
        }

        @Override
        public String mapMethodName(String owner, String name, String descriptor) {
            if ("<init>".equals(name) || "<clinit>".equals(name)) {
                return name;
            }
            MemberMapping direct = mappings.methods.get(new MemberKey(owner, name, descriptor));
            return direct != null ? direct.name : resolve(true, owner, name, descriptor);
        }

        private String resolve(boolean method, String owner, String name, String descriptor) {
            ClassInfo start = hierarchy.get(owner);
            if (start == null) {
                return name;
            }
            Integer ownAccess = (method ? start.methods : start.fields).get(ClassInfo.signature(name, descriptor));
            if (ownAccess != null && (!method || (ownAccess.intValue() & (Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE)) != 0)) {
                return name;
            }
            ArrayDeque<HierarchyNode> queue = new ArrayDeque<HierarchyNode>();
            if (start.superName != null) queue.add(new HierarchyNode(start.superName, 1));
            for (String interfaceName : start.interfaces) queue.add(new HierarchyNode(interfaceName, 1));
            Set<String> seen = new HashSet<String>();
            Set<String> found = new LinkedHashSet<String>();
            int bestDepth = Integer.MAX_VALUE;
            while (!queue.isEmpty()) {
                HierarchyNode node = queue.removeFirst();
                if (node.depth > bestDepth || !seen.add(node.name)) continue;
                MemberMapping mapping = (method ? mappings.methods : mappings.fields)
                        .get(new MemberKey(node.name, name, descriptor));
                if (mapping != null) {
                    bestDepth = node.depth;
                    found.add(mapping.name);
                    continue;
                }
                ClassInfo info = hierarchy.get(node.name);
                if (info == null) continue;
                Integer access = (method ? info.methods : info.fields).get(ClassInfo.signature(name, descriptor));
                if (access != null && (!method || (access.intValue() & (Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE)) != 0)) {
                    continue;
                }
                if (info.superName != null) queue.add(new HierarchyNode(info.superName, node.depth + 1));
                for (String interfaceName : info.interfaces) queue.add(new HierarchyNode(interfaceName, node.depth + 1));
            }
            if (found.size() == 1) return found.iterator().next();
            if (found.size() > 1) ambiguous.add((method ? "method " : "field ") + owner + "." + name + descriptor + " -> " + found);
            return name;
        }
    }

    private static final class StrictClassRemapper extends ClassRemapper {
        private final Remapper referenceRemapper;

        StrictClassRemapper(ClassVisitor visitor, Remapper referenceRemapper) {
            // Game-owned superclass/interface methods must also rename overrides declared by a
            // mod class. Using only the direct definition map leaves methods such as an
            // implementation of CustomActionEffect.execute named, causing AbstractMethodError in
            // the official runtime even though its descriptor and superclass were remapped.
            super(visitor, referenceRemapper);
            this.referenceRemapper = referenceRemapper;
        }

        @Override
        protected MethodVisitor createMethodRemapper(MethodVisitor methodVisitor) {
            return new MethodRemapper(methodVisitor, referenceRemapper);
        }

        @Override
        protected FieldVisitor createFieldRemapper(FieldVisitor fieldVisitor) {
            return new FieldRemapper(fieldVisitor, referenceRemapper);
        }

        @Override
        protected AnnotationVisitor createAnnotationRemapper(AnnotationVisitor annotationVisitor) {
            return new AnnotationRemapper(annotationVisitor, referenceRemapper);
        }

        @Override
        protected RecordComponentVisitor createRecordComponentRemapper(RecordComponentVisitor recordComponentVisitor) {
            return new RecordComponentRemapper(recordComponentVisitor, referenceRemapper);
        }
    }

    private static final class ClassInfo {
        final String name;
        final int access;
        final String superName;
        final String[] interfaces;
        final Map<String, Integer> fields = new HashMap<String, Integer>();
        final Map<String, Integer> methods = new HashMap<String, Integer>();

        ClassInfo(String name, int access, String superName, String[] interfaces) {
            this.name = name;
            this.access = access;
            this.superName = superName;
            this.interfaces = interfaces != null ? interfaces : new String[0];
        }

        static String signature(String name, String descriptor) {
            return name + "\u0000" + descriptor;
        }

        static String displaySignature(String signature) {
            return signature.replace('\u0000', ' ');
        }
    }

    private static final class HierarchyNode {
        final String name;
        final int depth;

        HierarchyNode(String name, int depth) {
            this.name = name;
            this.depth = depth;
        }
    }

    private static void rewriteMixinMetadata(Path input, Path output, MixinMetadataRemapper metadataRemapper) throws IOException {
        Set<String> writtenEntries = new HashSet<String>();
        byte[] buffer = new byte[8192];

        try (JarFile jarFile = new JarFile(input.toFile(), false);
             JarOutputStream jarOutput = new JarOutputStream(Files.newOutputStream(output))) {
            List<JarEntry> entries = new ArrayList<JarEntry>();
            Enumeration<JarEntry> enumeration = jarFile.entries();
            while (enumeration.hasMoreElements()) {
                entries.add(enumeration.nextElement());
            }
            entries.sort(Comparator.comparing(JarEntry::getName));
            for (JarEntry entry : entries) {
                String name = entry.getName();
                if (name == null || name.isEmpty() || isSignatureFile(name)) {
                    continue;
                }

                if (entry.isDirectory()) {
                    ensureDirectoryEntry(jarOutput, writtenEntries, name);
                    continue;
                }

                int slash = name.lastIndexOf('/');
                if (slash > 0) {
                    ensureDirectoryEntry(jarOutput, writtenEntries, name.substring(0, slash + 1));
                }

                byte[] bytes;
                try (InputStream inputStream = jarFile.getInputStream(entry)) {
                    bytes = readAllBytes(inputStream, buffer);
                }

                if (name.endsWith(".class")) {
                    bytes = metadataRemapper.rewriteClass(bytes);
                } else if (name.endsWith(".mixins.json")) {
                    bytes = metadataRemapper.rewriteMixinConfig(bytes);
                } else if ("META-INF/MANIFEST.MF".equalsIgnoreCase(name)) {
                    bytes = metadataRemapper.rewriteManifest(bytes);
                }

                if (writtenEntries.add(name)) {
                    JarEntry newEntry = new JarEntry(name);
                    newEntry.setTime(0L);
                    jarOutput.putNextEntry(newEntry);
                    jarOutput.write(bytes);
                    jarOutput.closeEntry();
                }
            }
        }
    }

    private static byte[] readAllBytes(InputStream inputStream, byte[] buffer) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int read;
        while ((read = inputStream.read(buffer)) >= 0) {
            if (read > 0) {
                output.write(buffer, 0, read);
            }
        }
        return output.toByteArray();
    }

    private static void ensureDirectoryEntry(JarOutputStream jarOutput, Set<String> writtenEntries, String name) throws IOException {
        String directoryName = name.endsWith("/") ? name : name + "/";
        if (writtenEntries.add(directoryName)) {
            JarEntry directoryEntry = new JarEntry(directoryName);
            directoryEntry.setTime(0L);
            jarOutput.putNextEntry(directoryEntry);
            jarOutput.closeEntry();
        }
    }

    private static boolean isSignatureFile(String entryName) {
        String upper = entryName.toUpperCase(Locale.ROOT);
        return upper.startsWith("META-INF/")
                && (upper.endsWith(".SF") || upper.endsWith(".RSA") || upper.endsWith(".DSA"));
    }

    private static final class MixinMetadataRemapper {
        private final Map<String, String> anyClassToFrom;
        private final Map<String, String> anyClassToTo;
        private final Map<MemberKey, MemberMapping> methods;
        private final Map<MemberKey, MemberMapping> fields;
        private final Map<NameKey, String> methodNames;
        private Map<String, ClassInfo> hierarchy = java.util.Collections.emptyMap();

        private MixinMetadataRemapper(Map<String, String> anyClassToFrom,
                                      Map<String, String> anyClassToTo,
                                      Map<MemberKey, MemberMapping> methods,
                                      Map<MemberKey, MemberMapping> fields,
                                      Map<NameKey, String> methodNames) {
            this.anyClassToFrom = anyClassToFrom;
            this.anyClassToTo = anyClassToTo;
            this.methods = methods;
            this.fields = fields;
            this.methodNames = methodNames;
        }

        private void setHierarchy(Map<String, ClassInfo> hierarchy) {
            this.hierarchy = hierarchy;
        }

        static MixinMetadataRemapper read(Path mappings, String fromNamespace, String toNamespace) throws IOException {
            List<String> lines = Files.readAllLines(mappings, StandardCharsets.UTF_8);
            if (lines.isEmpty()) {
                throw new IOException("Mappings file is empty: " + mappings);
            }

            String[] header = lines.get(0).split("\t", -1);
            if (header.length < 4 || !"tiny".equals(header[0]) || !"2".equals(header[1])) {
                throw new IOException("Expected Tiny v2 mappings: " + mappings);
            }

            int namespaceCount = header.length - 3;
            int fromIndex = namespaceIndex(header, fromNamespace);
            int toIndex = namespaceIndex(header, toNamespace);
            int officialIndex = namespaceIndex(header, "official");

            Map<String, String[]> classRows = new HashMap<String, String[]>();
            List<MemberRow> methodRows = new ArrayList<MemberRow>();
            List<MemberRow> fieldRows = new ArrayList<MemberRow>();
            String[] currentClassNames = null;

            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.startsWith("c\t")) {
                    String[] parts = line.split("\t", -1);
                    if (parts.length < 1 + namespaceCount) {
                        currentClassNames = null;
                        continue;
                    }

                    currentClassNames = new String[namespaceCount];
                    for (int ns = 0; ns < namespaceCount; ns++) {
                        currentClassNames[ns] = parts[1 + ns];
                    }
                    classRows.put(currentClassNames[officialIndex], currentClassNames);
                } else if (line.startsWith("\tm\t") && currentClassNames != null) {
                    String[] parts = line.split("\t", -1);
                    if (parts.length >= 3 + namespaceCount) {
                        methodRows.add(new MemberRow(currentClassNames, parts[2], copyNames(parts, 3, namespaceCount)));
                    }
                } else if (line.startsWith("\tf\t") && currentClassNames != null) {
                    String[] parts = line.split("\t", -1);
                    if (parts.length >= 3 + namespaceCount) {
                        fieldRows.add(new MemberRow(currentClassNames, parts[2], copyNames(parts, 3, namespaceCount)));
                    }
                }
            }

            Map<String, String> anyClassToFrom = new HashMap<String, String>();
            Map<String, String> anyClassToTo = new HashMap<String, String>();
            for (String[] names : classRows.values()) {
                String fromName = names[fromIndex];
                String toName = names[toIndex];
                if (isBlank(fromName) || isBlank(toName)) {
                    continue;
                }

                for (int ns = 0; ns < namespaceCount; ns++) {
                    String name = names[ns];
                    if (!isBlank(name)) {
                        anyClassToFrom.put(name, fromName);
                        anyClassToTo.put(name, toName);
                    }
                }
            }

            Map<MemberKey, MemberMapping> methods = new HashMap<MemberKey, MemberMapping>();
            Map<MemberKey, MemberMapping> fields = new HashMap<MemberKey, MemberMapping>();
            Map<NameKey, String> methodNames = new HashMap<NameKey, String>();

            for (MemberRow row : methodRows) {
                String ownerFrom = row.ownerNames[fromIndex];
                String ownerTo = row.ownerNames[toIndex];
                String nameFrom = row.names[fromIndex];
                String nameTo = row.names[toIndex];
                String descFrom = remapDescriptor(row.officialDescriptor, anyClassToFrom);
                String descTo = remapDescriptor(row.officialDescriptor, anyClassToTo);
                if (!isBlank(ownerFrom) && !isBlank(ownerTo) && !isBlank(nameFrom) && !isBlank(nameTo)) {
                    methods.put(new MemberKey(ownerFrom, nameFrom, descFrom),
                            new MemberMapping(ownerTo, nameTo, descTo));
                    NameKey nameKey = new NameKey(ownerFrom, nameFrom);
                    String previous = methodNames.put(nameKey, nameTo);
                    if (previous != null && !previous.equals(nameTo)) {
                        methodNames.put(nameKey, null);
                    }
                }
            }

            for (MemberRow row : fieldRows) {
                String ownerFrom = row.ownerNames[fromIndex];
                String ownerTo = row.ownerNames[toIndex];
                String nameFrom = row.names[fromIndex];
                String nameTo = row.names[toIndex];
                String descFrom = remapDescriptor(row.officialDescriptor, anyClassToFrom);
                String descTo = remapDescriptor(row.officialDescriptor, anyClassToTo);
                if (!isBlank(ownerFrom) && !isBlank(ownerTo) && !isBlank(nameFrom) && !isBlank(nameTo)) {
                    fields.put(new MemberKey(ownerFrom, nameFrom, descFrom),
                            new MemberMapping(ownerTo, nameTo, descTo));
                }
            }

            return new MixinMetadataRemapper(anyClassToFrom, anyClassToTo, methods, fields, methodNames);
        }

        byte[] rewriteClass(byte[] bytes) {
            ClassNode classNode = new ClassNode();
            new ClassReader(bytes).accept(classNode, 0);

            List<String> mixinTargets = findMixinTargets(classNode);
            boolean changed = false;
            changed |= rewriteAnnotations(classNode.visibleAnnotations, mixinTargets);
            changed |= rewriteAnnotations(classNode.invisibleAnnotations, mixinTargets);
            Map<MemberKey, MemberMapping> shadowFieldRenames = new HashMap<MemberKey, MemberMapping>();
            Map<MemberKey, MemberMapping> shadowMethodRenames = new HashMap<MemberKey, MemberMapping>();
            for (FieldNode fieldNode : classNode.fields) {
                if (hasAnnotation(fieldNode.visibleAnnotations, SHADOW_DESC)
                        || hasAnnotation(fieldNode.invisibleAnnotations, SHADOW_DESC)) {
                    String oldName = fieldNode.name;
                    String oldDescriptor = fieldNode.desc;
                    MemberMapping mapping = findMixinMemberMapping(
                            fields, oldName, oldDescriptor, mixinTargets, "@Shadow field");
                    shadowFieldRenames.put(new MemberKey(classNode.name, oldName, oldDescriptor), mapping);
                    fieldNode.name = mapping.name;
                    fieldNode.desc = mapping.descriptor;
                    changed = true;
                }
                if (fieldNode.value instanceof String) {
                    String remapped = remapClassNameText((String) fieldNode.value);
                    if (!remapped.equals(fieldNode.value)) {
                        fieldNode.value = remapped;
                        changed = true;
                    }
                }
            }
            for (MethodNode methodNode : classNode.methods) {
                if (hasAnnotation(methodNode.visibleAnnotations, SHADOW_DESC)
                        || hasAnnotation(methodNode.invisibleAnnotations, SHADOW_DESC)) {
                    String oldName = methodNode.name;
                    String oldDescriptor = methodNode.desc;
                    MemberMapping mapping = findMixinMemberMapping(
                            methods, oldName, oldDescriptor, mixinTargets, "@Shadow method");
                    shadowMethodRenames.put(new MemberKey(classNode.name, oldName, oldDescriptor), mapping);
                    methodNode.name = mapping.name;
                    methodNode.desc = mapping.descriptor;
                    changed = true;
                }
                changed |= rewriteAccessorAnnotations(methodNode.visibleAnnotations,
                        methodNode.desc, mixinTargets);
                changed |= rewriteAccessorAnnotations(methodNode.invisibleAnnotations,
                        methodNode.desc, mixinTargets);
                changed |= rewriteInvokerAnnotations(methodNode.visibleAnnotations,
                        methodNode.desc, mixinTargets);
                changed |= rewriteInvokerAnnotations(methodNode.invisibleAnnotations,
                        methodNode.desc, mixinTargets);
                changed |= rewriteAnnotations(methodNode.visibleAnnotations, mixinTargets);
                changed |= rewriteAnnotations(methodNode.invisibleAnnotations, mixinTargets);
                for (AbstractInsnNode instruction : methodNode.instructions) {
                    if (instruction instanceof FieldInsnNode) {
                        FieldInsnNode fieldInsn = (FieldInsnNode) instruction;
                        MemberMapping mapping = shadowFieldRenames.get(
                                new MemberKey(fieldInsn.owner, fieldInsn.name, fieldInsn.desc));
                        if (mapping != null) {
                            fieldInsn.name = mapping.name;
                            fieldInsn.desc = mapping.descriptor;
                            changed = true;
                        }
                    } else if (instruction instanceof MethodInsnNode) {
                        MethodInsnNode methodInsn = (MethodInsnNode) instruction;
                        MemberMapping mapping = shadowMethodRenames.get(
                                new MemberKey(methodInsn.owner, methodInsn.name, methodInsn.desc));
                        if (mapping != null) {
                            methodInsn.name = mapping.name;
                            methodInsn.desc = mapping.descriptor;
                            changed = true;
                        }
                    } else if (instruction instanceof LdcInsnNode) {
                        LdcInsnNode ldcInsnNode = (LdcInsnNode) instruction;
                        if (ldcInsnNode.cst instanceof String) {
                            String remapped = remapClassNameText((String) ldcInsnNode.cst);
                            if (!remapped.equals(ldcInsnNode.cst)) {
                                ldcInsnNode.cst = remapped;
                                changed = true;
                            }
                        }
                    }
                }
            }

            if (!changed) {
                return bytes;
            }

            ClassWriter classWriter = new ClassWriter(0);
            classNode.accept(classWriter);
            return classWriter.toByteArray();
        }

        private boolean rewriteAccessorAnnotations(List<AnnotationNode> annotations,
                                                   String methodDescriptor,
                                                   List<String> mixinTargets) {
            if (annotations == null) return false;
            Type methodType = Type.getMethodType(methodDescriptor);
            Type[] arguments = methodType.getArgumentTypes();
            Type returnType = methodType.getReturnType();
            String fieldDescriptor;
            if (arguments.length == 0 && returnType.getSort() != Type.VOID) {
                fieldDescriptor = returnType.getDescriptor();
            } else if (arguments.length == 1 && returnType.getSort() == Type.VOID) {
                fieldDescriptor = arguments[0].getDescriptor();
            } else {
                fieldDescriptor = null;
            }

            boolean changed = false;
            for (AnnotationNode annotation : annotations) {
                if (annotation == null || !ACCESSOR_DESC.equals(annotation.desc)
                        || annotation.values == null) continue;
                if (fieldDescriptor == null) {
                    throw new IllegalStateException("Unsupported @Accessor method descriptor: "
                            + methodDescriptor);
                }
                for (int i = 0; i < annotation.values.size(); i += 2) {
                    if (!"value".equals(annotation.values.get(i))
                            || !(annotation.values.get(i + 1) instanceof String)) continue;
                    String oldName = (String) annotation.values.get(i + 1);
                    if (oldName.isEmpty()) continue;
                    MemberMapping mapping = findMixinMemberMapping(fields, oldName,
                            fieldDescriptor, mixinTargets, "@Accessor field");
                    if (!oldName.equals(mapping.name)) {
                        annotation.values.set(i + 1, mapping.name);
                        changed = true;
                    }
                }
            }
            return changed;
        }

        private boolean rewriteInvokerAnnotations(List<AnnotationNode> annotations,
                                                  String methodDescriptor,
                                                  List<String> mixinTargets) {
            if (annotations == null) return false;
            boolean changed = false;
            for (AnnotationNode annotation : annotations) {
                if (annotation == null || !INVOKER_DESC.equals(annotation.desc)
                        || annotation.values == null) continue;
                for (int i = 0; i < annotation.values.size(); i += 2) {
                    if (!"value".equals(annotation.values.get(i))
                            || !(annotation.values.get(i + 1) instanceof String)) continue;
                    String oldName = (String) annotation.values.get(i + 1);
                    if (oldName.isEmpty() || "<init>".equals(oldName)) continue;
                    MemberMapping mapping = findMixinMemberMapping(methods, oldName,
                            methodDescriptor, mixinTargets, "@Invoker method");
                    if (!oldName.equals(mapping.name)) {
                        annotation.values.set(i + 1, mapping.name);
                        changed = true;
                    }
                }
            }
            return changed;
        }

        private MemberMapping findMixinMemberMapping(Map<MemberKey, MemberMapping> mappings,
                                                     String memberName,
                                                     String descriptor,
                                                     List<String> mixinTargets,
                                                     String memberKind) {
            String descriptorFrom = remapDescriptor(descriptor, anyClassToFrom);
            MemberMapping selected = null;
            for (String target : mixinTargets) {
                MemberMapping candidate = mappings.get(
                        new MemberKey(toFromClass(target), memberName, descriptorFrom));
                if (candidate == null) {
                    continue;
                }
                if (selected != null && (!selected.name.equals(candidate.name)
                        || !selected.descriptor.equals(candidate.descriptor))) {
                    throw new IllegalStateException(memberKind + " maps differently across targets; split the mixin: "
                            + memberName + descriptor + " targets=" + mixinTargets);
                }
                selected = candidate;
            }
            if (selected == null) {
                throw new IllegalStateException("No target mapping found for " + memberKind + " "
                        + memberName + descriptor + " targets=" + mixinTargets);
            }
            return selected;
        }

        private static boolean hasAnnotation(List<AnnotationNode> annotations, String descriptor) {
            if (annotations == null) {
                return false;
            }
            for (AnnotationNode annotation : annotations) {
                if (descriptor.equals(annotation.desc)) {
                    return true;
                }
            }
            return false;
        }

        byte[] rewriteMixinConfig(byte[] bytes) {
            return bytes;
        }

        byte[] rewriteManifest(byte[] bytes) throws IOException {
            Manifest manifest = new Manifest(new ByteArrayInputStream(bytes));
            Attributes attributes = manifest.getMainAttributes();
            rewriteManifestClassAttribute(attributes, Attributes.Name.MAIN_CLASS.toString());
            rewriteManifestClassAttribute(attributes, "Launcher-Agent-Class");
            rewriteManifestClassAttribute(attributes, "Premain-Class");
            rewriteManifestClassAttribute(attributes, "Agent-Class");

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            manifest.write(output);
            return output.toByteArray();
        }

        private void rewriteManifestClassAttribute(Attributes attributes, String name) {
            String className = attributes.getValue(name);
            if (className == null || className.isEmpty()) {
                return;
            }
            attributes.putValue(name, remapClassNameText(className));
        }

        private List<String> findMixinTargets(ClassNode classNode) {
            List<String> targets = new ArrayList<String>();
            collectMixinTargets(classNode.visibleAnnotations, targets);
            collectMixinTargets(classNode.invisibleAnnotations, targets);
            return targets;
        }

        private void collectMixinTargets(List<AnnotationNode> annotations, List<String> targets) {
            if (annotations == null) {
                return;
            }

            for (AnnotationNode annotation : annotations) {
                if (annotation == null || !MIXIN_DESC.equals(annotation.desc) || annotation.values == null) {
                    continue;
                }

                for (int i = 0; i < annotation.values.size(); i += 2) {
                    String name = (String) annotation.values.get(i);
                    Object value = annotation.values.get(i + 1);
                    if ("value".equals(name) || "targets".equals(name) || "target".equals(name)) {
                        collectClassStringTargets(value, targets);
                    }
                }
            }
        }

        private void collectClassStringTargets(Object value, List<String> targets) {
            if (value instanceof String) {
                String internalName = toInternalClassName((String) value);
                if (internalName != null) {
                    targets.add(toFromClass(internalName));
                }
            } else if (value instanceof List<?>) {
                for (Object entry : (List<?>) value) {
                    collectClassStringTargets(entry, targets);
                }
            } else if (value instanceof Type) {
                Type type = (Type) value;
                if (type.getSort() == Type.OBJECT) {
                    targets.add(toFromClass(type.getInternalName()));
                }
            }
        }

        private boolean rewriteAnnotations(List<AnnotationNode> annotations, List<String> mixinTargets) {
            boolean changed = false;
            if (annotations == null) {
                return false;
            }

            for (AnnotationNode annotation : annotations) {
                changed |= rewriteAnnotation(annotation, mixinTargets);
            }
            return changed;
        }

        private boolean rewriteAnnotation(AnnotationNode annotation, List<String> mixinTargets) {
            if (annotation == null || annotation.values == null) {
                return false;
            }

            boolean changed = false;
            for (int i = 0; i < annotation.values.size(); i += 2) {
                String name = (String) annotation.values.get(i);
                Object value = annotation.values.get(i + 1);
                RewriteResult result = rewriteAnnotationValue(annotation.desc, name, value, mixinTargets);
                if (result.changed) {
                    annotation.values.set(i + 1, result.value);
                    changed = true;
                }
            }
            return changed;
        }

        @SuppressWarnings("unchecked")
        private RewriteResult rewriteAnnotationValue(String annotationDesc, String name, Object value, List<String> mixinTargets) {
            if (value instanceof AnnotationNode) {
                boolean changed = rewriteAnnotation((AnnotationNode) value, mixinTargets);
                return new RewriteResult(value, changed);
            }

            if (value instanceof List<?>) {
                boolean changed = false;
                List<Object> values = (List<Object>) value;
                for (int i = 0; i < values.size(); i++) {
                    RewriteResult result = rewriteAnnotationValue(annotationDesc, name, values.get(i), mixinTargets);
                    if (result.changed) {
                        values.set(i, result.value);
                        changed = true;
                    }
                }
                return new RewriteResult(value, changed);
            }

            if (!(value instanceof String)) {
                return new RewriteResult(value, false);
            }

            String text = (String) value;
            String replacement = text;
            if (MIXIN_DESC.equals(annotationDesc) && ("targets".equals(name) || "target".equals(name))) {
                replacement = remapClassNameText(text);
            } else if ("method".equals(name)) {
                replacement = remapMethodSelector(text, mixinTargets);
            } else if (AT_DESC.equals(annotationDesc) && "target".equals(name)) {
                replacement = remapAtTarget(text);
            }

            return new RewriteResult(replacement, !replacement.equals(text));
        }

        private String remapClassNameText(String className) {
            String internalName = toInternalClassName(className);
            if (internalName == null) {
                return className;
            }

            String mapped = anyClassToTo.get(internalName);
            if (mapped == null) {
                return className;
            }

            return className.indexOf('/') >= 0 ? mapped : mapped.replace('/', '.');
        }

        private String remapMethodSelector(String selector, List<String> mixinTargets) {
            int descriptorStart = selector.indexOf('(');
            if (descriptorStart < 0) {
                return remapMethodName(selector, mixinTargets);
            }

            String methodName = selector.substring(0, descriptorStart);
            String descriptor = selector.substring(descriptorStart);
            String descriptorFrom = remapDescriptor(descriptor, anyClassToFrom);
            LinkedHashSet<String> mappedSelectors = new LinkedHashSet<String>();
            for (String target : mixinTargets) {
                MemberMapping mapping = methods.get(new MemberKey(toFromClass(target), methodName, descriptorFrom));
                if (mapping != null) {
                    mappedSelectors.add(mapping.name + mapping.descriptor);
                }
            }

            if (mappedSelectors.isEmpty()) {
                return methodName + remapDescriptor(descriptor, anyClassToTo);
            }
            if (mappedSelectors.size() > 1) {
                throw new IllegalStateException("Mixin selector maps differently across targets; split the mixin: "
                        + selector + " -> " + mappedSelectors + " targets=" + mixinTargets);
            }
            return mappedSelectors.iterator().next();
        }

        private String remapMethodName(String methodName, List<String> mixinTargets) {
            LinkedHashSet<String> mappedNames = new LinkedHashSet<String>();
            for (String target : mixinTargets) {
                String targetMappedName = methodNames.get(new NameKey(toFromClass(target), methodName));
                if (targetMappedName != null) {
                    mappedNames.add(targetMappedName);
                }
            }
            if (mappedNames.isEmpty()) {
                return methodName;
            }
            if (mappedNames.size() > 1) {
                throw new IllegalStateException("Mixin method name maps differently across targets; split the mixin: "
                        + methodName + " -> " + mappedNames + " targets=" + mixinTargets);
            }
            return mappedNames.iterator().next();
        }

        private String remapAtTarget(String target) {
            if (!target.startsWith("L")) {
                return target;
            }

            int ownerEnd = target.indexOf(';');
            if (ownerEnd <= 1 || ownerEnd + 1 >= target.length()) {
                return target;
            }

            String owner = target.substring(1, ownerEnd);
            String ownerFrom = toFromClass(owner);
            String ownerTo = toToClass(owner);
            String member = target.substring(ownerEnd + 1);

            int methodDescriptorStart = member.indexOf('(');
            if (methodDescriptorStart >= 0) {
                String methodName = member.substring(0, methodDescriptorStart);
                String descriptor = member.substring(methodDescriptorStart);
                String descriptorFrom = remapDescriptor(descriptor, anyClassToFrom);
                MemberMapping mapping = findReferenceMapping(
                        methods, true, ownerFrom, methodName, descriptorFrom);
                if (mapping != null) {
                    return "L" + ownerTo + ";" + mapping.name + mapping.descriptor;
                }

                return "L" + ownerTo + ";" + methodName + remapDescriptor(descriptor, anyClassToTo);
            }

            int fieldDescriptorStart = member.indexOf(':');
            if (fieldDescriptorStart >= 0) {
                String fieldName = member.substring(0, fieldDescriptorStart);
                String descriptor = member.substring(fieldDescriptorStart + 1);
                String descriptorFrom = remapDescriptor(descriptor, anyClassToFrom);
                MemberMapping mapping = findReferenceMapping(
                        fields, false, ownerFrom, fieldName, descriptorFrom);
                if (mapping != null) {
                    return "L" + ownerTo + ";" + mapping.name + ":" + mapping.descriptor;
                }

                return "L" + ownerTo + ";" + fieldName + ":" + remapDescriptor(descriptor, anyClassToTo);
            }

            return "L" + ownerTo + ";" + member;
        }

        private MemberMapping findReferenceMapping(Map<MemberKey, MemberMapping> mappings,
                                                   boolean method, String owner,
                                                   String name, String descriptor) {
            MemberMapping direct = mappings.get(new MemberKey(owner, name, descriptor));
            if (direct != null) return direct;
            ClassInfo start = hierarchy.get(owner);
            if (start == null) return null;
            ArrayDeque<HierarchyNode> queue = new ArrayDeque<HierarchyNode>();
            if (start.superName != null) queue.add(new HierarchyNode(start.superName, 1));
            for (String interfaceName : start.interfaces) {
                queue.add(new HierarchyNode(interfaceName, 1));
            }
            Set<String> seen = new HashSet<String>();
            MemberMapping found = null;
            int bestDepth = Integer.MAX_VALUE;
            while (!queue.isEmpty()) {
                HierarchyNode node = queue.removeFirst();
                if (node.depth > bestDepth || !seen.add(node.name)) continue;
                MemberMapping candidate = mappings.get(
                        new MemberKey(node.name, name, descriptor));
                if (candidate != null) {
                    if (found != null && (!found.name.equals(candidate.name)
                            || !found.descriptor.equals(candidate.descriptor))) {
                        return null;
                    }
                    found = candidate;
                    bestDepth = node.depth;
                    continue;
                }
                ClassInfo info = hierarchy.get(node.name);
                if (info == null) continue;
                Integer access = (method ? info.methods : info.fields)
                        .get(ClassInfo.signature(name, descriptor));
                if (access != null && (!method
                        || (access.intValue() & (Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE)) != 0)) {
                    continue;
                }
                if (info.superName != null) {
                    queue.add(new HierarchyNode(info.superName, node.depth + 1));
                }
                for (String interfaceName : info.interfaces) {
                    queue.add(new HierarchyNode(interfaceName, node.depth + 1));
                }
            }
            return found;
        }

        private String toFromClass(String internalName) {
            String mapped = anyClassToFrom.get(internalName);
            return mapped != null ? mapped : internalName;
        }

        private String toToClass(String internalName) {
            String mapped = anyClassToTo.get(internalName);
            return mapped != null ? mapped : internalName;
        }

        private static int namespaceIndex(String[] header, String namespace) throws IOException {
            for (int i = 3; i < header.length; i++) {
                if (namespace.equals(header[i])) {
                    return i - 3;
                }
            }
            throw new IOException("Missing namespace in mappings: " + namespace);
        }

        private static String[] copyNames(String[] parts, int offset, int count) {
            String[] names = new String[count];
            for (int i = 0; i < count; i++) {
                names[i] = parts[offset + i];
            }
            return names;
        }

        private static String toInternalClassName(String value) {
            if (value == null || value.isEmpty()) {
                return null;
            }
            if (value.indexOf('/') >= 0) {
                return value;
            }
            if (value.indexOf('.') >= 0) {
                return value.replace('.', '/');
            }
            return value;
        }

        private static String remapDescriptor(String descriptor, Map<String, String> classMap) {
            if (descriptor == null || descriptor.indexOf('L') < 0) {
                return descriptor;
            }

            StringBuilder output = new StringBuilder(descriptor.length());
            int index = 0;
            while (index < descriptor.length()) {
                char c = descriptor.charAt(index);
                if (c != 'L') {
                    output.append(c);
                    index++;
                    continue;
                }

                int end = descriptor.indexOf(';', index);
                if (end < 0) {
                    output.append(descriptor.substring(index));
                    break;
                }

                String className = descriptor.substring(index + 1, end);
                String mapped = classMap.get(className);
                output.append('L').append(mapped != null ? mapped : className).append(';');
                index = end + 1;
            }
            return output.toString();
        }

        private static boolean isBlank(String value) {
            return value == null || value.isEmpty();
        }
    }

    private static final class MemberRow {
        final String[] ownerNames;
        final String officialDescriptor;
        final String[] names;

        MemberRow(String[] ownerNames, String officialDescriptor, String[] names) {
            this.ownerNames = ownerNames;
            this.officialDescriptor = officialDescriptor;
            this.names = names;
        }
    }

    private static final class MemberMapping {
        final String owner;
        final String name;
        final String descriptor;

        MemberMapping(String owner, String name, String descriptor) {
            this.owner = owner;
            this.name = name;
            this.descriptor = descriptor;
        }
    }

    private static final class MemberKey {
        final String owner;
        final String name;
        final String descriptor;

        MemberKey(String owner, String name, String descriptor) {
            this.owner = owner;
            this.name = name;
            this.descriptor = descriptor;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof MemberKey)) {
                return false;
            }
            MemberKey that = (MemberKey) other;
            return Objects.equals(owner, that.owner)
                    && Objects.equals(name, that.name)
                    && Objects.equals(descriptor, that.descriptor);
        }

        @Override
        public int hashCode() {
            return Objects.hash(owner, name, descriptor);
        }
    }

    private static final class NameKey {
        final String owner;
        final String name;

        NameKey(String owner, String name) {
            this.owner = owner;
            this.name = name;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof NameKey)) {
                return false;
            }
            NameKey that = (NameKey) other;
            return Objects.equals(owner, that.owner)
                    && Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(owner, name);
        }
    }

    private static final class RewriteResult {
        final Object value;
        final boolean changed;

        RewriteResult(Object value, boolean changed) {
            this.value = value;
            this.changed = changed;
        }
    }
}
