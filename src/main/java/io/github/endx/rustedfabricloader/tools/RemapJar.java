package io.github.endx.rustedfabricloader.tools;

import net.fabricmc.loader.impl.lib.mappingio.MappingReader;
import net.fabricmc.loader.impl.lib.mappingio.tree.MemoryMappingTree;
import net.fabricmc.loader.impl.lib.tinyremapper.InputTag;
import net.fabricmc.loader.impl.lib.tinyremapper.NonClassCopyMode;
import net.fabricmc.loader.impl.lib.tinyremapper.OutputConsumerPath;
import net.fabricmc.loader.impl.lib.tinyremapper.TinyRemapper;
import net.fabricmc.loader.impl.lib.tinyremapper.TinyUtils;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public final class RemapJar {
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

    private static void remap(Path input, Path output, Path mappings, String fromNamespace, String toNamespace, List<Path> classpath)
            throws IOException, ExecutionException, InterruptedException {
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

        MemoryMappingTree tree = new MemoryMappingTree();
        MappingReader.read(mappings, tree);

        TinyRemapper remapper = TinyRemapper.newRemapper(new ConsoleLogger())
                .withMappings(TinyUtils.createMappingProvider(tree, fromNamespace, toNamespace))
                .renameInvalidLocals(false)
                .rebuildSourceFilenames(true)
                .build();

        try {
            InputTag inputTag = remapper.createInputTag();

            if (!classpath.isEmpty()) {
                remapper.readClassPathAsync(classpath.toArray(new Path[0])).get();
            }
            remapper.readInputsAsync(inputTag, input).get();

            try (OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(output).assumeArchive(true).build()) {
                outputConsumer.addNonClassFiles(input, NonClassCopyMode.FIX_META_INF, remapper);
                remapper.apply(outputConsumer, inputTag);
            }
        } finally {
            remapper.finish();
        }
    }

    private static final class ConsoleLogger implements TrLogger {
        @Override
        public void log(TrLogger.Level level, String message) {
            if (level == TrLogger.Level.ERROR || level == TrLogger.Level.WARN) {
                System.err.println("[" + level + "] " + message);
            } else {
                System.out.println("[" + level + "] " + message);
            }
        }
    }
}
