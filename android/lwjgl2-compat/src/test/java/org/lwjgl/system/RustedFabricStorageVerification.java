package org.lwjgl.system;

import java.nio.file.Files;
import java.nio.file.Path;

public final class RustedFabricStorageVerification {
    private RustedFabricStorageVerification() {
    }

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("rusted-fabric-shared-content-")
                .toAbsolutePath().normalize();
        System.setProperty("rusted.android.contentRoot", root.toString());
        try {
            requirePath("mods/units/pack/unit.ini", root.resolve("units/pack/unit.ini"));
            requirePath("/SD/mods/maps/test.tmx", root.resolve("maps/test.tmx"));
            requirePath("/SD/rusted_warfare_maps/test.tmx", root.resolve("maps/test.tmx"));
            requirePath("/SD/rustedWarfare/maps/test.tmx", root.resolve("maps/test.tmx"));
            requirePath("mods\\units\\pack\\unit.ini", root.resolve("units/pack/unit.ini"));
            if (!"assets/gui/menu.rml".equals(
                    RustedFabricStorage.remap("assets/gui/menu.rml"))) {
                throw new AssertionError("Unmanaged game path was redirected");
            }
            boolean traversalRejected = false;
            try {
                RustedFabricStorage.remap("mods/units/../maps/test.tmx");
            } catch (IllegalArgumentException expected) {
                traversalRejected = true;
            }
            if (!traversalRejected) throw new AssertionError("Traversal was not rejected");
            System.out.println("Android shared-content path bridge contracts passed");
        } finally {
            System.clearProperty("rusted.android.contentRoot");
            Files.delete(root);
        }
    }

    private static void requirePath(String input, Path expected) {
        Path actual = java.nio.file.Paths.get(RustedFabricStorage.remap(input))
                .toAbsolutePath().normalize();
        if (!actual.equals(expected.toAbsolutePath().normalize())) {
            throw new AssertionError(input + " mapped to " + actual + " instead of " + expected);
        }
    }
}
