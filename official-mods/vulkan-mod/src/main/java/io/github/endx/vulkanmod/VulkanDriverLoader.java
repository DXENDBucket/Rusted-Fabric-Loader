package io.github.endx.vulkanmod;

import io.github.endx.vulkanmod.spi.VulkanPlatformDriver;
import io.github.endx.vulkanmod.spi.VulkanProbeResult;
import io.github.endx.vulkanmod.spi.VulkanFrameCommands;
import io.github.endx.vulkanmod.spi.VulkanFrameSubmission;
import io.github.endx.vulkanmod.spi.VulkanSurfaceInfo;
import io.github.endx.vulkanmod.spi.VulkanSurfaceRequest;
import io.github.endx.vulkanmod.spi.VulkanTextureData;
import io.github.endx.vulkanmod.spi.VulkanWindowRequest;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** Extracts and child-first loads LWJGL 3 so it cannot replace the game's LWJGL 2 classes. */
final class VulkanDriverLoader {
    private static final String ROOT = "META-INF/vulkan-driver/";
    private static final String LWJGL_LIBRARY_PATH = "org.lwjgl.librarypath";

    private VulkanDriverLoader() { }

    static LoadedDriver loadDesktop() {
        Path cache = FabricLoader.getInstance().getGameDir().resolve(".rusted-fabric")
                .resolve("cache").resolve("vulkan-driver");
        return loadDesktop(cache);
    }

