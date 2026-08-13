package io.github.endx.rustedfabricloader.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Adds a locally supplied Enigma named namespace to the Loader's Tiny mappings.
 *
 * <p>The external mapping set is deliberately an input rather than a resource. This lets RFL
 * consume a user's legally obtained, pre-patched game without redistributing the external
 * project's mappings or implementation.</p>
 */
public final class ComposeExternalNamedMappings {
    private ComposeExternalNamedMappings() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 5) {
            throw new IllegalArgumentException("Usage: ComposeExternalNamedMappings "
                    + "<official-to-intermediary-enigma-dir> <intermediary-to-named-enigma-dir> "
                    + "<rfl-mappings.tiny> <output.tiny> <external-namespace>");
        }

        Path firstLayer = Paths.get(args[0]).toAbsolutePath().normalize();
        Path secondLayer = Paths.get(args[1]).toAbsolutePath().normalize();
        Path rflMappings = Paths.get(args[2]).toAbsolutePath().normalize();
        Path output = Paths.get(args[3]).toAbsolutePath().normalize();
        String externalNamespace = args[4].trim();
        if (!externalNamespace.matches("[A-Za-z0-9_.-]+")) {
            throw new IOException("Invalid external namespace: " + externalNamespace);
        }

        EnigmaMappings officialToIntermediate = EnigmaMappings.read(firstLayer);
        EnigmaMappings intermediateToNamed = EnigmaMappings.read(secondLayer);
        compose(rflMappings, output, externalNamespace, officialToIntermediate,
                intermediateToNamed);
    }

    private static void compose(Path rflMappings, Path output, String externalNamespace,
                                EnigmaMappings first, EnigmaMappings second) throws IOException {
        List<String> input = Files.readAllLines(rflMappings, StandardCharsets.UTF_8);
        if (input.isEmpty()) {
            throw new IOException("RFL mappings are empty: " + rflMappings);
        }
        String[] header = input.get(0).split("\t", -1);
        if (header.length < 6 || !"tiny".equals(header[0]) || !"2".equals(header[1])
                || !"official".equals(header[3])) {
            throw new IOException("Expected RFL Tiny v2 mappings with official as source: "
                    + rflMappings);
        }
        for (int i = 3; i < header.length; i++) {
            if (externalNamespace.equals(header[i])) {
                throw new IOException("Namespace already exists: " + externalNamespace);
            }
        }

        List<String> result = new ArrayList<String>(input.size());
        result.add(input.get(0) + "\t" + externalNamespace);
        String officialOwner = null;
        Set<MemberKey> emittedMembers = new HashSet<MemberKey>();

        for (int lineIndex = 1; lineIndex < input.size(); lineIndex++) {
            String line = input.get(lineIndex);
            if (line.startsWith("c\t")) {
                appendExternalOnlyMembers(result, officialOwner, emittedMembers, header.length - 3,
                        first, second);
                String[] parts = line.split("\t", -1);
                officialOwner = parts.length > 1 ? parts[1] : null;
                emittedMembers.clear();
                result.add(line + "\t" + externalClassName(officialOwner, first, second));
            } else if ((line.startsWith("\tm\t") || line.startsWith("\tf\t"))
                    && officialOwner != null) {
                String[] parts = line.split("\t", -1);
                if (parts.length < 4) {
                    result.add(line + "\t");
                    continue;
                }
                boolean method = "m".equals(parts[1]);
                String officialDescriptor = parts[2];
                String officialName = parts[3];
                emittedMembers.add(new MemberKey(officialOwner, officialName, officialDescriptor));
                String externalName = externalMemberName(method, officialOwner, officialName,
                        officialDescriptor, first, second);
                result.add(line + "\t" + externalName);
            } else {
                result.add(line);
            }
        }
        appendExternalOnlyMembers(result, officialOwner, emittedMembers, header.length - 3,
                first, second);

        Path parent = output.getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.write(output, result, StandardCharsets.UTF_8);
    }

    private static void appendExternalOnlyMembers(List<String> output, String officialOwner,
                                                  Set<MemberKey> emitted, int existingNamespaces,
                                                  EnigmaMappings first, EnigmaMappings second) {
        if (officialOwner == null) return;
        appendExternalOnlyMembers(output, officialOwner, emitted, existingNamespaces, false,
                first, second, first.fields);
        appendExternalOnlyMembers(output, officialOwner, emitted, existingNamespaces, true,
                first, second, first.methods);
    }

    private static void appendExternalOnlyMembers(List<String> output, String officialOwner,
                                                  Set<MemberKey> emitted, int existingNamespaces,
                                                  boolean method, EnigmaMappings first,
                                                  EnigmaMappings second,
                                                  Map<MemberKey, MemberMapping> source) {
        List<MemberKey> missing = new ArrayList<MemberKey>();
        for (MemberKey key : source.keySet()) {
            if (officialOwner.equals(key.owner) && !emitted.contains(key)) missing.add(key);
        }
        missing.sort(Comparator.comparing((MemberKey key) -> key.name)
                .thenComparing(key -> key.descriptor));
        for (MemberKey key : missing) {
            String externalName = externalMemberName(method, key.owner, key.name, key.descriptor,
                    first, second);
            StringBuilder row = new StringBuilder(method ? "\tm\t" : "\tf\t");
            row.append(key.descriptor);
            for (int namespace = 0; namespace < existingNamespaces; namespace++) {
                row.append('\t').append(key.name);
            }
            row.append('\t').append(externalName);
            output.add(row.toString());
        }
    }

    private static String externalClassName(String officialName, EnigmaMappings first,
                                            EnigmaMappings second) {
        if (officialName == null || officialName.isEmpty()) return officialName;
        String intermediate = first.mapClass(officialName);
        return second.mapClass(intermediate);
    }

    private static String externalMemberName(boolean method, String officialOwner,
                                             String officialName, String officialDescriptor,
                                             EnigmaMappings first, EnigmaMappings second) {
        String intermediateOwner = first.mapClass(officialOwner);
        String intermediateDescriptor = remapDescriptor(officialDescriptor, first.classes);
        MemberMapping intermediate = first.mapMember(method, officialOwner, officialName,
                officialDescriptor);
        String intermediateName = intermediate != null ? intermediate.targetName : officialName;
        MemberMapping named = second.mapMember(method, intermediateOwner, intermediateName,
                intermediateDescriptor);
        return named != null ? named.targetName : intermediateName;
    }

    private static String remapDescriptor(String descriptor, Map<String, String> classes) {
        if (descriptor == null || descriptor.indexOf('L') < 0) return descriptor;
        StringBuilder result = new StringBuilder(descriptor.length());
        int index = 0;
        while (index < descriptor.length()) {
            char current = descriptor.charAt(index);
            if (current != 'L') {
                result.append(current);
                index++;
                continue;
            }
            int end = descriptor.indexOf(';', index);
            if (end < 0) {
                result.append(descriptor.substring(index));
                break;
            }
            String className = descriptor.substring(index + 1, end);
            String mapped = classes.get(className);
            result.append('L').append(mapped != null ? mapped : className).append(';');
            index = end + 1;
        }
        return result.toString();
    }

    private static final class EnigmaMappings {
        final Map<String, String> classes = new HashMap<String, String>();
        final Map<MemberKey, MemberMapping> methods = new HashMap<MemberKey, MemberMapping>();
        final Map<MemberKey, MemberMapping> fields = new HashMap<MemberKey, MemberMapping>();

        static EnigmaMappings read(Path directory) throws IOException {
            if (!Files.isDirectory(directory)) {
                throw new IOException("Enigma mappings directory does not exist: " + directory);
            }
            EnigmaMappings result = new EnigmaMappings();
            List<Path> files = new ArrayList<Path>();
            try (Stream<Path> stream = Files.walk(directory)) {
                stream.filter(path -> Files.isRegularFile(path)
                                && path.getFileName().toString().endsWith(".mapping"))
                        .forEach(files::add);
            }
            Collections.sort(files, Comparator.comparing(Path::toString));
            for (Path file : files) result.readFile(file);
            if (result.classes.isEmpty()) {
                throw new IOException("No Enigma class mappings found in: " + directory);
            }
            return result;
        }

        String mapClass(String source) {
            String target = classes.get(source);
            return target != null && !target.isEmpty() ? target : source;
        }

        MemberMapping mapMember(boolean method, String owner, String name, String descriptor) {
            return (method ? methods : fields).get(new MemberKey(owner, name, descriptor));
        }

        private void readFile(Path file) throws IOException {
            ArrayDeque<ClassContext> classStack = new ArrayDeque<ClassContext>();
            for (String rawLine : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (rawLine.trim().isEmpty()) continue;
                int depth = leadingTabs(rawLine);
                String[] tokens = rawLine.substring(depth).trim().split("\\s+");
                if (tokens.length < 2) continue;
                String kind = tokens[0];
                if ("CLASS".equals(kind)) {
                    while (classStack.size() > depth) classStack.removeLast();
                    ClassContext parent = classStack.peekLast();
                    String source = nestedName(parent != null ? parent.source : null, tokens[1]);
                    String targetPart = tokens.length >= 3 ? tokens[2] : tokens[1];
                    String target = nestedName(parent != null ? parent.target : null, targetPart);
                    putUnique(classes, source, target, "class", file);
                    classStack.addLast(new ClassContext(source, target));
                    continue;
                }
                if (!("METHOD".equals(kind) || "FIELD".equals(kind)) || tokens.length < 3) {
                    continue;
                }
                while (classStack.size() > depth) classStack.removeLast();
                ClassContext owner = classStack.peekLast();
                if (owner == null) {
                    throw new IOException("Member without class in " + file + ": " + rawLine);
                }
                String sourceName = tokens[1];
                String targetName;
                String descriptor;
                if (tokens.length >= 4) {
                    targetName = tokens[2];
                    descriptor = tokens[3];
                } else {
                    targetName = sourceName;
                    descriptor = tokens[2];
                }
                Map<MemberKey, MemberMapping> destination = "METHOD".equals(kind) ? methods : fields;
                MemberKey key = new MemberKey(owner.source, sourceName, descriptor);
                putMember(destination, key, new MemberMapping(owner.target, targetName), kind,
                        file);
            }
        }

        private static int leadingTabs(String line) {
            int count = 0;
            while (count < line.length() && line.charAt(count) == '\t') count++;
            return count;
        }

        private static String nestedName(String parent, String name) {
            return parent == null || name.indexOf('/') >= 0 ? name : parent + "$" + name;
        }

        private static void putUnique(Map<String, String> mappings, String source, String target,
                                      String kind, Path file) throws IOException {
            String previous = mappings.get(source);
            if (previous == null || previous.equals(source)) {
                mappings.put(source, target);
                return;
            }
            // Enigma directories written without deleting stale files can contain both an old
            // identity file and the newer explicitly named file. The explicit name is authoritative.
            if (target.equals(source) || previous.equals(target)) return;
            if (!previous.equals(target)) {
                throw new IOException("Conflicting " + kind + " mapping in " + file + ": "
                        + source + " -> " + previous + " / " + target);
            }
        }

        private static void putMember(Map<MemberKey, MemberMapping> mappings, MemberKey key,
                                      MemberMapping value, String kind, Path file)
                throws IOException {
            MemberMapping previous = mappings.get(key);
            if (previous == null || previous.targetName.equals(key.name)) {
                mappings.put(key, value);
                return;
            }
            if (value.targetName.equals(key.name)
                    || (previous.targetOwner.equals(value.targetOwner)
                    && previous.targetName.equals(value.targetName))) {
                return;
            }
            throw new IOException("Conflicting " + kind.toLowerCase() + " mapping in "
                    + file + ": " + key.owner + "." + key.name + key.descriptor);
        }
    }

    private static final class ClassContext {
        final String source;
        final String target;

        ClassContext(String source, String target) {
            this.source = source;
            this.target = target;
        }
    }

    private static final class MemberMapping {
        final String targetOwner;
        final String targetName;

        MemberMapping(String targetOwner, String targetName) {
            this.targetOwner = targetOwner;
            this.targetName = targetName;
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
            if (this == other) return true;
            if (!(other instanceof MemberKey)) return false;
            MemberKey that = (MemberKey) other;
            return owner.equals(that.owner) && name.equals(that.name)
                    && descriptor.equals(that.descriptor);
        }

        @Override
        public int hashCode() {
            int result = owner.hashCode();
            result = 31 * result + name.hashCode();
            return 31 * result + descriptor.hashCode();
        }
    }
}
