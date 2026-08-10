package io.github.endx.rustedfabricloader;

import java.util.Locale;
import java.util.function.Consumer;

/** Resolves the renderer once, after client mods initialize but before the game main class runs. */
public final class RendererBackendSelection {
    public static final String REQUEST_PROPERTY = "rusted.fabric.renderer";
    public static final String RESOLVED_PROPERTY = "rusted.fabric.renderer.resolved";
    public static final String REQUIRED_PROPERTY = "rusted.fabric.renderer.required";
    public static final String VULKAN_PROVIDER_PROPERTY =
            "rusted.fabric.renderer.provider.vulkan";

    public enum Backend {
        OPENGL("opengl"),
        VULKAN("vulkan");

        private final String id;

        Backend(String id) { this.id = id; }
        public String id() { return id; }
    }

    private RendererBackendSelection() { }

    static Backend resolveAndPublish(Consumer<String> logger) {
        String existing = System.getProperty(RESOLVED_PROPERTY);
        if (existing != null && !existing.trim().isEmpty()) {
            return parseResolved(existing);
        }

        String request = normalize(System.getProperty(REQUEST_PROPERTY, "auto"));
        boolean vulkanAvailable = "available".equals(normalize(
                System.getProperty(VULKAN_PROVIDER_PROPERTY, "unavailable")));
        Backend resolved;
        switch (request) {
            case "auto":
                // Installing an experimental provider alone must not silently replace the
                // renderer. Providers opt into auto selection by publishing "preferred".
                resolved = "preferred".equals(normalize(
                        System.getProperty(VULKAN_PROVIDER_PROPERTY, "unavailable")))
                        ? Backend.VULKAN : Backend.OPENGL;
                break;
            case "opengl":
            case "slick":
            case "legacy":
                resolved = Backend.OPENGL;
                break;
            case "vulkan":
            case "rustedvk":
            case "rusted_vk":
                if (vulkanAvailable) {
                    resolved = Backend.VULKAN;
                } else if (Boolean.getBoolean(REQUIRED_PROPERTY)) {
                    throw new IllegalStateException(
                            "Vulkan renderer was required but no available provider registered");
                } else {
                    resolved = Backend.OPENGL;
                    logger.accept("Requested Vulkan renderer is unavailable; falling back to OpenGL");
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown renderer backend: " + request);
        }
        System.setProperty(RESOLVED_PROPERTY, resolved.id());
        logger.accept("Renderer selected before game startup: " + resolved.id()
                + " (requested=" + request + ")");
        return resolved;
    }

    private static Backend parseResolved(String value) {
        String normalized = normalize(value);
        if ("vulkan".equals(normalized)) return Backend.VULKAN;
        if ("opengl".equals(normalized)) return Backend.OPENGL;
        throw new IllegalStateException("Invalid resolved renderer backend: " + value);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