    static LoadedDriver loadDesktop(Path cacheRoot) {
        Properties properties = loadProperties();
        String driverClass = required(properties, "driverClass");
        String version = required(properties, "driverVersion");
        String[] names = required(properties, "files").split(",");
        Path cache = cacheRoot.resolve(version);
        List<URL> urls = new ArrayList<URL>();
        try {
            Files.createDirectories(cache);
            for (String rawName : names) {
                String name = rawName.trim();
                if (name.isEmpty()) continue;
                Path file = extract(cache, name);
                urls.add(file.toUri().toURL());
            }
            IsolatedDriverClassLoader loader = new IsolatedDriverClassLoader(
                    urls.toArray(new URL[0]), VulkanDriverLoader.class.getClassLoader());
            Class<?> type = Class.forName(driverClass, true, loader);
            Object instance = type.getDeclaredConstructor().newInstance();
            if (!(instance instanceof VulkanPlatformDriver)) {
                loader.close();
                throw new IllegalStateException("Driver does not implement VulkanPlatformDriver: "
                        + driverClass);
            }
            return new LoadedDriver((VulkanPlatformDriver) instance, loader);
        } catch (ReflectiveOperationException | IOException failure) {
            throw new IllegalStateException("Could not load isolated Vulkan driver", failure);
        }
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream input = resource("driver-bundle.properties")) {
            properties.load(input);
            return properties;
        } catch (IOException failure) {
            throw new IllegalStateException("Could not read Vulkan driver manifest", failure);
        }
    }

    private static Path extract(Path cache, String name) throws IOException {
        Path target = cache.resolve(name);
        byte[] bundled;
        try (InputStream input = resource(name)) {
            bundled = input.readAllBytes();
        }
        if (Files.isRegularFile(target)
                && MessageDigest.isEqual(digest(bundled), digest(Files.readAllBytes(target)))) {
            return target;
        }
        Path temporary = Files.createTempFile(cache, name, ".tmp");
        try {
            Files.write(temporary, bundled);
            try {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
        return target;
    }

    private static byte[] digest(byte[] value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private static InputStream resource(String name) {
        InputStream input = VulkanDriverLoader.class.getClassLoader()
                .getResourceAsStream(ROOT + name);
        if (input == null) throw new IllegalStateException(
                "Bundled Vulkan driver resource is missing: " + name);
        return input;
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("Vulkan driver manifest is missing " + key);
        }
        return value.trim();
    }

    static final class LoadedDriver implements AutoCloseable {
        private final VulkanPlatformDriver driver;
        private final IsolatedDriverClassLoader loader;

        private LoadedDriver(VulkanPlatformDriver driver, IsolatedDriverClassLoader loader) {
            this.driver = driver;
            this.loader = loader;
        }

        String name() { return driver.name(); }

        VulkanProbeResult probe() {
            return invoke(driver::probe);
        }

        VulkanSurfaceInfo createSurface(VulkanSurfaceRequest request) {
            return invoke(() -> driver.createSurface(request));
        }

        VulkanSurfaceInfo createNativeWindowSurface(VulkanWindowRequest request) {
            return invoke(() -> driver.createNativeWindowSurface(request));
        }

        long uploadTexture(VulkanTextureData texture) {
            return invoke(() -> driver.uploadTexture(texture));
        }

        long compileFragmentShader(
                io.github.endx.vulkanmod.spi.VulkanCustomFragmentShader shader) {
            return invoke(() -> driver.compileFragmentShader(shader));
        }

        void destroyFragmentShader(long shaderHandle) {
            invoke(() -> {
                driver.destroyFragmentShader(shaderHandle);
                return null;
            });
        }

        long compileShaderProgram(
                io.github.endx.vulkanmod.spi.VulkanCustomShaderProgram program) {
            return invoke(() -> driver.compileShaderProgram(program));
        }

        void destroyShaderProgram(long shaderHandle) {
            invoke(() -> {
                driver.destroyShaderProgram(shaderHandle);
                return null;
            });
        }

        boolean supportsFrameStream() {
            return invoke(driver::supportsFrameStream);
        }

        boolean customShaderUsesExpandedVertexInput(long shaderHandle) {
            return invoke(() -> driver.customShaderUsesExpandedVertexInput(shaderHandle));
        }

        long createRenderTarget(int width, int height) {
            return invoke(() -> driver.createRenderTarget(width, height));
        }

        void renderToTexture(long textureHandle, VulkanFrameCommands frame) {
            invoke(() -> {
                driver.renderToTexture(textureHandle, frame);
                return null;
            });
        }

        void updateTexture(long textureHandle, VulkanTextureData texture) {
            invoke(() -> {
                driver.updateTexture(textureHandle, texture);
                return null;
            });
        }

        VulkanTextureData readTexture(long textureHandle) {
            return invoke(() -> driver.readTexture(textureHandle));
        }

        void destroyTexture(long textureHandle) {
            invoke(() -> {
                driver.destroyTexture(textureHandle);
                return null;
            });
        }

        boolean setSurfaceVisible(boolean visible) {
            return invoke(() -> driver.setSurfaceVisible(visible));
        }

        boolean prepareSurfaceWindow(int width, int height, boolean visible) {
            return invoke(() -> driver.prepareSurfaceWindow(width, height, visible));
        }

        void maintainSurfaceWindow() {
            invoke(() -> {
                driver.maintainSurfaceWindow();
                return null;
            });
        }

        boolean isSurfaceCloseRequested() {
            return invoke(driver::isSurfaceCloseRequested);
        }

        java.util.List<io.github.endx.vulkanmod.spi.VulkanInputEvent> pollInputEvents() {
            return invoke(driver::pollInputEvents);
        }

        void setSystemCursorVisible(boolean visible) {
            invoke(() -> {
                driver.setSystemCursorVisible(visible);
                return null;
            });
        }

        VulkanSurfaceInfo presentFrame(VulkanFrameCommands frame) {
            return invoke(() -> driver.presentFrame(frame));
        }

        VulkanSurfaceInfo presentFrame(VulkanFrameSubmission submission) {
            return invoke(() -> driver.presentFrame(submission));
        }

        VulkanSurfaceInfo presentFrameStream(java.nio.ByteBuffer frameStream) {
            return invoke(() -> driver.presentFrameStream(frameStream));
        }

        VulkanSurfaceInfo presentFrameAndReveal(VulkanFrameCommands frame) {
            return invoke(() -> driver.presentFrameAndReveal(frame));
        }

        private <T> T invoke(java.util.function.Supplier<T> operation) {
            Thread thread = Thread.currentThread();
            ClassLoader previous = thread.getContextClassLoader();
            String previousLibraryPath = System.getProperty(LWJGL_LIBRARY_PATH);
            try {
                thread.setContextClassLoader(loader);
                return operation.get();
            } finally {
                thread.setContextClassLoader(previous);
                restoreProperty(LWJGL_LIBRARY_PATH, previousLibraryPath);
            }
        }

        @Override public void close() {
            RuntimeException driverFailure = null;
            try {
                invoke(() -> {
                    driver.close();
                    return null;
                });
            } catch (RuntimeException failure) {
                driverFailure = failure;
            }
            try {
                loader.close();
            } catch (IOException failure) {
                if (driverFailure != null) driverFailure.addSuppressed(failure);
                else throw new IllegalStateException("Could not close Vulkan driver", failure);
            }
            if (driverFailure != null) {
                throw driverFailure;
            }
        }
    }

    private static void restoreProperty(String name, String previousValue) {
        if (previousValue != null) System.setProperty(name, previousValue);
        else System.clearProperty(name);
    }

    private static final class IsolatedDriverClassLoader extends URLClassLoader {
        private IsolatedDriverClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        @Override protected Class<?> loadClass(String name, boolean resolve)
                throws ClassNotFoundException {
            if (name.startsWith("org.lwjgl.")
                    || name.startsWith("io.github.endx.vulkanmod.lwjgl3.")) {
                synchronized (getClassLoadingLock(name)) {
                    Class<?> loaded = findLoadedClass(name);
                    if (loaded == null) {
                        try {
                            loaded = findClass(name);
                        } catch (ClassNotFoundException ignored) {
                            loaded = super.loadClass(name, false);
                        }
                    }
                    if (resolve) resolveClass(loaded);
                    return loaded;
                }
            }
            return super.loadClass(name, resolve);
        }
    }
}
