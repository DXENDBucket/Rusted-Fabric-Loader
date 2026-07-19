package io.github.endx.rustedfabric.android.jvm;

import java.nio.file.Paths;

/** Code-free developer probe for a local user-owned desktop installation. */
public final class DesktopGameProbe {
    private DesktopGameProbe() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Expected one desktop game directory");
        }
        DesktopGameInspection inspection = DesktopGameLayout.inspect(Paths.get(args[0]));
        System.out.println("Importable: " + inspection.isImportable());
        System.out.println("Classpath entries: " + (inspection.isImportable()
                ? DesktopGameLayout.desktopClasspath(inspection.root()).size() : 0));
        for (String warning : inspection.warnings()) System.out.println("Warning: " + warning);
        for (String error : inspection.errors()) System.out.println("Error: " + error);
        if (!inspection.isImportable()) System.exit(2);
    }
}
