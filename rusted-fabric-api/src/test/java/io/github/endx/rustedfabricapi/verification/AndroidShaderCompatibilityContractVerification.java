package io.github.endx.rustedfabricapi.verification;

import io.github.endx.rustedfabricapi.internal.client.AndroidShaderCompatibility;
import rustedwarfare.client.render.GameImage;
import rustedwarfare.render.ShaderParameter;
import rustedwarfare.render.ShaderProgram;

final class AndroidShaderCompatibilityContractVerification {
    private AndroidShaderCompatibilityContractVerification() {
    }

    static void verify() {
        String previousPlatform = System.getProperty("rustedfabric.platform");
        String previousOverride = System.getProperty(
                "rustedfabric.android.rebindShaderTextures");
        String previousSourceRepair = System.getProperty(
                "rustedfabric.android.repairShaderSources");
        String previousPixelRepair = System.getProperty(
                "rustedfabric.android.rehydrateDiscardedPixels");
        try {
            ShaderParameter texture = new ShaderParameter();
            texture.texture = new GameImage();
            ShaderParameter value = new ShaderParameter();
            ShaderProgram shader = new ShaderProgram();
            shader.parameters = new ShaderParameter[]{texture, value};

            System.setProperty("rustedfabric.platform", "windows");
            require(AndroidShaderCompatibility.restoreTextureParametersOnActivation(shader) == 0
                            && !texture.dirty,
                    "desktop shader parameters were modified by the Android repair");

            System.setProperty("rustedfabric.platform", "android-jvm");
            require(AndroidShaderCompatibility.restoreTextureParametersOnActivation(shader) == 1
                            && texture.dirty && !value.dirty,
                    "Android texture-backed shader parameters were not restored");

            texture.dirty = false;
            System.setProperty("rustedfabric.android.rebindShaderTextures", "false");
            require(AndroidShaderCompatibility.restoreTextureParametersOnActivation(shader) == 0
                            && !texture.dirty,
                    "Android shader repair opt-out was ignored");

            String displacement = "#version 130\n"
                    + "uniform sampler2D u_texture;\n"
                    + "void main(){gl_FragColor=texture2D(u_texture,vec2(0.0));}\n";
            String repaired = AndroidShaderCompatibility.repairShaderSource(
                    "post_displacement", displacement);
            require(!repaired.startsWith("#version") && repaired.contains("texture2D"),
                    "Android displacement shader did not select the GLES2-compatible path");
            require(AndroidShaderCompatibility.repairShaderSource("plain", displacement)
                            .startsWith("uniform sampler2D"),
                    "built-in Android shader source was not normalized");
            require(AndroidShaderCompatibility.repairShaderSource(
                            "third_party_shader", displacement).equals(displacement),
                    "third-party Android shader source was modified");
            String hueShift = displacement.replace("#version 130", "#version 150");
            require(AndroidShaderCompatibility.repairShaderSource(
                            "hueShiftTeamColor", hueShift).startsWith("uniform sampler2D"),
                    "GLSL 1.50 team shader did not select the GLES2-compatible path");
            require(AndroidShaderCompatibility.shouldRehydrateDiscardedPixels(),
                    "Android discarded-pixel repair was not enabled by default");

            System.setProperty("rustedfabric.platform", "windows");
            require(AndroidShaderCompatibility.repairShaderSource(
                            "post_displacement", displacement).equals(displacement)
                            && !AndroidShaderCompatibility.shouldRehydrateDiscardedPixels(),
                    "Android compatibility changed the desktop image or shader path");
        } finally {
            restore("rustedfabric.platform", previousPlatform);
            restore("rustedfabric.android.rebindShaderTextures", previousOverride);
            restore("rustedfabric.android.repairShaderSources", previousSourceRepair);
            restore("rustedfabric.android.rehydrateDiscardedPixels", previousPixelRepair);
        }
    }

    private static void restore(String name, String value) {
        if (value == null) System.clearProperty(name);
        else System.setProperty(name, value);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
