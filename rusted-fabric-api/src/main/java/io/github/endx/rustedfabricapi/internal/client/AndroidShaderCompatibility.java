package io.github.endx.rustedfabricapi.internal.client;

import rustedwarfare.render.ShaderParameter;
import rustedwarfare.render.ShaderProgram;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/** Android GL4ES state repairs kept out of the public rendering API. */
public final class AndroidShaderCompatibility {
    private static final String REBIND_PROPERTY =
            "rustedfabric.android.rebindShaderTextures";
    private static final String SOURCE_REPAIR_PROPERTY =
            "rustedfabric.android.repairShaderSources";
    private static final String PIXEL_REHYDRATE_PROPERTY =
            "rustedfabric.android.rehydrateDiscardedPixels";
    private static final AtomicBoolean REPAIR_REPORTED = new AtomicBoolean();
    private static final AtomicBoolean SOURCE_REPAIR_REPORTED = new AtomicBoolean();
    private static final AtomicBoolean PIXEL_REPAIR_REPORTED = new AtomicBoolean();
    private static final Set<String> LEGACY_BUILTIN_SHADERS = Set.of(
            "error", "hueaddteamcolor", "hueshiftteamcolor", "plain", "post_base",
            "post_displacement", "puregreenteamcolor");

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
        return isAndroidJvm()
                && !"false".equalsIgnoreCase(System.getProperty(REBIND_PROPERTY, "true"));
    }

    /**
     * GL4ES converts the game's GLSL 1.30/1.50 shaders to GLES 3 and backports their legacy
     * sampling through a preprocessor macro. A number of mobile compilers reject the resulting
     * texture2D overload as a Vulkan-only type. Every built-in shader in this list actually uses
     * only legacy constructs. Removing their desktop version line makes GL4ES select and emit its
     * stable GLSL ES 1.00 header. Custom Java-mod shaders retain their declared GLSL version.
     */
    public static String repairShaderSource(String shaderName, String source) {
        if (!isAndroidJvm() || source == null
                || "false".equalsIgnoreCase(System.getProperty(SOURCE_REPAIR_PROPERTY, "true"))
                || shaderName == null
                || !LEGACY_BUILTIN_SHADERS.contains(shaderName.toLowerCase(Locale.ROOT))
                || !(source.startsWith("#version 130")
                || source.startsWith("#version 150"))) {
            return source;
        }
        int versionLineEnd = source.indexOf('\n');
        String repaired = versionLineEnd < 0 ? "" : source.substring(versionLineEnd + 1);
        if (SOURCE_REPAIR_REPORTED.compareAndSet(false, true)) {
            System.out.println("[Rusted Fabric API] Android GL4ES built-in shader "
                    + "compatibility is active");
        }
        return repaired;
    }

    /** Generated shadows and cloned textures can be team-coloured after their CPU pixels were
     * discarded. Android keeps a readable GL texture, so restore pixels on demand instead of
     * retaining every image buffer for the lifetime of the game. */
    public static boolean shouldRehydrateDiscardedPixels() {
        return isAndroidJvm()
                && !"false".equalsIgnoreCase(System.getProperty(
                        PIXEL_REHYDRATE_PROPERTY, "true"));
    }

    public static void reportDiscardedPixelRehydration() {
        if (PIXEL_REPAIR_REPORTED.compareAndSet(false, true)) {
            System.out.println("[Rusted Fabric API] Android discarded image pixel "
                    + "rehydration is active");
        }
    }

    private static boolean isAndroidJvm() {
        String platform = System.getProperty("rustedfabric.platform", "")
                .toLowerCase(Locale.ROOT);
        return platform.contains("android");
    }
}
