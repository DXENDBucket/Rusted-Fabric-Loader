package io.github.endx.rustedfabric.android.jvm;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Developer command for validating the complete ZIP/TAR.XZ import path on a workstation. */
public final class JvmRuntimeArchiveCli {
    private JvmRuntimeArchiveCli() {
    }

    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 2) {
            throw new IllegalArgumentException("Expected runtime archive and empty output directory");
        }
        Path archive = Paths.get(arguments[0]).toAbsolutePath().normalize();
        Path destination = Paths.get(arguments[1]).toAbsolutePath().normalize();
        if (Files.exists(destination)) {
            try (java.util.stream.Stream<Path> entries = Files.list(destination)) {
                if (entries.findAny().isPresent()) {
                    throw new IllegalArgumentException("Output directory must be empty: "
                            + destination);
                }
            }
        } else {
            Files.createDirectories(destination);
        }
        JvmRuntimeArchiveExtractor.Result result = JvmRuntimeArchiveExtractor.extract(
                archive, destination, (files, bytes, current) -> {
                    if (files % 500 == 0) {
                        System.out.println("Imported " + files + " files / " + bytes
                                + " bytes; " + current);
                    }
                });
        System.out.println("Imported Linux AArch64 Java 17 runtime: files=" + result.files()
                + ", bytes=" + result.bytes() + ", root=" + result.archiveRoot()
                + ", SHA-256=" + result.archiveSha256());
    }
}
