package io.github.endx.rustedfabricloader.tools;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

/** Remaps an external Mixin Jar and supplies only the Fabric metadata needed to load it locally. */
public final class PrepareExternalMixinMod {
    private static final String FABRIC_MOD_JSON = "fabric.mod.json";

    private PrepareExternalMixinMod() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 7) {
            throw new IllegalArgumentException("Usage: PrepareExternalMixinMod <input.jar> "
                    + "<output.jar> <mappings.tiny> <fromNs> <modId> <name> <version> "
                    + "[classpath.jar...]");
        }
        Path input = Paths.get(args[0]).toAbsolutePath().normalize();
        Path output = Paths.get(args[1]).toAbsolutePath().normalize();
        Path mappings = Paths.get(args[2]).toAbsolutePath().normalize();
        String fromNamespace = args[3];
        String modId = args[4];
        String name = args[5];
        String version = args[6];
        if (!modId.matches("[a-z][a-z0-9_-]{1,63}")) {
            throw new IOException("Invalid Fabric mod id: " + modId);
        }

        List<Path> classpath = new ArrayList<Path>();
        for (int index = 7; index < args.length; index++) {
            Path path = Paths.get(args[index]).toAbsolutePath().normalize();
            if (Files.isRegularFile(path)) classpath.add(path);
        }

        Path parent = output.getParent();
        if (parent != null) Files.createDirectories(parent);
        Path remapped = Files.createTempFile(parent, "external-mixin-remapped-", ".jar");
        try {
            // Installed RFL distributions run the original game namespace. Development named
            // runtimes can still consume this artifact through Fabric's normal remap workflow,
            // while the ordinary player launcher must receive an official-runtime mod directly.
            RemapJar.remap(input, remapped, mappings, fromNamespace, "official", classpath);
            packageMod(remapped, output, modId, name, version);
        } finally {
            Files.deleteIfExists(remapped);
        }
    }

    private static void packageMod(Path input, Path output, String modId, String name,
                                   String version) throws IOException {
        Files.deleteIfExists(output);
        List<JarEntry> entries = new ArrayList<JarEntry>();
        List<String> mixinConfigs = new ArrayList<String>();
        try (JarFile jar = new JarFile(input.toFile(), false)) {
            Enumeration<JarEntry> enumeration = jar.entries();
            while (enumeration.hasMoreElements()) {
                JarEntry entry = enumeration.nextElement();
                if (!entry.isDirectory() && !FABRIC_MOD_JSON.equals(entry.getName())) {
                    entries.add(entry);
                    if (entry.getName().endsWith(".mixins.json")) {
                        mixinConfigs.add(entry.getName());
                    }
                }
            }
            if (mixinConfigs.isEmpty()) {
                throw new IOException("External Jar contains no *.mixins.json configuration: "
                        + input);
            }
            entries.sort(Comparator.comparing(JarEntry::getName));
            mixinConfigs.sort(String::compareTo);

            try (JarOutputStream destination = new JarOutputStream(Files.newOutputStream(output))) {
                Set<String> written = new HashSet<String>();
                byte[] buffer = new byte[8192];
                for (JarEntry entry : entries) {
                    if (!written.add(entry.getName())) continue;
                    JarEntry copy = new JarEntry(entry.getName());
                    copy.setTime(0L);
                    destination.putNextEntry(copy);
                    try (InputStream stream = jar.getInputStream(entry)) {
                        int read;
                        while ((read = stream.read(buffer)) >= 0) {
                            if (read > 0) destination.write(buffer, 0, read);
                        }
                    }
                    destination.closeEntry();
                }

                JarEntry metadataEntry = new JarEntry(FABRIC_MOD_JSON);
                metadataEntry.setTime(0L);
                destination.putNextEntry(metadataEntry);
                destination.write(metadata(modId, name, version, mixinConfigs));
                destination.closeEntry();
            }
        }
    }

    private static byte[] metadata(String modId, String name, String version,
                                   List<String> mixinConfigs) {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", 1);
        root.addProperty("id", modId);
        root.addProperty("version", version);
        root.addProperty("name", name);
        root.addProperty("description", "Locally prepared compatibility bridge; not distributed by Rusted Fabric Loader.");
        root.addProperty("environment", "*");
        JsonArray mixins = new JsonArray();
        for (String config : mixinConfigs) mixins.add(config);
        root.add("mixins", mixins);
        JsonObject custom = new JsonObject();
        custom.addProperty("rusted_fabric_loader:local_external_compatibility", true);
        root.add("custom", custom);
        return new GsonBuilder().setPrettyPrinting().create().toJson(root)
                .getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}
