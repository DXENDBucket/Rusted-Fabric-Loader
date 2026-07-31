package io.github.endx.rustedfabric.android.jvm;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Developer command for validating the complete desktop-game ZIP import path. */
public final class DesktopGameArchiveCli {
    private DesktopGameArchiveCli() {
    }

    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 2) {
            throw new IllegalArgumentException("Expected desktop game ZIP and empty output directory");
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
        DesktopGameArchiveExtractor.Result result = DesktopGameArchiveExtractor.extract(
                archive, destination, (files, bytes, current) -> {
                    if (files % 500 == 0) {
                        System.out.println("Imported " + files + " files / " + bytes
                                + " bytes; " + current);
                    }
                });
        System.out.println("Imported Rusted Warfare desktop game: files=" + result.files()
                + ", bytes=" + result.bytes() + ", root=" + result.archiveRoot()
                + ", warnings=" + result.warnings());
    }
}
