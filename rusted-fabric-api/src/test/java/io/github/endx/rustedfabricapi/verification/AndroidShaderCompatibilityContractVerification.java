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
        } finally {
            restore("rustedfabric.platform", previousPlatform);
            restore("rustedfabric.android.rebindShaderTextures", previousOverride);
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
