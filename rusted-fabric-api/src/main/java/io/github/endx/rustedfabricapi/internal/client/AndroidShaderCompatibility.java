package io.github.endx.rustedfabricapi.internal.client;

import rustedwarfare.render.ShaderParameter;
import rustedwarfare.render.ShaderProgram;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/** Android GL4ES state repairs kept out of the public rendering API. */
public final class AndroidShaderCompatibility {
    private static final String REBIND_PROPERTY =
            "rustedfabric.android.rebindShaderTextures";
    private static final AtomicBoolean REPAIR_REPORTED = new AtomicBoolean();

    private AndroidShaderCompatibility() {
    }

    /**
     * GL4ES' fixed-pipeline emulator may reuse texture unit 1 between custom shader draws.
     * Rusted Warfare normally uploads an auxiliary sampler only when its Java image object
     * changes, which is sufficient on desktop OpenGL but can leave a stale sampler binding on
     * some Android drivers. Mark texture-backed parameters dirty whenever a custom shader is
     * activated so the native backend restores both the sampler and texture-size uniforms.
     */
    public static int restoreTextureParametersOnActivation(ShaderProgram shader) {
        if (!isRepairEnabled() || shader == null || shader.parameters == null) return 0;
        int restored = 0;
        for (ShaderParameter parameter : shader.parameters) {
            if (parameter != null && parameter.texture != null) {
                parameter.dirty = true;
                restored++;
            }
        }
        if (restored > 0 && REPAIR_REPORTED.compareAndSet(false, true)) {
            System.out.println("[Rusted Fabric API] Android GL4ES auxiliary shader "
                    + "texture rebinding is active");
        }
        return restored;
    }

    public static boolean isRepairEnabled() {
        String platform = System.getProperty("rustedfabric.platform", "")
                .toLowerCase(Locale.ROOT);
        return platform.contains("android")
                && !"false".equalsIgnoreCase(System.getProperty(REBIND_PROPERTY, "true"));
    }
}
